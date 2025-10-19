package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.bukkit.item.behavior.AxeItemBehavior;
import net.momirealms.craftengine.bukkit.item.behavior.FlintAndSteelItemBehavior;
import net.momirealms.craftengine.bukkit.item.factory.BukkitItemFactory;
import net.momirealms.craftengine.bukkit.item.listener.ArmorEventListener;
import net.momirealms.craftengine.bukkit.item.listener.DebugStickListener;
import net.momirealms.craftengine.bukkit.item.listener.ItemEventListener;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.util.ItemUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.modifier.IdModifier;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

public class BukkitItemManager extends AbstractItemManager<ItemStack> {
    static {
        registerVanillaItemExtraBehavior(FlintAndSteelItemBehavior.INSTANCE, ItemKeys.FLINT_AND_STEEL);
        registerVanillaItemExtraBehavior(AxeItemBehavior.INSTANCE, ItemKeys.AXES);
    }

    private static BukkitItemManager instance;
    private final BukkitItemFactory<? extends ItemWrapper<ItemStack>> factory;
    private final BukkitCraftEngine plugin;
    private final ItemEventListener itemEventListener;
    private final DebugStickListener debugStickListener;
    private final ArmorEventListener armorEventListener;
    private final NetworkItemHandler<ItemStack> networkItemHandler;

    public BukkitItemManager(BukkitCraftEngine plugin) {
        super(plugin);
        instance = this;
        this.plugin = plugin;
        this.factory = BukkitItemFactory.create(plugin);
        this.itemEventListener = new ItemEventListener(plugin);
        this.debugStickListener = new DebugStickListener(plugin);
        this.armorEventListener = new ArmorEventListener();
        this.networkItemHandler = VersionHelper.isOrAbove1_20_5() ? new ModernNetworkItemHandler() : new LegacyNetworkItemHandler();
        this.registerAllVanillaItems();
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this.itemEventListener, this.plugin.javaPlugin());
        Bukkit.getPluginManager().registerEvents(this.debugStickListener, this.plugin.javaPlugin());
        Bukkit.getPluginManager().registerEvents(this.armorEventListener, this.plugin.javaPlugin());
    }

    @Override
    public NetworkItemHandler<ItemStack> networkItemHandler() {
        return this.networkItemHandler;
    }

    public static BukkitItemManager instance() {
        return instance;
    }

    public Optional<ItemStack> s2c(ItemStack itemStack, Player player) {
        try {
            Item<ItemStack> wrapped = wrap(itemStack);
            if (wrapped == null) return Optional.empty();
            return this.networkItemHandler.s2c(wrapped, player).map(Item::getItem);
        } catch (Throwable e) {
            if (Config.debug()) {
                this.plugin.logger().warn("Failed to handle s2c items.", e);
            }
            return Optional.empty();
        }
    }

    public Optional<ItemStack> c2s(ItemStack itemStack) {
        try {
            Item<ItemStack> wrapped = wrap(itemStack);
            if (wrapped == null) return Optional.empty();
            return this.networkItemHandler.c2s(wrapped).map(Item::getItem);
        } catch (Throwable e) {
            if (Config.debug()) {
                this.plugin.logger().warn("Failed to handle c2s items.", e);
            }
            return Optional.empty();
        }
    }

    @Override
    public Optional<BuildableItem<ItemStack>> getVanillaItem(Key key) {
        Material material = Registry.MATERIAL.get(KeyUtils.toNamespacedKey(key));
        if (material == null) {
            return Optional.empty();
        }
        return Optional.of(new CloneableConstantItem(key, new ItemStack(material)));
    }

    @Override
    public int fuelTime(ItemStack itemStack) {
        if (ItemUtils.isEmpty(itemStack)) return 0;
        Optional<CustomItem<ItemStack>> customItem = wrap(itemStack).getCustomItem();
        return customItem.map(it -> it.settings().fuelTime()).orElse(0);
    }

    @Override
    public int fuelTime(Key id) {
        return getCustomItem(id).map(it -> it.settings().fuelTime()).orElse(0);
    }

    @Override
    public void disable() {
        this.unload();
        HandlerList.unregisterAll(this.itemEventListener);
        HandlerList.unregisterAll(this.debugStickListener);
        HandlerList.unregisterAll(this.armorEventListener);
    }

    @SuppressWarnings("deprecation")
    @Override
    public Item<ItemStack> fromByteArray(byte[] bytes) {
        return this.factory.wrap(Bukkit.getUnsafe().deserializeItem(bytes));
    }

    @Override
    public ItemStack buildCustomItemStack(Key id, Player player) {
        return Optional.ofNullable(customItems.get(id)).map(it -> it.buildItemStack(new ItemBuildContext(player, ContextHolder.EMPTY), 1)).orElse(null);
    }

    @Override
    public ItemStack buildItemStack(Key id, @Nullable Player player) {
        return Optional.ofNullable(buildCustomItemStack(id, player)).orElseGet(() -> createVanillaItemStack(id));
    }

    @Override
    public Item<ItemStack> createCustomWrappedItem(Key id, Player player) {
        return Optional.ofNullable(customItems.get(id)).map(it -> it.buildItem(player)).orElse(null);
    }

    private ItemStack createVanillaItemStack(Key id) {
        NamespacedKey key = NamespacedKey.fromString(id.toString());
        if (key == null) {
            this.plugin.logger().warn(id + " is not a valid namespaced key");
            return new ItemStack(Material.AIR);
        }
        Object item = FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.ITEM, KeyUtils.toResourceLocation(id));
        if (item == null) {
            this.plugin.logger().warn(id + " is not a valid material");
            return new ItemStack(Material.AIR);
        }
        return FastNMS.INSTANCE.method$CraftItemStack$asCraftMirror(FastNMS.INSTANCE.constructor$ItemStack(item, 1));
    }

    @Override
    public Item<ItemStack> createWrappedItem(Key id, @Nullable Player player) {
        return Optional.ofNullable(this.customItems.get(id)).map(it -> it.buildItem(player)).orElseGet(() -> {
            ItemStack itemStack = createVanillaItemStack(id);
            return wrap(itemStack);
        });
    }

    @Override
    public Item<ItemStack> wrap(ItemStack itemStack) {
        if (ItemUtils.isEmpty(itemStack)) return null;
        return this.factory.wrap(itemStack);
    }

    @Override
    public Key itemId(ItemStack itemStack) {
        Item<ItemStack> wrapped = wrap(itemStack);
        return wrapped.id();
    }

    @Override
    public Key customItemId(ItemStack itemStack) {
        Item<ItemStack> wrapped = wrap(itemStack);
        if (!wrapped.hasTag(IdModifier.CRAFT_ENGINE_ID)) return null;
        return wrapped.id();
    }

    @Override
    protected CustomItem.Builder<ItemStack> createPlatformItemBuilder(Holder<Key> id, Key materialId, Key clientBoundMaterialId) {
        Object item = FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.ITEM, KeyUtils.toResourceLocation(materialId));
        Object clientBoundItem = materialId == clientBoundMaterialId ? item : FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.ITEM, KeyUtils.toResourceLocation(clientBoundMaterialId));
        if (item == null) {
            throw new LocalizedResourceConfigException("warning.config.item.invalid_material", materialId.toString());
        }
        if (clientBoundItem == null) {
            throw new LocalizedResourceConfigException("warning.config.item.invalid_material", clientBoundMaterialId.toString());
        }
        return BukkitCustomItem.builder(item, clientBoundItem)
                .id(id)
                .material(materialId)
                .clientBoundMaterial(clientBoundMaterialId);
    }

    @SuppressWarnings("unchecked")
    private void registerAllVanillaItems() {
        try {
            for (Object item : (Iterable<?>) MBuiltInRegistries.ITEM) {
                Object resourceLocation = FastNMS.INSTANCE.method$Registry$getKey(MBuiltInRegistries.ITEM, item);
                Key itemKey = KeyUtils.resourceLocationToKey(resourceLocation);
                if (itemKey.namespace().equals("minecraft")) {
                    VANILLA_ITEMS.add(itemKey);
                    Holder.Reference<Key> holder =  BuiltInRegistries.OPTIMIZED_ITEM_ID.get(itemKey)
                            .orElseGet(() -> ((WritableRegistry<Key>) BuiltInRegistries.OPTIMIZED_ITEM_ID)
                                    .register(new ResourceKey<>(BuiltInRegistries.OPTIMIZED_ITEM_ID.key().location(), itemKey), itemKey));
                    Object mcHolder = ((Optional<Object>) CoreReflections.method$Registry$getHolder1.invoke(MBuiltInRegistries.ITEM, CoreReflections.method$ResourceKey$create.invoke(null, MRegistries.ITEM, resourceLocation))).get();
                    Set<Object> tags = (Set<Object>) CoreReflections.field$Holder$Reference$tags.get(mcHolder);
                    for (Object tag : tags) {
                        Key tagId = Key.of(CoreReflections.field$TagKey$location.get(tag).toString());
                        VANILLA_ITEM_TAGS.computeIfAbsent(tagId, (key) -> new ArrayList<>()).add(holder);
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            plugin.logger().warn("Failed to init vanilla items", e);
        }
    }
}
