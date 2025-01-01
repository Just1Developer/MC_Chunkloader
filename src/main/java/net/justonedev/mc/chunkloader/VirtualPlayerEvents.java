package net.justonedev.mc.chunkloader;

import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class VirtualPlayerEvents implements Listener {

    Plugin plugin;

    public VirtualPlayerEvents(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (plugin.virtualPlayerNames.contains(e.getPlayer().getName())) {
            e.setJoinMessage(null);
            return;
        }

        for (var virtualPlayer : plugin.virtualPlayers) {
            e.getPlayer().hidePlayer(plugin, virtualPlayer);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (plugin.virtualPlayerNames.contains(e.getPlayer().getName())) {
            e.setQuitMessage(null);
            plugin.virtualPlayerNames.remove(e.getPlayer().getName());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntityType() != EntityType.PLAYER) return;
        if (!plugin.virtualPlayers.contains((CraftPlayer) e.getEntity())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onChunkloaderTargetted(EntityDamageEvent e) {
        if (e.getEntityType() != EntityType.PLAYER) return;
        if (!plugin.virtualPlayers.contains((CraftPlayer) e.getEntity())) return;
        e.setCancelled(true);
    }

}
