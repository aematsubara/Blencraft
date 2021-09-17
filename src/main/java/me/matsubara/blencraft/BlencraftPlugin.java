package me.matsubara.blencraft;

import com.cryptomorin.xseries.ReflectionUtils;
import me.matsubara.blencraft.command.Main;
import me.matsubara.blencraft.listener.InventoryClick;
import me.matsubara.blencraft.listener.InventoryClose;
import me.matsubara.blencraft.listener.both.PlayerInteractUseEntity;
import me.matsubara.blencraft.listener.protocol.SteerVehicle;
import me.matsubara.blencraft.manager.ModelManager;
import me.matsubara.blencraft.manager.StandManager;
import me.matsubara.blencraft.model.Model;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlencraftPlugin extends JavaPlugin {

    // Managers.
    private ModelManager modelManager;
    private StandManager standManager;

    @Override
    public void onEnable() {
        // Disable plugin if server version is older than 1.12.
        if (ReflectionUtils.VER < 12) {
            getLogger().info("This plugin only works from 1.12 and up, disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register bukkit events.
        getServer().getPluginManager().registerEvents(new InventoryClick(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClose(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractUseEntity(this), this);

        // Register packet events.
        new SteerVehicle(this);

        // Register main command.
        PluginCommand mainCommand = getCommand("blencraft");
        if (mainCommand != null) {
            Main main = new Main(this);
            mainCommand.setExecutor(main);
            mainCommand.setTabCompleter(main);
        }

        // Initialize managers.
        modelManager = new ModelManager(this);
        standManager = new StandManager(this);

        saveResource("heads.yml", false);
    }

    @Override
    public void onDisable() {
        // If disabling on startup, prevent errors in console.
        if (modelManager != null) modelManager.getModels().forEach(Model::kill);
    }

    public ModelManager getModelManager() {
        return modelManager;
    }

    public StandManager getStandManager() {
        return standManager;
    }
}