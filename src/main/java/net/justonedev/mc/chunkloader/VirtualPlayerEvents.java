package net.justonedev.mc.chunkloader;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.ArrayList;
import java.util.List;

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
    public void onServerListPing(ServerListPingEvent e) {
        // 1) Calculate the “real” players (exclude virtual).
        int realPlayersCount = 0;
        List<Player> realPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!plugin.virtualPlayerNames.contains(p.getName())) {
                realPlayers.add(p);
            }
        }
    }

}
