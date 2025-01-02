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
        boolean loadedChanged = plugin.setChunkLoaded(chunk, true);
        new Chunkloader(plugin, loc, loadedChanged);
        plugin.saveAllChunkloadersActivity();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() != Plugin.MATERIAL) return;
        Location loc = e.getBlock().getLocation();
        if (!plugin.isChunkloader(loc)) return;
        // Check if there are no other chunkloaders in the chunk
        Chunk chunk = loc.getChunk();

        // Select new
        boolean needsNew = ((Lightable) e.getBlock().getBlockData()).isLit();
        boolean foundNew = false;
        Set<Chunkloader> remove = new HashSet<>();

        for (Chunkloader loader2 : plugin.allChunkloaders) {
            if (loader2.getLocation().getChunk().equals(chunk)) {
                if (loader2.getLocation().getBlock().getType() != Plugin.MATERIAL || loader2.getLocation().equals(loc)) {
                    loader2.despawn();
                    remove.add(loader2);
                    continue;
                }
                if (needsNew) {
                    boolean success = loader2.setActive(true);
                    if (!success) {
                        e.getPlayer().sendMessage("§eCould not activate Chunkloader: Limit reached.");
                    }
                }
                foundNew = true;
                break;
            }
        }

        if (!foundNew) {
            plugin.setChunkLoaded(chunk, false);
        }
        plugin.removeAllChunkloaders(remove);
        if (loc.getWorld() != null && e.getPlayer().getGameMode() != GameMode.CREATIVE) {   // Don't drop items in creative anyway
            e.setDropItems(false);
            loc.getWorld().dropItemNaturally(loc.add(0.5, 0.5, 0.5), Crafting.getItem());
        }
        plugin.saveAllChunkloadersActivity();
    }

    @EventHandler
    public void onLightChange(BlockRedstoneEvent e) {
        if (e.getBlock().getType() != Plugin.MATERIAL) return;
        if (!plugin.isChunkLoaded(e.getBlock().getChunk())) return; // Couldn't possibly be a chunk loader
        for (Chunkloader loader : plugin.allChunkloaders) {
            if (!e.getBlock().getLocation().equals(loader.getLocation())) continue;
            int newPower = loader.isActive() ? 15 : 0;
            e.setNewCurrent(newPower);
        }
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
