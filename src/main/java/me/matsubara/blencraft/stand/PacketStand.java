package me.matsubara.blencraft.stand;

import com.cryptomorin.xseries.ReflectionUtils;
import me.matsubara.blencraft.BlencraftPlugin;
import me.matsubara.blencraft.model.stand.StandSettings;
import me.matsubara.blencraft.util.PluginUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@SuppressWarnings({"ConstantConditions", "BooleanMethodIsAlwaysInverted"})
public final class PacketStand {

    // Instance of the entity.
    private Object stand;

    // Current location of the entity.
    private Location location;

    // Set with the unique id of the players who aren't seeing the entity due to the distance.
    private Set<UUID> ignored;

    // Entity attributes.
    private int entityId;
    private int[] passengersId = {};
    private StandSettings settings;

    // Most changes are made since 1.13 version.
    private final static boolean isMoreThan12 = ReflectionUtils.VER > 12;

    // The distance to see the armor stand.
    public final static int RENDER_DISTANCE = 50;

    // Classes.
    private final static Class<?> CRAFT_WORLD;
    private final static Class<?> CRAFT_CHAT_MESSAGE;

    private final static Class<?> WORLD;
    private final static Class<?> I_CHAT_BASE_COMPONENT;
    private final static Class<?> ENTITY;
    private final static Class<?> ENTITY_LIVING;
    private final static Class<?> ENTITY_ARMOR_STAND;
    private final static Class<?> PACKET_SPAWN_ENTITY_LIVING;
    private final static Class<?> PACKET_ENTITY_HEAD_ROTATION;
    private final static Class<?> PACKET_ENTITY_TELEPORT;
    private final static Class<?> PACKET_ENTITY_LOOK;
    private final static Class<?> VECTOR3F;
    private final static Class<?> DATA_WATCHER;
    private final static Class<?> PACKET_ENTITY_METADATA;
    private final static Class<?> PACKET_MOUNT;
    private final static Class<?> PACKET_ENTITY_EQUIPMENT;
    private final static Class<?> ENUM_ITEM_SLOT;
    private final static Class<?> ITEM_STACK;
    private final static Class<?> CRAFT_ITEM_STACK;
    private final static Class<?> PACKET_ENTITY_DESTROY;

    // Methods.
    private static Method getHandle;
    private static Method getDataWatcher;
    private static Method fromStringOrNull;
    private static Method getId;
    private static Method setLocation;
    private static Method setInvisible;
    private static Method setArms;
    private static Method setBasePlate;
    private static Method setSmall;
    private static Method setMarker;
    private static Method setCustomName;
    private static Method setCustomNameVisible;
    private static Method setHeadPose;
    private static Method setBodyPose;
    private static Method setLeftArmPose;
    private static Method setRightArmPose;
    private static Method setLeftLegPose;
    private static Method setRightLegPose;
    private static Method asNMSCopy;
    private static Method setFlag;

    // Constructors.
    private static Constructor<?> entityArmorStand;
    private static Constructor<?> packetSpawnEntityLiving;
    private static Constructor<?> packetEntityHeadRotation;
    private static Constructor<?> packetEntityTeleport;
    private static Constructor<?> packetEntityLook;
    private static Constructor<?> vector3f;
    private static Constructor<?> packetEntityMetadata;
    private static Constructor<?> packetMount;
    private static Constructor<?> packetEntityEquipment;
    private static Constructor<?> packetEntityDestroy;

    static {
        // Initialize classes.
        CRAFT_WORLD = ReflectionUtils.getCraftClass("CraftWorld");
        CRAFT_CHAT_MESSAGE = !isMoreThan12 ? null : ReflectionUtils.getCraftClass("util.CraftChatMessage");
        CRAFT_ITEM_STACK = ReflectionUtils.getCraftClass("inventory.CraftItemStack");
        WORLD = ReflectionUtils.getNMSClass("world.level", "World");
        I_CHAT_BASE_COMPONENT = ReflectionUtils.getNMSClass("network.chat", "IChatBaseComponent");
        ENTITY = ReflectionUtils.getNMSClass("world.entity", "Entity");
        ENTITY_LIVING = ReflectionUtils.getNMSClass("world.entity", "EntityLiving");
        ENTITY_ARMOR_STAND = ReflectionUtils.getNMSClass("world.entity.decoration", "EntityArmorStand");
        PACKET_SPAWN_ENTITY_LIVING = ReflectionUtils.getNMSClass("network.protocol.game", "PacketPlayOutSpawnEntityLiving");
        PACKET_ENTITY_HEAD_ROTATION = ReflectionUtils.getNMSClass("network.protocol.game", "PacketPlayOutEntityHeadRotation");
        PACKET_ENTITY_TELEPORT = ReflectionUtils.getNMSClass("network.protocol.game", "PacketPlayOutEntityTeleport");
        PACKET_ENTITY_LOOK = ReflectionUtils.getNMSClass("network.protocol.game", "PacketPlayOutEntity$PacketPlayOutEntityLook");
        VECTOR3F = ReflectionUtils.getNMSClass("core", "Vector3f");
        DATA_WATCHER = ReflectionUtils.getNMSClass("network.syncher", "DataWatcher");
        PACKET_ENTITY_METADATA = ReflectionUtils.getNMSClass("network.protocol.game", "PacketPlayOutEntityMetadata");
        PACKET_MOUNT = ReflectionUtils.getNMSClass("network.protocol.game", "PacketPlayOutMount");
        PACKET_ENTITY_EQUIPMENT = ReflectionUtils.getNMSClass("network.protocol.game", "PacketPlayOutEntityEquipment");
        ENUM_ITEM_SLOT = ReflectionUtils.getNMSClass("world.entity", "EnumItemSlot");
        ITEM_STACK = ReflectionUtils.getNMSClass("world.item", "ItemStack");
        PACKET_ENTITY_DESTROY = ReflectionUtils.getNMSClass("network.protocol.game", "PacketPlayOutEntityDestroy");

        try {
            // Initialize methods.
            getHandle = CRAFT_WORLD.getMethod("getHandle");
            getDataWatcher = ENTITY_ARMOR_STAND.getMethod("getDataWatcher");
            fromStringOrNull = !isMoreThan12 ? null : CRAFT_CHAT_MESSAGE.getMethod("fromStringOrNull", String.class);
            getId = ENTITY_ARMOR_STAND.getMethod("getId");
            setLocation = ENTITY_ARMOR_STAND.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class);
            setInvisible = ENTITY_ARMOR_STAND.getMethod("setInvisible", boolean.class);
            setArms = ENTITY_ARMOR_STAND.getMethod("setArms", boolean.class);
            setBasePlate = ENTITY_ARMOR_STAND.getMethod("setBasePlate", boolean.class);
            setSmall = ENTITY_ARMOR_STAND.getMethod("setSmall", boolean.class);
            setMarker = ENTITY_ARMOR_STAND.getMethod(ReflectionUtils.VER == 8 ? "n" : "setMarker", boolean.class);
            setCustomName = ENTITY_ARMOR_STAND.getMethod("setCustomName", isMoreThan12 ? I_CHAT_BASE_COMPONENT : String.class);
            setCustomNameVisible = ENTITY_ARMOR_STAND.getMethod("setCustomNameVisible", boolean.class);
            setHeadPose = ENTITY_ARMOR_STAND.getMethod("setHeadPose", VECTOR3F);
            setBodyPose = ENTITY_ARMOR_STAND.getMethod("setBodyPose", VECTOR3F);
            setLeftArmPose = ENTITY_ARMOR_STAND.getMethod("setLeftArmPose", VECTOR3F);
            setRightArmPose = ENTITY_ARMOR_STAND.getMethod("setRightArmPose", VECTOR3F);
            setLeftLegPose = ENTITY_ARMOR_STAND.getMethod("setLeftLegPose", VECTOR3F);
            setRightLegPose = ENTITY_ARMOR_STAND.getMethod("setRightLegPose", VECTOR3F);
            asNMSCopy = CRAFT_ITEM_STACK.getMethod("asNMSCopy", ItemStack.class);
            setFlag = ENTITY_ARMOR_STAND.getMethod("setFlag", int.class, boolean.class);

            // Initialize constructors.
            entityArmorStand = ENTITY_ARMOR_STAND.getConstructor(WORLD, double.class, double.class, double.class);
            packetSpawnEntityLiving = PACKET_SPAWN_ENTITY_LIVING.getConstructor(ENTITY_LIVING);
            packetEntityHeadRotation = PACKET_ENTITY_HEAD_ROTATION.getConstructor(ENTITY, byte.class);
            packetEntityTeleport = PACKET_ENTITY_TELEPORT.getConstructor(ENTITY);
            packetEntityLook = PACKET_ENTITY_LOOK.getConstructor(int.class, byte.class, byte.class, boolean.class);
            vector3f = VECTOR3F.getConstructor(float.class, float.class, float.class);
            packetEntityMetadata = PACKET_ENTITY_METADATA.getConstructor(int.class, DATA_WATCHER, boolean.class);
            packetMount = ReflectionUtils.VER > 16 ? PACKET_MOUNT.getConstructor(ENTITY) : PACKET_MOUNT.getConstructor();
            packetEntityEquipment = ReflectionUtils.VER > 15 ?
                    PACKET_ENTITY_EQUIPMENT.getConstructor(int.class, List.class) :
                    PACKET_ENTITY_EQUIPMENT.getConstructor(int.class, ENUM_ITEM_SLOT, ITEM_STACK);
            packetEntityDestroy = PACKET_ENTITY_DESTROY.getConstructor(int[].class);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public PacketStand(Location location, StandSettings settings) {
        Validate.notNull(location.getWorld(), "World can't be null.");

        try {
            Object craftWorld = CRAFT_WORLD.cast(location.getWorld());
            Object nmsWorld = getHandle.invoke(craftWorld);

            this.stand = entityArmorStand.newInstance(nmsWorld, location.getX(), location.getY(), location.getZ());
            this.location = location;
            this.ignored = new HashSet<>();

            this.entityId = (int) getId.invoke(stand);

            this.settings = settings;

            // Set the initial location of this entity.
            setLocation(location);

            // Set settings.
            setInvisible(settings.isInvisible());
            setSmall(settings.isSmall());
            setBasePlate(settings.hasBasePlate());
            setArms(settings.hasArms());
            setOnFire(settings.isOnFire());
            setMarker(settings.isMarker());

            // Set poses.
            setHeadPose(settings.getHeadPose());
            setBodyPose(settings.getBodyPose());
            setLeftArmPose(settings.getLeftArmPose());
            setRightArmPose(settings.getRightArmPose());
            setLeftLegPose(settings.getLeftLegPose());
            setRightLegPose(settings.getRightLegPose());

            // Set equipment.
            setEquipment(settings.getHelmet(), ItemSlot.HEAD);
            setEquipment(settings.getChestplate(), ItemSlot.CHEST);
            setEquipment(settings.getLeggings(), ItemSlot.LEGS);
            setEquipment(settings.getBoots(), ItemSlot.FEET);
            setEquipment(settings.getMainHand(), ItemSlot.MAINHAND);
            setEquipment(settings.getOffHand(), ItemSlot.OFFHAND);

            // Send spawn and teleport packet to all players.
            spawn();
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public int getEntityId() {
        return entityId;
    }

    public Location getLocation() {
        return location;
    }

    public StandSettings getSettings() {
        return settings;
    }

    /**
     * Spawn the entity to every player in the same world.
     */
    public void spawn() {
        for (Player player : location.getWorld().getPlayers()) {
            spawn(player);
        }
    }

    /**
     * Spawn the entity for a specific player.
     */
    public void spawn(Player player) {
        try {
            Object packetSpawn = packetSpawnEntityLiving.newInstance(stand);

            ReflectionUtils.sendPacket(player, packetSpawn);
            ignored.remove(player.getUniqueId());

            Bukkit.getScheduler().runTaskAsynchronously(JavaPlugin.getPlugin(BlencraftPlugin.class), () -> {
                showPassenger(player);
                updateLocation();
                updateEquipment();
                updateMetadata();
            });
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set the current location of the entity.
     * You must teleport the entity using teleport(), or teleport(Location) instead of this method.
     * If this entity isn't spawned yet, you don't need to call any method, just spawn the entity.
     *
     * @see PacketStand#updateLocation()
     * @see PacketStand#teleport(Location)
     */
    public void setLocation(Location location) {
        try {
            this.location = location;
            setLocation.invoke(stand, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Teleports the entity to the previously established location.
     *
     * @see PacketStand#setLocation(Location)
     */
    public void updateLocation() {
        try {
            Object packetTeleport = packetEntityTeleport.newInstance(stand);
            sendPacket(packetTeleport);

            byte yaw = (byte) (location.getYaw() * 256.0f / 360.0f);
            byte pitch = (byte) (location.getPitch() * 256.0f / 360.0f);

            Object packetRotation = packetEntityHeadRotation.newInstance(stand, yaw);
            sendPacket(packetRotation);

            Object packetLook = packetEntityLook.newInstance(entityId, yaw, pitch, true);
            sendPacket(packetLook);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set the current location of the entity.
     */
    public void teleport(Location location) {
        setLocation(location);
        updateLocation();
    }

    public void toggleInvisibility() {
        setInvisible(!settings.isInvisible());
        updateMetadata();
    }

    public void setInvisible(boolean invisible) {
        try {
            settings.setInvisible(invisible);
            setInvisible.invoke(stand, invisible);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void toggleArms() {
        setArms(!settings.hasArms());
        updateMetadata();
    }

    public void setArms(boolean arms) {
        try {
            settings.setArms(arms);
            setArms.invoke(stand, arms);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void toggleBasePlate() {
        setBasePlate(!settings.hasBasePlate());
        updateMetadata();
    }

    public void setBasePlate(boolean baseplate) {
        try {
            settings.setBasePlate(baseplate);
            setBasePlate.invoke(stand, !baseplate); // For some reason must be negated.
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void toggleSmall() {
        setSmall(!settings.isSmall());
        updateMetadata();
    }

    public void setSmall(boolean small) {
        try {
            settings.setSmall(small);
            setSmall.invoke(stand, small);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void toggleMarker() {
        setMarker(!settings.isMarker());
        updateMetadata();
    }

    public void setMarker(boolean marker) {
        try {
            settings.setMarker(marker);
            setMarker.invoke(stand, marker);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void toggleFire() {
        setOnFire(!settings.isOnFire());
    }

    public void setOnFire(boolean fire) {
        // Only works on 1.9+.
        if (ReflectionUtils.VER < 9) return;

        settings.setOnFire(fire);

        try {
            setFlag.invoke(stand, 0, fire);

            Object watcher = getDataWatcher.invoke(stand);

            Object packetMetadata = packetEntityMetadata.newInstance(entityId, watcher, true);
            sendPacket(packetMetadata);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void toggleGlowing() {
        setGlowing(!settings.isGlowing());
    }

    public void setGlowing(boolean glow) {
        // Only works on 1.9+.
        if (ReflectionUtils.VER < 9) return;

        settings.setGlowing(glow);

        try {
            setFlag.invoke(stand, 6, glow);

            Object watcher = getDataWatcher.invoke(stand);

            Object packetMetadata = packetEntityMetadata.newInstance(entityId, watcher, true);
            sendPacket(packetMetadata);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void setCustomName(@Nullable String name) {
        try {
            if (name != null) {
                name = PluginUtils.translate(name);
                name = isMoreThan12 ? (String) fromStringOrNull.invoke(null, name) : name;
            } else {
                name = "";
                setCustomNameVisible(false);
            }
            setCustomName.invoke(stand, name);
            settings.setCustomName(name);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void toggleCustomNameVisibility() {
        setCustomNameVisible(!settings.isCustomNameVisible());
        updateMetadata();
    }

    public void setCustomNameVisible(boolean customNameVisible) {
        try {
            settings.setCustomNameVisible(customNameVisible);
            setCustomNameVisible.invoke(stand, customNameVisible);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    /**
     * Set a player as a passenger of this entity and show to everyone.
     */
    public void setPassenger(Player player) {
        // If player isn't in range, can't be a passenger.
        if (player != null && isIgnored(player)) return;

        // If the array is empty, entity won't have passengers.
        this.passengersId = (player != null) ? new int[]{player.getEntityId()} : new int[]{};
        sendPassenger(null);
    }

    /**
     * Show passenger of this entity (if it has) to a player.
     */
    public void showPassenger(Player notSeeing) {
        if (!hasPassenger()) return;
        sendPassenger(notSeeing);
    }

    /**
     * Show the passenger of this entity to a certain player or everyone (if @to is null).
     */
    private void sendPassenger(@Nullable Player to) {
        try {
            Object packetMount;
            if (ReflectionUtils.VER > 16) {
                packetMount = PacketStand.packetMount.newInstance(stand);
            } else {
                packetMount = PacketStand.packetMount.newInstance();
            }

            Field a = PACKET_MOUNT.getDeclaredField("a");
            a.setAccessible(true);
            a.set(packetMount, entityId);

            Field b = PACKET_MOUNT.getDeclaredField("b");
            b.setAccessible(true);
            b.set(packetMount, passengersId);

            if (to != null) {
                ReflectionUtils.sendPacket(to, packetMount);
            } else {
                sendPacket(packetMount);
            }
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public boolean hasPassenger() {
        return passengersId != null && passengersId.length > 0;
    }

    public boolean isPassenger(Player player) {
        for (int passengerId : passengersId) {
            if (passengerId == player.getEntityId()) return true;
        }
        return false;
    }

    public void setHeadPose(EulerAngle headPose) {
        try {
            settings.setHeadPose(headPose);
            setHeadPose.invoke(stand, getVector3f(headPose));
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void setBodyPose(EulerAngle bodyPose) {
        try {
            settings.setBodyPose(bodyPose);
            setBodyPose.invoke(stand, getVector3f(bodyPose));
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void setLeftArmPose(EulerAngle leftArmPose) {
        try {
            settings.setLeftArmPose(leftArmPose);
            setLeftArmPose.invoke(stand, getVector3f(leftArmPose));
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void setRightArmPose(EulerAngle rightArmPose) {
        try {
            settings.setRightArmPose(rightArmPose);
            setRightArmPose.invoke(stand, getVector3f(rightArmPose));
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void setLeftLegPose(EulerAngle leftLegPose) {
        try {
            settings.setLeftLegPose(leftLegPose);
            setLeftLegPose.invoke(stand, getVector3f(leftLegPose));
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void setRightLegPose(EulerAngle rightLegPose) {
        try {
            settings.setRightLegPose(rightLegPose);
            setRightLegPose.invoke(stand, getVector3f(rightLegPose));
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public enum ItemSlot {
        MAINHAND,
        OFFHAND,
        FEET,
        LEGS,
        CHEST,
        HEAD;

        private final char[] alphabet = "abcdefghijklmnopqrstuvwxyz".toCharArray();

        public Object get() {
            try {
                Field field = ENUM_ITEM_SLOT.getField(ReflectionUtils.VER > 16 ? "" + alphabet[ordinal()] : name());
                return field.get(null);
            } catch (ReflectiveOperationException exception) {
                exception.printStackTrace();
            }
            return null;
        }
    }

    public void setEquipment(ItemStack item, ItemSlot slot) {
        if (slot == ItemSlot.MAINHAND) settings.setMainHand(item);
        if (slot == ItemSlot.OFFHAND) settings.setOffHand(item);
        if (slot == ItemSlot.HEAD) settings.setHelmet(item);
        if (slot == ItemSlot.CHEST) settings.setChestplate(item);
        if (slot == ItemSlot.LEGS) settings.setLeggings(item);
        if (slot == ItemSlot.FEET) settings.setBoots(item);

        try {
            Object itemStack = asNMSCopy.invoke(null, item);

            Object packetEquipment;
            if (ReflectionUtils.VER > 15) {
                Class<?> PAIR = Class.forName("com.mojang.datafixers.util.Pair");
                Method of = PAIR.getMethod("of", Object.class, Object.class);

                List<Object> list = new ArrayList<>();
                list.add(of.invoke(null, slot.get(), itemStack));

                packetEquipment = packetEntityEquipment.newInstance(entityId, list);
            } else {
                packetEquipment = packetEntityEquipment.newInstance(entityId, slot.get(), itemStack);
            }
            sendPacket(packetEquipment);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public Object getVector3f(EulerAngle angle) {
        try {
            return vector3f.newInstance((float) Math.toDegrees(angle.getX()), (float) Math.toDegrees(angle.getY()), (float) Math.toDegrees(angle.getZ()));
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    public void updateEquipment() {
        if (!settings.hasEquipment()) return;

        ItemStack helmet = settings.getHelmet();
        ItemStack chestplate = settings.getChestplate();
        ItemStack leggings = settings.getLeggings();
        ItemStack boots = settings.getBoots();
        ItemStack mainHand = settings.getMainHand();
        ItemStack offHand = settings.getOffHand();

        if (helmet != null) setEquipment(helmet, ItemSlot.HEAD);
        if (chestplate != null) setEquipment(chestplate, ItemSlot.CHEST);
        if (leggings != null) setEquipment(leggings, ItemSlot.LEGS);
        if (boots != null) setEquipment(boots, ItemSlot.FEET);
        if (mainHand != null) setEquipment(mainHand, ItemSlot.MAINHAND);
        if (offHand != null) setEquipment(offHand, ItemSlot.OFFHAND);
    }

    public void updateMetadata() {
        try {
            Object watcher = getDataWatcher.invoke(stand);
            Object packetMetadata = packetEntityMetadata.newInstance(entityId, watcher, true);
            sendPacket(packetMetadata);
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public void destroy() {
        for (Player player : location.getWorld().getPlayers()) {
            destroy(player);
        }
        ignored.clear();
    }

    @SuppressWarnings("PrimitiveArrayArgumentToVarargsMethod")
    public void destroy(Player player) {
        int[] ids = new int[]{entityId};

        try {
            Object packetDestroy = packetEntityDestroy.newInstance(ids);

            ReflectionUtils.sendPacket(player, packetDestroy);
            ignored.add(player.getUniqueId());
        } catch (ReflectiveOperationException exception) {
            exception.printStackTrace();
        }
    }

    public Object getHandle() {
        return stand;
    }

    public boolean isIgnored(Player player) {
        return ignored.contains(player.getUniqueId());
    }

    private void sendPacket(Object packet) {
        for (Player player : location.getWorld().getPlayers()) {
            if (isIgnored(player)) continue;
            ReflectionUtils.sendPacket(player, packet);
        }
    }
}