package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.VersionHelper;

public class EntityDataUtils {

    private EntityDataUtils() {}

    private static final int HAS_SHADOW = 0x01; // 1
    private static final int IS_SEE_THROUGH = 0x02; // 2
    private static final int USE_DEFAULT_BACKGROUND = 0x04; // 4
    private static final int LEFT_ALIGNMENT = 0x08; // 8
    private static final int RIGHT_ALIGNMENT = 0x10; // 16
    public static final int BLOCK_STATE_DATA_ID = VersionHelper.isOrAbove1_20_2() ? 23 : 22;
    public static final int TEXT_DATA_ID = VersionHelper.isOrAbove1_20_2() ? 23 : 22;
    public static final int DISPLAYED_ITEM_DATA_ID = VersionHelper.isOrAbove1_20_2() ? 23 : 22;
    public static final int CUSTOM_NAME_DATA_ID = 2;
    public static final int ITEM_DATA_ID = 8;

    public static byte encodeTextDisplayMask(boolean hasShadow, boolean isSeeThrough, boolean useDefaultBackground, int alignment) {
        int bitMask = 0;
        if (hasShadow) {
            bitMask |= HAS_SHADOW;
        }
        if (isSeeThrough) {
            bitMask |= IS_SEE_THROUGH;
        }
        if (useDefaultBackground) {
            bitMask |= USE_DEFAULT_BACKGROUND;
        }
        switch (alignment) {
            case 0: // CENTER
                break;
            case 1: // LEFT
                bitMask |= LEFT_ALIGNMENT;
                break;
            case 2: // RIGHT
                bitMask |= RIGHT_ALIGNMENT;
                break;
            default:
                throw new IllegalArgumentException("Invalid alignment value");
        }
        return (byte) bitMask;
    }

    private static final int IS_ON_FIRE = 0x01;            // 1
    private static final int IS_CROUCHING = 0x02;          // 2
    private static final int UNUSED = 0x04;                // 4
    private static final int IS_SPRINTING = 0x08;          // 8
    private static final int IS_SWIMMING = 0x10;           // 16
    private static final int IS_INVISIBLE = 0x20;          // 32
    private static final int HAS_GLOWING_EFFECT = 0x40;    // 64
    private static final int IS_FLYING_WITH_ELYTRA = 0x80; // 128

    public static byte encodeCommonMask(boolean isOnFire, boolean isCrouching, boolean isUnused,
                                        boolean isSprinting, boolean isSwimming, boolean isInvisible,
                                        boolean hasGlowingEffect, boolean isFlyingWithElytra) {
        int bitMask = 0;

        if (isOnFire) {
            bitMask |= IS_ON_FIRE;
        }
        if (isCrouching) {
            bitMask |= IS_CROUCHING;
        }
        if (isUnused) {
            bitMask |= UNUSED;
        }
        if (isSprinting) {
            bitMask |= IS_SPRINTING;
        }
        if (isSwimming) {
            bitMask |= IS_SWIMMING;
        }
        if (isInvisible) {
            bitMask |= IS_INVISIBLE;
        }
        if (hasGlowingEffect) {
            bitMask |= HAS_GLOWING_EFFECT;
        }
        if (isFlyingWithElytra) {
            bitMask |= IS_FLYING_WITH_ELYTRA;
        }

        return (byte) bitMask;
    }

    public static boolean isCrouching(byte mask) {
        return (mask & IS_CROUCHING) != 0;
    }
}
