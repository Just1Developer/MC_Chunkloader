package net.justonedev.mc.chunkloader;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkProviderServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.level.ChunkCoordIntPair;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.v1_21_R3.CraftServer;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ChunkLoading {

    private final Plugin plugin;
    public final Set<Chunk> loadedChunks;

    private int scheduler;

    public ChunkLoading(Plugin plugin) {
        this.plugin = plugin;
        loadedChunks = new HashSet<>();
    }

    private void startScheduler() {
        scheduler = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {


            //simulatePlayerPresence();
            if (true) return;



            Set<LivingEntity> entities = new HashSet<>();
            loadedChunks.forEach(chunk -> {
                chunk.load(true);
                Location middle = chunk.getBlock(5, 200, 5).getLocation();
                if (middle.getWorld() != null) {
                    var entity = middle.getWorld().spawn(middle, Villager.class, CreatureSpawnEvent.SpawnReason.COMMAND, false, e -> {
                        e.setSilent(true);
                        e.setPersistent(true);
                        e.setAI(false);
                    });
                    var pearl = middle.getWorld().spawn(middle, EnderPearl.class);
                    Bukkit.broadcastMessage("§bSpawned entity " + entity + " at " + entity.getLocation());
                    entities.add(entity);
                    // Simulate ticking activity
                    middle.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, middle, 10, 2, 2, 2, 0);


                    // ChatGPT:
                    LivingEntity activator = middle.getWorld().spawn(middle, ArmorStand.class, stand -> {
                        stand.setVisible(false);
                        stand.setGravity(false);
                        stand.setPersistent(true);
                        stand.setCustomName("Chunk Activator");
                        stand.setCustomNameVisible(false);
                    });
                    // Perform random block updates or effects
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        // Example: Simulate random block update or activity
                        middle.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, middle, 10, 2, 2, 2, 0);
                    }, 20L); // Delay by 1 second for visual effect

                    // Cleanup entity after some time (if desired)
                    Bukkit.getScheduler().runTaskLater(plugin, activator::remove, 20L * 60); // Remove after 60 seconds
                    Bukkit.getScheduler().runTaskLater(plugin, pearl::remove, 20L * 60); // Remove after 60 seconds
                }
                //Bukkit.broadcastMessage("§5Loading chunk " + chunk.getX() + " " + chunk.getZ());
            });
/*
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                for (LivingEntity entity : entities) {
                    entity.damage(1000);
                    Bukkit.broadcastMessage("§4Killed entity " + entity);
                }
            }, 50);*/
        }, 0, 20); // Every 120 ticks = 6 seconds
    }

    static class TempChunkAndEntityUUID {
        Chunk chunk;
        UUID uuid;

        public TempChunkAndEntityUUID(Chunk chunk, UUID uuid) {
            this.chunk = chunk;
            this.uuid = uuid;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TempChunkAndEntityUUID that = (TempChunkAndEntityUUID) o;
            return Objects.equals(chunk, that.chunk);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(chunk);
        }
    }

    private void stopScheduler() {
        Bukkit.getScheduler().cancelTask(scheduler);

    }

    boolean is = true;

    public void startLoadingChunk(Chunk chunk) {
        if (!loadedChunks.add(chunk)) return;
        //chunk.setForceLoaded(true);
        //chunk.addPluginChunkTicket(plugin);
        Bukkit.broadcastMessage("§eAdded ticket for chunk %d, %d".formatted(chunk.getX(), chunk.getZ()));
        if (loadedChunks.size() == 1) startScheduler();

        final int chunkX = chunk.getX();
        final int chunkZ = chunk.getZ();

        addTicket(chunk);
        if (!is) {
            is = true;
            Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                Bukkit.broadcastMessage("Chunk loaded:" + chunk.getWorld().getChunkAt(chunkX, chunkZ).isLoaded() + " (alternatively: " + chunk.getWorld().getChunkAt(chunkX * 16, chunkZ * 16).isLoaded() + ")");
                Bukkit.broadcastMessage("Other Chunk loaded:" + chunk.getWorld().getChunkAt(chunkX + 1000, chunkZ + 1000).isLoaded());
            }, 20, 20);
        }
        //Location middle = chunk.getBlock(8, chunk.getWorld().getHighestBlockYAt(chunk.getX() * 16 + 8, chunk.getZ() * 16 + 8), 8).getLocation();
        // Spawn a fake player in the chunk
        //spawnFakePlayer3(middle, "ChunkLoader_" + chunk.getX() + "_" + chunk.getZ());
    }

    public void stopLoadingChunk(Chunk chunk) {
        if (!loadedChunks.remove(chunk)) return;
        chunk.setForceLoaded(false);
        chunk.removePluginChunkTicket(plugin);
        //Bukkit.broadcastMessage("§cRemoved ticket for chunk %d, %d".formatted(chunk.getX(), chunk.getZ()));
        if (loadedChunks.isEmpty()) stopScheduler();
    }

    private void addTicket(Chunk chunk) {
        WorldServer worldServer = ((CraftWorld) chunk.getWorld()).getHandle();
        ChunkCoordIntPair chunkCoord = new ChunkCoordIntPair(chunk.getX(), chunk.getZ());
        ChunkProviderServer chunkProvider = worldServer.m();

        /*/ Add a player ticket to the chunk
        chunkProvider.a(TicketType.c, chunkCoord, 0, chunkCoord);   // priority 0, hopefully c is player
        chunkProvider.a(TicketType.d, chunkCoord, 0, chunkCoord);   // priority 0, hopefully c is player
        chunkProvider.a(TicketType.f, chunkCoord, 0, chunkCoord);   // priority 0, hopefully c is player
        chunkProvider.a(TicketType.g, chunkCoord, 0, chunkCoord);   // priority 0, hopefully c is player
        //*/

        testTicket(chunkProvider, TicketType.c, chunkCoord);   // priority 0, c is player, d is forced, e is ender_pearl and g is unknown

        net.minecraft.world.level.chunk.Chunk nmsChunk = worldServer.d(chunk.getX(), chunk.getZ());
        Bukkit.broadcastMessage("nmsChunk: " + nmsChunk);
        if (nmsChunk != null) {
            // Stelle sicher, dass der Chunk als "tickend" markiert wird
            nmsChunk.a(true); // Methode finden oder ersetzen, falls sie nicht verfügbar ist
            nmsChunk.b(true); // Methode finden oder ersetzen, falls sie nicht verfügbar ist

            //worldServer.a(nmsChunk);
            //worldServer.b(nmsChunk);
        }
        nmsChunk = worldServer.getChunkIfLoaded(chunk.getX(), chunk.getZ());
        Bukkit.broadcastMessage("nmsChunk2: " + nmsChunk);
        if (nmsChunk != null) {
            // Stelle sicher, dass der Chunk als "tickend" markiert wird
            nmsChunk.a(true); // Methode finden oder ersetzen, falls sie nicht verfügbar ist
            nmsChunk.b(true); // Methode finden oder ersetzen, falls sie nicht verfügbar ist
            //worldServer.a(nmsChunk);
            //worldServer.b(nmsChunk);
        }
    }

    private void testTicket(ChunkProviderServer chunkProvider, TicketType<ChunkCoordIntPair> ticketType, ChunkCoordIntPair chunkCoord) {
        // Add the ticket
        chunkProvider.b(ticketType, chunkCoord, 0, chunkCoord); // priority 0

        // Check if the chunk is loaded
        boolean isLoaded = chunkProvider.isChunkLoaded(chunkCoord.h, chunkCoord.i); // Prüft, ob der Chunk geladen ist
        boolean isLoaded2 = chunkProvider.isChunkLoaded(chunkCoord.i, chunkCoord.h); // Prüft, ob der Chunk geladen ist
        Bukkit.broadcastMessage("TicketType: " + ticketType + " - Chunk geladen: " + isLoaded + " bzw. " + isLoaded2);
    }

    private void removeTicket(Chunk chunk) {
        /*
        CraftWorld craftWorld = ((CraftWorld) chunk.getWorld());
        WorldServer worldServer = ((CraftWorld) chunk.getWorld()).getHandle();

        // Access the ChunkProviderServer
        net.minecraft.server.level.Server
        try (ChunkProviderServer chunkProvider = worldServer.m()) {

            // Add a player ticket to the chunk
            // This is NMS-specific and may change with server versions
            chunkProvider.addTicket(TicketType.PLAYER, new ChunkCoordIntPair(chunkX, chunkZ), 0, player);
            chunkProvider.b(TicketType.);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
    }



    // Temp code

    Set<Player> fakePlayers = new HashSet<>();

    public void end() {
        for (var player : fakePlayers) {
            player.remove();
        }
    }

    private void simulatePlayerPresence() {
        Bukkit.getWorlds().forEach(world -> {
            loadedChunks.forEach(chunk -> {
                if (!chunk.isLoaded()) {
                    chunk.load(true);
                }

                Location middle = chunk.getBlock(8, world.getHighestBlockYAt(chunk.getX() * 16 + 8, chunk.getZ() * 16 + 8), 8).getLocation();

                // Spawn a fake player in the chunk
                spawnFakePlayer(middle, "ChunkLoader_" + chunk.getX() + "_" + chunk.getZ());
                plugin.getLogger().info("Simulated player activity in chunk: (" + chunk.getX() + ", " + chunk.getZ() + ")");
            });
        });
    }

    private void spawnFakePlayer(Location location, String name) {
        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        if (craftWorld == null) return;

        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
        EntityPlayer fakePlayer = new EntityPlayer(
                craftServer.getServer(),
                craftWorld.getHandle(),
                gameProfile,
                null
        );

        fakePlayer.teleportTo(((CraftWorld) location.getWorld()).getHandle(), location.getX(), location.getY(), location.getZ(), new HashSet<>(), 0f, 0f, true, PlayerTeleportEvent.TeleportCause.COMMAND);

        // Add the fake player to the world
        PlayerConnection connection = fakePlayer.f;
        try {
            Class<?> craftWorldClass = craftWorld.getClass();
            Method getHandleMethod = craftWorldClass.getDeclaredMethod("getHandle");
            getHandleMethod.setAccessible(true);
            Object nmsWorld = getHandleMethod.invoke(craftWorld);

            Class<?> nmsWorldClass = nmsWorld.getClass();
            Method addEntityMethod = nmsWorldClass.getDeclaredMethod("addEntity", EntityPlayer.class);
            addEntityMethod.setAccessible(true);
            addEntityMethod.invoke(nmsWorld, fakePlayer);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        // Send player spawn packets (optional with ProtocolLib)
        Player bukkitPlayer = fakePlayer.getBukkitEntity();
        if (bukkitPlayer instanceof CraftPlayer craftPlayer) {
            craftPlayer.getHandle().f = connection; // Update connection
        }
        fakePlayers.add(bukkitPlayer);
    }

    private void spawnFakePlayer3(Location location, String name) {
        MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        CraftWorld nmsWorld = (CraftWorld) location.getWorld();
        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
        assert nmsWorld != null;
        ClientInformation information = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            information = ((CraftPlayer) p).getHandle().C();
        }
        if (information == null) {
            Bukkit.broadcastMessage("no connection");
            return;
        }
        EntityPlayer npc = new EntityPlayer(nmsServer, nmsWorld.getHandle(), gameProfile, information); // This will be the EntityPlayer (NPC) we send with the sendNPCPacket method.
        npc.getBukkitEntity().teleport(location);
        Bukkit.broadcastMessage("spawned npc");
        fakePlayers.add(npc.getBukkitEntity());
    }

    private void spawnFakePlayer2(Location location, String name) {
        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        if (craftWorld == null) return;

        GameProfile gameProfile = new GameProfile(UUID.randomUUID(), name);
        EntityPlayer fakePlayer = new EntityPlayer(
                craftServer.getServer(),
                craftWorld.getHandle(),
                gameProfile,
                new ClientInformation(null, 0, null, false, 0, null, false, false, null)
        );
    }

}
