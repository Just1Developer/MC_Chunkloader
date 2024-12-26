package net.justonedev.mc.chunkloader;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public class LoadEvents implements Listener {

    private final Plugin plugin;
    public LoadEvents(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        for (Chunkloader loader : plugin.allChunkloaders) {
            if (!loader.getLocation().getChunk().equals(e.getChunk())) continue;
            Bukkit.broadcastMessage("§cUnloading force loaded chunk at " + e.getChunk().getX() + " " + e.getChunk().getZ());
            Bukkit.broadcastMessage("§eLoc at: " + loader);
        }
    }

}
