package me.matsubara.blencraft.gui;

import me.matsubara.blencraft.BlencraftPlugin;
import me.matsubara.blencraft.model.Model;
import me.matsubara.blencraft.stand.PacketStand;
import me.matsubara.blencraft.util.InventoryUpdate;
import me.matsubara.blencraft.util.ItemBuilder;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class GUI implements InventoryHolder, Listener {

    // The instance of the main class.
    private final BlencraftPlugin plugin;

    // The player viewing this inventory.
    private final Player player;

    // The inventory being used.
    private final Inventory inventory;

    // The stands to show.
    private final List<PacketStand> stands;

    // Keyword to search.
    private final String keyword;

    // The current page.
    private int current;

    // The max amount of pages.
    private int pages;

    // The slots to show the content.
    private final static int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34};

    // The slot to put page navigator items and other stuff.
    private final static int[] HOTBAR = {37, 38, 39, 40, 41, 42, 43};

    public final static ItemStack PREVIOUS =
            new ItemBuilder("bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9", true)
                    .setDisplayName("&aPrevious")
                    .build();

    public final static ItemStack NEXT =
            new ItemBuilder("19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf", true)
                    .setDisplayName("&aNext")
                    .build();

    public final static ItemStack SETTINGS_SELECTION =
            new ItemBuilder("ANVIL")
                    .setDisplayName("&7Settings selection")
                    .build();

    public final static ItemStack DUPLICATE_SELECTION =
            new ItemBuilder("SLIME_BALL")
                    .setDisplayName("&5Duplicate selection")
                    .build();

    public final static ItemStack SEARCH =
            new ItemBuilder("COMPASS")
                    .setDisplayName("&9Search")
                    .build();

    public final static ItemStack SELECT_ALL_CLEAR_SELECTION =
            new ItemBuilder("BUCKET")
                    .setDisplayName("&aSelect all &7/ &4Clear selection")
                    .build();

    public final static ItemStack RENAME_PART =
            new ItemBuilder("BOOK_AND_QUILL")
                    .setDisplayName("&3Rename part(s)")
                    .build();

    public final static ItemStack CLOSE =
            new ItemBuilder("BARRIER")
                    .setDisplayName("&cClose")
                    .build();

    public GUI(JavaPlugin plugin, Model model, @Nullable String keyword) {
        this.plugin = (BlencraftPlugin) plugin;
        this.player = model.getBuilder();
        this.inventory = plugin.getServer().createInventory(this, 54);

        // Copy list of tameables, shouldn't be empty.
        this.stands = new ArrayList<>(model.getStands().values());

        this.keyword = keyword;

        if (keyword != null && !keyword.isEmpty()) {
            stands.removeIf(stand -> !model.getNameOf(stand).toLowerCase().contains(keyword.toLowerCase()));
        }

        if (stands.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No match!");
            return;
        }

        player.openInventory(inventory);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::updateInventory);
    }

    public void updateInventory() {
        inventory.clear();

        // Page formula.
        pages = (int) (Math.ceil((double) stands.size() / SLOTS.length));

        ItemStack background = new ItemBuilder("GRAY_STAINED_GLASS_PANE")
                .setDisplayName("&7")
                .build();

        // Set background items, except the last, since we're putting the close item there.
        for (int i = 0; i < 53; i++) {
            if (ArrayUtils.contains(SLOTS, i) || ArrayUtils.contains(HOTBAR, i)) continue;
            // Set background item in the current slot from the loop.
            inventory.setItem(i, background);
        }

        // If the current page isn't 0 (first page), show the previous page item in slot 37.
        if (current > 0) inventory.setItem(37, PREVIOUS);

        // If the current page isn't the last one, show the next page item in slot 43.
        if (current < pages - 1) inventory.setItem(43, NEXT);

        // *** Set some random stuff item between 37 (previous item) and 43 (next item). ***
        inventory.setItem(38, SETTINGS_SELECTION);
        inventory.setItem(39, DUPLICATE_SELECTION);
        inventory.setItem(40, SEARCH);
        inventory.setItem(41, SELECT_ALL_CLEAR_SELECTION);
        inventory.setItem(42, RENAME_PART);

        // Set close inventory item in slot 53 (last).
        inventory.setItem(53, CLOSE);

        // Assigning slots.
        Map<Integer, Integer> slotIndex = new HashMap<>();
        for (int i : SLOTS) {
            slotIndex.put(ArrayUtils.indexOf(SLOTS, i), i);
        }

        // Where to start.
        int startFrom = current * SLOTS.length;

        boolean isLastPage = current == pages - 1;

        Model model = plugin.getModelManager().getModel(player);
        if (model == null) return;

        for (int index = 0, aux = startFrom; isLastPage ? (index < stands.size() - startFrom) : (index < SLOTS.length); index++, aux++) {
            PacketStand stand = stands.get(aux);

            ItemStack item = new ItemStack(Material.ARMOR_STAND);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // If the current stand is the selected one, show with enchant glow.
                if (model.getCurrentIds().contains(stand.getEntityId())) {
                    meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + model.getNameOf(stand) + " (SELECTED)");
                    meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                } else {
                    meta.setDisplayName(ChatColor.GRAY + model.getNameOf(stand));
                }
                item.setItemMeta(meta);
            }

            inventory.setItem(slotIndex.get(index), new ItemBuilder(item)
                    .setLore(Arrays.asList(
                            "&7Represent a part of this model.",
                            "&7",
                            "&6Click to select part.",
                            "&6Right click to edit part.",
                            "&7",
                            "&6Shift click to delete this part.",
                            "&6Shift right click to duplicate this part."))
                    .modifyNBT("standId", stand.getEntityId())
                    .build());
        }

        // Update inventory title to show the current page.
        String title = "Stands (" + (current + 1) + "/" + pages + ")";
        InventoryUpdate.updateInventory(plugin, player, title);
    }

    public void previousPage(boolean isShiftClick) {
        // If is shift click, go to the first page.
        if (isShiftClick) {
            current = 0;
            updateInventory();
            return;
        }

        // Go to the previous page.
        current--;
        updateInventory();
    }

    public void nextPage(boolean isShiftClick) {
        // If is shift click, go to the last page.
        if (isShiftClick) {
            current = pages - 1;
            updateInventory();
            return;
        }

        // Go to the next page.
        current++;
        updateInventory();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}