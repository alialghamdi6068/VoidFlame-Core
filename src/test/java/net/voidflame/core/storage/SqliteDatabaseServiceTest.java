package net.voidflame.core.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SqliteDatabaseServiceTest {
    @Test
    void createsSingleDatabaseAndPersistsData(@TempDir Path temp) {
        SqliteDatabaseService db = new SqliteDatabaseService(temp);
        StorageService storage = new SqliteStorageService(db);
        storage.put("test", "key", "value").join();
        assertTrue(db.databasePath().endsWith("database.db"));
        assertEquals("value", storage.get("test", "key").join());
        db.close();
    }
}
