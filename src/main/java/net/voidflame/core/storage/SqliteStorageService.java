package net.voidflame.core.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Default SQLite-backed implementation of the public StorageService API. */
public final class SqliteStorageService implements StorageService {
    private final DatabaseService database;
    public SqliteStorageService(DatabaseService database) { this.database = database; }
    @Override public DatabaseService database() { return database; }
    @Override public CompletableFuture<Void> upsertPlayer(UUID uuid, String name) {
        return database.execute("""
                INSERT INTO player_data(uuid, name, first_seen, last_seen)
                VALUES (?, ?, datetime('now'), datetime('now'))
                ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, last_seen=datetime('now')
                """, uuid.toString(), name);
    }
    @Override public CompletableFuture<Void> put(String module, String key, String value) {
        return database.execute("""
                INSERT INTO module_data(module, data_key, data_value, updated_at)
                VALUES (?, ?, ?, datetime('now'))
                ON CONFLICT(module, data_key)
                DO UPDATE SET data_value=excluded.data_value, updated_at=datetime('now')
                """, module, key, value);
    }
    @Override public CompletableFuture<String> get(String module, String key) {
        return database.query("SELECT data_value FROM module_data WHERE module=? AND data_key=?", module, key)
                .thenApply(rows -> rows.isEmpty() ? null : String.valueOf(rows.getFirst().get("data_value")));
    }
    @Override public CompletableFuture<List<Map<String, Object>>> query(String sql, Object... parameters) {
        return database.query(sql, parameters);
    }
}