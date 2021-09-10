package me.matsubara.blencraft.manager;

import me.matsubara.blencraft.BlencraftPlugin;
import me.matsubara.blencraft.model.Model;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class ModelManager {

    private final BlencraftPlugin plugin;
    private final List<Model> models;

    public ModelManager(BlencraftPlugin plugin) {
        this.plugin = plugin;
        this.models = new ArrayList<>();
    }

    public UUID newModel(@Nullable Player builder, String fileName, @Nullable String fileFolder, Location center, float yaw) {
        Model model = new Model(plugin, builder, fileName, fileFolder, center, yaw);
        models.add(model);
        return model.getUniqueId();
    }

    public boolean isBuilding(Player player) {
        for (Model model : models) {
            if (model.getBuilder() == null) continue;
            if (model.getBuilder().equals(player)) return true;
        }
        return false;
    }

    public Model getModel(UUID uuid) {
        for (Model model : models) {
            if (model.getUniqueId().equals(uuid)) return model;
        }
        return null;
    }

    public Model getModel(Player player) {
        for (Model model : models) {
            if (model.getBuilder() == null) continue;
            if (model.getBuilder().equals(player)) return model;
        }
        return null;
    }

    public void getModelAndThen(Player player, Consumer<Model> consumer) {
        if (!isBuilding(player)) return;

        Model model = getModel(player);
        consumer.accept(model);
    }

    public boolean exists(String name) {
        return new File(getModelFolder(), name + ".yml").exists();
    }

    public List<Model> getModels() {
        return models;
    }

    public String getModelFolder() {
        return plugin.getDataFolder() + File.separator + "models";
    }
}