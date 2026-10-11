package net.voidflame.core.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Shared economy API for VoidFlame SMP modules.
 * Feature plugins must use this API instead of writing account balances directly.
 */
public interface EconomyService {
    CompletableFuture<Long> balance(UUID playerId);
    CompletableFuture<Long> shardBalance(UUID playerId);
    CompletableFuture<Long> depositShards(UUID playerId, long amount, String reason);
    CompletableFuture<Boolean> withdrawShards(UUID playerId, long amount, String reason);
    CompletableFuture<Long> deposit(UUID playerId, long amount, String reason);
    CompletableFuture<Long> depositOnce(UUID playerId, long amount, String reason, String idempotencyKey);
    CompletableFuture<Boolean> withdraw(UUID playerId, long amount, String reason);
    CompletableFuture<Boolean> transfer(UUID senderId, UUID targetId, long amount);
}
