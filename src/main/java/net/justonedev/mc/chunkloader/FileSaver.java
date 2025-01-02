package net.justonedev.mc.chunkloader;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class FileSaver {

    private static final String FILENAME = "config.yml";
    private static final String FILENAME_ACTIVE = "active.yml";

    public static void saveAll(Set<Chunkloader> chunkloaders) {
        FileConfiguration cfg = getCfg();
        FileConfiguration cfg2 = getCfgActive();
        cfg.set("chunkloaders", chunkloaders.size());
        List<Chunkloader> loaders = new ArrayList<>(chunkloaders);
        for (int i = 0; i < chunkloaders.size(); i++) {
            save(loaders.get(i), i, cfg, cfg2);
        }
        saveToFile(cfg);
        saveToFile2(cfg2);
    }

    public static void saveActivityOnly(Set<Chunkloader> chunkloaders) {
        FileConfiguration cfg2 = getCfgActive();
        List<Chunkloader> loaders = new ArrayList<>(chunkloaders);
        for (int i = 0; i < chunkloaders.size(); i++) {
            saveActivity(loaders.get(i), i, cfg2);
        }
        saveToFile2(cfg2);
    }

    public static Set<Chunkloader> loadAll(Plugin plugin) {
        FileConfiguration cfg = getCfg();
        FileConfiguration cfg2 = getCfgActive();
        Set<Chunkloader> allChunkloaders = new HashSet<>();
        int loaders = cfg.getInt("chunkloaders");
        for (int i = 0; i < loaders; i++) {
            var loader = load(i, cfg, cfg2, plugin);
            if (loader != null) allChunkloaders.add(loader);
        }
        return allChunkloaders;
    }

    public static void cleanUp() {
        FileConfiguration cfg = getCfg();
        FileConfiguration cfg2 = getCfgActive();
        int loaders = cfg.getInt("chunkloaders");

        while (cfg.isSet("loader.%d.x".formatted(loaders)) || cfg2.isSet("loader.%d".formatted(loaders))) {
            cfg.set("loader.%d".formatted(loaders), null);
            cfg2.set("loader.%d".formatted(loaders), null);
            loaders++;
        }
        saveToFile(cfg);
    }

    private static FileConfiguration getCfg() {
        return YamlConfiguration.loadConfiguration(new File(Plugin.getFolder(), FILENAME));
    }

    private static FileConfiguration getCfgActive() {
        return YamlConfiguration.loadConfiguration(new File(Plugin.getFolder(), FILENAME_ACTIVE));
    }

    private static Chunkloader load(int index, FileConfiguration cfg, FileConfiguration cfg2, Plugin plugin) {
        String worldname = cfg.getString("loader.%d.world".formatted(index));
        if (worldname == null) return null;
        World world = Bukkit.getWorld(worldname);
        if (world == null) return null;
        int x = cfg.getInt("loader.%d.x".formatted(index)),
                y = cfg.getInt("loader.%d.y".formatted(index)),
                z = cfg.getInt("loader.%d.z".formatted(index));
        Location loc = new Location(world, x, y, z);
        boolean active = cfg2.getBoolean("loader.%d".formatted(index));
        return new Chunkloader(plugin, loc, active);
    }

    private static void save(Chunkloader chunkloader, int index, FileConfiguration cfg, FileConfiguration cfg2) {
        cfg.set("loader.%d.world".formatted(index), Objects.requireNonNull(chunkloader.getLocation().getWorld()).getName());
        cfg.set("loader.%d.x".formatted(index), chunkloader.getLocation().getBlockX());
        cfg.set("loader.%d.y".formatted(index), chunkloader.getLocation().getBlockY());
        cfg.set("loader.%d.z".formatted(index), chunkloader.getLocation().getBlockZ());
        saveActivity(chunkloader, index, cfg2);
    }

    private static void saveActivity(Chunkloader chunkloader, int index, FileConfiguration cfg2) {
        cfg2.set("loader.%d".formatted(index), chunkloader.isActive());
    }

    private static void saveToFile(FileConfiguration cfg) {
        File file = new File(Plugin.getFolder(), FILENAME);
        try {
            cfg.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe(e.getMessage());
        }
    }

    private static void saveToFile2(FileConfiguration cfg2) {
        File file = new File(Plugin.getFolder(), FILENAME_ACTIVE);
        try {
            cfg2.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().severe(e.getMessage());
        }
    }
}
