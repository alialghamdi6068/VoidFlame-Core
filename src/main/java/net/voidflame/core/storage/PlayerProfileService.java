package net.voidflame.core.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerProfileService {
    private final DatabaseService database;

    public PlayerProfileService(DatabaseService database) {
        this.database = database;
    }

    public CompletableFuture<PlayerProfile> load(UUID uuid, String name) {
        return database.query("""
                SELECT uuid,name,first_join,last_join,coins,wins,losses,winstreak,best_winstreak,elo,rank,preferences_json,statistics_json
                FROM player_profiles WHERE uuid=?
                """, uuid.toString()).thenCompose(rows -> {
            if (!rows.isEmpty()) return CompletableFuture.completedFuture(map(rows.getFirst()));
            PlayerProfile profile = PlayerProfile.defaults(uuid, name, System.currentTimeMillis());
            return save(profile).thenApply(ignored -> profile);
        });
    }

    public CompletableFuture<Void> save(PlayerProfile profile) {
        return database.execute("""
                INSERT INTO player_profiles(uuid,name,first_join,last_join,coins,wins,losses,winstreak,best_winstreak,elo,rank,preferences_json,statistics_json)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(uuid) DO UPDATE SET
                  name=excluded.name,last_join=excluded.last_join,coins=excluded.coins,
                  wins=excluded.wins,losses=excluded.losses,winstreak=excluded.winstreak,
                  best_winstreak=excluded.best_winstreak,elo=excluded.elo,rank=excluded.rank,
                  preferences_json=excluded.preferences_json,statistics_json=excluded.statistics_json
                """,
                profile.uuid().toString(), profile.name(), profile.firstJoin(), profile.lastJoin(),
                profile.coins(), profile.wins(), profile.losses(), profile.winstreak(), profile.bestWinstreak(),
                profile.elo(), profile.rank(), profile.preferencesJson(), profile.statisticsJson());
    }

    public CompletableFuture<Void> touch(UUID uuid, String name) {
        long now = System.currentTimeMillis();
        return database.execute("""
                INSERT INTO player_profiles(uuid,name,first_join,last_join,coins,wins,losses,winstreak,best_winstreak,elo,rank,preferences_json,statistics_json)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(uuid) DO UPDATE SET name=excluded.name,last_join=excluded.last_join
                """, uuid.toString(), name, now, now, 0, 0, 0, 0, 0, 1000.0, "member", "{}", "{}");
    }

    public CompletableFuture<List<PlayerProfile>> topByElo(int limit) {
        return database.query("""
                SELECT uuid,name,first_join,last_join,coins,wins,losses,winstreak,best_winstreak,elo,rank,preferences_json,statistics_json
                FROM player_profiles ORDER BY elo DESC LIMIT ?
                """, Math.max(1, limit)).thenApply(rows -> rows.stream().map(this::map).toList());
    }

    private PlayerProfile map(Map<String,Object> row) {
        return new PlayerProfile(
                UUID.fromString(String.valueOf(row.get("uuid"))),
                String.valueOf(row.get("name")),
                ((Number) row.get("first_join")).longValue(),
                ((Number) row.get("last_join")).longValue(),
                ((Number) row.get("coins")).longValue(),
                ((Number) row.get("wins")).longValue(),
                ((Number) row.get("losses")).longValue(),
                ((Number) row.get("winstreak")).longValue(),
                ((Number) row.get("best_winstreak")).longValue(),
                ((Number) row.get("elo")).doubleValue(),
                String.valueOf(row.get("rank")),
                String.valueOf(row.get("preferences_json")),
                String.valueOf(row.get("statistics_json"))
        );
    }
}
