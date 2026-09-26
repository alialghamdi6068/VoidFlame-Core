package net.voidflame.core.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class WorldService {
    private final JavaPlugin plugin;
    private final Set<String> enabled = ConcurrentHashMap.newKeySet();

    public WorldService(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfiguredWorlds();
    }

    public void loadConfiguredWorlds() {
        enabled.clear();
        for (String name : plugin.getConfig().getStringList("worlds.enabled")) {
            if (name != null && !name.isBlank()) enabled.add(name);
        }
    }

    public Set<String> enabledWorlds() {
        return Collections.unmodifiableSet(new TreeSet<>(enabled));
    }

    public boolean isEnabled(String name) {
        return name != null && enabled.contains(name);
    }

    public World get(String name) {
        return name == null ? null : Bukkit.getWorld(name);
    }

    public World load(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("World name is required.");
        World existing = Bukkit.getWorld(name);
        if (existing != null) {
            enabled.add(existing.getName());
            persist();
            return existing;
        }
        World world = new WorldCreator(name).createWorld();
        if (world == null) throw new IllegalStateException("Failed to load world: " + name);
        enabled.add(world.getName());
        persist();
        return world;
    }

    public boolean unload(String name, boolean save) {
        World world = Bukkit.getWorld(name);
        if (world == null) {
            enabled.remove(name);
            persist();
            return false;
        }
        World fallback = Bukkit.getWorlds().stream()
                .filter(w -> !w.getName().equalsIgnoreCase(world.getName()))
                .findFirst().orElse(null);
        if (fallback != null) {
            for (Player player : new ArrayList<>(world.getPlayers())) {
                player.teleportAsync(fallback.getSpawnLocation());
            }
        }
        boolean result = Bukkit.unloadWorld(world, save);
        if (result) {
            enabled.remove(name);
            persist();
        }
        return result;
    }

    public void enable(String name) {
        load(name);
    }

    public void disable(String name) {
        if (Bukkit.getWorlds().size() <= 1 && Bukkit.getWorld(name) != null)
            throw new IllegalStateException("The server must keep at least one loaded world.");
        if (!unload(name, true)) throw new IllegalArgumentException("World is not loaded: " + name);
    }

    public void persist() {
        plugin.getConfig().set("worlds.enabled", new ArrayList<>(new TreeSet<>(enabled)));
        plugin.saveConfig();
    }

    public void teleport(Player player, String name) {
        World world = get(name);
        if (world == null) throw new IllegalArgumentException("World is not loaded: " + name);
        player.teleportAsync(world.getSpawnLocation());
    }
}
