package net.voidflame.core.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.time.ZoneId;

public record CoreConfig(
        boolean metrics,
        boolean debug,
        String locale,
        ZoneId timezone
) {
    public CoreConfig(FileConfiguration config) {
        this(
                config.getBoolean("settings.metrics", false),
                config.getBoolean("settings.debug", false),
                config.getString("settings.locale", "en_US"),
                parseZone(config.getString("settings.timezone", "UTC"))
        );
    }

    private static ZoneId parseZone(String value) {
        try {
            return ZoneId.of(value);
        } catch (Exception ignored) {
            return ZoneId.of("UTC");
        }
    }
}
