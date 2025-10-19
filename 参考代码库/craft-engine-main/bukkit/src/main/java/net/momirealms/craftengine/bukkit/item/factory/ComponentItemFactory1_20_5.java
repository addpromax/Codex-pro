package net.momirealms.craftengine.bukkit.item.factory;

import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.item.ComponentItemWrapper;
import net.momirealms.craftengine.bukkit.item.ComponentTypes;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistryOps;
import net.momirealms.craftengine.bukkit.util.EnchantmentUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.item.data.Enchantment;
import net.momirealms.craftengine.core.item.data.FireworkExplosion;
import net.momirealms.craftengine.core.item.data.Trim;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ComponentItemFactory1_20_5 extends BukkitItemFactory<ComponentItemWrapper> {

    public ComponentItemFactory1_20_5(CraftEngine plugin) {
        super(plugin);
    }

    @Override
    protected void customId(ComponentItemWrapper item, Key id) {
        FastNMS.INSTANCE.setCustomItemId(item.getLiteralObject(), id.toString());
    }

    @Override
    protected Optional<Key> customId(ComponentItemWrapper item) {
        return Optional.ofNullable(FastNMS.INSTANCE.getCustomItemId(item.getLiteralObject())).map(Key::of);
    }

    @Override
    protected ComponentItemWrapper wrapInternal(ItemStack item) {
        return new ComponentItemWrapper(item);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object getJavaTag(ComponentItemWrapper item, Object... path) {
        Map<String, Object> rootMap = (Map<String, Object>) item.getJavaComponent(ComponentTypes.CUSTOM_DATA).orElse(null);
        if (rootMap == null) return null;
        Object currentObj = rootMap;
        for (int i = 0; i < path.length; i++) {
            Object pathSegment = path[i];
            if (pathSegment == null) return null;
            currentObj = ((Map<String, Object>) currentObj).get(pathSegment.toString());
            if (currentObj == null) return null;
            if (i == path.length - 1) {
                return currentObj;
            }
            if (!(currentObj instanceof Map)) {
                return null;
            }
        }
        return currentObj;
    }

    @Override
    protected Tag getNBTTag(ComponentItemWrapper item, Object... path) {
        CompoundTag rootTag = (CompoundTag) item.getSparrowNBTComponent(ComponentTypes.CUSTOM_DATA).orElse(null);
        if (rootTag == null) return null;
        Tag currentTag = rootTag;
        for (int i = 0; i < path.length; i++) {
            Object pathSegment = path[i];
            if (pathSegment == null) return null;
            CompoundTag t = (CompoundTag) currentTag;
            currentTag = t.get(pathSegment.toString());
            if (currentTag == null) return null;
            if (i == path.length - 1) {
                return currentTag;
            }
            if (!(currentTag instanceof CompoundTag)) {
                return null;
            }
        }
        return currentTag;
    }

    @Override
    protected void setTag(ComponentItemWrapper item, Object value, Object... path) {
        Tag valueTag;
        if (value instanceof Tag tag) {
            valueTag = tag;
        } else if (value instanceof JsonElement je) {
            valueTag = MRegistryOps.JSON.convertTo(MRegistryOps.SPARROW_NBT, je);
        } else if (CoreReflections.clazz$Tag.isInstance(value)) {
            valueTag = MRegistryOps.NBT.convertTo(MRegistryOps.SPARROW_NBT, value);
        } else {
            assert MRegistryOps.JAVA != null;
            valueTag = MRegistryOps.JAVA.convertTo(MRegistryOps.SPARROW_NBT, value);
        }

        CompoundTag rootTag = (CompoundTag) item.getSparrowNBTComponent(ComponentTypes.CUSTOM_DATA).orElse(new CompoundTag());

        if (path == null || path.length == 0) {
            if (valueTag instanceof CompoundTag) {
                rootTag = (CompoundTag) valueTag;
            } else {
                throw new IllegalArgumentException("Cannot set non-CompoundTag as root without path");
            }
        } else {
            CompoundTag currentTag = rootTag;
            for (int i = 0; i < path.length - 1; i++) {
                Object pathSegment = path[i];
                if (pathSegment == null) throw new NullPointerException("Path segment cannot be null");

                String key = pathSegment.toString();
                Tag nextTag = currentTag.get(key);

                if (!(nextTag instanceof CompoundTag)) {
                    nextTag = new CompoundTag();
                    currentTag.put(key, nextTag);
                }
                currentTag = (CompoundTag) nextTag;
            }

            String finalKey = path[path.length - 1].toString();
            currentTag.put(finalKey, valueTag);
        }

        item.setSparrowNBTComponent(ComponentTypes.CUSTOM_DATA, rootTag);
    }

    @Override
    protected boolean hasTag(ComponentItemWrapper item, Object... path) {
        return getNBTTag(item, path) != null;
    }

    @Override
    protected boolean removeTag(ComponentItemWrapper item, Object... path) {
        CompoundTag rootTag = (CompoundTag) item.getSparrowNBTComponent(ComponentTypes.CUSTOM_DATA).orElse(null);
        if (rootTag == null || path == null || path.length == 0) return false;

        if (path.length == 1) {
            String key = path[0].toString();
            if (rootTag.containsKey(key)) {
                rootTag.remove(key);
                item.setSparrowNBTComponent(ComponentTypes.CUSTOM_DATA, rootTag);
                return true;
            }
            return false;
        }

        CompoundTag parentTag = rootTag;
        for (int i = 0; i < path.length - 1; i++) {
            Object pathSegment = path[i];
            if (pathSegment == null) return false;

            String key = pathSegment.toString();
            Tag childTag = parentTag.get(key);

            if (!(childTag instanceof CompoundTag)) {
                return false;
            }
            parentTag = (CompoundTag) childTag;
        }

        String finalKey = path[path.length - 1].toString();
        if (parentTag.containsKey(finalKey)) {
            parentTag.remove(finalKey);
            item.setSparrowNBTComponent(ComponentTypes.CUSTOM_DATA, rootTag);
            return true;
        }
        return false;
    }

    @Override
    protected Optional<String> tooltipStyle(ComponentItemWrapper item) {
        throw new UnsupportedOperationException("This feature is not available on 1.21.2+");
    }

    @Override
    protected void tooltipStyle(ComponentItemWrapper item, String data) {
        throw new UnsupportedOperationException("This feature is not available on 1.21.2+");
    }

    @Override
    protected void setComponent(ComponentItemWrapper item, Object type, Object value) {
        item.setComponent(type, value);
    }

    @Override
    protected void setJavaComponent(ComponentItemWrapper item, Object type, Object value) {
        item.setJavaComponent(type, value);
    }

    @Override
    protected void setJsonComponent(ComponentItemWrapper item, Object type, JsonElement value) {
        item.setJsonComponent(type, value);
    }

    @Override
    protected void setNBTComponent(ComponentItemWrapper item, Object type, Tag value) {
        item.setSparrowNBTComponent(type, value);
    }

    @Override
    protected void resetComponent(ComponentItemWrapper item, Object type) {
        item.resetComponent(type);
    }

    @Override
    protected Object getExactComponent(ComponentItemWrapper item, Object type) {
        return item.getComponentExact(type);
    }

    @Override
    protected Object getJavaComponent(ComponentItemWrapper item, Object type) {
        return item.getJavaComponent(type).orElse(null);
    }

    @Override
    protected JsonElement getJsonComponent(ComponentItemWrapper item, Object type) {
        return item.getJsonComponent(type).orElse(null);
    }

    @Override
    protected Tag getNBTComponent(ComponentItemWrapper item, Object type) {
        return item.getSparrowNBTComponent(type).orElse(null);
    }

    @Override
    protected boolean hasComponent(ComponentItemWrapper item, Object type) {
        return item.hasComponent(type);
    }

    @Override
    protected void removeComponent(ComponentItemWrapper item, Object type) {
        item.removeComponent(type);
    }

    @Override
    protected void customModelData(ComponentItemWrapper item, Integer data) {
        if (data == null) {
            item.resetComponent(ComponentTypes.CUSTOM_MODEL_DATA);
        } else {
            item.setJavaComponent(ComponentTypes.CUSTOM_MODEL_DATA, data);
        }
    }

    @Override
    protected Optional<Integer> customModelData(ComponentItemWrapper item) {
        return item.getJavaComponent(ComponentTypes.CUSTOM_MODEL_DATA);
    }

    @Override
    protected void customNameJson(ComponentItemWrapper item, String json) {
        if (json == null) {
            item.resetComponent(ComponentTypes.CUSTOM_NAME);
        } else {
            item.setJavaComponent(ComponentTypes.CUSTOM_NAME, json);
        }
    }

    @Override
    protected Optional<String> customNameJson(ComponentItemWrapper item) {
        return item.getJavaComponent(ComponentTypes.CUSTOM_NAME);
    }

    @Override
    protected void itemNameJson(ComponentItemWrapper item, String json) {
        if (json == null) {
            item.resetComponent(ComponentTypes.ITEM_NAME);
        } else {
            item.setJavaComponent(ComponentTypes.ITEM_NAME, json);
        }
    }

    @Override
    protected Optional<String> itemNameJson(ComponentItemWrapper item) {
        return item.getJavaComponent(ComponentTypes.ITEM_NAME);
    }

    @Override
    protected void skull(ComponentItemWrapper item, String skullData) {
        if (skullData == null) {
            item.resetComponent(ComponentTypes.PROFILE);
        } else {
            Map<String, Object> profile = Map.of("properties", List.of(Map.of("name", "textures", "value", skullData)));
            item.setJavaComponent(ComponentTypes.PROFILE, profile);
        }
    }

    @Override
    protected Optional<List<String>> loreJson(ComponentItemWrapper item) {
        return item.getJavaComponent(ComponentTypes.LORE);
    }

    @Override
    protected void loreJson(ComponentItemWrapper item, List<String> lore) {
        if (lore == null || lore.isEmpty()) {
            item.resetComponent(ComponentTypes.LORE);
        } else {
            item.setJavaComponent(ComponentTypes.LORE, lore);
        }
    }

    @Override
    protected boolean unbreakable(ComponentItemWrapper item) {
        return item.hasComponent(ComponentTypes.UNBREAKABLE);
    }

    @Override
    protected void unbreakable(ComponentItemWrapper item, boolean unbreakable) {
        if (unbreakable) {
            item.resetComponent(ComponentTypes.UNBREAKABLE);
        } else {
            item.setJavaComponent(ComponentTypes.UNBREAKABLE, true);
        }
    }

    @Override
    protected Optional<Boolean> glint(ComponentItemWrapper item) {
        return Optional.ofNullable((Boolean) item.getComponentExact(ComponentTypes.ENCHANTMENT_GLINT_OVERRIDE));
    }

    @Override
    protected void glint(ComponentItemWrapper item, Boolean glint) {
        if (glint == null) {
            item.resetComponent(ComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        } else {
            item.setJavaComponent(ComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, glint);
        }
    }

    @Override
    protected Optional<Integer> damage(ComponentItemWrapper item) {
        return item.getJavaComponent(ComponentTypes.DAMAGE);
    }

    @Override
    protected void damage(ComponentItemWrapper item, Integer damage) {
        if (damage == null) {
            item.resetComponent(ComponentTypes.DAMAGE);
        } else {
            item.setJavaComponent(ComponentTypes.DAMAGE, damage);
        }
    }

    @Override
    protected Optional<Integer> dyedColor(ComponentItemWrapper item) {
        if (!item.hasComponent(ComponentTypes.DYED_COLOR)) return Optional.empty();
        Object javaObj = getJavaComponent(item, ComponentTypes.DYED_COLOR);
        if (javaObj instanceof Integer integer) {
            return Optional.of(integer);
        } else if (javaObj instanceof Map<?, ?> map) {
            return Optional.of((int) map.get("rgb"));
        }
        return Optional.empty();
    }

    @Override
    protected void dyedColor(ComponentItemWrapper item, Integer color) {
        if (color == null) {
            item.resetComponent(ComponentTypes.DYED_COLOR);
        } else {
            item.setJavaComponent(ComponentTypes.DYED_COLOR, color);
        }
    }

    @Override
    protected int maxDamage(ComponentItemWrapper item) {
        Optional<Integer> damage = item.getJavaComponent(ComponentTypes.MAX_DAMAGE);
        return damage.orElseGet(() -> (int) item.getItem().getType().getMaxDurability());
    }

    @Override
    protected void maxDamage(ComponentItemWrapper item, Integer damage) {
        if (damage == null) {
            item.resetComponent(ComponentTypes.MAX_DAMAGE);
        } else {
            item.setJavaComponent(ComponentTypes.MAX_DAMAGE, damage);
        }
    }

    @Override
    protected Optional<Enchantment> getEnchantment(ComponentItemWrapper item, Key key) {
        Object enchant = item.getComponentExact(ComponentTypes.ENCHANTMENTS);
        try {
            Map<String, Integer> map = EnchantmentUtils.toMap(enchant);
            Integer level = map.get(key.toString());
            if (level == null) return Optional.empty();
            return Optional.of(new Enchantment(key, level));
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to get enchantment " + key, e);
            return Optional.empty();
        }
    }

    @Override
    protected void enchantments(ComponentItemWrapper item, List<Enchantment> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            item.resetComponent(ComponentTypes.ENCHANTMENTS);
        } else {
            Map<String, Integer> enchants = new HashMap<>();
            for (Enchantment enchantment : enchantments) {
                enchants.put(enchantment.id().toString(), enchantment.level());
            }
            item.setJavaComponent(ComponentTypes.ENCHANTMENTS, enchants);
        }
    }

    @Override
    protected void storedEnchantments(ComponentItemWrapper item, List<Enchantment> enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            item.resetComponent(ComponentTypes.STORED_ENCHANTMENTS);
        } else {
            Map<String, Integer> enchants = new HashMap<>();
            for (Enchantment enchantment : enchantments) {
                enchants.put(enchantment.id().toString(), enchantment.level());
            }
            item.setJavaComponent(ComponentTypes.STORED_ENCHANTMENTS, enchants);
        }
    }

    @Override
    protected void itemFlags(ComponentItemWrapper item, List<String> flags) {
        throw new UnsupportedOperationException("This feature is not available on 1.20.5+");
    }

    @Override
    protected int maxStackSize(ComponentItemWrapper item) {
        Optional<Integer> stackSize = item.getJavaComponent(ComponentTypes.MAX_STACK_SIZE);
        return stackSize.orElseGet(() -> item.getItem().getType().getMaxStackSize());
    }

    @Override
    protected void maxStackSize(ComponentItemWrapper item, Integer maxStackSize) {
        if (maxStackSize == null) {
            item.resetComponent(ComponentTypes.MAX_STACK_SIZE);
        } else {
            item.setJavaComponent(ComponentTypes.MAX_STACK_SIZE, maxStackSize);
        }
    }

    @Override
    protected void repairCost(ComponentItemWrapper item, Integer data) {
        if (data == null) {
            item.resetComponent(ComponentTypes.REPAIR_COST);
        } else {
            item.setJavaComponent(ComponentTypes.REPAIR_COST, data);
        }
    }

    @Override
    protected Optional<Integer> repairCost(ComponentItemWrapper item) {
        return item.getJavaComponent(ComponentTypes.REPAIR_COST);
    }

    @Override
    protected void trim(ComponentItemWrapper item, Trim trim) {
        if (trim == null) {
            item.resetComponent(ComponentTypes.TRIM);
        } else {
            item.setJavaComponent(ComponentTypes.TRIM, Map.of(
                    "pattern", trim.pattern(),
                    "material", trim.material()
            ));
        }
    }

    @Override
    protected Optional<Trim> trim(ComponentItemWrapper item) {
        Optional<Object> trim = item.getJavaComponent(ComponentTypes.TRIM);
        if (trim.isEmpty()) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked")
        Map<String, String> trimMap = (Map<String, String>) trim.get();
        return Optional.of(new Trim(trimMap.get("pattern"), trimMap.get("material")));
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Optional<FireworkExplosion> fireworkExplosion(ComponentItemWrapper item) {
        Optional<Object> optionalExplosion = item.getJavaComponent(ComponentTypes.FIREWORK_EXPLOSION);
        if (optionalExplosion.isEmpty()) return Optional.empty();
        Map<String, Object> explosions = MiscUtils.castToMap(optionalExplosion.get(), false);
        FireworkExplosion.Shape shape = Optional.ofNullable(FireworkExplosion.Shape.byName((String) explosions.get("shape"))).orElse(FireworkExplosion.Shape.SMALL_BALL);
        boolean hasTrail = (boolean) explosions.getOrDefault("has_trail", false);
        boolean hasTwinkler = (boolean) explosions.getOrDefault("has_twinkle", false);
        List<Integer> colors = (List<Integer>) Optional.ofNullable(explosions.get("colors")).orElse(new IntArrayList());
        List<Integer> fadeColors = (List<Integer>) Optional.ofNullable(explosions.get("fade_colors")).orElse(new IntArrayList());
        return Optional.of(new FireworkExplosion(
                shape,
                new IntArrayList(colors),
                new IntArrayList(fadeColors),
                hasTrail,
                hasTwinkler
        ));
    }

    @Override
    protected void fireworkExplosion(ComponentItemWrapper item, FireworkExplosion explosion) {
        if (explosion == null) {
            item.resetComponent(ComponentTypes.FIREWORK_EXPLOSION);
        } else {
            item.setJavaComponent(ComponentTypes.FIREWORK_EXPLOSION, Map.of(
                    "shape", explosion.shape().getName(),
                    "has_trail", explosion.hasTrail(),
                    "has_twinkle", explosion.hasTwinkle(),
                    "colors", explosion.colors(),
                    "fade_colors", explosion.fadeColors()
            ));
        }
    }

    @Override
    protected ComponentItemWrapper mergeCopy(ComponentItemWrapper item1, ComponentItemWrapper item2) {
        Object itemStack1 = item1.getLiteralObject();
        Object itemStack2 = item2.getLiteralObject();
        Object itemStack3 = FastNMS.INSTANCE.method$ItemStack$transmuteCopy(itemStack1, FastNMS.INSTANCE.method$ItemStack$getItem(itemStack2), item2.count());
        FastNMS.INSTANCE.method$ItemStack$applyComponents(itemStack3, FastNMS.INSTANCE.method$ItemStack$getComponentsPatch(itemStack2));
        return new ComponentItemWrapper(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(itemStack3));
    }

    @Override
    protected void merge(ComponentItemWrapper item1, ComponentItemWrapper item2) {
        Object itemStack1 = item1.getLiteralObject();
        Object itemStack2 = item2.getLiteralObject();
        try {
            FastNMS.INSTANCE.method$ItemStack$applyComponents(itemStack1, FastNMS.INSTANCE.method$ItemStack$getComponentsPatch(itemStack2));
        } catch (Exception e) {
            this.plugin.logger().warn("Failed to merge item", e);
        }
    }

    @Override
    protected ComponentItemWrapper transmuteCopy(ComponentItemWrapper item, Key newItem, int amount) {
        Object itemStack1 = item.getLiteralObject();
        Object itemStack2 = FastNMS.INSTANCE.method$ItemStack$transmuteCopy(itemStack1, FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.ITEM, KeyUtils.toResourceLocation(newItem)), amount);
        return new ComponentItemWrapper(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(itemStack2));
    }

    @Override
    protected ComponentItemWrapper unsafeTransmuteCopy(ComponentItemWrapper item, Object newItem, int amount) {
        Object itemStack1 = item.getLiteralObject();
        Object itemStack2 = FastNMS.INSTANCE.method$ItemStack$transmuteCopy(itemStack1, newItem, amount);
        return new ComponentItemWrapper(FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(itemStack2));
    }
}