package net.justonedev.mc.chunkloader;

import net.minecraft.server.level.EntityPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;

import javax.annotation.Nullable;
import java.util.Objects;

public class Chunkloader {

    private final Plugin plugin;
    private final Location location;
    private boolean active;
    @Nullable
    private CraftPlayer player;

    public Chunkloader(Plugin plugin, Location location) {
        this.plugin = plugin;
        this.location = location;
    }

    public Chunkloader(Plugin plugin, Location location, boolean active) {
        this(plugin, location);
        setActive(active);
    }

    public Location getLocation() {
        return location;
    }

    public boolean isActive() {
        return active;
    }

    public boolean setActive(boolean active) {
        this.active = active;
        Bukkit.broadcastMessage("1 " + active);
        if (active) {
            Bukkit.broadcastMessage("2");
            EntityPlayer entityPlayer = plugin.spawnVirtualPlayer(location);
            Bukkit.broadcastMessage("3" + entityPlayer);
            if (entityPlayer == null) {
                Bukkit.broadcastMessage("4");
                this.active = false;
                player = null;
                Bukkit.getLogger().warning("Chunkloader failed to activate at location " + location);
                return false;
            }
            Bukkit.broadcastMessage("5");
            player = entityPlayer.getBukkitEntity();
            plugin.setChunkLoaded(location.getChunk(), true);
        } else {
            Bukkit.broadcastMessage("6");
            despawn();
            player = null;
        }
        Bukkit.broadcastMessage("8");
        updateBlock();
        return true;
    }

    public void despawn() {
        if (player != null) {
            plugin.despawnVirtualPlayer(player);
        }
    }

    public void updateBlock() {
        Block block = location.getBlock();
        if (block instanceof Lightable) {
            Bukkit.getLogger().warning("Failed to update block at " + location);
            plugin.removeChunkloader(this);
            return;
        }
        Lightable lightable = (Lightable) block.getBlockData();
        lightable.setLit(active);
        block.setBlockData(lightable, false);   // false to avoid physics updates: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/block/Block.html#setBlockData(org.bukkit.block.data.BlockData,boolean)
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Chunkloader that = (Chunkloader) o;
        return Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(location);
    }

    @Override
    public String toString() {
        return "Chunkloader={%s, Active=%b}".formatted(location, active);
    }
}
