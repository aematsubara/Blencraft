package me.matsubara.blencraft.gui;

import me.matsubara.blencraft.model.stand.StandSettings;
import me.matsubara.blencraft.stand.PacketStand;
import me.matsubara.blencraft.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class EquipmentGUI implements InventoryHolder {

    private final Inventory inventory;
    private final PacketStand stand;

    public final static ItemStack SAVE = new ItemBuilder("BOOK_AND_QUILL")
            .setDisplayName("&aSave")
            .build();

    public final static ItemStack CLOSE = new ItemBuilder("BARRIER")
            .setDisplayName("&4Close")
            .build();

    public final static ItemStack SPACE = new ItemBuilder("GRAY_STAINED_GLASS_PANE")
            .setDisplayName("&7")
            .build();

    public EquipmentGUI(Player player, PacketStand stand) {
        this.inventory = Bukkit.createInventory(this, 9, "Equipment");
        this.stand = stand;

        this.inventory.setItem(0, SAVE);

        StandSettings settings = stand.getSettings();

        if (settings.getMainHand() != null) this.inventory.setItem(1, settings.getMainHand());
        if (settings.getHelmet() != null) this.inventory.setItem(2, settings.getHelmet());
        if (settings.getChestplate() != null) this.inventory.setItem(3, settings.getChestplate());
        if (settings.getLeggings() != null) this.inventory.setItem(4, settings.getLeggings());
        if (settings.getBoots() != null) this.inventory.setItem(5, settings.getBoots());
        if (settings.getOffHand() != null) this.inventory.setItem(6, settings.getOffHand());

        this.inventory.setItem(7, SPACE);
        this.inventory.setItem(8, CLOSE);
        player.openInventory(inventory);
    }

    public PacketStand getStand() {
        return stand;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}