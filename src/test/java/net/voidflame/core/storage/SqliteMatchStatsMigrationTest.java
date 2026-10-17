package net.voidflame.core.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqliteMatchStatsMigrationTest {
    @Test
    void recordsWinsLossesStreakAndEloExactlyOnce(@TempDir Path temp) {
        SqliteDatabaseService db = new SqliteDatabaseService(temp);
        try {
            long now = System.currentTimeMillis();
            db.execute(
                    "INSERT INTO player_profiles(uuid, name, first_join, last_join, elo) VALUES (?, ?, ?, ?, ?)",
                    "winner", "winner", now, now, 1000.0
            ).join();
            db.execute(
                    "INSERT INTO player_profiles(uuid, name, first_join, last_join, elo) VALUES (?, ?, ?, ?, ?)",
                    "loser", "loser", now, now, 1000.0
            ).join();

            String insert = "INSERT OR IGNORE INTO duel_matches " +
                    "(match_id, player_a, player_b, kit, arena, mode, winner, loser, duration_ms, result, elo_change_a, elo_change_b, timestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            db.execute(insert, "match-1", "winner", "loser", "SWORD", "arena-1", "DUEL",
                    "winner", "loser", 30_000L, "COMPLETED", 16.0, -16.0, now).join();

            List<Map<String, Object>> first = db.query(
                    "SELECT uuid, wins, losses, winstreak, best_winstreak, elo FROM player_profiles ORDER BY uuid"
            ).join();
            assertEquals(0, ((Number) first.get(0).get("wins")).intValue());
            assertEquals(1, ((Number) first.get(0).get("losses")).intValue());
            assertEquals(0, ((Number) first.get(0).get("winstreak")).intValue());
            assertEquals(984.0, ((Number) first.get(0).get("elo")).doubleValue(), 0.001);
            assertEquals(1, ((Number) first.get(1).get("wins")).intValue());
            assertEquals(1, ((Number) first.get(1).get("winstreak")).intValue());
            assertEquals(1, ((Number) first.get(1).get("best_winstreak")).intValue());
            assertEquals(1016.0, ((Number) first.get(1).get("elo")).doubleValue(), 0.001);

            db.execute(insert, "match-1", "winner", "loser", "SWORD", "arena-1", "DUEL",
                    "winner", "loser", 30_000L, "COMPLETED", 16.0, -16.0, now).join();

            List<Map<String, Object>> afterDuplicate = db.query(
                    "SELECT uuid, wins, losses, winstreak, best_winstreak, elo FROM player_profiles ORDER BY uuid"
            ).join();
            assertEquals(1, ((Number) afterDuplicate.get(0).get("losses")).intValue());
            assertEquals(984.0, ((Number) afterDuplicate.get(0).get("elo")).doubleValue(), 0.001);
            assertEquals(1, ((Number) afterDuplicate.get(1).get("wins")).intValue());
            assertEquals(1016.0, ((Number) afterDuplicate.get(1).get("elo")).doubleValue(), 0.001);
        } finally {
            db.close();
        }
    }
}
