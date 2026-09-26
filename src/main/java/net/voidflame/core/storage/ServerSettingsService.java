package net.voidflame.core.storage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Central per-server settings backed by Core StorageService. */
public final class ServerSettingsService {
    private final StorageService storage;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public ServerSettingsService(StorageService storage) {
        this.storage = storage;
    }

    public CompletableFuture<String> get(String key, String defaultValue) {
        String cached = cache.get(key);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return storage.get("server-settings", key).thenApply(value -> {
            String resolved = value == null ? defaultValue : value;
            cache.put(key, resolved);
            return resolved;
        });
    }

    public String getCached(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    public CompletableFuture<Void> set(String key, String value) {
        cache.put(key, value);
        return storage.put("server-settings", key, value);
    }

    public void invalidate(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }
}
