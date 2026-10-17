package net.voidflame.core.storage;

import net.voidflame.core.api.MatchResultService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;

public final class SqliteMatchResultService implements MatchResultService {
    private static final double K_FACTOR = 32.0;
    private final DatabaseService database;
    private final Logger logger;

    public SqliteMatchResultService(DatabaseService database, Logger logger) {
        this.database = Objects.requireNonNull(database, "database");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void record(MatchResult result) {
        if (result == null || result.matchId() == null || result.playerA() == null
                || result.playerB() == null || result.winner() == null || result.loser() == null
                || result.playerA().equals(result.playerB())
                || !result.winner().equals(result.playerA()) && !result.winner().equals(result.playerB())
                || !result.loser().equals(result.playerA()) && !result.loser().equals(result.playerB())
                || result.winner().equals(result.loser())) {
            logger.warning("Ignored invalid duel match result.");
            return;
        }

        database.query(
                "SELECT uuid, elo FROM player_profiles WHERE uuid IN (?, ?)",
                result.playerA().toString(), result.playerB().toString()
        ).thenCompose(rows -> {
            double eloA = elo(rows, result.playerA(), 1000.0);
            double eloB = elo(rows, result.playerB(), 1000.0);
            double expectedA = 1.0 / (1.0 + Math.pow(10.0, (eloB - eloA) / 400.0));
            double actualA = result.winner().equals(result.playerA()) ? 1.0 : 0.0;
            double deltaA = Math.round(K_FACTOR * (actualA - expectedA) * 100.0) / 100.0;
            double deltaB = -deltaA;
            long now = System.currentTimeMillis();

            List<DatabaseService.Statement> statements = new ArrayList<>();
            statements.add(new DatabaseService.Statement(
                    "INSERT INTO player_profiles(uuid, name, first_join, last_join) VALUES (?, ?, ?, ?) " +
                            "ON CONFLICT(uuid) DO UPDATE SET last_join=excluded.last_join",
                    result.playerA().toString(), result.playerA().toString(), now, now));
            statements.add(new DatabaseService.Statement(
                    "INSERT INTO player_profiles(uuid, name, first_join, last_join) VALUES (?, ?, ?, ?) " +
                            "ON CONFLICT(uuid) DO UPDATE SET last_join=excluded.last_join",
                    result.playerB().toString(), result.playerB().toString(), now, now));
            statements.add(new DatabaseService.Statement(
                    "INSERT OR IGNORE INTO duel_matches " +
                            "(match_id, player_a, player_b, kit, arena, mode, winner, loser, duration_ms, result, elo_change_a, elo_change_b, timestamp) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'COMPLETED', ?, ?, ?)",
                    result.matchId().toString(), result.playerA().toString(), result.playerB().toString(),
                    result.kit() == null ? "unknown" : result.kit(),
                    result.arena() == null ? "" : result.arena(),
                    result.mode() == null ? "DUEL" : result.mode(),
                    result.winner().toString(), result.loser().toString(),
                    Math.max(0L, result.durationMs()), deltaA, deltaB, now
            ));
            return database.transaction(statements);
        }).exceptionally(error -> {
            logger.severe("Failed to persist duel result " + result.matchId() + ": " + error.getMessage());
            return null;
        });
    }

    private static double elo(List<Map<String, Object>> rows, UUID player, double fallback) {
        String id = player.toString();
        for (Map<String, Object> row : rows) {
            if (!id.equals(String.valueOf(row.get("uuid")))) continue;
            Object value = row.get("elo");
            if (value instanceof Number number && Double.isFinite(number.doubleValue())) {
                return number.doubleValue();
            }
        }
        return fallback;
    }
}
