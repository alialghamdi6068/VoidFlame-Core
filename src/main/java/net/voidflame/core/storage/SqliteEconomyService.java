package net.voidflame.core.storage;

import net.voidflame.core.api.EconomyService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Single-process serialized account mutations. All custom modules must route money
 * changes through this service so balance checks and ledger writes stay consistent.
 */
public final class SqliteEconomyService implements EconomyService {
    private final DatabaseService database;
    private final ReentrantLock mutationLock = new ReentrantLock(true);

    public SqliteEconomyService(DatabaseService database) {
        this.database = database;
    }

    @Override
    public CompletableFuture<Long> balance(UUID playerId) {
        if (playerId == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Player UUID is required."));
        return CompletableFuture.supplyAsync(() -> {
            mutationLock.lock();
            try {
                ensureAccount(playerId);
                return readBalance(playerId);
            } finally {
                mutationLock.unlock();
            }
        });
    }

    @Override
    public CompletableFuture<Long> shardBalance(UUID playerId) {
        if (playerId == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Player UUID is required."));
        return CompletableFuture.supplyAsync(() -> {
            mutationLock.lock();
            try { ensureAccount(playerId); return readShardBalance(playerId); }
            finally { mutationLock.unlock(); }
        });
    }

    @Override
    public CompletableFuture<Long> depositShards(UUID playerId, long amount, String reason) {
        validateAmount(playerId, amount);
        return CompletableFuture.supplyAsync(() -> {
            mutationLock.lock();
            try {
                ensureAccount(playerId);
                long before = readShardBalance(playerId);
                long after = Math.addExact(before, amount);
                long now = System.currentTimeMillis();
                database.transaction(List.of(
                    new DatabaseService.Statement("UPDATE economy_accounts SET shards=?, updated_at=? WHERE uuid=?", after, now, playerId.toString()),
                    new DatabaseService.Statement("INSERT INTO economy_transactions(transaction_id,actor_uuid,target_uuid,transaction_type,amount,balance_after,details,created_at) VALUES (?,?,?,'SHARD_DEPOSIT',?,?,?,?)",
                        UUID.randomUUID().toString(), playerId.toString(), playerId.toString(), amount, after, safeReason(reason), now)
                )).join();
                return after;
            } finally { mutationLock.unlock(); }
        });
    }

    @Override
    public CompletableFuture<Boolean> withdrawShards(UUID playerId, long amount, String reason) {
        validateAmount(playerId, amount);
        return CompletableFuture.supplyAsync(() -> {
            mutationLock.lock();
            try {
                ensureAccount(playerId);
                long before = readShardBalance(playerId);
                if (before < amount) return false;
                long after = before - amount;
                long now = System.currentTimeMillis();
                database.transaction(List.of(
                    new DatabaseService.Statement("UPDATE economy_accounts SET shards=?, updated_at=? WHERE uuid=? AND shards>=?", after, now, playerId.toString(), amount),
                    new DatabaseService.Statement("INSERT INTO economy_transactions(transaction_id,actor_uuid,target_uuid,transaction_type,amount,balance_after,details,created_at) VALUES (?,?,?,'SHARD_WITHDRAW',?,?,?,?)",
                        UUID.randomUUID().toString(), playerId.toString(), playerId.toString(), amount, after, safeReason(reason), now)
                )).join();
                return true;
            } finally { mutationLock.unlock(); }
        });
    }

    private long readShardBalance(UUID playerId) {
        List<Map<String, Object>> rows = database.query("SELECT shards FROM economy_accounts WHERE uuid=?", playerId.toString()).join();
        if (rows.isEmpty()) throw new IllegalStateException("Economy account was not created.");
        return ((Number) rows.getFirst().get("shards")).longValue();
    }

    @Override
    public CompletableFuture<Long> deposit(UUID playerId, long amount, String reason) {
        validateAmount(playerId, amount);
        return CompletableFuture.supplyAsync(() -> {
            mutationLock.lock();
            try {
                ensureAccount(playerId);
                long before = readBalance(playerId);
                long after = Math.addExact(before, amount);
                String tx = UUID.randomUUID().toString();
                database.transaction(List.of(
                    new DatabaseService.Statement(
                        "UPDATE economy_accounts SET balance=?, updated_at=? WHERE uuid=?",
                        after, System.currentTimeMillis(), playerId.toString()
                    ),
                    new DatabaseService.Statement(
                        "INSERT INTO economy_transactions(transaction_id, actor_uuid, target_uuid, transaction_type, amount, balance_after, details, created_at) VALUES (?, ?, ?, 'DEPOSIT', ?, ?, ?, ?)",
                        tx, playerId.toString(), playerId.toString(), amount, after, safeReason(reason), System.currentTimeMillis()
                    )
                )).join();
                return after;
            } finally {
                mutationLock.unlock();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> withdraw(UUID playerId, long amount, String reason) {
        validateAmount(playerId, amount);
        return CompletableFuture.supplyAsync(() -> {
            mutationLock.lock();
            try {
                ensureAccount(playerId);
                long before = readBalance(playerId);
                if (before < amount) return false;
                long after = before - amount;
                database.transaction(List.of(
                    new DatabaseService.Statement(
                        "UPDATE economy_accounts SET balance=?, updated_at=? WHERE uuid=? AND balance>=?",
                        after, System.currentTimeMillis(), playerId.toString(), amount
                    ),
                    new DatabaseService.Statement(
                        "INSERT INTO economy_transactions(transaction_id, actor_uuid, target_uuid, transaction_type, amount, balance_after, details, created_at) VALUES (?, ?, ?, 'WITHDRAW', ?, ?, ?, ?)",
                        UUID.randomUUID().toString(), playerId.toString(), playerId.toString(), amount, after, safeReason(reason), System.currentTimeMillis()
                    )
                )).join();
                return true;
            } finally {
                mutationLock.unlock();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> transfer(UUID senderId, UUID targetId, long amount) {
        if (senderId == null || targetId == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Both player UUIDs are required."));
        if (senderId.equals(targetId)) return CompletableFuture.completedFuture(false);
        validateAmount(senderId, amount);
        return CompletableFuture.supplyAsync(() -> {
            mutationLock.lock();
            try {
                ensureAccount(senderId);
                ensureAccount(targetId);
                long senderBefore = readBalance(senderId);
                if (senderBefore < amount) return false;
                long targetBefore = readBalance(targetId);
                long senderAfter = senderBefore - amount;
                long targetAfter = Math.addExact(targetBefore, amount);
                long now = System.currentTimeMillis();
                String tx = UUID.randomUUID().toString();
                database.transaction(List.of(
                    new DatabaseService.Statement(
                        "UPDATE economy_accounts SET balance=?, updated_at=? WHERE uuid=? AND balance>=?",
                        senderAfter, now, senderId.toString(), amount
                    ),
                    new DatabaseService.Statement(
                        "UPDATE economy_accounts SET balance=?, updated_at=? WHERE uuid=?",
                        targetAfter, now, targetId.toString()
                    ),
                    new DatabaseService.Statement(
                        "INSERT INTO economy_transactions(transaction_id, actor_uuid, target_uuid, transaction_type, amount, balance_after, details, created_at) VALUES (?, ?, ?, 'PAY', ?, ?, '', ?)",
                        tx, senderId.toString(), targetId.toString(), amount, senderAfter, now
                    )
                )).join();
                return true;
            } finally {
                mutationLock.unlock();
            }
        });
    }

    private void ensureAccount(UUID playerId) {
        long now = System.currentTimeMillis();
        database.execute(
            "INSERT INTO economy_accounts(uuid, balance, shards, created_at, updated_at) VALUES (?, 0, 0, ?, ?) ON CONFLICT(uuid) DO NOTHING",
            playerId.toString(), now, now
        ).join();
    }

    private long readBalance(UUID playerId) {
        List<Map<String, Object>> rows = database.query(
            "SELECT balance FROM economy_accounts WHERE uuid=?", playerId.toString()
        ).join();
        if (rows.isEmpty()) throw new IllegalStateException("Economy account was not created.");
        return ((Number) rows.getFirst().get("balance")).longValue();
    }

    private static void validateAmount(UUID playerId, long amount) {
        if (playerId == null) throw new IllegalArgumentException("Player UUID is required.");
        if (amount <= 0) throw new IllegalArgumentException("Amount must be greater than zero.");
    }

    private static String safeReason(String reason) {
        if (reason == null || reason.isBlank()) return "unspecified";
        return reason.length() > 200 ? reason.substring(0, 200) : reason;
    }
}
