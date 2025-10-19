package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.entity.AbstractEntity;

public abstract class HitResult {
    protected final Vec3d location;

    protected HitResult(Vec3d pos) {
        this.location = pos;
    }

    public double distanceTo(AbstractEntity entity) {
        double d = this.location.x() - entity.x();
        double e = this.location.y() - entity.y();
        double f = this.location.z() - entity.z();
        return d * d + e * e + f * f;
    }

    public abstract HitResult.Type getType();

    public Vec3d getLocation() {
        return this.location;
    }

    public static enum Type {
        MISS,
        BLOCK,
        ENTITY;
    }
}
