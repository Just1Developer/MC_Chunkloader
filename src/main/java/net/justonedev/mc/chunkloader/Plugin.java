package net.justonedev.mc.chunkloader;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class Plugin extends JavaPlugin implements Listener {

    public static final Material MATERIAL = Material.REDSTONE_LAMP;
    private static Plugin plugin;

    public static final boolean ENTITY_HIGHLIGHTING = false;

    public Set<Chunkloader> allChunkloaders;
    public ChunkLoading chunkLoading;

    @Override
    public void onEnable() {
        plugin = this;
        chunkLoading = new ChunkLoading(this);
        // Plugin startup logic
        allChunkloaders = FileSaver.loadAll();
        FileSaver.cleanUp();
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new VirtualPlayerEvents(this), this);
        pluginManager.registerEvents(new ChunkLoaderEvents(this), this);
        pluginManager.registerEvents(new Crafting(this), this);

        // For Debugging:
        pluginManager.registerEvents(this, this);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            allChunkloaders.forEach(l -> { if (l.isActive()) chunkLoading.startLoadingChunk(l.getLocation().getChunk()); } );
        }, 5);
    }

    public final Set<CraftPlayer> virtualPlayers = new HashSet<>();
    public final Set<String> virtualPlayerNames = new HashSet<>();

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!e.getPlayer().getName().equals("Just1Developer")) return;
        var spawnedVirtualPlayer = VirtualPlayers.spawnVirtualPlayer(this, virtualPlayers, virtualPlayerNames, e.getPlayer().getLocation());
        Bukkit.broadcastMessage("entity: " + spawnedVirtualPlayer);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        chunkLoading.end();
        for (CraftPlayer virtualPlayer : virtualPlayers) {
            virtualPlayer.kickPlayer("Server Restart");
        }
    }

    public static File getFolder() {
        return plugin.getDataFolder();
    }
    public void addChunkloader(Chunkloader chunkloader) {
        boolean added = allChunkloaders.add(chunkloader);
        if (added) FileSaver.saveAll(allChunkloaders);
    }

    public boolean containsChunkloader(Location location) {
        return allChunkloaders.contains(new Chunkloader(location));
    }

    public boolean removeChunkloader(Location location) {
        return removeChunkloader(new Chunkloader(location));
    }
    public boolean removeChunkloader(Chunkloader chunkloader) {
        boolean removed = allChunkloaders.remove(chunkloader);
        if (removed) FileSaver.saveAll(allChunkloaders);
        return removed;
    }
    public void removeAllChunkloaders(Collection<Chunkloader> chunkloaders) {
        boolean removed = allChunkloaders.removeAll(chunkloaders);
        if (removed) FileSaver.saveAll(allChunkloaders);
    }

}
