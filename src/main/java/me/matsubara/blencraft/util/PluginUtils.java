package me.matsubara.blencraft.util;

import com.cryptomorin.xseries.ReflectionUtils;
import com.cryptomorin.xseries.SkullUtils;
import com.cryptomorin.xseries.XMaterial;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginUtils {

    private final static Pattern PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    private static final BlockFace[] AXIS = {
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST};

    private static final BlockFace[] RADIAL = {
            BlockFace.NORTH,
            BlockFace.NORTH_EAST,
            BlockFace.EAST,
            BlockFace.SOUTH_EAST,
            BlockFace.SOUTH,
            BlockFace.SOUTH_WEST,
            BlockFace.WEST,
            BlockFace.NORTH_WEST};

    public static BlockFace getFace(float yaw, boolean subCardinal) {
        return (subCardinal ? RADIAL[Math.round(yaw / 45f) & 0x7] : AXIS[Math.round(yaw / 90f) & 0x3]).getOppositeFace();
    }

    public static Vector getDirection(BlockFace face) {
        int modX = face.getModX(), modY = face.getModY(), modZ = face.getModZ();
        Vector direction = new Vector(modX, modY, modZ);
        if (modX != 0 || modY != 0 || modZ != 0) direction.normalize();
        return direction;
    }

    public static BlockFace getNextFace(BlockFace face) {
        int index = ArrayUtils.indexOf(AXIS, face) + 1;
        return AXIS[index > (AXIS.length - 1) ? 0 : index];
    }

    public static BlockFace getPreviousFace(BlockFace face) {
        int index = ArrayUtils.indexOf(AXIS, face) - 1;
        return AXIS[index < 0 ? (AXIS.length - 1) : index];
    }

    public static Vector offsetVector(Vector vector, float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(-yawDegrees), pitch = Math.toRadians(-pitchDegrees);

        double cosYaw = Math.cos(yaw), cosPitch = Math.cos(pitch);
        double sinYaw = Math.sin(yaw), sinPitch = Math.sin(pitch);

        double initialX, initialY, initialZ, x, y, z;

        initialX = vector.getX();
        initialY = vector.getY();
        x = initialX * cosPitch - initialY * sinPitch;
        y = initialX * sinPitch + initialY * cosPitch;

        initialZ = vector.getZ();
        initialX = x;
        z = initialZ * cosYaw - initialX * sinYaw;
        x = initialZ * sinYaw + initialX * cosYaw;

        return new Vector(x, y, z);
    }

    public static ItemStack createHead(String url, boolean isMCUrl) {
        ItemStack item = XMaterial.PLAYER_HEAD.parseItem();
        if (item == null) return null;

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return null;

        item.setItemMeta(SkullUtils.applySkin(meta, isMCUrl ? "http://textures.minecraft.net/texture/" + url : url));
        return item;
    }

    public static String translate(String message) {
        Validate.notNull(message, "Message can't be null.");

        if (ReflectionUtils.VER < 16) return oldTranslate(message);

        Matcher matcher = PATTERN.matcher(oldTranslate(message));
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of(matcher.group(1)).toString());
        }

        return matcher.appendTail(buffer).toString();
    }

    public static List<String> translate(List<String> messages) {
        Validate.notNull(messages, "Messages can't be null.");

        messages.replaceAll(PluginUtils::translate);
        return messages;
    }

    private static String oldTranslate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
