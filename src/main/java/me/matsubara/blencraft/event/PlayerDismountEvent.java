package me.matsubara.blencraft.event;

import me.matsubara.blencraft.model.Model;
import me.matsubara.blencraft.stand.PacketStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class PlayerDismountEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final PacketStand stand;
    private final Model model;

    private boolean cancelled;

    public PlayerDismountEvent(Player player, PacketStand stand, Model model) {
        super(true);
        this.player = player;
        this.stand = stand;
        this.model = model;
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}