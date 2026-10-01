package net.voidflame.core.api;

import org.bukkit.entity.Player;

public interface KitService {
    boolean apply(Player player, String kitId);
    boolean exists(String kitId);

    /** Opens the owning Kits plugin's layout editor without moving GUI ownership into Core. */
    default boolean openEditor(Player player, String kitId, String layoutName) { return false; }
}
