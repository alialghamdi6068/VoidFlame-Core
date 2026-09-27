package net.voidflame.core.api;

import java.util.List;
import java.util.UUID;

/** Typed service for team/FFA match results that cannot be represented by 1v1 MatchResult. */
public interface PartyMatchResultService {
    void record(PartyMatchResult result);

    record PartyMatchResult(
            UUID matchId,
            List<UUID> players,
            List<UUID> winners,
            String kit,
            String mode,
            String arena,
            long durationMs
    ) {
        public PartyMatchResult {
            players = players == null ? List.of() : List.copyOf(players);
            winners = winners == null ? List.of() : List.copyOf(winners);
        }
    }
}
