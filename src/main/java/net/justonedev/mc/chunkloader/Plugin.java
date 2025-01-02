package net.justonedev.mc.chunkloader;

import net.minecraft.server.level.EntityPlayer;
import net.minecraft.world.level.ChunkCoordIntPair;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.event.Listener;
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

    private Set<ChunkCoordIntPair> loadedChunks;
    public Set<Chunkloader> allChunkloaders;

    @Override
    public void onEnable() {
        plugin = this;
        loadedChunks = new HashSet<>();
        allChunkloaders = new HashSet<>();
        // Plugin startup logic
        FileSaver.cleanUp();
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new VirtualPlayerEvents(this), this);
        pluginManager.registerEvents(new ChunkLoaderEvents(this), this);
        pluginManager.registerEvents(new Crafting(this), this);

        Bukkit.getScheduler().runTaskLater(this, this::loadAllChunkloaders, 5);
    }

    private void loadAllChunkloaders() {
        allChunkloaders = FileSaver.loadAll(this);
    }

    public final Set<CraftPlayer> virtualPlayers = new HashSet<>();
    public final Set<String> virtualPlayerNames = new HashSet<>();

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        for (CraftPlayer virtualPlayer : virtualPlayers) {
            virtualPlayer.kickPlayer("Server Restart");
        }
    }

    public boolean setChunkLoaded(Chunk chunk, boolean isLoaded) {
        ChunkCoordIntPair coord = new ChunkCoordIntPair(chunk.getX(), chunk.getZ());
        if (isLoaded) return loadedChunks.add(coord);
        else return loadedChunks.remove(coord);
    }

    public boolean isChunkLoaded(Chunk chunk) {
        ChunkCoordIntPair coord = new ChunkCoordIntPair(chunk.getX(), chunk.getZ());
        return loadedChunks.contains(coord);
    }

    public static File getFolder() {
        return plugin.getDataFolder();
    }
    public void addChunkloader(Chunkloader chunkloader) {
        boolean added = allChunkloaders.add(chunkloader);
        if (added) {
            FileSaver.saveAll(allChunkloaders);
            setChunkLoaded(chunkloader.getLocation().getChunk(), true);
        }
    }

    public boolean containsChunkloader(Location location) {
        return allChunkloaders.contains(new Chunkloader(this, location));
    }

    public boolean isChunkloader(Location location) {
        return allChunkloaders.contains(new Chunkloader(this, location));
    }
    public void removeChunkloader(Chunkloader chunkloader) {
        boolean removed = allChunkloaders.remove(chunkloader);
        if (removed) FileSaver.saveAll(allChunkloaders);
    }
    public void removeAllChunkloaders(Collection<Chunkloader> chunkloaders) {
        boolean removed = allChunkloaders.removeAll(chunkloaders);
        if (removed) FileSaver.saveAll(allChunkloaders);
    }

    public EntityPlayer spawnVirtualPlayer(Location location) {
        // Cap:
        int cap = getServer().getMaxPlayers() - 3;
        if (getServer().isWhitelistEnforced()) cap -= getServer().getWhitelistedPlayers().size();
        if (getServer().getOnlinePlayers().size() >= cap) return null;
        return VirtualPlayers.spawnVirtualPlayer(this, virtualPlayers, virtualPlayerNames, location);
    }
    public void despawnVirtualPlayer(CraftPlayer player) {
        virtualPlayers.remove(player);
        player.kickPlayer("Removed.");
        virtualPlayerNames.remove(player.getName());
    }

}
