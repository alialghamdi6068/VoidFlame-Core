package net.voidflame.core.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Stable public service contract shared by VoidFlame modules. */
public interface StorageService {
    DatabaseService database();
    CompletableFuture<Void> upsertPlayer(UUID uuid, String name);
    CompletableFuture<Void> put(String module, String key, String value);
    CompletableFuture<String> get(String module, String key);
    CompletableFuture<List<Map<String, Object>>> query(String sql, Object... parameters);
}