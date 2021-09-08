package me.matsubara.blencraft.listener;

import me.matsubara.blencraft.BlencraftPlugin;
import me.matsubara.blencraft.gui.EquipmentGUI;
import me.matsubara.blencraft.gui.GUI;
import me.matsubara.blencraft.gui.SettingsGUI;
import me.matsubara.blencraft.model.Model;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public final class InventoryClose implements Listener {

    private final BlencraftPlugin plugin;

    public InventoryClose(BlencraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer().getType() != EntityType.PLAYER) return;

        Player player = (Player) event.getPlayer();

        Inventory inventory = event.getInventory();
        if (inventory.getHolder() == null) return;

        if (inventory.getHolder() instanceof EquipmentGUI) {
            runTask(() -> new SettingsGUI(player, ((EquipmentGUI) inventory.getHolder()).getStand()));
        } else if (inventory.getHolder() instanceof SettingsGUI) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Inventory top = player.getOpenInventory().getTopInventory();
                if (top.getHolder() != null && top.getHolder() instanceof EquipmentGUI) return;

                Model model = plugin.getModelManager().getModel(player);
                if (model != null) runTask(() -> new GUI(plugin, model, null));
            }, 2L);
        }
    }

    private void runTask(Runnable runnable) {
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }
}