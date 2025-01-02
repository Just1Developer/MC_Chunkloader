package net.justonedev.mc.chunkloader;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import com.mojang.authlib.GameProfile;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.status.PacketStatusOutServerInfo;
import net.minecraft.network.protocol.status.ServerPing;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConnection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_21_R3.CraftServer;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        getServer().getScheduler().runTaskLater(this, this::injectNettyProtocolLib, 2);

        VirtualPlayers.printDeobfuscated(this);
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

    public void injectNettyProtocolLib() {
        // Get the ProtocolManager instance
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        // Add a packet listener for server info packets
        protocolManager.addPacketListener(new PacketAdapter(this, PacketType.Status.Server.PONG) {
            @Override
            public void onPacketSending(PacketEvent event) {
                try {
                    // Intercept and modify the server ping packet
                    WrappedServerPing ping = event.getPacket().getServerPings().read(0);
                    if (ping == null) {
                        Bukkit.getLogger().severe("WrappedServerPing is null!");
                        return;
                    }

                    Collection<Player> realPlayers = Bukkit.getOnlinePlayers().stream()
                            .filter(player -> !virtualPlayers.contains((CraftPlayer) player))
                            .collect(Collectors.toList());

                    // Modify the visible player count
                    ping.setPlayersOnline(realPlayers.size() + 5);

                    // Modify the hover player list
                    ping.setBukkitPlayers(realPlayers);

                } catch (Exception e) {
                    Bukkit.getLogger().severe("Failed to access numPlayers Field:" + e.getMessage());
                    Bukkit.getLogger().severe("%s | %s".formatted(e.getMessage(), e.getClass()));
                }
            }
        });
    }

    public void injectNetty() {
        MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        ServerConnection serverConnection = nmsServer.ah();
        //for (ChannelFuture future : connection.)
        try {
            // The ServerConnection class typically has a public List<ChannelFuture> field
            Field futuresField = serverConnection.getClass().getDeclaredField("f");
            futuresField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<ChannelFuture> channelFutures = (List<ChannelFuture>) futuresField.get(serverConnection);

            // Inject our handler into each Channel pipeline
            for (ChannelFuture future : channelFutures) {
                Channel channel = future.channel();
                if (channel.pipeline().get("fakePingHider") == null) {
                    channel.pipeline().addFirst( "fakePingHider", new FakePingHiderHandler());
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to inject netty channel futures:" + e.getMessage());
            Bukkit.getLogger().severe(e.getMessage());
            Bukkit.getLogger().severe(Arrays.toString(e.getStackTrace()));
        }
    }


    private class FakePingHiderHandler extends ChannelOutboundHandlerAdapter {

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            // Check if this is the status response packet
            if (msg instanceof PacketStatusOutServerInfo) {
                // We reflect into the packet’s fields to get the "ServerStatus" object
                msg = rewritePingPacket((PacketStatusOutServerInfo) msg);
                getLogger().info("Rewrite handler: Intercepted PacketStatusOutServerInfo!");
            }
            super.write(ctx, msg, promise);
        }

        private PacketStatusOutServerInfo rewritePingPacket(PacketStatusOutServerInfo oldPacket) {
            // 1) Get the current ServerPing record from the packet
            ServerPing oldPing = oldPacket.b();

            // oldPing has:
            //   IChatBaseComponent b()  -> The MOTD/description
            //   Optional<ServerPingPlayerSample> c()  -> The players section
            //   Optional<ServerPing.ServerData> d()   -> The version data
            //   Optional<ServerPing.a> e()            -> The favicon
            //   boolean f()                           -> enforcesSecureChat?

            // 2) Get the players sample (if present)
            Optional<ServerPing.ServerPingPlayerSample> playersOpt = oldPing.b(); // careful: 'b()' vs 'c()' depends on your code
            // According to your code:
            //   b() -> returns Optional<ServerPingPlayerSample> c
            //   c() -> returns Optional<ServerPing.ServerData> d
            //   so the method to get players is oldPing.b() or oldPing.c()?
            // From your source, we see 'public Optional<ServerPingPlayerSample> b() { return this.c; }'
            // So the correct call is oldPing.b(), not .c().

            Optional<ServerPing.ServerPingPlayerSample> playersRecordOpt = oldPing.b();
            if (playersRecordOpt.isEmpty()) {
                // No players sample present at all, so nothing to rewrite.
                return oldPacket;
            }

            // 3) Extract the existing player sample data
            ServerPing.ServerPingPlayerSample oldSample = playersRecordOpt.get();
            int maxPlayers    = oldSample.a(); // "b" in the record is named a() at runtime
            int onlinePlayers = oldSample.b(); // "c" is b()
            List<GameProfile> sampleProfiles = new ArrayList<>(oldSample.c()); // "d" is c()

            // 4) Remove or adjust “fake” players
            //    For example, remove any whose name is in a known set of fakes
            sampleProfiles.removeIf(gp -> isFakePlayerName(gp.getName()));

            // Maybe you want to recalc the online count to match the new sample size
            // Or do onlinePlayers - (numberOfRemovedPlayers)
            int newOnline = sampleProfiles.size();
            // or if you keep the original online minus however many you removed

            // 5) Build a new ServerPingPlayerSample record with your updated data
            ServerPing.ServerPingPlayerSample newSample = new ServerPing.ServerPingPlayerSample(
                    maxPlayers + 5,
                    newOnline + 6,
                    sampleProfiles
            );

            // 6) Build a brand-new ServerPing record with the updated player sample
            ServerPing newPing = new ServerPing(
                    oldPing.a(),              // description (IChatBaseComponent)
                    Optional.of(newSample),   // new players sample
                    oldPing.c(),              // keep version data the same
                    oldPing.d(),              // keep favicon the same
                    oldPing.e()               // keep enforcesSecureChat
            );

            // 7) Build and return a brand-new packet with the updated ping
            return new PacketStatusOutServerInfo(newPing);
        }

        private boolean isFakePlayerName(String name) {
            return virtualPlayerNames.contains(name);
        }

        // You have to reflect into the sample entry to get the name.
        // In Mojang code it might be a "ServerPingPlayerSample" with a getName() method.
        // In obf, you might need reflection again.
        private String getPlayerNameFromEntry(Object entry) {
            // Something like:
            //   Field nameField = entry.getClass().getDeclaredField("b"); // or "name"
            //   return (String) nameField.get(entry);
            // Or, if there's a method getName():
            try {
                Field nameField = entry.getClass().getDeclaredField("b");
                nameField.setAccessible(true);
                return (String) nameField.get(entry);
            } catch (Exception e) {
                return "???";
            }
        }
    }

}
