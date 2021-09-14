package me.matsubara.blencraft.manager;

import me.matsubara.blencraft.BlencraftPlugin;
import me.matsubara.blencraft.model.Model;
import me.matsubara.blencraft.model.stand.StandSettings;
import me.matsubara.blencraft.stand.PacketStand;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class StandManager implements Listener {

    private final BlencraftPlugin plugin;

    public StandManager(BlencraftPlugin plugin) {
        this.plugin = plugin;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public PacketStand spawn(Location location, StandSettings settings) {
        return new PacketStand(location, settings);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        for (Model model : plugin.getModelManager().getModels()) {
            for (PacketStand stand : model.getStands().values()) {

                Location standLocation = stand.getLocation();
                if (standLocation.getWorld() == null || !standLocation.getWorld().equals(player.getWorld())) continue;

                boolean outOfRange = stand.getLocation().distance(player.getLocation()) > PacketStand.RENDER_DISTANCE;
                if (!outOfRange) stand.spawn(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) return;

        Player player = event.getPlayer();

        for (Model model : plugin.getModelManager().getModels()) {
            for (PacketStand stand : model.getStands().values()) {

                Location standLocation = stand.getLocation();
                if (standLocation.getWorld() == null || !standLocation.getWorld().equals(player.getWorld())) continue;

                boolean outOfRange = stand.getLocation().distance(event.getTo()) > PacketStand.RENDER_DISTANCE;

                if (stand.isIgnored(player)) {
                    if (!outOfRange) {
                        stand.spawn(player);
                    }
                    continue;
                }

                if (outOfRange) stand.destroy(player);
            }
        }
    }
}