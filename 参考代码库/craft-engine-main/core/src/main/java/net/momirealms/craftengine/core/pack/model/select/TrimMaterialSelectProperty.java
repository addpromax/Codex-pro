package net.momirealms.craftengine.core.pack.model.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.LegacyModelPredicate;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.Map;

public class TrimMaterialSelectProperty implements SelectProperty, LegacyModelPredicate<String> {
    public static final Factory FACTORY = new Factory();
    public static final Reader READER = new Reader();
    public static final TrimMaterialSelectProperty INSTANCE = new TrimMaterialSelectProperty();
    private static final Map<String, Float> LEGACY_TRIM_DATA = new HashMap<>();
    static {
        LEGACY_TRIM_DATA.put("minecraft:quartz", 0.1f);
        LEGACY_TRIM_DATA.put("minecraft:iron", 0.2f);
        LEGACY_TRIM_DATA.put("minecraft:netherite", 0.3f);
        LEGACY_TRIM_DATA.put("minecraft:redstone", 0.4f);
        LEGACY_TRIM_DATA.put("minecraft:copper", 0.5f);
        LEGACY_TRIM_DATA.put("minecraft:gold", 0.6f);
        LEGACY_TRIM_DATA.put("minecraft:emerald", 0.7f);
        LEGACY_TRIM_DATA.put("minecraft:diamond", 0.8f);
        LEGACY_TRIM_DATA.put("minecraft:lapis", 0.9f);
        LEGACY_TRIM_DATA.put("minecraft:amethyst", 1.0f);
        // INVALID
        LEGACY_TRIM_DATA.put("minecraft:resin", 1.1F);
    }

    @Override
    public Key type() {
        return SelectProperties.TRIM_MATERIAL;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
    }

    @Override
    public String legacyPredicateId(Key material) {
        if (isArmor(material)) {
            return "trim_type";
        }
        return null;
    }

    @Override
    public Number toLegacyValue(String value) {
        Float f = LEGACY_TRIM_DATA.get(value);
        if (f == null) {
            throw new IllegalArgumentException("Invalid trim material '" + value + "'");
        }
        return f;
    }

    public boolean isArmor(Key material) {
        String s = material.toString();
        return s.contains("helmet") || s.contains("chestplate") || s.contains("leggings") || s.contains("boots");
    }

    public static class Factory implements SelectPropertyFactory {
        @Override
        public SelectProperty create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }

    public static class Reader implements SelectPropertyReader {
        @Override
        public SelectProperty read(JsonObject json) {
            return INSTANCE;
        }
    }
}
