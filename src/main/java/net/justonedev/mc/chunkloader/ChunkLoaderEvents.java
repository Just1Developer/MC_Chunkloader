package net.justonedev.mc.chunkloader;

import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChunkLoaderEvents implements Listener {

    private final Plugin plugin;
    public ChunkLoaderEvents(Plugin plugin) {
        this.plugin = plugin;
    }

    // Placement

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.getBlock().getType() != Plugin.MATERIAL) return;
        if (!Crafting.isChunkloader(e.getItemInHand())) return;
        Location loc = e.getBlock().getLocation();
        Chunk chunk = loc.getChunk();
        Chunkloader loader = new Chunkloader(loc, !chunk.isForceLoaded());
        updateLightableToActive(loader);
        chunk.setForceLoaded(true);
        plugin.addChunkloader(loader);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() != Plugin.MATERIAL) return;
        Location loc = e.getBlock().getLocation();
        if (!plugin.removeChunkloader(loc)) return;
        // Check if there are no other chunkloaders in the chunk
        Chunk chunk = loc.getChunk();
        plugin.removeChunkloader(loc);

        // Select new
        boolean needsNew = ((Lightable) e.getBlock().getBlockData()).isLit();
        boolean foundNew = false;
        Set<Chunkloader> remove = new HashSet<>();

        for (Chunkloader loader2 : plugin.allChunkloaders) {
            if (loader2.getLocation().getChunk().equals(chunk)) {
                if (loader2.getLocation().getBlock().getType() != Plugin.MATERIAL) {
                    remove.add(loader2);
                    continue;
                }
                if (needsNew) updateLightable(loader2, true);
                foundNew = true;
                break;
            }
        }

        if (!foundNew) chunk.setForceLoaded(false);
        plugin.removeAllChunkloaders(remove);
        if (loc.getWorld() != null && e.getPlayer().getGameMode() != GameMode.CREATIVE) {   // Don't drop items in creative anyway
            e.setDropItems(false);
            loc.getWorld().dropItemNaturally(loc.add(0.5, 0.5, 0.5), Crafting.getItem());
        }
    }

    @EventHandler
    public void onLightChange(BlockRedstoneEvent e) {
        if (e.getBlock().getType() != Plugin.MATERIAL) return;
        if (!e.getBlock().getLocation().getChunk().isForceLoaded()) return; // Couldn't possibly be a chunk loader
        for (Chunkloader loader : plugin.allChunkloaders) {
            if (!e.getBlock().getLocation().equals(loader.getLocation())) continue;
            int newPower = loader.isActive() ? 15 : 0;
            e.setNewCurrent(newPower);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void updateLightable(Chunkloader chunkloader, boolean lit) {
        chunkloader.setActive(lit);
        updateLightable(chunkloader.getLocation().getBlock(), lit);
    }

    private void updateLightableToActive(Chunkloader chunkloader) {
        updateLightable(chunkloader.getLocation().getBlock(), chunkloader.isActive());
    }

    private void updateLightable(Block block, boolean lit) {
        Lightable lightable = (Lightable) block.getBlockData();
        lightable.setLit(lit);
        block.setBlockData(lightable, false);   // false to avoid physics updates: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/block/Block.html#setBlockData(org.bukkit.block.data.BlockData,boolean)
    }

    // Protection

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        e.getBlocks().forEach(block -> {
            if (e.isCancelled()) return;
            if (plugin.containsChunkloader(block.getLocation())) {
                e.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        e.getBlocks().forEach(block -> {
            if (e.isCancelled()) return;
            if (plugin.containsChunkloader(block.getLocation())) {
                e.setCancelled(true);
            }
        });
    }

    @EventHandler
    public void onExplode(BlockExplodeEvent e) {
        handleExplode(e.blockList());
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        handleExplode(e.blockList());
    }

    private void handleExplode(List<Block> blocks) {
        for (int i = 0; i < blocks.size(); i++) {
            if (plugin.containsChunkloader(blocks.get(i).getLocation())) {
                blocks.remove(i);
                i--;
            }
        }
    }

}
