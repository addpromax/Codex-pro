package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;

import java.util.Objects;

public final class MTagKeys {
    private MTagKeys() {}

    public static final Object Item$WOOL = create(MRegistries.ITEM, "wool");
    public static final Object Block$WALLS = create(MRegistries.BLOCK, "walls");

    private static Object create(Object registry, String location) {
        Object resourceLocation = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", location);
        Object tagKey = FastNMS.INSTANCE.method$TagKey$create(registry, resourceLocation);
        return Objects.requireNonNull(tagKey);
    }
}
