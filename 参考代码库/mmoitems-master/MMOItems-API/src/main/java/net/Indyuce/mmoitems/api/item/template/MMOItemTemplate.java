package net.Indyuce.mmoitems.api.item.template;

import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.util.ui.FriendlyFeedbackCategory;
import io.lumine.mythic.lib.api.util.ui.FriendlyFeedbackProvider;
import io.lumine.mythic.lib.util.PostLoadAction;
import io.lumine.mythic.lib.util.PreloadedObject;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.ItemTier;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.ItemReference;
import net.Indyuce.mmoitems.api.item.build.MMOItemBuilder;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.player.RPGPlayer;
import net.Indyuce.mmoitems.api.util.NumericStatFormula;
import net.Indyuce.mmoitems.api.util.message.FFPMMOItems;
import net.Indyuce.mmoitems.stat.data.random.RandomStatData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import io.lumine.mythic.lib.util.lang3.Validate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MMOItemTemplate implements ItemReference, PreloadedObject {
    private final Type type;
    private final String id;
    private final int revId;

    /**
     * Base item data
     */
    private final Map<ItemStat, RandomStatData> base = new HashMap<>();

    @Nullable
    private NumericStatFormula modifierCapacity;

    @Nullable
    private ModifierNode modifierGroup;
    private final Set<TemplateOption> options = new HashSet<>();

    private final PostLoadAction postLoadAction = new PostLoadAction(config -> {
        FriendlyFeedbackProvider ffp = new FriendlyFeedbackProvider(FFPMMOItems.get());
        ffp.activatePrefix(true, getType().getId() + " " + getId());

        // Extra options
        if (config.contains("option")) {
            if (config.contains("option.capacity", true))
                try {
                    modifierCapacity = new NumericStatFormula(config.getConfigurationSection("option.capacity"));
                } catch (RuntimeException exception) {
                    ffp.log(FriendlyFeedbackCategory.ERROR, "Could not load modifier capacity: {0}", exception.getMessage());
                }

            for (TemplateOption option : TemplateOption.values())
                if (config.getBoolean("option." + option.name().toLowerCase().replace("_", "-")))
                    options.add(option);
        }

        // Read modifiers
        try {
            modifierGroup = config.contains("modifiers") ? new ModifierNode(getId(), config) : null;
            if (modifierGroup != null) modifierGroup.getPostLoadAction().performAction();
        } catch (Exception exception) {
            ffp.log(FriendlyFeedbackCategory.ERROR, "Could not load modifier group: {0}", exception.getMessage());
        }

        // Read base item data
        Validate.notNull(config.getConfigurationSection("base"), FriendlyFeedbackProvider.quickForConsole(FFPMMOItems.get(), "Could not find base item data"));
        for (String key : config.getConfigurationSection("base").getKeys(false))
            try {
                final String id = UtilityMethods.enumName(key);
                final ItemStat stat = MMOItems.plugin.getStats().get(id);
                Validate.notNull(stat, FriendlyFeedbackProvider.quickForConsole(FFPMMOItems.get(), "Could not find stat with ID '$i{0}$b'", id));
                RandomStatData data = stat.whenInitialized(config.get("base." + key));
                if (data != null)
                    base.put(stat, data);

            } catch (IllegalArgumentException exception) {

                // Log
                ffp.log(FriendlyFeedbackCategory.INFORMATION, "Could not load base item data '$f{0}$b': {1}", key, exception.getMessage());
            }

        // Print all failures
        ffp.sendTo(FriendlyFeedbackCategory.INFORMATION, MMOItems.getConsole());
    });

    /**
     * Public constructor which can be used to register extra item templates
     * using other addons or plugins
     *
     * @param type The item type of your template
     * @param id   The template identifier, it's ok if two templates with
     *             different item types share the same ID
     */
    public MMOItemTemplate(Type type, String id) {
        this.type = type;
        this.id = id;
        this.revId = 1;
    }

    /**
     * Used to load MMOItem templates from config files
     *
     * @param type   The item type of your template
     * @param config The config file read to load the template
     */
    public MMOItemTemplate(Type type, ConfigurationSection config) {
        Validate.notNull(config, "Could not load template config");

        postLoadAction.cacheConfig(config);

        this.type = type;
        this.id = config.getName().toUpperCase().replace("-", "_").replace(" ", "_");
        this.revId = config.getInt("base.revision-id", 1);
    }

    @NotNull
    @Override
    public PostLoadAction getPostLoadAction() {
        return postLoadAction;
    }

    @NotNull
    public Map<ItemStat, RandomStatData> getBaseItemData() {
        return base;
    }

    @Nullable
    public ModifierNode getModifierGroup() {
        return modifierGroup;
    }

    public boolean hasModifierGroup() {
        return modifierGroup != null;
    }

    @Nullable
    public NumericStatFormula getModifierCapacity() {
        return modifierCapacity;
    }

    public boolean hasModifierCapacity() {
        return modifierCapacity != null;
    }

    @NotNull
    @Deprecated
    public Map<String, ModifierNode> getModifiers() {
        Map<String, ModifierNode> built = new HashMap<>();
        if (modifierGroup != null) exploreMap(built, modifierGroup);
        return built;
    }

    private void exploreMap(Map<String, ModifierNode> built, ModifierNode node) {
        node.getChildren().forEach(child -> exploreMap(built, child));
        built.put(node.getId(), node);
    }

    @Deprecated
    public boolean hasModifier(String id) {
        return getModifiers().containsKey(id);
    }

    @Nullable
    @Deprecated
    public ModifierNode getModifier(String id) {
        return getModifiers().get(id);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public String getId() {
        return id;
    }

    public int getRevisionId() {
        return revId;
    }

    public boolean hasOption(TemplateOption option) {
        return options.contains(option);
    }

    public MMOItemBuilder newBuilder() {
        return newBuilder((RPGPlayer) null);
    }

    public MMOItemBuilder newBuilder(@Nullable Player player) {
        return newBuilder(player == null ? null : PlayerData.get(player).getRPG());
    }

    public MMOItemBuilder newBuilder(@Nullable PlayerData player) {
        return newBuilder(player == null ? null : player.getRPG());
    }

    /**
     * By default, item templates have item level 0 and no random tier. If the
     * template has the 'tiered' recipe option, a random tier will be picked. If
     * the template has the 'level-item' option, a random level will be picked
     *
     * @param player The player for whom you are generating the item. Seems to only
     *               matter when rolling for the 'item level'
     * @return Item builder with random level and tier?
     */
    public MMOItemBuilder newBuilder(@Nullable RPGPlayer player) {
        return newBuilder(player, false);
    }

    /**
     * By default, item templates have item level 0 and no random tier. If the
     * template has the 'tiered' recipe option, a random tier will be picked. If
     * the template has the 'level-item' option, a random level will be picked
     *
     * @param player     The player for whom you are generating the item. Seems to only
     *                   matter when rolling for the 'item level'
     * @param forDisplay Should it take modifiers into account
     * @return Item builder with random level and tier?
     */
    @NotNull
    public MMOItemBuilder newBuilder(@Nullable RPGPlayer player, boolean forDisplay) {

        // No player ~ default settings
        if (player == null) return newBuilder(0, null);

        // Read from player
        int itemLevel = hasOption(TemplateOption.LEVEL_ITEM) ? MMOItems.plugin.getTemplates().rollLevel(player.getLevel()) : 0;
        ItemTier itemTier = hasOption(TemplateOption.TIERED) ? MMOItems.plugin.getTemplates().rollTier() : null;
        return new MMOItemBuilder(this, itemLevel, itemTier, forDisplay);
    }

    /**
     * @param itemLevel The desired item level
     * @param itemTier  The desired item tier, can be null
     * @return Item builder with specific item level and tier
     */
    @NotNull
    public MMOItemBuilder newBuilder(int itemLevel, @Nullable ItemTier itemTier) {
        return new MMOItemBuilder(this, itemLevel, itemTier);
    }

    /**
     * Used for simple boolean template options
     */
    public enum TemplateOption {

        /**
         * When the item is being generated, modifiers are rolled in a random
         * order so you never the same modifiers again and again.
         */
        ROLL_MODIFIER_CHECK_ORDER,

        /**
         * When building an item based on this template, if no tier is
         * specified, a random tier will be chosen for the item. By default, the
         * item has no tier OR has the tier given in the config file.
         */
        TIERED,

        /**
         * When building an item based on this template, if no level is
         * specified, a random level will be rolled based on the player's level
         * or 0 if no player is specified. By default, items are generated with
         * level 0
         */
        LEVEL_ITEM
    }

    /**
     * @return Attempts to get the crafted amount registered in the Stat.
     * <p></p>
     * Default is <b>1</b> obviously.
     * @deprecated Don't use this method, the Crafted Amount Stat will be deleted in the near future.
     */
    @Deprecated
    public int getCraftedAmount() {

		/* Attempt to find	|
		NumericStatFormula ofAmount = (NumericStatFormula) base.get(ItemStats.CRAFT_AMOUNT);

		// Found?
		if (ofAmount != null) {

			// Well what does it read
			return SilentNumbers.ceil(ofAmount.calculate(0));
		}
		 */

        // No
        return 1;
    }
}
