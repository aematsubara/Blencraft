package me.matsubara.blencraft.model;

import com.cryptomorin.xseries.XItemStack;
import com.cryptomorin.xseries.messages.ActionBar;
import me.matsubara.blencraft.BlencraftPlugin;
import me.matsubara.blencraft.command.Main;
import me.matsubara.blencraft.model.stand.StandSettings;
import me.matsubara.blencraft.stand.PacketStand;
import me.matsubara.blencraft.util.PluginUtils;
import me.matsubara.blencraft.stand.RotationType;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class Model {

    // Instance of the plugin.
    private final BlencraftPlugin plugin;

    // The player that is building this model, if so.
    private Player builder;

    // The UUID of this model.
    private final UUID modelUniqueId;

    // Name of the model.
    private final String name;

    // Center point of the model.
    private final Location center;

    // Map with all stands associated with a name.
    private final Map<String, PacketStand> stands;

    // Temporary map for store new stands when duplicating.
    private final Map<String, PacketStand> temp;

    // The ids of the currents stands.
    private final List<Integer> currentIds;

    // Speed when moving stand.
    private double speed;

    // The current rotation type (position/pose).
    private RotationType rotation;

    // File and configuration.
    private File file;
    private FileConfiguration configuration;

    public Model(BlencraftPlugin plugin, @Nullable Player builder, String fileName, @Nullable String fileFolder, Location center, float yaw) {
        this(plugin, builder, fileName, null, fileFolder, center, yaw);
    }

    public Model(BlencraftPlugin plugin, @Nullable Player builder, String fileName, @Nullable UUID oldUniqueId, @Nullable String fileFolder, Location center, float yaw) {
        this.plugin = plugin;
        this.builder = builder;

        this.modelUniqueId = (oldUniqueId != null) ? oldUniqueId : UUID.randomUUID();
        this.name = fileName;

        this.center = center;
        this.center.setDirection(PluginUtils.getDirection(PluginUtils
                .getFace(yaw, false)
                .getOppositeFace()));

        this.stands = new LinkedHashMap<>();
        this.temp = new LinkedHashMap<>();

        this.currentIds = new ArrayList<>();
        this.speed = 0.05d;
        this.rotation = RotationType.POSITION;
        loadFile(fileFolder);
        loadModel();
        if (builder != null) startActionBar();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void loadFile(@Nullable String folder) {
        file = new File(folder != null ? folder : plugin.getModelManager().getModelFolder(), name + ".yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        configuration = new YamlConfiguration();
        try {
            configuration.load(file);
        } catch (IOException | InvalidConfigurationException exception) {
            exception.printStackTrace();
        }
    }

    private void startActionBar() {
        new BukkitRunnable() {
            @Override
            public void run() {

                Player player = getBuilder();

                try {
                    if (player == null || !player.isOnline() || !plugin.getModelManager().isBuilding(player)) {
                        cancel();
                        return;
                    }
                    String message = PluginUtils.translate(String.format("&e&l%s&a, Selected: &b%s&a, Speed: &d%s&a, Rotation: &6%s &7%s", name, getCurrentName(), getSpeed(), rotation.name(), getRotationTypeData()));
                    ActionBar.sendActionBar(player, message);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 5L);
    }

    public void saveChanges() {
        // Remove old entries first.
        ConfigurationSection section = configuration.getConfigurationSection("parts");
        if (section != null) configuration.set("parts", null);

        for (String name : stands.keySet()) {
            PacketStand stand = stands.get(name);

            // Save offset.
            Location offset = stand.getLocation().clone().subtract(center);
            configuration.set("parts." + name + ".offset.x", getFixed(offset.getX()));
            configuration.set("parts." + name + ".offset.y", getFixed(offset.getY()));
            configuration.set("parts." + name + ".offset.z", getFixed(offset.getZ()));
            configuration.set("parts." + name + ".offset.yaw", getFixed(stand.getLocation().getYaw()));
            configuration.set("parts." + name + ".offset.pitch", 0.0f);

            StandSettings settings = stand.getSettings().clone();

            // Save settings.
            configuration.set("parts." + name + ".settings.invisible", settings.isInvisible());
            configuration.set("parts." + name + ".settings.small", settings.isSmall());
            configuration.set("parts." + name + ".settings.baseplate", settings.hasBasePlate());
            configuration.set("parts." + name + ".settings.arms", settings.hasArms());
            configuration.set("parts." + name + ".settings.fire", settings.isOnFire());
            configuration.set("parts." + name + ".settings.marker", settings.isMarker());
            configuration.set("parts." + name + ".settings.glow", settings.isGlowing());

            // Not saved if null.
            configuration.set("parts." + name + ".settings.custom-name", settings.getCustomName());
            configuration.set("parts." + name + ".settings.custom-name-visible", settings.getCustomName() == null ? null : settings.isCustomNameVisible());

            // Save euler angles.
            saveAngle(name, "head", settings.getHeadPose());
            saveAngle(name, "body", settings.getBodyPose());
            saveAngle(name, "left-arm", settings.getLeftArmPose());
            saveAngle(name, "right-arm", settings.getRightArmPose());
            saveAngle(name, "left-leg", settings.getLeftLegPose());
            saveAngle(name, "right-leg", settings.getRightLegPose());

            // Save equipment.
            saveEquipment(name, "helmet", settings.getHelmet());
            saveEquipment(name, "chestplate", settings.getChestplate());
            saveEquipment(name, "leggings", settings.getLeggings());
            saveEquipment(name, "boots", settings.getBoots());
            saveEquipment(name, "main-hand", settings.getMainHand());
            saveEquipment(name, "off-hand", settings.getOffHand());
        }

        saveConfig();

        if (builder != null) builder.sendMessage(ChatColor.GOLD + "Model saved, you can leave now!");
    }

    public void saveAngle(String partName, String pose, EulerAngle angle) {
        if (angle == null || angle.equals(EulerAngle.ZERO)) return;
        configuration.set("parts." + partName + ".pose." + pose + ".x", getPoseAxis(angle.getX()));
        configuration.set("parts." + partName + ".pose." + pose + ".y", getPoseAxis(angle.getY()));
        configuration.set("parts." + partName + ".pose." + pose + ".z", getPoseAxis(angle.getZ()));
    }

    private void saveEquipment(String partName, String equipment, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            // Remove given equipment part section.
            configuration.set("parts." + partName + ".equipment." + equipment, null);

            // Remove equipment section if is empty.
            ConfigurationSection section = configuration.getConfigurationSection("parts." + partName + ".equipment");
            if (section != null && section.getKeys(false).isEmpty()) {
                configuration.set("parts." + partName + ".equipment", null);
            }
        } else {
            String sectionPath = "parts." + partName + ".equipment." + equipment;
            ConfigurationSection section = configuration.createSection(sectionPath);
            XItemStack.serialize(item, section);

            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

                dataOutput.writeObject(item);
                dataOutput.close();

                String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
                configuration.set(sectionPath, base64);
            } catch (IOException ignore) {

            }
        }
        saveConfig();
    }

    private void saveConfig() {
        try {
            getConfig().save(file);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        return configuration;
    }

    public boolean addNew(String name, StandSettings settings, @Nullable Location copyLocation, @Nullable Float yaw, boolean isDuplicateSelection) {
        if (stands.containsKey(name) || name.contains(".") || name.contains(" ")) return false;

        Validate.notNull(center.getWorld(), "World can't be null");

        Location finalLocation = copyLocation != null ? copyLocation : center;
        if (yaw != null) finalLocation.setYaw(finalLocation.getYaw() + yaw);

        PacketStand stand = plugin.getStandManager().spawn(finalLocation, settings);

        // When adding a new part, clear all selected.
        if (!isDuplicateSelection) {
            stands.put(name, stand);
            currentIds.clear();
        } else {
            temp.put(name, stand);
        }
        currentIds.add(stand.getEntityId());
        // No need to glow if the model isn't being building.
        if (builder != null) glowSelected(stand);
        return true;
    }

    public Map<String, PacketStand> getStands() {
        return stands;
    }

    public Map<String, PacketStand> getTemp() {
        return temp;
    }

    public List<Integer> getCurrentIds() {
        return currentIds;
    }

    private double getPoseAxis(double base) {
        return getFixed(Math.toDegrees(base));
    }

    private double getFixed(double base) {
        // 4 is perfect for small details.
        return new BigDecimal(base).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    public void getCurrentsAndThen(Consumer<PacketStand> consumer) {
        for (PacketStand stand : stands.values()) {
            if (!currentIds.contains(stand.getEntityId())) continue;
            consumer.accept(stand);
        }
    }

    public void getCurrentsAndThen(Consumer<PacketStand> consumer, Collection<Integer> ids) {
        for (PacketStand stand : stands.values()) {
            if (!ids.contains(stand.getEntityId())) continue;
            consumer.accept(stand);
        }
    }

    public String getRotationTypeData() {
        if (currentIds.isEmpty()) return "(UNKNOWN)";
        if (currentIds.size() > 1) return "(MULTIPLE)";

        PacketStand current = getById(currentIds.get(0));
        if (current == null) return "(UNKNOWN)";

        Location location = current.getLocation();

        String format = "{x: %s, y: %s, z: %s}";

        if (rotation == RotationType.POSITION) {
            Location difference = location.clone().subtract(center);
            String x = getFixed(location.getX()) + " (" + getFixed(difference.getX()) + ")";
            String y = getFixed(location.getY()) + " (" + getFixed(difference.getY()) + ")";
            String z = getFixed(location.getZ()) + " (" + getFixed(difference.getZ()) + ")";
            return String.format(format, x, y, z);
        }

        EulerAngle euler = rotation.getPose(current);
        return String.format(format, getPoseAxis(euler.getX()), getPoseAxis(euler.getY()), getPoseAxis(euler.getZ()));
    }

    public String getCurrentName() {
        if (currentIds.isEmpty()) return "(UNKNOWN)";
        if (currentIds.size() > 1) return "(MULTIPLE)";

        PacketStand current = getById(currentIds.get(0));
        if (current == null) return "(UNKNOWN)";

        for (Map.Entry<String, PacketStand> entry : stands.entrySet()) {
            if (entry.getValue().getEntityId() == current.getEntityId()) return entry.getKey();
        }

        return "(UNKNOWN)";
    }

    public Player getBuilder() {
        return builder;
    }

    public void setBuilder(Player builder) {
        this.builder = builder;
    }

    public RotationType getRotation() {
        return rotation;
    }

    public void setRotation(RotationType rotation) {
        this.rotation = rotation;
    }

    public void kill() {
        stands.forEach((name, stand) -> stand.destroy());
        stands.clear();

        Block block = center.clone().subtract(0.0d, 1.0d, 0.0d).getBlock();

        // Remove base block.
        if (block.getType() == Material.OBSIDIAN) {
            block.setType(Material.AIR);
        }
    }

    private void loadModel() {
        ConfigurationSection section = configuration.getConfigurationSection("parts");
        if (section == null) return;

        for (String path : section.getKeys(false)) {
            String defaultPath = "parts." + path + ".";

            // Load offsets.
            double xOffset = configuration.getDouble(defaultPath + "offset.x");
            double yOffset = configuration.getDouble(defaultPath + "offset.y");
            double zOffset = configuration.getDouble(defaultPath + "offset.z");

            Vector offset = new Vector(xOffset, yOffset, zOffset);

            // Pitch not needed.
            float yaw = (float) configuration.getDouble(defaultPath + "offset.yaw");

            Location location = center.clone().add(PluginUtils.offsetVector(offset, center.getYaw(), center.getPitch()));

            StandSettings settings = new StandSettings();

            // Set settings.
            settings.setInvisible(configuration.getBoolean(defaultPath + "settings.invisible"));
            settings.setSmall(configuration.getBoolean(defaultPath + "settings.small"));
            settings.setBasePlate(configuration.getBoolean(defaultPath + "settings.baseplate"));
            settings.setArms(configuration.getBoolean(defaultPath + "settings.arms"));
            settings.setOnFire(configuration.getBoolean(defaultPath + "settings.fire"));
            settings.setMarker(configuration.getBoolean(defaultPath + "settings.marker"));

            // Set poses.
            settings.setHeadPose(loadAngle(path, "head"));
            settings.setBodyPose(loadAngle(path, "body"));
            settings.setLeftArmPose(loadAngle(path, "left-arm"));
            settings.setRightArmPose(loadAngle(path, "right-arm"));
            settings.setLeftLegPose(loadAngle(path, "left-leg"));
            settings.setRightLegPose(loadAngle(path, "right-leg"));

            // Set equipment.
            settings.setHelmet(loadEquipment(path, "helmet"));
            settings.setChestplate(loadEquipment(path, "chestplate"));
            settings.setLeggings(loadEquipment(path, "leggings"));
            settings.setBoots(loadEquipment(path, "boots"));
            settings.setMainHand(loadEquipment(path, "main-hand"));
            settings.setOffHand(loadEquipment(path, "off-hand"));

            addNew(path, settings, location, yaw, false);
        }
    }

    public void rotate(PacketStand stand, boolean left) {
        if (stand != null) {
            Location location = stand.getLocation().clone();

            float value;
            if (left) {
                value = (float) getFixed(location.getYaw() - speed);
                if (value < 0.0f) value = 360.0f + value;
            } else {
                value = (float) getFixed(location.getYaw() + speed);
                if (value > 360.0f) value = -(360.0f - value);
            }
            location.setYaw(value);

            builder.sendMessage(ChatColor.GOLD + getNameOf(stand) + "'s current yaw: " + getFixed(location.getYaw()));
            stand.teleport(location);
        }
    }

    public void move(PacketStand stand, ItemStack item, boolean left) {
        if (stand == null) return;

        if (rotation == RotationType.POSITION) {
            Location location = stand.getLocation().clone();

            if (item.isSimilar(Main.X_AXIS)) {
                location.add(new Vector(left ? -speed : speed, 0.0d, 0.0d));
            } else if (item.isSimilar(Main.Y_AXIS)) {
                location.add(new Vector(0.0d, left ? -speed : speed, 0.0d));
            } else {
                location.add(new Vector(0.0d, 0.0d, left ? -speed : speed));
            }

            stand.teleport(location);
        } else {
            EulerAngle current = rotation.getPose(stand);

            double speed = (float) Math.toRadians(this.speed);

            if (item.isSimilar(Main.X_AXIS)) {
                RotationType.setPose(stand, this, current.getX() + (left ? -speed : speed), RotationType.Axis.X);
            } else if (item.isSimilar(Main.Y_AXIS)) {
                RotationType.setPose(stand, this, current.getY() + (left ? -speed : speed), RotationType.Axis.Y);
            } else {
                RotationType.setPose(stand, this, current.getZ() + (left ? -speed : speed), RotationType.Axis.Z);
            }
        }
    }

    public void setRotation(boolean left) {
        int index = rotation.ordinal();

        if (left) {
            index--;
            if (index < 0) index = RotationType.values().length - 1;
        } else {
            index++;
            if (index == RotationType.values().length) index = 0;
        }

        setRotation(RotationType.values()[index]);
    }

    private ItemStack loadEquipment(String path, String equipment) {
        String defaultPath = "parts." + path + ".equipment." + equipment;
        if (configuration.get(defaultPath) == null) return null;

        String base64 = configuration.getString(defaultPath);

        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();

            if (item != null) return item;
        } catch (IOException | ClassNotFoundException ignore) {

        }

        return null;
    }

    private EulerAngle loadAngle(String path, String pose) {
        String defaultPath = "parts." + path + ".pose.";

        if (configuration.get(defaultPath + pose) != null) {
            double x = configuration.getDouble(defaultPath + pose + ".x");
            double y = configuration.getDouble(defaultPath + pose + ".y");
            double z = configuration.getDouble(defaultPath + pose + ".z");
            return new EulerAngle(Math.toRadians(x), Math.toRadians(y), Math.toRadians(z));
        }

        return EulerAngle.ZERO;
    }

    public String getNameOf(PacketStand stand) {
        for (Map.Entry<String, PacketStand> entry : stands.entrySet()) {
            if (entry.getValue().getEntityId() == stand.getEntityId()) return entry.getKey();
        }
        return null;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void delete(PacketStand stand) {
        // Remove part from config if exists.
        if (configuration.get("parts." + name) != null) {
            configuration.set("parts." + name, null);
            saveConfig();
        }
        stands.remove(getNameOf(stand)).destroy();
    }

    public void rename(String newName) {
        if (currentIds.size() == 1 && stands.size() == 1) {
            String nameOf = getNameOf(getById(currentIds.get(0)));
            if (nameOf == null) return;

            if (stands.containsKey(newName) || temp.containsKey(newName)) {
                if (builder != null) builder.sendMessage(ChatColor.RED + "That name is being used.");
                return;
            }

            renameStand(nameOf, newName);

            stands.clear();
            stands.putAll(temp);
            temp.clear();

            if (builder != null) builder.sendMessage(ChatColor.GOLD + "Part renamed!");
            return;
        }

        AtomicBoolean shouldContinue = new AtomicBoolean(true);

        getCurrentsAndThen(stand -> {
            String nameOf = currentIds.size() == 1 ? newName : newName + "_" + (currentIds.indexOf(stand.getEntityId()) + 1);
            if (stands.containsKey(nameOf) || temp.containsKey(nameOf)) {
                shouldContinue.set(false);
            }
        });

        if (!shouldContinue.get()) {
            if (builder != null) {
                builder.sendMessage(ChatColor.RED + (currentIds.size() == 1 ? "That name is being used." : "Some of the selected parts can't be renamed, try with other name."));
            }
            return;
        }

        for (int entityId : currentIds) {
            PacketStand stand = getById(entityId);
            if (stand == null) continue;

            String nameOf = currentIds.size() == 1 ? newName : newName + "_" + (currentIds.indexOf(stand.getEntityId()) + 1);

            renameStand(getNameOf(stand), nameOf);

            stands.clear();
            stands.putAll(temp);
            temp.clear();
        }

        if (builder != null) builder.sendMessage(ChatColor.GOLD + "Part(s) renamed!");
    }

    public void renameStand(String name, String newName) {
        // Create a temporary list of all the stands.
        List<Map.Entry<String, PacketStand>> entryList = new ArrayList<>(stands.entrySet());

        // Find the index of the given name.
        int indexOf = -1;
        for (Map.Entry<String, PacketStand> entry : entryList) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                indexOf = entryList.indexOf(entry);
                break;
            }
        }

        // If not found, return.
        if (indexOf == -1) return;

        if (indexOf == 0) {
            temp.put(newName, entryList.remove(indexOf).getValue());
            addInto(entryList);
        } else if (indexOf == entryList.size() - 1) {
            Map.Entry<String, PacketStand> tempEntry = entryList.remove(indexOf);
            addInto(entryList);
            temp.put(newName, tempEntry.getValue());
        } else {
            // Put first part.
            addInto(entryList.subList(0, indexOf));

            // Put renamed.
            temp.put(newName, entryList.remove(indexOf).getValue());

            // Put second part.
            addInto(entryList.subList(indexOf, entryList.size()));
        }

        // Remove all data stored.
        entryList.clear();
    }

    private void addInto(List<Map.Entry<String, PacketStand>> list) {
        for (Map.Entry<String, PacketStand> entry : list) {
            //stands.put(entry.getKey(), entry.getValue());
            temp.put(entry.getKey(), entry.getValue());
        }
    }

    public void glowSelected(PacketStand stand) {
        if (stand.getSettings().isGlowing()) return;

        stand.setGlowing(true);
        plugin.getServer().getScheduler().runTaskLater(plugin, stand::toggleGlowing, 30L);
    }

    public Location getCenter() {
        return center;
    }

    public UUID getUniqueId() {
        return modelUniqueId;
    }

    public PacketStand getByName(String name) {
        for (Map.Entry<String, PacketStand> stand : stands.entrySet()) {
            if (stand.getKey().equalsIgnoreCase(name)) return stand.getValue();
        }
        return null;
    }

    public PacketStand getById(int id) {
        for (PacketStand stand : stands.values()) {
            if (stand.getEntityId() == id) return stand;
        }
        return null;
    }
}