package net.voidflame.core.world;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WorldCommands implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "voidflame.world.manage";
    private static final String SPAWN_PERMISSION = "voidflame.spawn";

    private final WorldService worlds;

    public WorldCommands(WorldService worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String root = command.getName().toLowerCase(Locale.ROOT);
        if (root.equals("spawn")) {
            if (args.length != 0) {
                sender.sendMessage(ChatColor.RED + "Usage: /spawn");
                return true;
            }
            return spawn(sender);
        }
        return admin(sender, args);
    }

    private boolean spawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
            return true;
        }
        if (!player.hasPermission(SPAWN_PERMISSION)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use /spawn.");
            return true;
        }
        List<String> enabled = new ArrayList<>(worlds.enabledWorlds());
        World target = null;
        String configured = worlds.mainSpawnWorld();
        if (configured != null) target = worlds.get(configured);
        if (target == null && !enabled.isEmpty()) target = worlds.get(enabled.get(0));
        if (target == null) target = player.getWorld();
        worlds.teleportToSpawn(player, target.getName());
        player.sendMessage(ChatColor.GREEN + "Teleported to the main spawn.");
        return true;
    }

    private boolean admin(CommandSender sender, String[] args) {
        if (sender instanceof Player player && net.voidflame.core.CoreAdminAccess.isBedrock(player)) {
            sender.sendMessage(ChatColor.RED + "Java staff only.");
            return true;
        }
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendAdminHelp(sender);
            return true;
        }

        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "list" -> listAdmin(sender);
                case "info" -> info(sender, args);
                case "enable", "load" -> enable(sender, args);
                case "disable", "unload" -> disable(sender, args);
                case "tp", "teleport" -> adminTeleport(sender, args);
                case "setspawn" -> setSpawn(sender);
                case "reload" -> reload(sender);
                default -> sendAdminHelp(sender);
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            sender.sendMessage(ChatColor.RED + ex.getMessage());
        }
        return true;
    }

    private void listAdmin(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "VoidFlame World Manager");
        for (World world : worlds.loadedWorlds()) {
            String state = worlds.isEnabled(world.getName()) ? ChatColor.GREEN + "ENABLED" : ChatColor.YELLOW + "LOADED";
            sender.sendMessage(ChatColor.DARK_GRAY + "• " + ChatColor.LIGHT_PURPLE + world.getName()
                    + ChatColor.GRAY + " | " + state + ChatColor.GRAY + " | " + world.getEnvironment());
        }
    }

    private void info(CommandSender sender, String[] args) {
        require(args, 2, "World name is required.");
        World world = worlds.get(args[1]);
        if (world == null) throw new IllegalArgumentException("World is not loaded: " + args[1]);
        sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "World: " + world.getName());
        sender.sendMessage(ChatColor.GRAY + "Environment: " + ChatColor.WHITE + world.getEnvironment());
        sender.sendMessage(ChatColor.GRAY + "Players: " + ChatColor.WHITE + world.getPlayers().size());
        sender.sendMessage(ChatColor.GRAY + "Spawn: " + ChatColor.WHITE
                + String.format("%.1f, %.1f, %.1f", world.getSpawnLocation().getX(),
                world.getSpawnLocation().getY(), world.getSpawnLocation().getZ()));
        sender.sendMessage(ChatColor.GRAY + "Enabled by Core: " + (worlds.isEnabled(world.getName())
                ? ChatColor.GREEN + "yes" : ChatColor.RED + "no"));
    }

    private void enable(CommandSender sender, String[] args) {
        require(args, 2, "World name is required.");
        worlds.enable(args[1]);
        sender.sendMessage(ChatColor.GREEN + "World enabled: " + ChatColor.WHITE + args[1]);
    }

    private void disable(CommandSender sender, String[] args) {
        require(args, 2, "World name is required.");
        worlds.disable(args[1]);
        sender.sendMessage(ChatColor.YELLOW + "World disabled: " + ChatColor.WHITE + args[1]);
    }

    private void adminTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            throw new IllegalArgumentException("This command can only be used by a player.");
        }
        require(args, 2, "World name is required.");
        World world = worlds.get(args[1]);
        if (world == null || !worlds.isEnabled(world.getName())) {
            throw new IllegalArgumentException("That world is not enabled.");
        }
        worlds.teleportToSpawn(player, world.getName());
        sender.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.WHITE + world.getName());
    }

    private void setSpawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            throw new IllegalArgumentException("This command can only be used by a player.");
        }
        worlds.setSpawn(player);
        sender.sendMessage(ChatColor.GREEN + "World spawn updated.");
    }

    private void reload(CommandSender sender) {
        worlds.reloadEnabledWorlds();
        sender.sendMessage(ChatColor.GREEN + "World configuration reloaded.");
    }

    private void sendAdminHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "VoidFlame World Manager");
        sender.sendMessage(ChatColor.GRAY + "/vfworld list");
        sender.sendMessage(ChatColor.GRAY + "/vfworld info <world>");
        sender.sendMessage(ChatColor.GRAY + "/vfworld enable <world>");
        sender.sendMessage(ChatColor.GRAY + "/vfworld disable <world>");
        sender.sendMessage(ChatColor.GRAY + "/vfworld tp <world>");
        sender.sendMessage(ChatColor.GRAY + "/vfworld setspawn");
        sender.sendMessage(ChatColor.GRAY + "/vfworld reload");
    }

    private void require(String[] args, int size, String message) {
        if (args.length < size || args[size - 1].isBlank()) throw new IllegalArgumentException(message);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("spawn")) return List.of();
        if (!sender.hasPermission(ADMIN_PERMISSION)) return List.of();
        if (args.length == 1) {
            return filter(List.of("help", "list", "info", "enable", "load", "disable", "unload",
                    "tp", "teleport", "setspawn", "reload"), args[0]);
        }
        if (args.length == 2 && List.of("info", "enable", "load", "disable", "unload", "tp", "teleport")
                .contains(args[0].toLowerCase(Locale.ROOT))) {
            return worldNames(args[1]);
        }
        return List.of();
    }

    private List<String> worldNames(String prefix) {
        List<String> names = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (world.getName().toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                names.add(world.getName());
            }
        }
        return names;
    }

    private List<String> filter(List<String> values, String prefix) {
        return values.stream().filter(value -> value.startsWith(prefix.toLowerCase(Locale.ROOT))).toList();
    }
}
