package net.voidflame.core.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MatchResultService {
    void record(MatchResult result);

    /**
     * Persist a result and complete only after the persistence operation finishes.
     * Existing providers remain source-compatible; production providers should override this method.
     */
    default CompletableFuture<Void> recordAsync(MatchResult result) {
        record(result);
        return CompletableFuture.completedFuture(null);
    }

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
