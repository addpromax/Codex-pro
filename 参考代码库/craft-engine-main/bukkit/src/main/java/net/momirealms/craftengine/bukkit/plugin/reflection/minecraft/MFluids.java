package net.momirealms.craftengine.bukkit.plugin.reflection.minecraft;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;

public final class MFluids {
    private MFluids() {}

    public static final Object WATER;
    public static final Object FLOWING_WATER;
    public static final Object LAVA;
    public static final Object FLOWING_LAVA;
    public static final Object EMPTY;
    public static final Object EMPTY$defaultState;

    private static Object getById(String id) throws ReflectiveOperationException {
        Object rl = FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", id);
        return CoreReflections.method$Registry$get.invoke(MBuiltInRegistries.FLUID, rl);
    }

    static {
        try {
            WATER = getById("water");
            FLOWING_WATER = getById("flowing_water");
            LAVA = getById("lava");
            FLOWING_LAVA = getById("flowing_lava");
            EMPTY = getById("empty");
            EMPTY$defaultState = CoreReflections.method$Fluid$defaultFluidState.invoke(EMPTY);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to init Fluids", e);
        }
    }
}
