package me.matsubara.blencraft.listener.protocol;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import me.matsubara.blencraft.BlencraftPlugin;
import me.matsubara.blencraft.event.PlayerSteerVehiclePacketEvent;
import me.matsubara.blencraft.model.Model;
import me.matsubara.blencraft.stand.PacketStand;
import org.bukkit.entity.Player;

public final class SteerVehicle extends PacketAdapter {

    private final BlencraftPlugin plugin;

    public SteerVehicle(BlencraftPlugin plugin) {
        super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.STEER_VEHICLE);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();

        // Get required values from the packet.
        StructureModifier<Float> floats = event.getPacket().getFloat();
        float sideways = floats.readSafely(0);
        float forward = floats.readSafely(1);

        StructureModifier<Boolean> booleans = event.getPacket().getBooleans();
        boolean jump = booleans.readSafely(0);
        boolean dismount = booleans.readSafely(1);

        // Let player dismount if is a passenger of a packet stand.
        models:
        for (Model model : plugin.getModelManager().getModels()) {
            for (PacketStand stand : model.getStands().values()) {
                if (!stand.isPassenger(player)) continue;

                PlayerSteerVehiclePacketEvent steerEvent = new PlayerSteerVehiclePacketEvent(
                        player,
                        stand,
                        model,
                        PlayerSteerVehiclePacketEvent.Direction.getSideways(sideways),
                        PlayerSteerVehiclePacketEvent.Direction.getForward(forward),
                        jump,
                        dismount);

                plugin.getServer().getPluginManager().callEvent(steerEvent);

                // Let player dismount if not cancelled.
                if (dismount && !steerEvent.isCancelled()) {
                    stand.setPassenger(null);
                }

                break models;
            }
        }
    }
}