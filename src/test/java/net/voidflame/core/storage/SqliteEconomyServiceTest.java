package net.voidflame.core.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SqliteEconomyServiceTest {
    @TempDir Path tempDir;

    @Test
    void depositTransferAndWithdrawPreserveBalances() {
        try (SqliteDatabaseService database = new SqliteDatabaseService(tempDir)) {
            SqliteEconomyService economy = new SqliteEconomyService(database);
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();

            assertEquals(0L, economy.balance(alice).join());
            assertEquals(100L, economy.deposit(alice, 100, "test-deposit").join());
            assertTrue(economy.transfer(alice, bob, 35).join());
            assertEquals(65L, economy.balance(alice).join());
            assertEquals(35L, economy.balance(bob).join());
            assertFalse(economy.withdraw(alice, 500, "insufficient").join());
            assertEquals(65L, economy.balance(alice).join());

            var ledger = database.query("SELECT COUNT(*) AS count FROM economy_transactions").join();
            assertEquals(2L, ((Number) ledger.getFirst().get("count")).longValue());
        }
    }

    @Test
    void idempotentDepositCannotCreditTwice() {
        try (SqliteDatabaseService database = new SqliteDatabaseService(tempDir)) {
            SqliteEconomyService economy = new SqliteEconomyService(database);
            UUID player = UUID.randomUUID();
            assertEquals(50L, economy.depositOnce(player, 50, "refund", "refund:test-order").join());
            assertEquals(50L, economy.depositOnce(player, 50, "refund", "refund:test-order").join());
            assertEquals(50L, economy.balance(player).join());
            var ledger = database.query("SELECT COUNT(*) AS count FROM economy_transactions WHERE transaction_id='refund:test-order'").join();
            assertEquals(1L, ((Number) ledger.getFirst().get("count")).longValue());
        }
    }

    @Test
    void rejectsNonPositiveAmounts() {
        try (SqliteDatabaseService database = new SqliteDatabaseService(tempDir)) {
            SqliteEconomyService economy = new SqliteEconomyService(database);
            assertThrows(IllegalArgumentException.class, () -> economy.deposit(UUID.randomUUID(), 0, "invalid"));
            assertThrows(IllegalArgumentException.class, () -> economy.withdraw(UUID.randomUUID(), -1, "invalid"));
        }
    }
}
