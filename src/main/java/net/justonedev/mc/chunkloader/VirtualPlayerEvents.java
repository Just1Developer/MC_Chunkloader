package net.justonedev.mc.chunkloader;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class VirtualPlayerEvents implements Listener {

    Plugin plugin;

    public VirtualPlayerEvents(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (plugin.virtualPlayerNames.contains(e.getPlayer().getName())) {
            e.setJoinMessage(null);
            return;
        }

        for (var virtualPlayer : plugin.virtualPlayers) {
            e.getPlayer().hidePlayer(plugin, virtualPlayer);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (plugin.virtualPlayerNames.contains(e.getPlayer().getName())) {
            e.setQuitMessage(null);
            plugin.virtualPlayerNames.remove(e.getPlayer().getName());
        }
    }


    // StandardPaperServerListPingEventImpl
    //[23:18:25 INFO]: [ChunkLoader] Fields: 0          <<<<<<< 0 Fields
    //[23:18:25 INFO]: [ChunkLoader] Methods: 42
    //[23:18:25 INFO]: processRequest
    //[23:18:25 INFO]: getPlayerSample
    //[23:18:25 INFO]: getListedPlayers
    //[23:18:25 INFO]: iterator
    //[23:18:25 INFO]: isCancelled
    //[23:18:25 INFO]: getVersion
    //[23:18:25 INFO]: setVersion
    //[23:18:25 INFO]: getProtocolVersion
    //[23:18:25 INFO]: getServerIcon
    //[23:18:25 INFO]: getMaxPlayers
    //[23:18:25 INFO]: shouldHidePlayers
    //[23:18:25 INFO]: setHidePlayers
    //[23:18:25 INFO]: setNumPlayers                    <<<<<<<<<<<< Relevant
    //[23:18:25 INFO]: setProtocolVersion
    //[23:18:25 INFO]: getClient
    //[23:18:25 INFO]: setCancelled
    //[23:18:25 INFO]: setServerIcon
    //[23:18:25 INFO]: getNumPlayers
    //[23:18:25 INFO]: getAddress
    //[23:18:25 INFO]: getHandlers
    //[23:18:25 INFO]: getHostname
    //[23:18:25 INFO]: shouldSendChatPreviews
    //[23:18:25 INFO]: setMaxPlayers
    //[23:18:25 INFO]: motd
    //[23:18:25 INFO]: motd
    //[23:18:25 INFO]: setMotd
    //[23:18:25 INFO]: getMotd
    //[23:18:25 INFO]: getHandlerList
    //[23:18:25 INFO]: callEvent
    //[23:18:25 INFO]: getEventName
    //[23:18:25 INFO]: isAsynchronous
    //[23:18:25 INFO]: equals
    //[23:18:25 INFO]: toString
    //[23:18:25 INFO]: hashCode
    //[23:18:25 INFO]: getClass
    //[23:18:25 INFO]: notify
    //[23:18:25 INFO]: notifyAll
    //[23:18:25 INFO]: wait
    //[23:18:25 INFO]: wait
    //[23:18:25 INFO]: wait
    //[23:18:25 INFO]: spliterator
    //[23:18:25 INFO]: forEach
    //

    private static final String ANONYMOUS = "Anonymous Player";

    @EventHandler
    public void onServerListPing(ServerListPingEvent e) {
        Collection<Player> realPlayers = Bukkit.getOnlinePlayers().stream()
                .filter(player -> !plugin.virtualPlayers.contains((CraftPlayer) player))
                .collect(Collectors.toList());

        printMethods(e);
        Bukkit.broadcastMessage("[ChunkLoader] Name: " + e.getClass().getName() + " || " + e.getClass().getName().contains(".paper."));

        // This may look complicated, but it works (for paper at least)

        if (e.getClass().getName().contains(".paper.")) {
            try {
                Method numPlayersMethod = e.getClass().getMethod("setNumPlayers", int.class);
                numPlayersMethod.setAccessible(true);
                numPlayersMethod.invoke(e, realPlayers.size());
            } catch (Exception ex) {
                Bukkit.getLogger().severe("Failed to access numPlayers Field:" + ex.getMessage());
                Bukkit.getLogger().severe("%s | %s".formatted(ex.getMessage(), ex.getClass()));
            }

            try {
                Method setPlayerSampleMethod = e.getClass().getMethod("getPlayerSample");
                List<?> sample = (List<?>) setPlayerSampleMethod.invoke(e);
                printMethods(sample.getFirst());

                sample.removeIf(player -> {
                    try {
                        Method nameMethod = player.getClass().getMethod("getName");
                        nameMethod.setAccessible(true);
                        String name = (String) nameMethod.invoke(player);
                        return name.equals(ANONYMOUS) || plugin.virtualPlayerNames.contains(name);
                    } catch (Exception ex) {
                        return false;
                    }
                });
            } catch (Exception ex) {
                Bukkit.getLogger().severe("Failed to access Method: " + ex.getMessage());
                Bukkit.getLogger().severe("%s | %s".formatted(ex.getMessage(), ex.getClass()));
                try {
                    Method setPlayerSampleMethod = e.getClass().getMethod("getPlayerSample");
                    @SuppressWarnings("unchecked")
                    List<String> sample = (List<String>) setPlayerSampleMethod.invoke(e);
                    sample.removeIf(player -> plugin.virtualPlayerNames.contains(player));
                } catch (Exception ex2) {
                    Bukkit.getLogger().severe("Failed to access Method: " + ex2.getMessage());
                    Bukkit.getLogger().severe("%s | %s".formatted(ex2.getMessage(), ex2.getClass()));
                }
            }
        } else {
            Bukkit.getLogger().info("Spigot Approach: TODO");
            try {
                // Access the `numPlayers` field (if it exists)
                Field numPlayersField = e.getClass().getField("numPlayers");
                numPlayersField.setAccessible(true);

                // Set the desired number of players
                numPlayersField.set(e, 42);
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                Bukkit.getLogger().severe("Failed to access Field/Method: " + ex.getMessage());
                Bukkit.getLogger().severe("%s | %s".formatted(ex.getMessage(), ex.getClass()));
            }
        }

    }

    private void printMethods(Object obj) {
        Bukkit.broadcastMessage("[ChunkLoader] Fields: " + obj.getClass().getFields().length);
        for (var field : obj.getClass().getFields()) {
            Bukkit.broadcastMessage(field.getName());
        }
        Bukkit.broadcastMessage("[ChunkLoader] Methods: " + obj.getClass().getMethods().length);
        for (Method method : obj.getClass().getMethods()) {
            String modifiers = Arrays.stream(method.getModifiers() == 0 ? new int[] {} : new int[] { method.getModifiers() })
                    .mapToObj(Modifier::toString)
                    .collect(Collectors.joining(" "));

            String returnType = method.getReturnType().getSimpleName();
            String methodName = method.getName();
            String params = Arrays.stream(method.getParameterTypes())
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(", "));

            String methodSignature = String.format("%s %s %s(%s)",
                    modifiers.isEmpty() ? "" : modifiers,
                    returnType,
                    methodName,
                    params
            );

            // Log to console instead of broadcasting to players
            Bukkit.getLogger().info(methodSignature);
        }
    }

}
