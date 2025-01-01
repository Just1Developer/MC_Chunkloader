package net.justonedev.mc.chunkloader;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.EnumProtocolDirection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.WorldServer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.PlayerConnection;
import net.minecraft.world.entity.EnumMainHand;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.EnumChatVisibility;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_21_R3.CraftServer;
import org.bukkit.craftbukkit.v1_21_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;

import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class VirtualPlayers {

    private static final ClientInformation clientInfo = new ClientInformation(
            "en_us",           // client locale
            8,                 // render distance
            EnumChatVisibility.c, // chat visibility enum
            true,              // chat colors enabled?
            127,               // displayed skin parts (bitmask)
            EnumMainHand.b,     // main hand
            false,             // text filtering enabled?
            false,              // allows listing?
            ParticleStatus.c
    );

    /*
        [20:15:02 INFO]: EnumChatVisibility.a = FULL
        [20:15:02 INFO]: EnumChatVisibility.b = SYSTEM
        [20:15:02 INFO]: EnumChatVisibility.c = HIDDEN
        [20:15:02 INFO]: EnumMainHand.a = LEFT
        [20:15:02 INFO]: EnumMainHand.b = RIGHT
        [20:15:02 INFO]: ParticleStatus.a = ALL
        [20:15:02 INFO]: ParticleStatus.b = DECREASED
        [20:15:02 INFO]: ParticleStatus.c = MINIMAL
        [20:15:02 INFO]: EnumProtocolDirection.a = SERVERBOUND
        [20:15:02 INFO]: EnumProtocolDirection.b = CLIENTBOUND
    * */

    public static void printDeobfuscated(Plugin plugin) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            Bukkit.broadcastMessage("§eEnumChatVisibility.a = " + EnumChatVisibility.a.toString());
            Bukkit.broadcastMessage("§eEnumChatVisibility.b = " + EnumChatVisibility.b.toString());
            Bukkit.broadcastMessage("§eEnumChatVisibility.c = " + EnumChatVisibility.c.toString());
            Bukkit.broadcastMessage("§eEnumMainHand.a = " + EnumMainHand.a.toString());
            Bukkit.broadcastMessage("§eEnumMainHand.b = " + EnumMainHand.b.toString());
            Bukkit.broadcastMessage("§dParticleStatus.a = " + ParticleStatus.a.toString());
            Bukkit.broadcastMessage("§dParticleStatus.b = " + ParticleStatus.b.toString());
            Bukkit.broadcastMessage("§dParticleStatus.c = " + ParticleStatus.c.toString());
            Bukkit.broadcastMessage("§cEnumProtocolDirection.a = " + EnumProtocolDirection.a.toString());
            Bukkit.broadcastMessage("§cEnumProtocolDirection.b = " + EnumProtocolDirection.b.toString());
        }, 5);
    }

    /**
     * Spawns an invisible "virtual" NMS player in the given location.
     * This keeps the chunk loaded but is not visible to other players.
     */
    public static EntityPlayer spawnVirtualPlayer(Plugin plugin, Set<CraftPlayer> virtualPlayers, Set<String> virtualPlayerNames, Location location) {
        // 1) Get NMS server & NMS world
        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        MinecraftServer nmsServer = craftServer.getServer();

        CraftWorld craftWorld = (CraftWorld) location.getWorld();
        if (craftWorld == null) return null;
        WorldServer nmsWorld = craftWorld.getHandle();

        // 2) Create a fake GameProfile
        String name = getName();
        GameProfile profile = new GameProfile(
                UUID.randomUUID(),
                name
        );

        // 4) Create the EntityPlayer
        EntityPlayer nmsPlayer = new EntityPlayer(nmsServer, nmsWorld, profile, clientInfo);

        // 5) Create a dummy NetworkManager + PlayerConnection
        NetworkManager networkManager = new NetworkManager(EnumProtocolDirection.a); // SERVERBOUND
        networkManager.n = new EmbeddedChannel();   // To allow the handshake without nullpointer
        networkManager.o = new InetSocketAddress("127.0.0.1", 9999);    // To allow "disconnecting"

        // TOTALLY FAKE EXAMPLE – adapt to your actual code
        CommonListenerCookie cookie = new CommonListenerCookie(
                profile,            // profile
                0,              // some flags or info
                clientInfo,           // client info
                false      // ???
        );

        nmsPlayer.f = new PlayerConnection(nmsServer, networkManager, nmsPlayer, cookie);

        // 6) Set position
        nmsPlayer.getBukkitEntity().teleport(location);

        // 7) Add to server’s player list
        virtualPlayerNames.add(name);
        craftServer.getHandle().a(networkManager, nmsPlayer, cookie);
        virtualPlayers.add(nmsPlayer.getBukkitEntity());
        // SPECTATOR *would* solve our issues, but that doesn't trigger ticking...
        nmsPlayer.getBukkitEntity().setGameMode(GameMode.CREATIVE); // Creative so mobs don't target it. I hope this is not exploitable
        setPosition(nmsPlayer, nmsWorld, location);

        // 8) Hide from all real players
        hideFromAllPlayers(plugin, virtualPlayerNames, nmsPlayer);

        // c is setOnFire
        // k() is setInvisible
        // j() is setHighlighted
        nmsPlayer.k(true);
        nmsPlayer.j(net.justonedev.mc.chunkloader.Plugin.ENTITY_HIGHLIGHTING);

        return nmsPlayer;
    }

    private static String getName() {
        String name = "VL%d".formatted(System.nanoTime());
        return name.length() <= 16 ? name : name.substring(0, 16);
    }

    public static void setPosition(
            EntityPlayer nmsPlayer,
            WorldServer nmsWorld,
            Location location
    ) {
        // We want absolute position & rotation, so use an empty or none-of(Relative.class) set
        Set<Relative> relativeSet = EnumSet.noneOf(Relative.class);

        // false => we don’t do the internal “d(this)” logic
        // TeleportCause.PLUGIN => up to you
        nmsPlayer.teleportTo(
                nmsWorld,
                location.getX(),
                location.getY(),
                location.getZ(),
                relativeSet,
                180f,
                0f,
                false,
                PlayerTeleportEvent.TeleportCause.PLUGIN
        );
    }

    private static void hideFromAllPlayers(Plugin plugin, Set<String> virtualPlayers, EntityPlayer nmsPlayer) {
        Player bukkitPlayer = nmsPlayer.getBukkitEntity();
        for (Player realPlayer : Bukkit.getOnlinePlayers()) {
            if (virtualPlayers.contains(realPlayer.getName())) continue;
            realPlayer.hidePlayer(plugin, bukkitPlayer);
        }
    }
}
