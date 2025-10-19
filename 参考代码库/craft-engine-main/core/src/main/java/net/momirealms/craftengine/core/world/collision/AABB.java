package net.momirealms.craftengine.core.world.collision;

import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.EntityHitResult;
import net.momirealms.craftengine.core.world.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class AABB {
    private static final double EPSILON = 1.0E-7;
    public final double minX;
    public final double minY;
    public final double minZ;
    public final double maxX;
    public final double maxY;
    public final double maxZ;

    public AABB(double x1, double y1, double z1, double x2, double y2, double z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public AABB(Vec3d pos1, Vec3d pos2) {
        this.minX = Math.min(pos1.x, pos2.x);
        this.minY = Math.min(pos1.y, pos2.y);
        this.minZ = Math.min(pos1.z, pos2.z);
        this.maxX = Math.max(pos1.x, pos2.x);
        this.maxY = Math.max(pos1.y, pos2.y);
        this.maxZ = Math.max(pos1.z, pos2.z);
    }

    public static AABB fromInteraction(Vec3d pos, double width, double height) {
        return new AABB(
            pos.x - width / 2,
            pos.y,
            pos.z - width / 2,
            pos.x + width / 2,
            pos.y + height,
            pos.z + width / 2
        );
    }

    public Optional<EntityHitResult> clip(Vec3d min, Vec3d max) {
        double[] traceDistance = {1.0};
        double deltaX = max.x - min.x;
        double deltaY = max.y - min.y;
        double deltaZ = max.z - min.z;

        Direction direction = calculateCollisionDirection(min, traceDistance, deltaX, deltaY, deltaZ);
        return direction != null
                ? Optional.of(new EntityHitResult(direction, min.add(traceDistance[0] * deltaX, traceDistance[0] * deltaY, traceDistance[0] * deltaZ)))
                : Optional.empty();
    }

    private Direction calculateCollisionDirection(Vec3d intersectingVector, double[] traceDistance, double deltaX, double deltaY, double deltaZ) {
        Direction direction = null;

        // Check each axis for potential collision
        direction = checkAxis(deltaX, deltaY, deltaZ, Direction.WEST, Direction.EAST,
                minX, maxX, intersectingVector.x, intersectingVector.y, intersectingVector.z,
                minY, maxY, minZ, maxZ, traceDistance, direction);

        direction = checkAxis(deltaY, deltaZ, deltaX, Direction.DOWN, Direction.UP,
                minY, maxY, intersectingVector.y, intersectingVector.z, intersectingVector.x,
                minZ, maxZ, minX, maxX, traceDistance, direction);

        direction = checkAxis(deltaZ, deltaX, deltaY, Direction.NORTH, Direction.SOUTH,
                minZ, maxZ, intersectingVector.z, intersectingVector.x, intersectingVector.y,
                minX, maxX, minY, maxY, traceDistance, direction);

        return direction;
    }

    private Direction checkAxis(double primaryDelta, double secondary1Delta, double secondary2Delta,
                                Direction positiveDir, Direction negativeDir,
                                double positiveFace, double negativeFace,
                                double startPrimary, double startSecondary1, double startSecondary2,
                                double secondary1Min, double secondary1Max,
                                double secondary2Min, double secondary2Max,
                                double[] traceDistance, @Nullable Direction currentDir) {
        if (primaryDelta > EPSILON) {
            return checkFace(traceDistance, currentDir, positiveFace,
                    primaryDelta, secondary1Delta, secondary2Delta,
                    secondary1Min, secondary1Max, secondary2Min, secondary2Max,
                    positiveDir, startPrimary, startSecondary1, startSecondary2);
        } else if (primaryDelta < -EPSILON) {
            return checkFace(traceDistance, currentDir, negativeFace,
                    primaryDelta, secondary1Delta, secondary2Delta,
                    secondary1Min, secondary1Max, secondary2Min, secondary2Max,
                    negativeDir, startPrimary, startSecondary1, startSecondary2);
        }
        return currentDir;
    }

    private static Direction checkFace(double[] traceDistance, @Nullable Direction currentDir,
                                       double facePosition,
                                       double primaryDelta, double secondary1Delta, double secondary2Delta,
                                       double secondary1Min, double secondary1Max,
                                       double secondary2Min, double secondary2Max,
                                       Direction direction,
                                       double startPrimary, double startSecondary1, double startSecondary2) {
        double d = (facePosition - startPrimary) / primaryDelta;
        if (d <= 0.0 || d >= traceDistance[0]) {
            return currentDir;
        }

        double secondary1 = startSecondary1 + d * secondary1Delta;
        double secondary2 = startSecondary2 + d * secondary2Delta;

        if (isWithinBounds(secondary1, secondary1Min, secondary1Max) &&
                isWithinBounds(secondary2, secondary2Min, secondary2Max)) {
            traceDistance[0] = d;
            return direction;
        }
        return currentDir;
    }

    private static boolean isWithinBounds(double value, double min, double max) {
        return (value >= min - EPSILON) && (value <= max + EPSILON);
    }

    @Override
    public String toString() {
        return "AABB{" +
                "minX=" + minX +
                ", minY=" + minY +
                ", minZ=" + minZ +
                ", maxX=" + maxX +
                ", maxY=" + maxY +
                ", maxZ=" + maxZ +
                '}';
    }
}
