package me.matsubara.blencraft.listener.protocol;

import com.cryptomorin.xseries.ReflectionUtils;
import io.netty.channel.Channel;
import me.matsubara.blencraft.BlencraftPlugin;
import me.matsubara.blencraft.event.PlayerSteerVehiclePacketEvent;
import me.matsubara.blencraft.model.Model;
import me.matsubara.blencraft.stand.PacketStand;
import me.matsubara.blencraft.util.tinyprotocol.Reflection;
import me.matsubara.blencraft.util.tinyprotocol.TinyProtocol;
import org.bukkit.entity.Player;

public final class SteerVehicle extends TinyProtocol {

    private final BlencraftPlugin plugin;

    private final Class<?> PACKET_STEER_VEHICLE = ReflectionUtils.getNMSClass("network.protocol.game", "PacketPlayInSteerVehicle");

    // Floats.
    private final Reflection.FieldAccessor<Float> sidewaysField = Reflection.getField(PACKET_STEER_VEHICLE, float.class, 0);
    private final Reflection.FieldAccessor<Float> forwardField = Reflection.getField(PACKET_STEER_VEHICLE, float.class, 1);

    // Booleans.
    private final Reflection.FieldAccessor<Boolean> jumpField = Reflection.getField(PACKET_STEER_VEHICLE, boolean.class, 0);
    private final Reflection.FieldAccessor<Boolean> unmountField = Reflection.getField(PACKET_STEER_VEHICLE, boolean.class, 1);

    public SteerVehicle(BlencraftPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public Object onPacketInAsync(Player sender, Channel channel, Object packet) {
        if (!PACKET_STEER_VEHICLE.isInstance(packet)) return super.onPacketInAsync(sender, channel, packet);

        // Get required values from the packet.
        float sideways = sidewaysField.get(packet);
        float forward = forwardField.get(packet);

        boolean jump = jumpField.get(packet);
        boolean dismount = unmountField.get(packet);

        // Let player dismount if is a passenger of a packet stand.
        models:
        for (Model model : plugin.getModelManager().getModels()) {
            for (PacketStand stand : model.getStands().values()) {
                if (!stand.isPassenger(sender)) continue;

                PlayerSteerVehiclePacketEvent steerEvent = new PlayerSteerVehiclePacketEvent(
                        sender,
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

        return null;
    }
}