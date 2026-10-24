package net.voidflame.core.storage;

import net.voidflame.core.api.PartyMatchResultService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/** Persists party/team/FFA results idempotently in the shared Core database. */
public final class SqlitePartyMatchResultService implements PartyMatchResultService {
    private final DatabaseService database;
    private final PlayerProfileService profiles;
    private final Logger logger;

    public SqlitePartyMatchResultService(DatabaseService database, PlayerProfileService profiles, Logger logger) {
        this.database = Objects.requireNonNull(database, "database");
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void record(PartyMatchResult result) {
        recordAsync(result);
    }

    @Override
    public CompletableFuture<Void> recordAsync(PartyMatchResult result) {
        if (!valid(result)) {
            logger.warning("Ignored invalid party match result.");
            return CompletableFuture.completedFuture(null);
        }

        long now = System.currentTimeMillis();
        List<DatabaseService.Statement> statements = new ArrayList<>();
        for (UUID player : result.players()) {
            String id = player.toString();
            statements.add(new DatabaseService.Statement(
                    "INSERT INTO player_profiles(uuid,name,first_join,last_join) VALUES (?,?,?,?) "
                            + "ON CONFLICT(uuid) DO UPDATE SET last_join=excluded.last_join",
                    id, id, now, now));
        }
        statements.add(new DatabaseService.Statement(
                "INSERT OR IGNORE INTO party_matches(match_id,kit,mode,arena,duration_ms,timestamp) "
                        + "VALUES (?,?,?,?,?,?)",
                result.matchId().toString(),
                safe(result.kit(), "unknown", 64),
                safe(result.mode(), "PARTY", 32),
                safe(result.arena(), "unknown", 128),
                Math.max(0L, result.durationMs()),
                now));

        Set<UUID> winners = Set.copyOf(result.winners());
        for (UUID player : result.players()) {
            statements.add(new DatabaseService.Statement(
                    "INSERT OR IGNORE INTO party_match_players(match_id,uuid,result) VALUES (?,?,?)",
                    result.matchId().toString(), player.toString(),
                    winners.contains(player) ? "WIN" : "LOSS"));
        }

        return database.transaction(statements)
                .thenRun(() -> result.players().forEach(profiles::invalidate))
                .whenComplete((ignored, error) -> {
                    if (error != null) {
                        logger.severe("Failed to persist party match result " + result.matchId() + ": "
                                + error.getMessage());
                    }
                });
    }

    private static boolean valid(PartyMatchResult result) {
        if (result == null || result.matchId() == null || result.players() == null || result.winners() == null
                || result.players().size() < 2 || result.winners().isEmpty()
                || result.winners().size() >= result.players().size()) return false;
        Set<UUID> players = new HashSet<>(result.players());
        Set<UUID> winners = new HashSet<>(result.winners());
        return players.size() == result.players().size()
                && winners.size() == result.winners().size()
                && !players.contains(null)
                && !winners.contains(null)
                && players.containsAll(winners);
    }

    private static String safe(String value, String fallback, int maxLength) {
        if (value == null || value.isBlank()) return fallback;
        String trimmed = value.trim();
        return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
    }
}
