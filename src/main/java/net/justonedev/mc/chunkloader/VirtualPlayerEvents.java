package net.justonedev.mc.chunkloader;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_21_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerListPingEvent;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class VirtualPlayerEvents implements Listener {

    private static final String ANONYMOUS = "Anonymous Player";
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

    @EventHandler
    public void onServerListPing(ServerListPingEvent e) {
        Collection<Player> realPlayers = Bukkit.getOnlinePlayers().stream()
                .filter(player -> !plugin.virtualPlayers.contains((CraftPlayer) player))
                .collect(Collectors.toList());

        // This may look complicated, but it works (for paper at least)

        if (!e.getClass().getName().contains(".paper.")) return;
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
            if (sample.isEmpty()) return;

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
