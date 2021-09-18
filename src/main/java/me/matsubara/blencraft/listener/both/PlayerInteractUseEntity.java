package me.matsubara.blencraft.listener.both;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.cryptomorin.xseries.ReflectionUtils;
import me.matsubara.blencraft.BlencraftPlugin;
import me.matsubara.blencraft.command.Main;
import me.matsubara.blencraft.event.PlayerInteractPacketEvent;
import me.matsubara.blencraft.gui.GUI;
import me.matsubara.blencraft.model.Model;
import me.matsubara.blencraft.model.stand.StandSettings;
import me.matsubara.blencraft.stand.PacketStand;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class PlayerInteractUseEntity extends PacketAdapter implements Listener {

    private final BlencraftPlugin plugin;

    public PlayerInteractUseEntity(BlencraftPlugin plugin) {
        super(plugin, ListenerPriority.HIGHEST, PacketType.Play.Client.USE_ENTITY);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        int entityId = event.getPacket().getIntegers().readSafely(0);

        EnumWrappers.EntityUseAction action;
        if (ReflectionUtils.VER > 16) {
            action = event.getPacket().getEnumEntityUseActions().readSafely(0).getAction();
        } else {
            action = event.getPacket().getEntityUseActions().readSafely(0);
        }

        boolean isLeft = action == EnumWrappers.EntityUseAction.ATTACK;

        Player player = event.getPlayer();

        // Only call interact packet event if the player isn't building.
        if (!plugin.getModelManager().isBuilding(player)) {

            for (Model model : plugin.getModelManager().getModels()) {
                PacketStand stand = model.getById(entityId);
                if (stand == null) continue;

                PlayerInteractPacketEvent interactEvent = new PlayerInteractPacketEvent(
                        player,
                        stand,
                        model,
                        PlayerInteractPacketEvent.InteractType.getByBoolean(isLeft));

                plugin.getServer().getPluginManager().callEvent(interactEvent);
                return;
            }

            return;
        }

        Model model = plugin.getModelManager().getModel(player);
        if (model == null) return;
        if (model.getById(entityId) == null) return;

        // For some reason, when left cliking a packet entity calls PlayerInteractEvent, but not when right cliking.
        if (isLeft) return;

        @SuppressWarnings("deprecation") ItemStack item = player.getItemInHand();
        if (item.getType() == Material.AIR) return;

        handle(null, model, item, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getModelManager().isBuilding(player)) return;

        Model model = plugin.getModelManager().getModel(player);
        if (model == null) return;

        if (event.getAction() == Action.PHYSICAL) return;

        boolean left = event.getAction().name().startsWith("LEFT");

        ItemStack item = event.getItem();
        if (item == null) return;

        handle(event, model, item, left);
    }

    public void handle(@Nullable Cancellable event, Model model, ItemStack item, boolean left) {
        if (item.isSimilar(Main.ADD_NEW)) {
            openNewPartGUI(model);
        } else if (item.isSimilar(Main.LIST)) {
            new GUI(plugin, model, null);
        } else if (item.isSimilar(Main.ROTATION)) {
            model.getCurrentsAndThen(stand -> model.rotate(stand, left));
        } else if (item.isSimilar(Main.TYPE)) {
            model.setRotation(left);
        } else if (item.isSimilar(Main.X_AXIS) || item.isSimilar(Main.Y_AXIS) || item.isSimilar(Main.Z_AXIS)) {
            model.getCurrentsAndThen(stand -> model.move(stand, item, left));
        } else if (item.isSimilar(Main.SPEED)) {
            openSpeedGUI(model);
        } else return;

        if (event != null) event.setCancelled(true);
    }

    private void openNewPartGUI(Model model) {
        Player player = model.getBuilder();

        // Open new part GUI.
        new AnvilGUI.Builder()
                .title("New part name")
                .itemLeft(new ItemStack(Material.PAPER))
                .text("Name...")
                .onComplete((opener, text) -> {
                    if (!model.addNew(text, new StandSettings(), null, null, false)) {
                        opener.sendMessage(ChatColor.RED + "That name already exist or isn't valid, use another.");
                    } else {
                        opener.sendMessage(ChatColor.GOLD + "You've added a new part.");
                    }
                    return AnvilGUI.Response.close();
                })
                .plugin(plugin)
                .open(player);
    }

    private void openSpeedGUI(Model model) {
        Player player = model.getBuilder();

        // Open move modifier GUI.
        new AnvilGUI.Builder()
                .title("Set current speed")
                .itemLeft(new ItemStack(Material.PAPER))
                .text("Speed...")
                .onComplete((opener, text) -> {
                    if (!text.isEmpty()) {
                        try {
                            double speed = Double.parseDouble(text);
                            model.setSpeed(speed);
                            player.sendMessage(ChatColor.YELLOW + "Current speed changed to " + speed + "!");
                        } catch (NumberFormatException ignored) {
                            player.sendMessage(ChatColor.RED + "You must specify a number!");
                        }
                    }
                    return AnvilGUI.Response.close();
                })
                .plugin(plugin)
                .open(player);
    }
}