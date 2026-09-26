package net.voidflame.core.api;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ServiceRegistry {
    /** Public API marker; services are registered through Bukkit ServicesManager. */
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    public <T> void register(Class<T> type, T service) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(service, "service");
        services.put(type, service);
    }

    public <T> void unregister(Class<T> type) {
        services.remove(type);
    }

    public <T> T get(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return type.cast(services.get(type));
    }

    public <T> T require(Class<T> type) {
        T value = get(type);
        if (value == null) {
            throw new IllegalStateException(
                    "Required VoidFlame service is not registered: " + type.getName()
            );
        }
        return value;
    }

    public boolean contains(Class<?> type) {
        return services.containsKey(type);
    }

    public void clear() {
        services.clear();
    }
}
