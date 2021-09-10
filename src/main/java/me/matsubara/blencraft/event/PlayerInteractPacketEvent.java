package me.matsubara.blencraft.event;

import me.matsubara.blencraft.model.Model;
import me.matsubara.blencraft.stand.PacketStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class PlayerInteractPacketEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PacketStand stand;
    private final Model model;
    private final InteractType type;

    public PlayerInteractPacketEvent(Player player, PacketStand stand, Model model, InteractType type) {
        super(true);
        this.player = player;
        this.stand = stand;
        this.model = model;
        this.type = type;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public Player getPlayer() {
        return player;
    }

    public PacketStand getStand() {
        return stand;
    }

    public Model getModel() {
        return model;
    }

    public InteractType getType() {
        return type;
    }

    public enum InteractType {
        LEFT_CLICK,
        RIGHT_CLICK;

        public static InteractType getByBoolean(boolean isLeft) {
            return isLeft ? LEFT_CLICK : RIGHT_CLICK;
        }
    }
}