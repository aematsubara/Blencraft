package me.matsubara.blencraft.manager;

import me.matsubara.blencraft.model.Model;
import me.matsubara.blencraft.BlencraftPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void saveToModels(@NotNull JavaPlugin resourcePlugin, @NotNull String resourcePath) {
        if (resourcePath.equals("")) {
            throw new IllegalArgumentException("Resource path cannot be null or empty.");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream inputStream = resourcePlugin.getResource(resourcePath);
        if (inputStream == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found.");
        }

        File outFile = new File(getModelFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(getModelFolder(), resourcePath.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        if (outFile.exists()) return;

        try {
            OutputStream outputStream = new FileOutputStream(outFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, exception);
        }
    }
}