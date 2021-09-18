package me.matsubara.blencraft;

import com.comphenix.protocol.ProtocolLibrary;
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

import java.io.File;

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

        // Disable plugin if ProtocolLib isn't installed.
        if (!getServer().getPluginManager().isPluginEnabled("ProtocolLib")) {
            getLogger().info("This plugin depends on ProtocolLib, disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PlayerInteractUseEntity useEntity = new PlayerInteractUseEntity(this);

        // Register protocol events.
        ProtocolLibrary.getProtocolManager().addPacketListener(useEntity);
        ProtocolLibrary.getProtocolManager().addPacketListener(new SteerVehicle(this));

        // Register bukkit events.
        getServer().getPluginManager().registerEvents(new InventoryClick(this), this);
        getServer().getPluginManager().registerEvents(new InventoryClose(this), this);
        getServer().getPluginManager().registerEvents(useEntity, this);

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

        File headsFile = new File(getDataFolder(), "heads.yml");
        if (!headsFile.exists()) saveResource("heads.yml", false);
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