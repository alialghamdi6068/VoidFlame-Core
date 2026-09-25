package net.voidflame.core.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqliteDatabaseServiceTest {
    @Test
    void createsSingleDatabaseAndPersistsData(@TempDir Path temp) {
        SqliteDatabaseService db = new SqliteDatabaseService(temp);
        assertTrue(db.databasePath().endsWith("database.db"));
        db.put("test", "key", "value");
        assertEquals("value", new StorageService(db).get("test", "key").join());
        db.close();
    }

    private static final class StorageService extends net.voidflame.core.storage.StorageService {
        StorageService(DatabaseService database) { super(database); }
    }
}
