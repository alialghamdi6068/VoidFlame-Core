package net.voidflame.core.storage;

import java.util.UUID;

public record PlayerProfile(
        UUID uuid, String name, long firstJoin, long lastJoin,
        long coins, long wins, long losses, long winstreak, long bestWinstreak,
        double elo, String rank, String preferencesJson, String statisticsJson
) {
    public static PlayerProfile defaults(UUID uuid, String name, long now) {
        return new PlayerProfile(uuid, name, now, now, 0, 0, 0, 0, 0, 1000.0,
                "member", "{}", "{}");
    }
}
