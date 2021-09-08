package me.matsubara.blencraft.command;

import me.matsubara.blencraft.BlencraftPlugin;
import me.matsubara.blencraft.model.Model;
import me.matsubara.blencraft.util.ItemBuilder;
import me.matsubara.blencraft.util.PluginUtils;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class Main implements CommandExecutor, TabCompleter {

    private final BlencraftPlugin plugin;

    public final static ItemStack ADD_NEW = new ItemBuilder("ARMOR_STAND").setDisplayName("&aAdd new").build();

    public final static ItemStack LIST = new ItemBuilder("CHEST").setDisplayName("&eOpen list").build();

    public final static ItemStack ROTATION = new ItemBuilder("COMPASS").setDisplayName("&bRotate").build();

    public final static ItemStack TYPE = new ItemBuilder("REDSTONE").setDisplayName("&9Rotation").build();

    public final static ItemStack SPEED = new ItemBuilder("CLOCK").setDisplayName("&3Speed").build();

    public final static ItemStack X_AXIS =
            new ItemBuilder("BANNER")
                    .setBannerColor(DyeColor.RED)
                    .addItemFlags(ItemFlag.HIDE_POTION_EFFECTS)
                    .setDisplayName("&c&lX")
                    .build();

    public final static ItemStack Y_AXIS =
            new ItemBuilder("BANNER")
                    .setBannerColor(DyeColor.LIME)
                    .addItemFlags(ItemFlag.HIDE_POTION_EFFECTS)
                    .setDisplayName("&a&lY")
                    .build();

    public final static ItemStack Z_AXIS =
            new ItemBuilder("BANNER")
                    .setBannerColor(DyeColor.BLUE)
                    .addItemFlags(ItemFlag.HIDE_POTION_EFFECTS)
                    .setDisplayName("&9&lZ")
                    .build();

    public Main(BlencraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can't be executed from the console!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args.length > 2) {
            player.sendMessage(ChatColor.RED + "More arguments!");
            return true;
        }

        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "leave":
                    // Leave modeling.
                    leave(player);
                    break;
                case "save":
                    // If the player isn't build mode, return.
                    if (!plugin.getModelManager().isBuilding(player)) {
                        player.sendMessage(ChatColor.RED + "You're not in build mode.");
                        return true;
                    }

                    // Save model changes async.
                    player.sendMessage(ChatColor.RED + "Saving model, please, wait before leaving...");
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> plugin.getModelManager().getModelAndThen(player, Model::saveChanges));
                    break;
                case "reload":
                    // Reload plugin.
                    plugin.reloadConfig();
                    player.sendMessage(ChatColor.GOLD + "Reloaded!");
                    break;
                case "items":
                    setHotbarItems(player);
                    break;
                case "heads":
                    File heads = new File(plugin.getDataFolder(), "heads.yml");
                    if (!heads.exists()) {
                        player.sendMessage(ChatColor.RED + "You must create the file and add textures.");
                        return true;
                    } else {
                        FileConfiguration configuration = new YamlConfiguration();
                        try {
                            configuration.load(heads);

                            List<String> list = configuration.getStringList("heads");
                            if (list.isEmpty()) {
                                player.sendMessage(ChatColor.RED + "You must add textures first.");
                                return true;
                            }

                            Location target = player.getTargetBlock(null, 5).getLocation().add(0.0d, 1.0d, 0.0d);
                            target.getBlock().setType(Material.CHEST);

                            Chest chest = (Chest) target.getBlock().getState();

                            for (String texture : list) {
                                ItemStack item = PluginUtils.createHead(texture, true);
                                if (item != null) chest.getBlockInventory().addItem(item);
                            }

                        } catch (IOException | InvalidConfigurationException exception) {
                            player.sendMessage(ChatColor.RED + "An error ocurred when loading heads.yml file.");
                        }
                    }
                    break;
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("head")) {
            player.getInventory().addItem(new ItemBuilder(args[1], true).build());
            return true;
        }

        if (args[0].equalsIgnoreCase("create") || args[0].equalsIgnoreCase("load")) {
            boolean isCreate = args[0].equalsIgnoreCase("create");

            // If the player is in build mode, return.
            if (plugin.getModelManager().isBuilding(player)) {
                player.sendMessage(ChatColor.RED + "You're already in build mode!");
                return true;
            }

            // If the player has any item in his inventory, return.
            if (!hasEmptyInventory(player)) {
                player.sendMessage(ChatColor.RED + "Your inventory must be empty.");
                return true;
            }

            boolean exists = plugin.getModelManager().exists(args[1]);

            // If the model already exists and is create command, return.
            if (isCreate && exists) {
                player.sendMessage("That model already exists.");
                return true;
            }

            // If the model doesn't exist and is load command, return.
            if (!isCreate && !exists) {
                player.sendMessage(ChatColor.RED + "That model doesn't exists.");
                return true;
            }

            if (!isCreate) player.sendMessage(ChatColor.GOLD + "Loading model...");
            createModel(player, args[1]);
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("create", "save", "reload", "leave", "load"), new ArrayList<>());
        } else if (args.length == 2 && args[0].equalsIgnoreCase("load")) {
            return StringUtil.copyPartialMatches(args[1], getModelList(), new ArrayList<>());
        }
        return null;
    }

    private List<String> getModelList() {
        File modelsFolder = new File(plugin.getModelManager().getModelFolder());
        if (!modelsFolder.exists()) return Collections.emptyList();

        String[] files = modelsFolder.list((directory, name) -> name.endsWith(".yml"));
        if (files != null) {
            return Arrays.stream(files).map(name -> name.replace(".yml", "")).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public void leave(Player player) {
        // If the player isn't build mode, return.
        if (!plugin.getModelManager().isBuilding(player)) {
            player.sendMessage(ChatColor.RED + "You're not in build mode.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "You've leaved the build mode.");
        player.getInventory().clear();

        plugin.getModelManager().getModelAndThen(player, model -> {
            // Remove player instance and remove stands.
            model.setBuilder(null);
            model.kill();

            // Remove model from the manager.
            plugin.getModelManager().getModels().remove(model);
        });
    }

    public void createModel(Player player, String modelName) {
        // Get the top block of the target, and put an obsidian block.
        Location targetLocation = player.getTargetBlock(null, 5).getLocation();

        Location center = targetLocation.add(0.0d, 1.0d, 0.0d);
        center.getBlock().setType(Material.OBSIDIAN);

        // Set center to top block of obsidian.
        center = center.clone().add(0.5d, 1.0d, 0.5d);

        setHotbarItems(player);

        plugin.getModelManager().newModel(new Model(plugin, player, modelName, null, center, player.getLocation().getYaw()));
    }

    public void setHotbarItems(Player player) {
        PlayerInventory inventory = player.getInventory();

        inventory.setItem(0, ADD_NEW);
        inventory.setItem(1, LIST);
        inventory.setItem(2, ROTATION);
        inventory.setItem(3, TYPE);
        inventory.setItem(4, SPEED);
        // 5 - EMPTY
        inventory.setItem(6, X_AXIS);
        inventory.setItem(7, Y_AXIS);
        inventory.setItem(8, Z_AXIS);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean hasEmptyInventory(Player player) {
        return player.getInventory().firstEmpty() == 0;
    }
}