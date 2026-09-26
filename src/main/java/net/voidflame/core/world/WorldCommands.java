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
    private static final String ADMIN_PERMISSION = "voidflame.core.worlds.admin";
    private static final String USE_PERMISSION = "voidflame.core.worlds.use";

    private final WorldService worlds;

    public WorldCommands(WorldService worlds) {
        this.worlds = worlds;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String root = command.getName().toLowerCase(Locale.ROOT);

        if (root.equals("worlds")) {
            return listPublic(sender);
        }

        if (root.equals("world") || root.equals("spawn")) {
            return publicTeleport(sender, root, args);
        }

        return admin(sender, args);
    }

    private boolean listPublic(CommandSender sender) {
        if (!sender.hasPermission(USE_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to view worlds.");
            return true;
        }

        sender.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "VoidFlame Worlds");
        List<World> loaded = worlds.loadedWorlds();
        if (loaded.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "No worlds are currently available.");
            return true;
        }

        for (World world : loaded) {
            sender.sendMessage(ChatColor.DARK_GRAY + "• " + ChatColor.LIGHT_PURPLE + world.getName()
                    + ChatColor.GRAY + " • " + world.getEnvironment().name());
        }
        return true;
    }

    private boolean publicTeleport(CommandSender sender, String root, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
            return true;
        }
        if (!player.hasPermission(USE_PERMISSION)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to teleport between worlds.");
            return true;
        }

        String target = args.length == 0 ? player.getWorld().getName() : args[0];
        if (root.equals("spawn") && args.length == 0) {
            worlds.teleportToSpawn(player, player.getWorld().getName());
            player.sendMessage(ChatColor.GREEN + "Teleported to the world spawn.");
            return true;
        }

        World world = worlds.get(target);
        if (world == null || !worlds.isEnabled(world.getName())) {
            player.sendMessage(ChatColor.RED + "That world is not available.");
            return true;
        }

        worlds.teleportToSpawn(player, world.getName());
        player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.WHITE + world.getName()
                + ChatColor.GREEN + ".");
        return true;
    }

    private boolean admin(CommandSender sender, String[] args) {
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
        String root = command.getName().toLowerCase(Locale.ROOT);

        if (root.equals("worlds")) return List.of();

        if (root.equals("world") || root.equals("spawn")) {
            if (args.length == 1) return worldNames(args[0]);
            return List.of();
        }

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
        return values.stream()
                .filter(value -> value.startsWith(prefix.toLowerCase(Locale.ROOT)))
                .toList();
    }
}
