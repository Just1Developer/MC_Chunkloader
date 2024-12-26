package net.justonedev.mc.chunkloader;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collection;
import java.util.Set;

public final class Plugin extends JavaPlugin {

    public static final Material MATERIAL = Material.REDSTONE_LAMP;
    private static Plugin plugin;

    public Set<Chunkloader> allChunkloaders;

    @Override
    public void onEnable() {
        plugin = this;
        // Plugin startup logic
        allChunkloaders = FileSaver.loadAll();
        FileSaver.cleanUp();
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new LoadEvents(this), this);
        pluginManager.registerEvents(new ChunkLoaderEvents(this), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
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
    public boolean containsChunkloader(Chunkloader chunkloader) {
        return allChunkloaders.contains(chunkloader);
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
