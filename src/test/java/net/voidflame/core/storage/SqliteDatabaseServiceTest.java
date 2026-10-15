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
