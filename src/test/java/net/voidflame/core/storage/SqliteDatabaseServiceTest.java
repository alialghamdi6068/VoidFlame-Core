package net.voidflame.core.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SqliteDatabaseServiceTest {
    @Test
    void createsSingleDatabaseAndPersistsData(@TempDir Path temp) {
        SqliteDatabaseService db = new SqliteDatabaseService(temp);
        try {
            StorageService storage = new SqliteStorageService(db);
            storage.put("test", "key", "value").join();
            assertTrue(db.databasePath().endsWith("database.db"));
            assertEquals("value", storage.get("test", "key").join());
        } finally {
            db.close();
        }
    }

    @Test
    void rowCountMismatchRollsBackTheEntireTransaction(@TempDir Path temp) {
        SqliteDatabaseService db = new SqliteDatabaseService(temp);
        try {
            db.execute("CREATE TABLE IF NOT EXISTS transaction_test(id INTEGER PRIMARY KEY, balance INTEGER NOT NULL)").join();
            db.execute("CREATE TABLE IF NOT EXISTS transaction_ledger(id INTEGER PRIMARY KEY, amount INTEGER NOT NULL)").join();
            db.execute("INSERT INTO transaction_test(id,balance) VALUES (1,10)").join();

            List<DatabaseService.Statement> statements = List.of(
                    new DatabaseService.Statement("UPDATE transaction_test SET balance=balance-? WHERE id=?", 4, 1),
                    new DatabaseService.Statement("INSERT INTO transaction_ledger(amount) SELECT ? WHERE 0=1", 4)
            );

            assertThrows(java.util.concurrent.CompletionException.class,
                    () -> db.transactionExpectingCounts(statements, List.of(1, 1)).join());
            assertEquals(10, ((Number) db.query("SELECT balance FROM transaction_test WHERE id=1")
                    .join().getFirst().get("balance")).intValue());
            assertEquals(0, ((Number) db.query("SELECT COUNT(*) AS c FROM transaction_ledger")
                    .join().getFirst().get("c")).intValue());
        } finally {
            db.close();
        }
    }

    @Test
    void backupAndRestoreRecoverCommittedData(@TempDir Path temp) {
        SqliteDatabaseService db = new SqliteDatabaseService(temp);
        try {
            db.execute("CREATE TABLE IF NOT EXISTS recovery_test(id INTEGER PRIMARY KEY, value TEXT NOT NULL)").join();
            db.execute("INSERT INTO recovery_test(value) VALUES (?)", "before").join();

            Path backup = db.backup(temp.resolve("backups")).join();

            db.execute("UPDATE recovery_test SET value='after' WHERE id=1").join();
            assertEquals("after", db.query("SELECT value FROM recovery_test WHERE id=1").join().getFirst().get("value"));

            db.restore(backup).join();

            List<Map<String,Object>> rows = db.query("SELECT value FROM recovery_test WHERE id=1").join();
            assertEquals("before", rows.getFirst().get("value"));
        } finally {
            db.close();
        }
    }
}
