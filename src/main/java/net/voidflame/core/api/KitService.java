package net.voidflame.core.api;

import org.bukkit.entity.Player;

public interface KitService {
    boolean apply(Player player, String kitId);
    boolean exists(String kitId);
}
