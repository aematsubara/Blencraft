package me.matsubara.blencraft.listener;

import com.cryptomorin.xseries.XSound;
import me.matsubara.blencraft.BlencraftPlugin;
import me.matsubara.blencraft.gui.EquipmentGUI;
import me.matsubara.blencraft.gui.GUI;
import me.matsubara.blencraft.gui.SettingsGUI;
import me.matsubara.blencraft.model.Model;
import me.matsubara.blencraft.stand.PacketStand;
import me.matsubara.blencraft.util.NBTEditor;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public final class InventoryClick implements Listener {

    private final BlencraftPlugin plugin;

    public InventoryClick(BlencraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked().getType() != EntityType.PLAYER) return;

        Player player = (Player) event.getWhoClicked();

        if (!plugin.getModelManager().isBuilding(player)) return;

        Model model = plugin.getModelManager().getModel(player);
        // Shouldn't be null, just to remove warning.
        if (model == null) return;

        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().getHolder() == null) return;

        ItemStack current = event.getCurrentItem();
        if (current == null) return;

        if (event.getClickedInventory().getHolder() instanceof GUI) {
            handleList(event, model);
        } else if (event.getClickedInventory().getHolder() instanceof SettingsGUI) {
            handleSettings(event, model);
        } else if (event.getClickedInventory().getHolder() instanceof EquipmentGUI) {
            handleEquipment(event);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void handleList(InventoryClickEvent event, Model model) {
        GUI gui = (GUI) event.getClickedInventory().getHolder();

        Player player = (Player) event.getWhoClicked();
        ItemStack current = event.getCurrentItem();

        if (current.isSimilar(GUI.PREVIOUS)) {
            gui.previousPage(event.getClick().isShiftClick());
        } else if (current.isSimilar(GUI.NEXT)) {
            gui.nextPage(event.getClick().isShiftClick());
        } else if (current.isSimilar(GUI.SEARCH)) {
            new AnvilGUI.Builder()
                    .onComplete((clicker, text) -> {
                        if (text.isEmpty()) {
                            clicker.sendMessage(ChatColor.RED + "You need to specify a keyword.");
                        } else {
                            new GUI(plugin, model, text);
                        }
                        return AnvilGUI.Response.close();
                    })
                    .title("Search...")
                    .text("Search...")
                    .itemLeft(new ItemStack(Material.PAPER))
                    .plugin(plugin)
                    .open((Player) event.getWhoClicked());
        } else if (current.isSimilar(GUI.SETTINGS_SELECTION)) {
            if (model.getCurrentIds().size() > 1) {
                new SettingsGUI(player, null);
            } else {
                player.sendMessage(ChatColor.RED + "You need to select at least 2 stands to use this option!");
                plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
            }
        } else if (current.isSimilar(GUI.SELECT_ALL_CLEAR_SELECTION)) {
            if (event.getClick() == ClickType.LEFT) {
                if (model.getCurrentIds().size() == model.getStands().size()) {
                    player.sendMessage(ChatColor.RED + "Already selected all!");
                } else {
                    model.getCurrentIds().clear();
                    model.getStands().values().forEach(stand -> {
                        model.getCurrentIds().add(stand.getEntityId());
                        model.glowSelected(stand);
                    });
                    player.sendMessage(ChatColor.GOLD + "Selected all parts!");
                    gui.updateInventory();
                    event.setCancelled(true);
                    return;
                }
            } else if (event.getClick() == ClickType.RIGHT) {
                // Clear selections and update inventory.
                if (model.getCurrentIds().isEmpty()) {
                    player.sendMessage(ChatColor.RED + "No selections to clear!");
                } else {
                    model.getCurrentIds().clear();
                    player.sendMessage(ChatColor.RED + "Cleared selections!");
                    gui.updateInventory();
                    event.setCancelled(true);
                    return;
                }
            }
            plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
        } else if (current.isSimilar(GUI.RENAME_PART)) {
            if (model.getCurrentIds().isEmpty()) {
                player.sendMessage(ChatColor.RED + "No selections to rename!");
                plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
            } else {
                openRenameGUI(player, model);
            }
        } else if (current.isSimilar(GUI.CLOSE)) {
            plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
        } else if (current.isSimilar(GUI.DUPLICATE_SELECTION)) {
            if (model.getCurrentIds().size() > 0) {
                if (model.getCurrentIds().size() == 1) {
                    // Duplicate single.
                    PacketStand stand = model.getById(model.getCurrentIds().get(0));
                    if (stand != null) openAnvilGUI(player, model, stand);
                    event.setCancelled(true);
                    return;
                }

                // Create a copy before clearing selections.
                List<Integer> copy = new ArrayList<>(model.getCurrentIds());

                // Clear selections before duplicating.
                model.getCurrentIds().clear();

                model.getCurrentsAndThen(stand -> {
                    String name = model.getNameOf(stand);
                    do {
                        name = name.concat("_2");
                    } while (model.getStands().containsKey(name) || model.getTemp().containsKey(name));
                    handleDuplicate(model, name, stand, true);
                }, copy);
                model.getStands().putAll(model.getTemp());
                model.getTemp().clear();
                player.sendMessage(ChatColor.GOLD + "Selected parts duplicated and selected!");
            } else {
                player.sendMessage(ChatColor.RED + "You need to select at least 1 stand to use this option!");
            }
            plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
        } else if (current.getType() == Material.ARMOR_STAND) {
            PacketStand stand = model.getById(NBTEditor.getInt(current, "standId"));

            if (event.getClick() == ClickType.LEFT) {
                // Select.
                ItemMeta meta = current.getItemMeta();
                if (meta == null) return;

                int entityId = stand.getEntityId();

                if (model.getStands().size() > 1) {
                    // Set glowing effect and add/remove to selected.
                    if (model.getCurrentIds().contains(entityId)) {
                        model.getCurrentIds().remove((Integer) entityId);

                        // Show glow to the stands left.
                        model.getCurrentsAndThen(model::glowSelected);

                        meta.setDisplayName(ChatColor.GRAY + model.getNameOf(stand));
                        meta.removeEnchant(Enchantment.ARROW_DAMAGE);
                    } else {
                        model.getCurrentIds().add(entityId);

                        // Show glow to new selected.
                        model.glowSelected(model.getById(entityId));

                        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + model.getNameOf(stand) + " (SELECTED)");
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                        meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
                    }
                    current.setItemMeta(meta);
                } else {
                    if (!model.getCurrentIds().contains(entityId)) {
                        player.sendMessage(ChatColor.GOLD + "Part selected!");
                        model.getCurrentIds().add(entityId);
                    } else {
                        player.sendMessage(ChatColor.RED + "Part already selected!");
                    }
                    plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
                }

                // Whenever we add numbers, we sort them ascending.
                if (!model.getCurrentIds().isEmpty()) Collections.sort(model.getCurrentIds());

                XSound.play(player, "BLOCK_WOODEN_PRESSURE_PLATE_CLICK_ON");

            } else if (event.getClick() == ClickType.RIGHT) {
                // Settings.
                new SettingsGUI(player, stand);
            } else if (event.getClick() == ClickType.SHIFT_LEFT) {
                String currentName = model.getNameOf(stand);

                List<String> keys = new ArrayList<>(model.getStands().keySet());

                int indexof = keys.indexOf(currentName);
                // First.
                if (indexof == 0) {
                    event.setCancelled(true);
                    return;
                }

                Collections.swap(keys, indexof, indexof - 1);

                for (String key : keys) {
                    model.getTemp().put(key, model.getStands().get(key));
                }

                model.getStands().clear();
                model.getStands().putAll(model.getTemp());
                model.getTemp().clear();

                new GUI(plugin, model, gui.getKeyword(), gui.getCurrent());
            } else if (event.getClick() == ClickType.SHIFT_RIGHT) {
                String currentName = model.getNameOf(stand);

                List<String> keys = new ArrayList<>(model.getStands().keySet());

                int indexof = keys.indexOf(currentName);
                // Last.
                if (indexof == keys.size() - 1) {
                    event.setCancelled(true);
                    return;
                }

                Collections.swap(keys, indexof, indexof + 1);

                for (String key : keys) {
                    model.getTemp().put(key, model.getStands().get(key));
                }

                model.getStands().clear();
                model.getStands().putAll(model.getTemp());
                model.getTemp().clear();

                new GUI(plugin, model, gui.getKeyword(), gui.getCurrent());
            } else if (event.getClick().name().contains("DROP")) {
                // Delete.
                model.delete(stand);
                player.sendMessage(ChatColor.GOLD + "Part deleted.");
                plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
            }
        }

        event.setCancelled(true);
    }

    @SuppressWarnings("ConstantConditions")
    public void handleSettings(InventoryClickEvent event, Model model) {
        PacketStand stand = ((SettingsGUI) event.getClickedInventory().getHolder()).getStand();

        Player player = (Player) event.getWhoClicked();
        ItemStack current = event.getCurrentItem();

        if (current.isSimilar(SettingsGUI.EQUIPMENT)) {
            if (stand != null) {
                new EquipmentGUI(player, stand);
            } else {
                event.getWhoClicked().sendMessage(ChatColor.RED + "Can't open equipment menu with multiple selection.");
                plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
            }
            event.setCancelled(true);
            return;
        } else if (current.isSimilar(SettingsGUI.INVISIBLE)) {
            handleEquipment(model, PacketStand::toggleInvisibility, stand);
        } else if (current.isSimilar(SettingsGUI.ARMS)) {
            handleEquipment(model, PacketStand::toggleArms, stand);
        } else if (current.isSimilar(SettingsGUI.BASEPLATE)) {
            handleEquipment(model, PacketStand::toggleBasePlate, stand);
        } else if (current.isSimilar(SettingsGUI.SMALL)) {
            handleEquipment(model, PacketStand::toggleSmall, stand);
        } else if (current.isSimilar(SettingsGUI.MARKER)) {
            handleEquipment(model, PacketStand::toggleMarker, stand);
        } else if (current.isSimilar(SettingsGUI.FIRE)) {
            handleEquipment(model, PacketStand::toggleFire, stand);
        } else if (current.isSimilar(SettingsGUI.MOUNT)) {
            if (stand != null) {
                stand.setPassenger(stand.isPassenger(player) ? null : player);
            } else {
                player.sendMessage(ChatColor.RED + "Can't sit with multiple selection.");
            }
            plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
            event.setCancelled(true);
        } else if (current.isSimilar(SettingsGUI.CLOSE)) {
            plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
            event.setCancelled(true);
            return;
        } else {
            // Shouldn't be called since every slot is used.
            event.setCancelled(true);
            return;
        }

        XSound.play(player, "BLOCK_WOODEN_PRESSURE_PLATE_CLICK_ON");

        event.setCancelled(true);
    }

    private void handleEquipment(Model model, Consumer<PacketStand> consumer, @Nullable PacketStand stand) {
        if (stand != null) {
            consumer.accept(stand);
        } else {
            model.getCurrentsAndThen(consumer);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void handleEquipment(InventoryClickEvent event) {
        PacketStand stand = ((EquipmentGUI) event.getClickedInventory().getHolder()).getStand();

        if (event.getCurrentItem().isSimilar(EquipmentGUI.SAVE)) {
            stand.setEquipment(event.getClickedInventory().getItem(1), PacketStand.ItemSlot.MAINHAND);
            stand.setEquipment(event.getClickedInventory().getItem(2), PacketStand.ItemSlot.HEAD);
            stand.setEquipment(event.getClickedInventory().getItem(3), PacketStand.ItemSlot.CHEST);
            stand.setEquipment(event.getClickedInventory().getItem(4), PacketStand.ItemSlot.LEGS);
            stand.setEquipment(event.getClickedInventory().getItem(5), PacketStand.ItemSlot.FEET);
            stand.setEquipment(event.getClickedInventory().getItem(6), PacketStand.ItemSlot.OFFHAND);
            event.setCancelled(true);
        } else if (event.getCurrentItem().isSimilar(EquipmentGUI.CLOSE)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> event.getWhoClicked().closeInventory());
            event.setCancelled(true);
        }
    }

    // If duplicate is null, then only works from
    private void openAnvilGUI(Player player, Model model, PacketStand duplicate) {
        new AnvilGUI.Builder()
                .title("Copy part name")
                .itemLeft(new ItemStack(Material.PAPER))
                .text("Name...")
                .onComplete((opener, text) -> {
                    handleDuplicate(model, text, duplicate, false);
                    return AnvilGUI.Response.close();
                })
                .plugin(plugin)
                .open(player);
    }

    private void openRenameGUI(Player player, Model model) {
        new AnvilGUI.Builder()
                .title("Rename part")
                .itemLeft(new ItemStack(Material.PAPER))
                .text("Name...")
                .onComplete((opener, text) -> {
                    model.rename(text);
                    return AnvilGUI.Response.close();
                })
                .plugin(plugin)
                .open(player);
    }

    private void handleDuplicate(Model model, String name, PacketStand duplicate, boolean isDuplicateSelection) {
        Player opener = model.getBuilder();
        if (opener == null) return;

        if (!model.addNew(name, duplicate.getSettings().clone(), duplicate.getLocation().clone(), null, isDuplicateSelection)) {
            opener.sendMessage(ChatColor.RED + "That name already exist or isn't valid, use another.");
        } else {
            /*if (!isDuplicateSelection) */
            opener.sendMessage(ChatColor.GOLD + "Part duplicated!");
        }
    }
}