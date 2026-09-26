package net.voidflame.core.api;

import org.bukkit.Location;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Typed cross-plugin contract for arena allocation. Implementations own the
 * concrete arena lifecycle and reset state; consumers never need reflection.
 */
public interface ArenaService {
    Optional<ArenaHandle> acquireHandle();
    CompletableFuture<Boolean> reset(String arenaName);
    long availableCount();
    List<String> allNames();

    record ArenaHandle(String name, Location spawnA, Location spawnB) {
        public ArenaHandle {
            spawnA = spawnA == null ? null : spawnA.clone();
            spawnB = spawnB == null ? null : spawnB.clone();
        }
        @Override public Location spawnA() { return spawnA == null ? null : spawnA.clone(); }
        @Override public Location spawnB() { return spawnB == null ? null : spawnB.clone(); }
    }
}
