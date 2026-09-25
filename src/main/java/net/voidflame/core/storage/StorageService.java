package net.voidflame.core.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StorageService {
    private final DatabaseService database;

    public StorageService(DatabaseService database) {
        this.database = database;
    }

    public DatabaseService database() {
        return database;
    }

    public CompletableFuture<Void> upsertPlayer(UUID uuid, String name) {
        return database.execute("""
                INSERT INTO player_data(uuid, name, first_seen, last_seen)
                VALUES (?, ?, datetime('now'), datetime('now'))
                ON CONFLICT(uuid) DO UPDATE SET name=excluded.name, last_seen=datetime('now')
                """, uuid.toString(), name);
    }

    public CompletableFuture<Void> put(String module, String key, String value) {
        return database.execute("""
                INSERT INTO module_data(module, data_key, data_value, updated_at)
                VALUES (?, ?, ?, datetime('now'))
                ON CONFLICT(module, data_key)
                DO UPDATE SET data_value=excluded.data_value, updated_at=datetime('now')
                """, module, key, value);
    }

    public CompletableFuture<String> get(String module, String key) {
        return database.query(
                "SELECT data_value FROM module_data WHERE module=? AND data_key=?",
                module, key
        ).thenApply(rows -> rows.isEmpty() ? null : String.valueOf(rows.getFirst().get("data_value")));
    }

    public CompletableFuture<List<Map<String, Object>>> query(String sql, Object... parameters) {
        return database.query(sql, parameters);
    }
}
