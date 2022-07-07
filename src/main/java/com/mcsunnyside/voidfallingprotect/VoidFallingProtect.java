package com.mcsunnyside.voidfallingprotect;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VoidFallingProtect extends JavaPlugin {

    private final List<String> worldBlacklist = new ArrayList<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            Class.forName("org.bukkit.generator.WorldInfo").getMethod("getMinHeight");
            Bukkit.getPluginManager().registerEvents(new PlayerMoveListenerNew(), this);
        } catch (ClassNotFoundException | NoSuchMethodException throwable) {
            Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(), this);
        }
        if (!getConfig().isSet("world-blacklist")) {
            getConfig().set("world-blacklist", new ArrayList<>());
            saveConfig();
        }
        for (String worldName : getConfig().getStringList("world-blacklist")) {
            worldBlacklist.add(worldName.toLowerCase(Locale.ROOT));
        }
        // Plugin startup logic
    }

    @Override
    public void onDisable() {
        worldBlacklist.clear();
        // Plugin shutdown logic
    }

    public void teleportToSafeLoc(Player player) {
        getLogger().warning("Prevent player " + player.getName() + " fell out of the world.");
        Location location = player.getLocation();
        boolean safeFound = true;
        int highestWorldHeight = player.getWorld().getMaxHeight() - 1;
        while (location.getBlock().getType() != Material.AIR || location.add(0, 1, 0).getBlock().getType() != Material.AIR) {
            if (location.getBlockY() > highestWorldHeight) {
                safeFound = false;
                break;
            }
            location = location.add(0, 1, 0);
        }
        if (safeFound) {
            player.teleport(location);
        } else {
            player.teleport(player.getWorld().getSpawnLocation());
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("message", "Unsafe movement detected, we teleport you to a safe location to avoid you drop into the void!")));
    }

    class PlayerMoveListener implements Listener {

        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onPlayerMove(PlayerMoveEvent event) {
            if (event instanceof PlayerTeleportEvent) {
                return;
            }
            if (event.getTo() == null) {
                return;
            }
            if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
                return;
            }
            World world = event.getPlayer().getWorld();
            if (worldBlacklist.contains(world.getName().toLowerCase(Locale.ROOT))) {
                return;
            }
            if (event.getTo().getBlockY() >= 0) {
                return;
            }
            if (event.getFrom().getBlockY() < 0) {
                return;
            }
            if (!event.getFrom().getBlock().getType().isSolid()) {
                return;
            }
            event.setCancelled(true);
            Bukkit.getScheduler().runTaskLater(VoidFallingProtect.this, () -> teleportToSafeLoc(event.getPlayer()), 1);
        }
    }

    class PlayerMoveListenerNew implements Listener {

        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onPlayerMove(PlayerMoveEvent event) {
            if (event instanceof PlayerTeleportEvent) {
                return;
            }
            if (event.getTo() == null) {
                return;
            }
            if (event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
                return;
            }
            World world = event.getPlayer().getWorld();
            if (worldBlacklist.contains(world.getName().toLowerCase(Locale.ROOT))) {
                return;
            }
            if (event.getTo().getBlockY() >= 0) {
                return;
            }
            int fromY = event.getFrom().getBlockY();
            if (fromY < 0) {
                int minHeight = world.getMinHeight();
                if (minHeight < 0 && fromY > minHeight) {
                    return;
                }
            }
            if (!event.getFrom().getBlock().getType().isSolid()) {
                return;
            }
            event.setCancelled(true);
            Bukkit.getScheduler().runTaskLater(VoidFallingProtect.this, () -> teleportToSafeLoc(event.getPlayer()), 1);
        }
    }
}
