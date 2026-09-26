package net.voidflame.core.storage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent per-player settings backed exclusively by VoidFlame-Core StorageService.
 * Consumers may use the in-memory cache for hot-path decisions.
 */
public final class PlayerSettingsService {
    private final StorageService storage;
    private final Map<UUID, Map<String, Boolean>> cache = new ConcurrentHashMap<>();

    public PlayerSettingsService(StorageService storage) {
        this.storage = storage;
    }

    public boolean getCached(UUID uuid, String key, boolean defaultValue) {
        Map<String, Boolean> values = cache.get(uuid);
        return values == null ? defaultValue : values.getOrDefault(key, defaultValue);
    }

    public CompletableFuture<Boolean> get(UUID uuid, String key, boolean defaultValue) {
        Boolean cached = cache.getOrDefault(uuid, Map.of()).get(key);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return storage.get("settings:" + uuid, key).thenApply(raw -> {
            boolean value = raw == null ? defaultValue : Boolean.parseBoolean(raw);
            cache.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>()).put(key, value);
            return value;
        });
    }

    public CompletableFuture<Void> set(UUID uuid, String key, boolean value) {
        cache.computeIfAbsent(uuid, ignored -> new ConcurrentHashMap<>()).put(key, value);
        return storage.put("settings:" + uuid, key, Boolean.toString(value));
    }

    public void invalidate(UUID uuid) {
        cache.remove(uuid);
    }

    public void clear() {
        cache.clear();
    }
}
