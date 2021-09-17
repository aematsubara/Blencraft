package me.matsubara.blencraft.event;

import me.matsubara.blencraft.model.Model;
import me.matsubara.blencraft.stand.PacketStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class PlayerSteerVehiclePacketEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PacketStand stand;
    private final Model model;

    private final Direction sideways, forward;
    private final boolean isJump, isUnmount;

    private boolean cancelled;

    public PlayerSteerVehiclePacketEvent(Player player, PacketStand stand, Model model, Direction sideways, Direction forward, boolean isJump, boolean isUnmount) {
        super(true);
        this.player = player;
        this.stand = stand;
        this.model = model;
        this.sideways = sideways;
        this.forward = forward;
        this.isJump = isJump;
        this.isUnmount = isUnmount;
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

    public Direction getSideways() {
        return sideways;
    }

    public Direction getForward() {
        return forward;
    }

    public boolean isJump() {
        return isJump;
    }

    public boolean isUnmount() {
        return isUnmount;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        // Only for cancel unmount.
        this.cancelled = cancelled;
    }

    public enum Direction {
        NONE,
        LEFT_SIDE,
        RIGHT_SIDE,
        BACKWARD,
        FORWARD;

        public boolean isNone() {
            return this == NONE;
        }

        public boolean isLeftSide() {
            return this == LEFT_SIDE;
        }

        public boolean isRightSide() {
            return this == RIGHT_SIDE;
        }

        public boolean isBackward() {
            return this == BACKWARD;
        }

        public boolean isForward() {
            return this == FORWARD;
        }

        public static Direction getSideways(float sideways) {
            return (sideways == 0.0f) ? NONE : (sideways > 0.0f) ? LEFT_SIDE : RIGHT_SIDE;
        }

        public static Direction getForward(float forward) {
            return (forward == 0.0f) ? NONE : (forward > 0.0f) ? FORWARD : BACKWARD;
        }
    }
}