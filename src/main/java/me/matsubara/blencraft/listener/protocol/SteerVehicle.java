package me.matsubara.blencraft.listener.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import me.matsubara.blencraft.BlencraftPlugin;
import me.matsubara.blencraft.event.PlayerDismountEvent;
import me.matsubara.blencraft.model.Model;
import me.matsubara.blencraft.stand.PacketStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class SteerVehicle extends PacketAdapter {

    private final BlencraftPlugin plugin;

    public SteerVehicle(Plugin plugin, ListenerPriority listenerPriority, PacketType... types) {
        super(plugin, listenerPriority, types);
        this.plugin = (BlencraftPlugin) plugin;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();

        // Get required values from the packet.
        StructureModifier<Boolean> booleans = event.getPacket().getBooleans();
        boolean dismount = booleans.read(1);

        // Let player dismount if is a passenger of a packet stand.
        models:
        for (Model model : plugin.getModelManager().getModels()) {
            for (PacketStand stand : model.getStands().values()) {
                if (dismount && stand.isPassenger(player)) {
                    PlayerDismountEvent dismountEvent = new PlayerDismountEvent(player, stand, model);
                    plugin.getServer().getPluginManager().callEvent(dismountEvent);
                    if (!dismountEvent.isCancelled()) stand.setPassenger(null);
                    break models;
                }
            }
        }
    }
}