package net.voidflame.core.storage;

import net.voidflame.core.api.PartyMatchResultService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlitePartyMatchResultServiceTest {
    @Test
    void partyResultsUpdateEachPlayersStatsExactlyOnce(@TempDir Path temp) {
        SqliteDatabaseService db = new SqliteDatabaseService(temp);
        try {
            PlayerProfileService profiles = new PlayerProfileService(db);
            SqlitePartyMatchResultService service =
                    new SqlitePartyMatchResultService(db, profiles, Logger.getLogger("party-result-test"));
            UUID winnerA = UUID.randomUUID();
            UUID winnerB = UUID.randomUUID();
            UUID loserA = UUID.randomUUID();
            UUID loserB = UUID.randomUUID();
            UUID matchId = UUID.randomUUID();
            PartyMatchResultService.PartyMatchResult result = new PartyMatchResultService.PartyMatchResult(
                    matchId, List.of(winnerA, winnerB, loserA, loserB), List.of(winnerA, winnerB),
                    "SWORD", "TWO_V_TWO", "arena-1", 45_000L);

            service.recordAsync(result).join();
            List<Map<String, Object>> first = db.query(
                    "SELECT uuid,wins,losses,winstreak,best_winstreak,elo FROM player_profiles WHERE uuid IN (?,?,?,?)",
                    winnerA.toString(), winnerB.toString(), loserA.toString(), loserB.toString()).join();
            assertEquals(4, first.size());
            for (Map<String, Object> row : first) {
                String id = String.valueOf(row.get("uuid"));
                if (id.equals(winnerA.toString()) || id.equals(winnerB.toString())) {
                    assertEquals(1, ((Number) row.get("wins")).intValue());
                    assertEquals(1, ((Number) row.get("winstreak")).intValue());
                } else {
                    assertEquals(1, ((Number) row.get("losses")).intValue());
                    assertEquals(0, ((Number) row.get("winstreak")).intValue());
                }
                assertEquals(1000.0, ((Number) row.get("elo")).doubleValue(), 0.001);
            }

            service.recordAsync(result).join();
            List<Map<String, Object>> duplicate = db.query(
                    "SELECT uuid,wins,losses,elo FROM player_profiles WHERE uuid IN (?,?,?,?)",
                    winnerA.toString(), winnerB.toString(), loserA.toString(), loserB.toString()).join();
            for (Map<String, Object> row : duplicate) {
                String id = String.valueOf(row.get("uuid"));
                assertEquals(1, ((Number) row.get(id.equals(winnerA.toString()) || id.equals(winnerB.toString())
                        ? "wins" : "losses")).intValue());
                assertEquals(1000.0, ((Number) row.get("elo")).doubleValue(), 0.001);
            }
        } finally {
            db.close();
        }
    }
}
