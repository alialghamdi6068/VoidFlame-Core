package net.voidflame.core.scheduler;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CoreScheduler {
    private final JavaPlugin plugin;
    private final Set<BukkitTask> tasks = ConcurrentHashMap.newKeySet();

    public CoreScheduler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public BukkitTask runSync(Runnable action) {
        BukkitTask task = plugin.getServer().getScheduler().runTask(plugin, action);
        tasks.add(task);
        return task;
    }

    public BukkitTask runAsync(Runnable action) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskAsynchronously(plugin, action);
        tasks.add(task);
        return task;
    }

    public BukkitTask runLater(Runnable action, long delayTicks) {
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, action, delayTicks);
        tasks.add(task);
        return task;
    }

    public void cancelAll() {
        tasks.forEach(task -> {
            if (!task.isCancelled()) {
                task.cancel();
            }
        });
        tasks.clear();
    }
}
