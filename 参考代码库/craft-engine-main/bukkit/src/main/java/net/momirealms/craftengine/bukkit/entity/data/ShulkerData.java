package net.momirealms.craftengine.bukkit.entity.data;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;

public class ShulkerData<T> extends MobData<T> {
    public static final ShulkerData<Object> AttachFace = new ShulkerData<>(ShulkerData.class, EntityDataValue.Serializers$DIRECTION, CoreReflections.instance$Direction$DOWN);
    public static final ShulkerData<Byte> Peek = new ShulkerData<>(ShulkerData.class, EntityDataValue.Serializers$BYTE, (byte) 0);
    public static final ShulkerData<Byte> Color = new ShulkerData<>(ShulkerData.class, EntityDataValue.Serializers$BYTE, (byte) 16);

    public ShulkerData(Class<?> clazz, Object serializer, T defaultValue) {
        super(clazz, serializer, defaultValue);
    }
}