package net.voidflame.core.api;

import java.util.List;

import org.bukkit.entity.Player;

public interface KitService {
    boolean apply(Player player, String kitId);
    boolean exists(String kitId);
    default List<String> listIds() { return List.of(); }

    /** Opens the owning Kits plugin's layout editor without moving GUI ownership into Core. */
    default boolean openEditor(Player player, String kitId, String layoutName) { return false; }
}
