package me.matsubara.blencraft.stand;

import me.matsubara.blencraft.model.Model;
import me.matsubara.blencraft.model.stand.StandSettings;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.NotNull;

public enum RotationType {
    POSITION,
    HEAD_POSE,
    BODY_POSE,
    LEFT_ARM_POSE,
    RIGHT_ARM_POSE,
    LEFT_LEG_POSE,
    RIGHT_LEG_POSE;

    @NotNull
    public EulerAngle getPose(PacketStand stand) {
        StandSettings settings = stand.getSettings();
        switch (this) {
            case HEAD_POSE:
                return settings.getHeadPose();
            case BODY_POSE:
                return settings.getBodyPose();
            case LEFT_ARM_POSE:
                return settings.getLeftArmPose();
            case RIGHT_ARM_POSE:
                return settings.getRightArmPose();
            case LEFT_LEG_POSE:
                return settings.getLeftLegPose();
            default:
                return settings.getRightLegPose();
        }
    }

    public static void setPose(PacketStand stand, Model model, double value, Axis axis) {
        if (stand == null) return;

        EulerAngle angle = model.getRotation().getPose(stand);

        if (axis.x()) {
            angle = angle.setX(value);
        } else if (axis.y()) {
            angle = angle.setY(value);
        } else {
            angle = angle.setZ(value);
        }

        switch (model.getRotation()) {
            case HEAD_POSE:
                stand.setHeadPose(angle);
                break;
            case BODY_POSE:
                stand.setBodyPose(angle);
                break;
            case LEFT_ARM_POSE:
                stand.setLeftArmPose(angle);
                break;
            case RIGHT_ARM_POSE:
                stand.setRightArmPose(angle);
                break;
            case LEFT_LEG_POSE:
                stand.setLeftLegPose(angle);
                break;
            case RIGHT_LEG_POSE:
                stand.setRightLegPose(angle);
                break;
            default:
                return;
        }

        // Update metadata after changing poses.
        stand.updateMetadata();
    }

    public enum Axis {
        X,
        Y,
        Z;

        public boolean x() {
            return this == X;
        }

        public boolean y() {
            return this == Y;
        }
    }
}