package net.voidflame.core.storage;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Tracks survival profile basics in Core's shared database.
 * Session playtime is checkpointed once per minute and when players disconnect.
 */
public final class SmpPlayerStatsListener implements Listener {
    private final JavaPlugin plugin;
    private final DatabaseService database;
    private final Map<UUID, Long> sessionStarts = new ConcurrentHashMap<>();

    public SmpPlayerStatsListener(JavaPlugin plugin, DatabaseService database) {
        this.plugin = plugin;
        this.database = database;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        sessionStarts.put(player.getUniqueId(), now);
        database.execute("""
                INSERT INTO player_profiles_smp(uuid,last_known_name,first_join,last_join,kills,deaths,playtime_seconds)
                VALUES(?,?,?,?,0,0,0)
                ON CONFLICT(uuid) DO UPDATE SET
                  last_known_name=excluded.last_known_name,
                  last_join=excluded.last_join
                """, player.getUniqueId().toString(), player.getName(), now, now)
            .whenComplete((ignored, error) -> logFailure("initialize SMP profile for " + player.getName(), error));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        flushSession(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        UUID victimId = event.getEntity().getUniqueId();
        database.execute("UPDATE player_profiles_smp SET deaths=deaths+1 WHERE uuid=?", victimId.toString())
            .whenComplete((ignored, error) -> logFailure("record SMP death for " + victimId, error));

        Player killer = event.getEntity().getKiller();
        if (killer != null && !killer.getUniqueId().equals(victimId)) {
            UUID killerId = killer.getUniqueId();
            database.execute("UPDATE player_profiles_smp SET kills=kills+1 WHERE uuid=?", killerId.toString())
                .whenComplete((ignored, error) -> logFailure("record SMP kill for " + killerId, error));
        }
    }

    public CompletableFuture<Void> flushOnlineSessions() {
        long now = System.currentTimeMillis();
        List<CompletableFuture<Void>> writes = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : sessionStarts.entrySet()) {
            UUID id = entry.getKey();
            long started = entry.getValue();
            long elapsedSeconds = Math.max(0L, (now - started) / 1000L);
            if (elapsedSeconds == 0L || !sessionStarts.replace(id, started, now)) continue;
            writes.add(database.execute(
                    "UPDATE player_profiles_smp SET playtime_seconds=playtime_seconds+?,last_join=? WHERE uuid=?",
                    elapsedSeconds, now, id.toString())
                .whenComplete((ignored, error) -> logFailure("checkpoint SMP playtime for " + id, error)));
        }
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private void flushSession(UUID id, String name) {
        Long started = sessionStarts.remove(id);
        if (started == null) return;
        long now = System.currentTimeMillis();
        long elapsedSeconds = Math.max(0L, (now - started) / 1000L);
        if (elapsedSeconds == 0L) return;
        database.execute(
                "UPDATE player_profiles_smp SET playtime_seconds=playtime_seconds+?,last_known_name=?,last_join=? WHERE uuid=?",
                elapsedSeconds, name, now, id.toString())
            .whenComplete((ignored, error) -> logFailure("save SMP session for " + id, error));
    }

    private void logFailure(String action, Throwable error) {
        if (error != null) plugin.getLogger().log(Level.SEVERE, "Could not " + action + ".", error);
    }
}
