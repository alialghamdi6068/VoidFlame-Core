package net.voidflame.core;

import net.voidflame.core.storage.DatabaseService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.nio.file.Files;
import java.nio.file.Path;

public final class RestoreCommand implements CommandExecutor {
    private final VoidFlameCorePlugin plugin;
    public RestoreCommand(VoidFlameCorePlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("voidflame.database.restore")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage("§cUsage: /vfrestore <backup-file>");
            return true;
        }
        Path backup = plugin.getDataFolder().toPath().resolve("backups").resolve(args[0]).normalize();
        Path backups = plugin.getDataFolder().toPath().resolve("backups").normalize();
        if (!backup.startsWith(backups) || !Files.isRegularFile(backup)) {
            sender.sendMessage("§cBackup file not found.");
            return true;
        }
        sender.sendMessage("§eValidating and restoring database...");
        plugin.database().restore(backup).thenRun(() ->
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> sender.sendMessage("§aDatabase restored. Restart the server before allowing players back in.")))
                .exceptionally(error -> {
                    plugin.getLogger().severe("Database restore failed: " + error.getMessage());
                    plugin.getServer().getScheduler().runTask(plugin,
                            () -> sender.sendMessage("§cDatabase restore failed safely; the live database was not replaced."));
                    return null;
                });
        return true;
    }
}
