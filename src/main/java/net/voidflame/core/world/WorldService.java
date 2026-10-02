package net.voidflame.core.world;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
            if (name != null && !name.isBlank()) enabled.add(name.trim());
        }
    }

    public Set<String> enabledWorlds() {
        return Collections.unmodifiableSet(new TreeSet<>(enabled));
    }

    public boolean isEnabled(String name) {
        return name != null && enabled.stream().anyMatch(w -> w.equalsIgnoreCase(name));
    }

    public World get(String name) {
        if (name == null) return null;
        return Bukkit.getWorlds().stream()
                .filter(world -> world.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public World load(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("World name is required.");
        }

        World existing = get(name);
        if (existing != null) {
            enabled.add(existing.getName());
            persist();
            return existing;
        }

        if (multiverseInstalled()) {
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv load " + name)) {
                throw new IllegalStateException("Multiverse-Core failed to load world: " + name);
            }

            World loaded = get(name);
            if (loaded == null) {
                throw new IllegalStateException("Multiverse-Core reported success but world is not loaded: " + name);
            }

            enabled.add(loaded.getName());
            persist();
            return loaded;
        }

        World world = new WorldCreator(name).createWorld();
        if (world == null) {
            throw new IllegalStateException("Failed to load world: " + name);
        }

        enabled.add(world.getName());
        persist();
        return world;
    }

    public boolean unload(String name, boolean save) {
        World world = get(name);
        if (world == null) {
            enabled.removeIf(w -> w.equalsIgnoreCase(name));
            persist();
            return false;
        }

        if (Bukkit.getWorlds().size() <= 1) {
            throw new IllegalStateException("The server must keep at least one loaded world.");
        }

        if (!world.getPlayers().isEmpty()) {
            throw new IllegalStateException("World cannot be unloaded while players are inside it.");
        }

        boolean result;
        if (multiverseInstalled()) {
            result = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mv unload " + name);
            if (result && get(name) != null) {
                throw new IllegalStateException("Multiverse-Core reported success but world is still loaded: " + name);
            }
        } else {
            result = Bukkit.unloadWorld(world, save);
        }

        if (result) {
            enabled.removeIf(w -> w.equalsIgnoreCase(name));
            persist();
        }
        return result;
    }

    public void enable(String name) {
        load(name);
    }

    public void disable(String name) {
        if (!unload(name, true)) {
            throw new IllegalArgumentException("World is not loaded: " + name);
        }
    }

    public void reloadEnabledWorlds() {
        loadConfiguredWorlds();
        for (String name : enabledWorlds()) {
            load(name);
        }
    }

    public void setSpawn(Player player) {
        World world = player.getWorld();
        world.setSpawnLocation(player.getLocation());
        plugin.getConfig().set("worlds.spawns." + world.getName() + ".x", player.getLocation().getX());
        plugin.getConfig().set("worlds.spawns." + world.getName() + ".y", player.getLocation().getY());
        plugin.getConfig().set("worlds.spawns." + world.getName() + ".z", player.getLocation().getZ());
        plugin.getConfig().set("worlds.spawns." + world.getName() + ".yaw", player.getLocation().getYaw());
        plugin.getConfig().set("worlds.spawns." + world.getName() + ".pitch", player.getLocation().getPitch());
        plugin.saveConfig();
    }

    public void teleportToSpawn(Player player, String worldName) {
        World world = get(worldName);
        if (world == null) {
            throw new IllegalArgumentException("World is not loaded: " + worldName);
        }
        player.teleportAsync(world.getSpawnLocation());
    }

    public void teleport(Player player, String worldName) {
        teleportToSpawn(player, worldName);
    }

    public List<World> loadedWorlds() {
        return List.copyOf(Bukkit.getWorlds());
    }

    public Map<String, String> worldSummary() {
        Map<String, String> result = new LinkedHashMap<>();
        for (World world : Bukkit.getWorlds()) {
            result.put(world.getName(), world.getEnvironment().name());
        }
        return result;
    }

    public void persist() {
        plugin.getConfig().set("worlds.enabled", new ArrayList<>(new TreeSet<>(enabled)));
        plugin.saveConfig();
    }

    private boolean multiverseInstalled() {
        return Bukkit.getPluginManager().getPlugin("Multiverse-Core") != null;
    }
}
