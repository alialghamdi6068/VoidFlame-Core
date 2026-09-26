package net.voidflame.core.api;

import java.util.UUID;

public interface MatchResultService {
    void record(MatchResult result);

    record MatchResult(
            UUID matchId,
            UUID playerA,
            UUID playerB,
            UUID winner,
            UUID loser,
            String kit,
            String mode,
            String arena,
            long durationMs
    ) {}
}
