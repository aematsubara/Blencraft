package me.matsubara.blencraft.gui;

import me.matsubara.blencraft.stand.PacketStand;
import me.matsubara.blencraft.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SettingsGUI implements InventoryHolder {

    private final Inventory inventory;
    private final PacketStand stand;

    public final static ItemStack EQUIPMENT = new ItemBuilder("DIAMOND_CHESTPLATE")
            .setDisplayName("&6Equipment")
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();

    public final static ItemStack INVISIBLE = new ItemBuilder("POTION")
            .setDisplayName("&bInvisible")
            .setBasePotionData(PotionType.INVISIBILITY)
            .addItemFlags(ItemFlag.HIDE_POTION_EFFECTS)
            .build();

    public final static ItemStack ARMS = new ItemBuilder("STICK")
            .setDisplayName("&dArms")
            .build();

    public final static ItemStack BASEPLATE = new ItemBuilder("OAK_PRESSURE_PLATE")
            .setDisplayName("&eBaseplate")
            .build();

    public final static ItemStack SMALL = new ItemBuilder("RED_MUSHROOM")
            .setDisplayName("&aSmall")
            .build();

    public final static ItemStack MARKER = new ItemBuilder("REDSTONE_TORCH")
            .setDisplayName("&2Marker")
            .build();

    public final static ItemStack FIRE = new ItemBuilder("FLINT_AND_STEEL")
            .setDisplayName("&cFire")
            .build();

    public final static ItemStack MOUNT = new ItemBuilder("SADDLE")
            .setDisplayName("&3Sit")
            .build();

    public final static ItemStack CUSTOM_NAME = new ItemBuilder("NAME_TAG")
            .setDisplayName("&1Custom name")
            .build();

    public final static ItemStack CUSTOM_NAME_VISIBLE = new ItemBuilder("LEVER")
            .setDisplayName("&5Custom name visible")
            .build();

    public final static ItemStack CLOSE = new ItemBuilder("BARRIER")
            .setDisplayName("&4Close")
            .build();

    public SettingsGUI(Player player, @Nullable PacketStand stand) {
        this.inventory = Bukkit.createInventory(this, 18, "Settings".concat(stand != null ? "" : " (Multiple)"));
        this.inventory.setItem(0, EQUIPMENT);
        this.inventory.setItem(1, INVISIBLE);
        this.inventory.setItem(2, ARMS);
        this.inventory.setItem(3, BASEPLATE);
        this.inventory.setItem(4, SMALL);
        this.inventory.setItem(5, MARKER);
        this.inventory.setItem(6, FIRE);
        this.inventory.setItem(7, MOUNT);
        this.inventory.setItem(8, CLOSE);
        this.inventory.setItem(9, CUSTOM_NAME);
        this.inventory.setItem(10, CUSTOM_NAME_VISIBLE);
        this.stand = stand;
        player.openInventory(inventory);
    }

    public PacketStand getStand() {
        return stand;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}