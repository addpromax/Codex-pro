package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviors;
import net.momirealms.craftengine.core.item.data.Enchantment;
import net.momirealms.craftengine.core.item.data.JukeboxPlayable;
import net.momirealms.craftengine.core.item.modifier.*;
import net.momirealms.craftengine.core.item.setting.EquipmentData;
import net.momirealms.craftengine.core.item.setting.ItemEquipment;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.ResourceLocation;
import net.momirealms.craftengine.core.pack.misc.Equipment;
import net.momirealms.craftengine.core.pack.model.*;
import net.momirealms.craftengine.core.pack.model.generation.AbstractModelGenerator;
import net.momirealms.craftengine.core.pack.model.generation.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.select.ChargeTypeSelectProperty;
import net.momirealms.craftengine.core.pack.model.select.TrimMaterialSelectProperty;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.plugin.context.event.EventFunctions;
import net.momirealms.craftengine.core.plugin.context.text.TextProvider;
import net.momirealms.craftengine.core.plugin.context.text.TextProviders;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.*;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.type.Either;
import org.joml.Vector3f;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class AbstractItemManager<I> extends AbstractModelGenerator implements ItemManager<I> {
    protected static final Map<Key, List<ItemBehavior>> VANILLA_ITEM_EXTRA_BEHAVIORS = new HashMap<>();
    protected static final Set<Key> VANILLA_ITEMS = new HashSet<>(1024);
    protected static final Map<Key, List<Holder<Key>>> VANILLA_ITEM_TAGS = new HashMap<>();

    private final ItemParser itemParser;
    protected final Map<String, ExternalItemProvider<I>> externalItemProviders = new HashMap<>();
    protected final Map<String, Function<Object, ItemDataModifier<I>>> dataFunctions = new HashMap<>();
    protected final Map<Key, CustomItem<I>> customItems = new HashMap<>();
    protected final Map<Key, List<Holder<Key>>> customItemTags;
    protected final Map<Key, Map<Integer, Key>> cmdConflictChecker;
    protected final Map<Key, ModernItemModel> modernItemModels1_21_4;
    protected final Map<Key, TreeSet<LegacyOverridesModel>> modernItemModels1_21_2;
    protected final Map<Key, TreeSet<LegacyOverridesModel>> legacyOverrides;
    protected final Map<Key, TreeMap<Integer, ModernItemModel>> modernOverrides;
    protected final Map<Key, Equipment> equipmentsToGenerate;
    // Cached command suggestions
    protected final List<Suggestion> cachedSuggestions = new ArrayList<>();
    protected final List<Suggestion> cachedTotemSuggestions = new ArrayList<>();

    protected AbstractItemManager(CraftEngine plugin) {
        super(plugin);
        this.itemParser = new ItemParser();
        this.registerFunctions();
        this.legacyOverrides = new HashMap<>();
        this.modernOverrides = new HashMap<>();
        this.customItemTags = new HashMap<>();
        this.cmdConflictChecker = new HashMap<>();
        this.modernItemModels1_21_4 = new HashMap<>();
        this.modernItemModels1_21_2 = new HashMap<>();
        this.equipmentsToGenerate = new HashMap<>();
    }

    @Override
    public void registerDataType(Function<Object, ItemDataModifier<I>> factory, String... alias) {
        for (String a : alias) {
            dataFunctions.put(a, factory);
        }
    }

    protected static void registerVanillaItemExtraBehavior(ItemBehavior behavior, Key... items) {
        for (Key key : items) {
            VANILLA_ITEM_EXTRA_BEHAVIORS.computeIfAbsent(key, k -> new ArrayList<>()).add(behavior);
        }
    }

    protected void applyDataFunctions(Map<String, Object> dataSection, Consumer<ItemDataModifier<I>> consumer) {
        if (dataSection != null) {
            for (Map.Entry<String, Object> dataEntry : dataSection.entrySet()) {
                Optional.ofNullable(dataFunctions.get(dataEntry.getKey())).ifPresent(function -> {
                    try {
                        consumer.accept(function.apply(dataEntry.getValue()));
                    } catch (IllegalArgumentException e) {
                        plugin.logger().warn("Invalid data format", e);
                    }
                });
            }
        }
    }

    @Override
    public ConfigParser parser() {
        return this.itemParser;
    }

    @Override
    public ExternalItemProvider<I> getExternalItemProvider(String name) {
        return this.externalItemProviders.get(name);
    }

    @Override
    public boolean registerExternalItemProvider(ExternalItemProvider<I> externalItemProvider) {
        if (this.externalItemProviders.containsKey(externalItemProvider.plugin())) return false;
        this.externalItemProviders.put(externalItemProvider.plugin(), externalItemProvider);
        return true;
    }

    @Override
    public void unload() {
        super.clearModelsToGenerate();
        this.customItems.clear();
        this.cachedSuggestions.clear();
        this.cachedTotemSuggestions.clear();
        this.legacyOverrides.clear();
        this.modernOverrides.clear();
        this.customItemTags.clear();
        this.equipmentsToGenerate.clear();
        this.cmdConflictChecker.clear();
        this.modernItemModels1_21_4.clear();
        this.modernItemModels1_21_2.clear();
    }

    @Override
    public Optional<CustomItem<I>> getCustomItem(Key key) {
        return Optional.ofNullable(this.customItems.get(key));
    }

    @Override
    public boolean addCustomItem(CustomItem<I> customItem) {
        Key id = customItem.id();
        if (this.customItems.containsKey(id)) return false;
        this.customItems.put(id, customItem);
        // cache command suggestions
        this.cachedSuggestions.add(Suggestion.suggestion(id.toString()));
        // totem animations
        if (VersionHelper.isOrAbove1_21_2()) {
            this.cachedTotemSuggestions.add(Suggestion.suggestion(id.toString()));
        } else if (customItem.material().equals(ItemKeys.TOTEM_OF_UNDYING)) {
            this.cachedTotemSuggestions.add(Suggestion.suggestion(id.toString()));
        }
        // tags
        Set<Key> tags = customItem.settings().tags();
        for (Key tag : tags) {
            this.customItemTags.computeIfAbsent(tag, k -> new ArrayList<>()).add(customItem.idHolder());
        }
        // equipment generation
        ItemEquipment equipment = customItem.settings().equipment();
        if (equipment != null) {
            EquipmentData data = equipment.data();
            Equipment equipmentJson = this.equipmentsToGenerate.computeIfAbsent(data.assetId(), k -> new Equipment());
            equipmentJson.addAll(equipment);
        }
        return true;
    }

    @Override
    public List<Holder<Key>> tagToItems(Key tag) {
        List<Holder<Key>> items = new ArrayList<>();
        List<Holder<Key>> holders = VANILLA_ITEM_TAGS.get(tag);
        if (holders != null) {
            items.addAll(holders);
        }
        List<Holder<Key>> customItems = this.customItemTags.get(tag);
        if (customItems != null) {
            items.addAll(customItems);
        }
        return items;
    }

    @Override
    public List<Holder<Key>> tagToVanillaItems(Key tag) {
        return Collections.unmodifiableList(VANILLA_ITEM_TAGS.getOrDefault(tag, List.of()));
    }

    @Override
    public List<Holder<Key>> tagToCustomItems(Key tag) {
        return Collections.unmodifiableList(this.customItemTags.getOrDefault(tag, List.of()));
    }

    @Override
    public Collection<Suggestion> cachedSuggestions() {
        return Collections.unmodifiableCollection(this.cachedSuggestions);
    }

    @Override
    public Collection<Suggestion> cachedTotemSuggestions() {
        return Collections.unmodifiableCollection(this.cachedTotemSuggestions);
    }

    @Override
    public Optional<List<ItemBehavior>> getItemBehavior(Key key) {
        Optional<CustomItem<I>> customItemOptional = getCustomItem(key);
        if (customItemOptional.isPresent()) {
            CustomItem<I> customItem = customItemOptional.get();
            Key vanillaMaterial = customItem.material();
            List<ItemBehavior> behavior = VANILLA_ITEM_EXTRA_BEHAVIORS.get(vanillaMaterial);
            if (behavior != null) {
                return Optional.of(Stream.concat(customItem.behaviors().stream(), behavior.stream()).toList());
            } else {
                return Optional.of(List.copyOf(customItem.behaviors()));
            }
        } else {
            List<ItemBehavior> behavior = VANILLA_ITEM_EXTRA_BEHAVIORS.get(key);
            if (behavior != null) {
                return Optional.of(List.copyOf(behavior));
            } else {
                return Optional.empty();
            }
        }
    }

    @Override
    public Collection<Key> items() {
        return Collections.unmodifiableCollection(this.customItems.keySet());
    }

    @Override
    public Map<Key, ModernItemModel> modernItemModels1_21_4() {
        return Collections.unmodifiableMap(this.modernItemModels1_21_4);
    }

    @Override
    public Map<Key, TreeSet<LegacyOverridesModel>> modernItemModels1_21_2() {
        return Collections.unmodifiableMap(this.modernItemModels1_21_2);
    }

    @Override
    public Collection<Key> vanillaItems() {
        return Collections.unmodifiableCollection(VANILLA_ITEMS);
    }

    @Override
    public Map<Key, TreeSet<LegacyOverridesModel>> legacyItemOverrides() {
        return Collections.unmodifiableMap(this.legacyOverrides);
    }

    @Override
    public Map<Key, TreeMap<Integer, ModernItemModel>> modernItemOverrides() {
        return Collections.unmodifiableMap(this.modernOverrides);
    }

    @Override
    public Map<Key, Equipment> equipmentsToGenerate() {
        return Collections.unmodifiableMap(this.equipmentsToGenerate);
    }

    @Override
    public boolean isVanillaItem(Key item) {
        return VANILLA_ITEMS.contains(item);
    }

    protected abstract CustomItem.Builder<I> createPlatformItemBuilder(Holder<Key> id, Key material, Key clientBoundMaterial);

    public class ItemParser implements ConfigParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] {"items", "item"};

        private boolean isModernFormatRequired() {
            return Config.packMaxVersion().isAtOrAbove(MinecraftVersions.V1_21_4);
        }

        private boolean needsLegacyCompatibility() {
            return Config.packMinVersion().isBelow(MinecraftVersions.V1_21_4);
        }

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.ITEM;
        }

        @Override
        public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
            if (AbstractItemManager.this.customItems.containsKey(id)) {
                throw new LocalizedResourceConfigException("warning.config.item.duplicate");
            }

            // register for recipes
            Holder.Reference<Key> holder = BuiltInRegistries.OPTIMIZED_ITEM_ID.get(id)
                    .orElseGet(() -> ((WritableRegistry<Key>) BuiltInRegistries.OPTIMIZED_ITEM_ID)
                            .register(new ResourceKey<>(BuiltInRegistries.OPTIMIZED_ITEM_ID.key().location(), id), id));

            boolean isVanillaItem = isVanillaItem(id);
            Key material = Key.from(isVanillaItem ? id.value() : ResourceConfigUtils.requireNonEmptyStringOrThrow(section.get("material"), "warning.config.item.missing_material").toLowerCase(Locale.ENGLISH));
            Key clientBoundMaterial = section.containsKey("client-bound-material") ? Key.from(section.get("client-bound-material").toString().toLowerCase(Locale.ENGLISH)) : material;
            int customModelData = ResourceConfigUtils.getAsInt(section.getOrDefault("custom-model-data", 0), "custom-model-data");
            if (customModelData < 0) {
                throw new LocalizedResourceConfigException("warning.config.item.invalid_custom_model_data", String.valueOf(customModelData));
            }
            if (customModelData > 16_777_216) {
                throw new LocalizedResourceConfigException("warning.config.item.bad_custom_model_data", String.valueOf(customModelData));
            }

            Key itemModelKey = null;

            CustomItem.Builder<I> itemBuilder = createPlatformItemBuilder(holder, material, clientBoundMaterial);
            boolean hasItemModelSection = section.containsKey("item-model");

            // To get at least one model provider
            // Sets some basic model info
            if (customModelData > 0) {
                itemBuilder.dataModifier(new CustomModelDataModifier<>(customModelData));
            }
            // Requires the item to have model before apply item-model
            else if (!hasItemModelSection && section.containsKey("model") && VersionHelper.isOrAbove1_21_2()) {
                // check server version here because components require 1.21.2+
                // customize or use the id
                itemModelKey = Key.from(section.getOrDefault("item-model", id.toString()).toString());
                if (ResourceLocation.isValid(itemModelKey.toString())) {
                    itemBuilder.dataModifier(new ItemModelModifier<>(itemModelKey));
                } else {
                    itemModelKey = null;
                }
            }

            if (hasItemModelSection && VersionHelper.isOrAbove1_21_2()) {
                itemModelKey = Key.from(section.get("item-model").toString());
                itemBuilder.dataModifier(new ItemModelModifier<>(itemModelKey));
            }

            // Get item data
            applyDataFunctions(MiscUtils.castToMap(section.get("data"), true), itemBuilder::dataModifier);
            applyDataFunctions(MiscUtils.castToMap(section.get("client-bound-data"), true), itemBuilder::clientBoundDataModifier);

            // Add custom it here to make sure that id is always applied
            if (!isVanillaItem)
                itemBuilder.dataModifier(new IdModifier<>(id));

            CustomItem<I> customItem = itemBuilder
                    .behaviors(ItemBehaviors.fromObj(pack, path, id, ResourceConfigUtils.get(section, "behavior", "behaviors")))
                    .settings(Optional.ofNullable(ResourceConfigUtils.get(section, "settings"))
                            .map(map -> ItemSettings.fromMap(MiscUtils.castToMap(map, true)))
                            .map(it -> isVanillaItem ? it.canPlaceRelatedVanillaBlock(true) : it)
                            .orElse(ItemSettings.of().canPlaceRelatedVanillaBlock(isVanillaItem)))
                    .events(EventFunctions.parseEvents(ResourceConfigUtils.get(section, "events", "event")))
                    .build();

            addCustomItem(customItem);

            // add it to category
            if (section.containsKey("category")) {
                AbstractItemManager.this.plugin.itemBrowserManager().addExternalCategoryMember(id, MiscUtils.getAsStringList(section.get("category")).stream().map(Key::of).toList());
            }

            // model part, can be null
            // but if it exists, either custom model data or item model should be configured
            Map<String, Object> modelSection = MiscUtils.castToMap(section.get("model"), true);
            Map<String, Object> legacyModelSection = MiscUtils.castToMap(section.get("legacy-model"), true);
            if (modelSection == null && legacyModelSection == null) {
                return;
            }
            // 如果设置了model，但是没有模型值？
            if (customModelData == 0 && itemModelKey == null) {
                throw new LocalizedResourceConfigException("warning.config.item.missing_model_id");
            }
            // 1.21.4+必须要配置model区域
            if (isModernFormatRequired() && modelSection == null) {
                throw new LocalizedResourceConfigException("warning.config.item.missing_model");
            }

            // 新版格式
            ItemModel modernModel = null;
            // 旧版格式
            TreeSet<LegacyOverridesModel> legacyOverridesModels = null;

            if (isModernFormatRequired() || (needsLegacyCompatibility() && legacyModelSection == null)) {
                modernModel = ItemModels.fromMap(modelSection);
                for (ModelGeneration generation : modernModel.modelsToGenerate()) {
                    prepareModelGeneration(generation);
                }
            }
            if (needsLegacyCompatibility()) {
                if (legacyModelSection != null) {
                    LegacyItemModel legacyItemModel = LegacyItemModel.fromMap(legacyModelSection, customModelData);
                    for (ModelGeneration generation : legacyItemModel.modelsToGenerate()) {
                        prepareModelGeneration(generation);
                    }
                    legacyOverridesModels = new TreeSet<>(legacyItemModel.overrides());
                } else {
                    legacyOverridesModels = new TreeSet<>();
                    processModelRecursively(modernModel, new LinkedHashMap<>(), legacyOverridesModels, clientBoundMaterial, customModelData);
                    if (legacyOverridesModels.isEmpty()) {
                        TranslationManager.instance().log("warning.config.item.legacy_model.cannot_convert", path.toString(), id.asString());
                    }
                }
            }

            // use custom model data
            if (customModelData != 0) {
                // use custom model data
                // check conflict
                Map<Integer, Key> conflict = AbstractItemManager.this.cmdConflictChecker.computeIfAbsent(clientBoundMaterial, k -> new HashMap<>());
                if (conflict.containsKey(customModelData)) {
                    throw new LocalizedResourceConfigException("warning.config.item.custom_model_data_conflict", String.valueOf(customModelData), conflict.get(customModelData).toString());
                }
                conflict.put(customModelData, id);
                // Parse models
                if (isModernFormatRequired() && modernModel != null) {
                    TreeMap<Integer, ModernItemModel> map = AbstractItemManager.this.modernOverrides.computeIfAbsent(clientBoundMaterial, k -> new TreeMap<>());
                    map.put(customModelData, new ModernItemModel(
                            modernModel,
                            ResourceConfigUtils.getAsBoolean(section.getOrDefault("oversized-in-gui", true), "oversized-in-gui"),
                            ResourceConfigUtils.getAsBoolean(section.getOrDefault("hand-animation-on-swap", true), "hand-animation-on-swap")
                    ));
                }
                if (needsLegacyCompatibility() && legacyOverridesModels != null && !legacyOverridesModels.isEmpty()) {
                    TreeSet<LegacyOverridesModel> lom = AbstractItemManager.this.legacyOverrides.computeIfAbsent(clientBoundMaterial, k -> new TreeSet<>());
                    lom.addAll(legacyOverridesModels);
                }
            }

            // use item model
            if (itemModelKey != null) {
                if (isModernFormatRequired() && modernModel != null) {
                    AbstractItemManager.this.modernItemModels1_21_4.put(itemModelKey, new ModernItemModel(
                            modernModel,
                            ResourceConfigUtils.getAsBoolean(section.getOrDefault("oversized-in-gui", true), "oversized-in-gui"),
                            ResourceConfigUtils.getAsBoolean(section.getOrDefault("hand-animation-on-swap", true), "hand-animation-on-swap")
                    ));
                }
                if (Config.packMaxVersion().isAtOrAbove(MinecraftVersions.V1_21_2) && needsLegacyCompatibility() && legacyOverridesModels != null && !legacyOverridesModels.isEmpty()) {
                    TreeSet<LegacyOverridesModel> lom = AbstractItemManager.this.modernItemModels1_21_2.computeIfAbsent(itemModelKey, k -> new TreeSet<>());
                    lom.addAll(legacyOverridesModels);
                }
            }
        }
    }

    private void registerFunctions() {
        registerDataType((obj) -> {
            Map<String, Object> data = MiscUtils.castToMap(obj, false);
            String plugin = data.get("plugin").toString();
            String id = data.get("id").toString();
            ExternalItemProvider<I> provider = AbstractItemManager.this.getExternalItemProvider(plugin);
            return new ExternalModifier<>(id, Objects.requireNonNull(provider, "Item provider " + plugin + " not found"));
        }, "external");
        registerDataType((obj) -> {
            String name = obj.toString();
            return new CustomNameModifier<>(Config.nonItalic() ? "<!i>" + name : name);
        }, "custom-name");
        registerDataType((obj) -> {
            String name = obj.toString();
            return new ItemNameModifier<>(Config.nonItalic() ? "<!i>" + name : name);
        }, "item-name", "display-name");
        registerDataType((obj) -> {
            List<String> lore = MiscUtils.getAsStringList(obj).stream().map(it -> "<!i>" + it).toList();
            return new LoreModifier<>(lore);
        }, "lore", "display-lore", "description");
        registerDataType((obj) -> {
            Map<String, List<String>> dynamicLore = new LinkedHashMap<>();
            if (obj instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    dynamicLore.put(entry.getKey().toString(), MiscUtils.getAsStringList(entry.getValue()));
                }
            }
            return new DynamicLoreModifier<>(dynamicLore);
        }, "dynamic-lore");
        registerDataType((obj) -> {
            if (obj instanceof Integer integer) {
                return new DyedColorModifier<>(integer);
            } else {
                Vector3f vector3f = MiscUtils.getAsVector3f(obj, "dyed-color");
                return new DyedColorModifier<>(0 << 24 /*不可省略*/ | MCUtils.fastFloor(vector3f.x) << 16 | MCUtils.fastFloor(vector3f.y) << 8 | MCUtils.fastFloor(vector3f.z));
            }
        }, "dyed-color");
        if (!VersionHelper.isOrAbove1_21_5()) {
            registerDataType((obj) -> {
                Map<String, Object> data = MiscUtils.castToMap(obj, false);
                return new TagsModifier<>(data);
            }, "tags", "tag", "nbt");
        }
        registerDataType((obj) -> {
            boolean value = TypeUtils.checkType(obj, Boolean.class);
            return new UnbreakableModifier<>(value);
        }, "unbreakable");
        registerDataType((obj) -> {
            int customModelData = ResourceConfigUtils.getAsInt(obj, "custom-model-data");
            return new CustomModelDataModifier<>(customModelData);
        }, "custom-model-data");
        registerDataType((obj) -> {
            Map<String, Object> data = MiscUtils.castToMap(obj, false);
            List<Enchantment> enchantments = new ArrayList<>();
            for (Map.Entry<String, Object> e : data.entrySet()) {
                if (e.getValue() instanceof Number number) {
                    enchantments.add(new Enchantment(Key.of(e.getKey()), number.intValue()));
                }
            }
            return new EnchantmentModifier<>(enchantments);
        }, "enchantment", "enchantments", "enchant");
        registerDataType((obj) -> {
            Map<String, Object> data = MiscUtils.castToMap(obj, false);
            String material = data.get("material").toString().toLowerCase(Locale.ENGLISH);
            String pattern = data.get("pattern").toString().toLowerCase(Locale.ENGLISH);
            return new TrimModifier<>(material, pattern);
        }, "trim");
        registerDataType((obj) -> {
            Map<String, Object> data = MiscUtils.castToMap(obj, false);
            Map<String, TextProvider> arguments = new HashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                arguments.put(entry.getKey(), TextProviders.fromString(entry.getValue().toString()));
            }
            return new ArgumentModifier<>(arguments);
        }, "args", "argument", "arguments");
        if (VersionHelper.isOrAbove1_20_5()) {
            registerDataType((obj) -> {
                Map<String, Object> data = MiscUtils.castToMap(obj, false);
                return new ComponentModifier<>(data);
            }, "components", "component");
            registerDataType((obj) -> {
                List<String> data = MiscUtils.getAsStringList(obj);
                return new RemoveComponentModifier<>(data);
            }, "remove-components", "remove-component");
            registerDataType((obj) -> {
               Map<String, Object> data = MiscUtils.castToMap(obj, false);
                int nutrition = ResourceConfigUtils.getAsInt(data.get("nutrition"), "nutrition");
                float saturation = ResourceConfigUtils.getAsFloat(data.get("saturation"), "saturation");
                return new FoodModifier<>(nutrition, saturation, ResourceConfigUtils.getAsBoolean(data.getOrDefault("can-always-eat", false), "can-always-eat"));
            }, "food");
        }
        if (VersionHelper.isOrAbove1_21()) {
            registerDataType((obj) -> {
                String song = obj.toString();
                return new JukeboxSongModifier<>(new JukeboxPlayable(song, true));
            }, "jukebox-playable");
        }
        if (VersionHelper.isOrAbove1_21_2()) {
            registerDataType((obj) -> {
                String id = obj.toString();
                return new TooltipStyleModifier<>(Key.of(id));
            }, "tooltip-style");
            registerDataType((obj) -> {
                Map<String, Object> data = MiscUtils.castToMap(obj, false);
                return new EquippableModifier<>(EquipmentData.fromMap(data));
            }, "equippable");
            registerDataType((obj) -> {
                String id = obj.toString();
                return new ItemModelModifier<>(Key.of(id));
            }, "item-model");
        }
    }

    protected void processModelRecursively(
            ItemModel currentModel,
            Map<String, Object> accumulatedPredicates,
            Collection<LegacyOverridesModel> resultList,
            Key materialId,
            int customModelData
    ) {
        if (currentModel instanceof ConditionItemModel conditionModel) {
            handleConditionModel(conditionModel, accumulatedPredicates, resultList, materialId, customModelData);
        } else if (currentModel instanceof RangeDispatchItemModel rangeModel) {
            handleRangeModel(rangeModel, accumulatedPredicates, resultList, materialId, customModelData);
        } else if (currentModel instanceof SelectItemModel selectModel) {
            handleSelectModel(selectModel, accumulatedPredicates, resultList, materialId, customModelData);
        } else if (currentModel instanceof BaseItemModel baseModel) {
            resultList.add(new LegacyOverridesModel(
                    new LinkedHashMap<>(accumulatedPredicates),
                    baseModel.path(),
                    customModelData
            ));
        } else if (currentModel instanceof SpecialItemModel specialModel) {
            resultList.add(new LegacyOverridesModel(
                    new LinkedHashMap<>(accumulatedPredicates),
                    specialModel.base(),
                    customModelData
            ));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleConditionModel(
            ConditionItemModel model,
            Map<String, Object> parentPredicates,
            Collection<LegacyOverridesModel> resultList,
            Key materialId,
            int customModelData
    ) {
        if (model.property() instanceof LegacyModelPredicate predicate) {
            String predicateId = predicate.legacyPredicateId(materialId);
            Map<String, Object> truePredicates = mergePredicates(
                    parentPredicates,
                    predicateId,
                    predicate.toLegacyValue(true)
            );
            processModelRecursively(
                    model.onTrue(),
                    truePredicates,
                    resultList,
                    materialId,
                    customModelData
            );
            Map<String, Object> falsePredicates = mergePredicates(
                    parentPredicates,
                    predicateId,
                    predicate.toLegacyValue(false)
            );
            processModelRecursively(
                    model.onFalse(),
                    falsePredicates,
                    resultList,
                    materialId,
                    customModelData
            );
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleRangeModel(
            RangeDispatchItemModel model,
            Map<String, Object> parentPredicates,
            Collection<LegacyOverridesModel> resultList,
            Key materialId,
            int customModelData
    ) {
        if (model.property() instanceof LegacyModelPredicate predicate) {
            String predicateId = predicate.legacyPredicateId(materialId);
            for (Map.Entry<Float, ItemModel> entry : model.entries().entrySet()) {
                Map<String, Object> merged = mergePredicates(
                        parentPredicates,
                        predicateId,
                        predicate.toLegacyValue(entry.getKey())
                );
                processModelRecursively(
                        entry.getValue(),
                        merged,
                        resultList,
                        materialId,
                        customModelData
                );
            }
            if (model.fallBack() != null) {
                Map<String, Object> merged = mergePredicates(
                        parentPredicates,
                        predicateId,
                        predicate.toLegacyValue(0f)
                );
                processModelRecursively(
                        model.fallBack(),
                        merged,
                        resultList,
                        materialId,
                        customModelData
                );
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void handleSelectModel(
            SelectItemModel model,
            Map<String, Object> parentPredicates,
            Collection<LegacyOverridesModel> resultList,
            Key materialId,
            int customModelData
    ) {
        if (model.property() instanceof LegacyModelPredicate predicate) {
            String predicateId = predicate.legacyPredicateId(materialId);
            for (Map.Entry<Either<String, List<String>>, ItemModel> entry : model.whenMap().entrySet()) {
                List<String> cases = entry.getKey().fallbackOrMapPrimary(List::of);
                for (String caseValue : cases) {
                    Number legacyValue = predicate.toLegacyValue(caseValue);
                    if (predicate instanceof TrimMaterialSelectProperty property && property.isArmor(materialId)) {
                        if (legacyValue.floatValue() > 1f) {
                            continue;
                        }
                    }
                    Map<String, Object> merged = mergePredicates(
                            parentPredicates,
                            predicateId,
                            legacyValue
                    );
                    // Additional check for crossbow
                    if (predicate instanceof ChargeTypeSelectProperty && materialId.equals(ItemKeys.CROSSBOW)) {
                        merged = mergePredicates(
                                merged,
                                "charged",
                                1
                        );
                    }
                    processModelRecursively(
                            entry.getValue(),
                            merged,
                            resultList,
                            materialId,
                            customModelData
                    );
                }
            }
            // Additional check for crossbow
            if (model.fallBack() != null) {
                if (predicate instanceof ChargeTypeSelectProperty && materialId.equals(ItemKeys.CROSSBOW)) {
                    processModelRecursively(
                            model.fallBack(),
                            mergePredicates(
                                    parentPredicates,
                                    "charged",
                                    0
                            ),
                            resultList,
                            materialId,
                            customModelData
                    );
                } else if (predicate instanceof TrimMaterialSelectProperty property && property.isArmor(materialId)) {
                    processModelRecursively(
                            model.fallBack(),
                            mergePredicates(
                                    parentPredicates,
                                    "trim_type",
                                    0f
                            ),
                            resultList,
                            materialId,
                            customModelData
                    );
                }
            }
        }
    }

    private Map<String, Object> mergePredicates(
            Map<String, Object> existing,
            String newKey,
            Number newValue
    ) {
        Map<String, Object> merged = new LinkedHashMap<>(existing);
        if (newKey == null) return merged;
        merged.put(newKey, newValue);
        return merged;
    }
}
