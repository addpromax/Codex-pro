package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.SupportedNBTTagValues;
import io.lumine.mythic.lib.api.util.AltChar;
import io.lumine.mythic.lib.gson.*;
import io.lumine.mythic.lib.skill.trigger.TriggerType;
import io.lumine.mythic.lib.util.annotation.BackwardsCompatibility;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.ReadMMOItem;
import net.Indyuce.mmoitems.api.util.NumericStatFormula;
import net.Indyuce.mmoitems.api.util.NumericStatFormula.FormulaSaveOption;
import net.Indyuce.mmoitems.gui.edition.AbilityListEdition;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.skill.RegisteredSkill;
import net.Indyuce.mmoitems.stat.data.AbilityData;
import net.Indyuce.mmoitems.stat.data.AbilityListData;
import net.Indyuce.mmoitems.stat.data.random.RandomAbilityData;
import net.Indyuce.mmoitems.stat.data.random.RandomAbilityListData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import io.lumine.mythic.lib.util.lang3.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Abilities extends ItemStat<RandomAbilityListData, AbilityListData> {
    public Abilities() {
        super("ABILITY", Material.BLAZE_POWDER, "Item Abilities",
                new String[]{"Make your item cast amazing abilities", "to kill monsters or buff yourself."}, new String[]{"!block", "all"});
    }

    @Override
    public RandomAbilityListData whenInitialized(Object object) {
        Validate.isTrue(object instanceof ConfigurationSection, "Must specify a valid config section");
        ConfigurationSection config = (ConfigurationSection) object;
        RandomAbilityListData list = new RandomAbilityListData();

        for (String key : config.getKeys(false))
            list.add(new RandomAbilityData(config.getConfigurationSection(key)));

        return list;
    }

    private String generalFormat, modifierIfAny, modifierForEach, modifierSplitter, abilitySplitter, modifierNewLine;
    private boolean useAbilitySplitter;
    private int modifiersPerLine;

    @BackwardsCompatibility(version = "6.10")
    @Override
    public void loadConfiguration(@NotNull ConfigurationSection legacyLanguageFile, @NotNull Object configObject) {

        // COMPATIBILITY CODE FOR MI <6.10
        if (!(configObject instanceof ConfigurationSection)) {
            generalFormat = legacyLanguageFile.getString("ability-format");
            modifierIfAny = "\n";
            modifierNewLine = "\n";
            modifierForEach = legacyLanguageFile.getString("ability-modifier");
            modifierSplitter = "";
            abilitySplitter = legacyLanguageFile.getString("ability-splitter");
            useAbilitySplitter = abilitySplitter != null && !abilitySplitter.isEmpty();
            modifiersPerLine = 1;
            return;
        }

        final ConfigurationSection config = (ConfigurationSection) configObject;
        generalFormat = config.getString("general-format");
        modifierIfAny = config.getString("modifier-if-any", ""); // Backwards compatibility
        modifierNewLine = "\n" + config.getString("modifier-new-line", ""); // Backwards compatibility
        modifierForEach = config.getString("modifier-foreach");
        modifierSplitter = config.getString("modifier-splitter");
        abilitySplitter = config.getString("ability-splitter.format");
        useAbilitySplitter = config.getBoolean("ability-splitter.enabled");
        modifiersPerLine = config.getInt("modifiers-per-line");
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull AbilityListData data) {
        final List<String> abilityLore = new ArrayList<>();

        data.getAbilities().forEach(ability -> {
            final StringBuilder builder = new StringBuilder(generalFormat
                    .replace("{trigger}", MMOItems.plugin.getLanguage().getTriggerTypeName(ability.getTrigger()))
                    .replace("{ability}", ability.getAbility().getName()));

            if (!ability.getModifiers().isEmpty()) builder.append(modifierIfAny);

            boolean newLine = false;
            int lineCounter = 0;
            for (String modifier : ability.getModifiers()) {
                final String formattedValue = MythicLib.plugin.getMMOConfig().decimals.format(ability.getParameter(modifier));
                item.getLore().registerPlaceholder("ability_" + ability.getAbility().getHandler().getId().toLowerCase() + "_" + modifier, formattedValue);
                builder.append(newLine ? modifierNewLine : modifierSplitter);
                builder.append(modifierForEach
                        .replace("{modifier}", ability.getAbility().getParameterName(modifier))
                        .replace("{value}", formattedValue));
                newLine = false;
                lineCounter++;

                if (modifiersPerLine > 0 && lineCounter >= modifiersPerLine) {
                    lineCounter = 0;
                    newLine = true;
                }
            }

            abilityLore.add(builder.toString());

            if (useAbilitySplitter)
                abilityLore.add(abilitySplitter);
        });

        if (useAbilitySplitter && !abilityLore.isEmpty())
            abilityLore.remove(abilityLore.size() - 1);

        // Modify tags
        item.getLore().insert("abilities", abilityLore);
        item.addItemTag(getAppliedNBT(data));
    }

    @Override
    public String getLegacyTranslationPath() {
        return "ability-format";
    }

    @NotNull
    @Override
    public ArrayList<ItemTag> getAppliedNBT(@NotNull AbilityListData data) {

        // Make Array
        ArrayList<ItemTag> ret = new ArrayList<>();

        // Convert to JSON
        JsonArray jsonArray = new JsonArray();
        for (AbilityData ab : data.getAbilities()) {
            jsonArray.add(ab.toJson());
        }

        // Put
        ret.add(new ItemTag(getNBTPath(), jsonArray.toString()));

        return ret;
    }

    @Override
    public void whenClicked(@NotNull EditionInventory inv, @NotNull InventoryClickEvent event) {
        new AbilityListEdition(inv.getNavigator(), inv.getEdited()).open(inv);
    }

    @Override
    public void whenInput(@NotNull EditionInventory inv, @NotNull String message, Object... info) {
        String configKey = (String) info[0];
        String edited = (String) info[1];

        String format = message.toUpperCase().replace("-", "_").replace(" ", "_").replaceAll("[^A-Z0-9_]", "");

        if (edited.equals("ability")) {
            Validate.isTrue(MMOItems.plugin.getSkills().hasSkill(format),
                    format + " is not a valid ability! You may check the ability list using /mi list ability.");
            RegisteredSkill ability = MMOItems.plugin.getSkills().getSkill(format);

            inv.getEditedSection().set("ability." + configKey, null);
            inv.getEditedSection().set("ability." + configKey + ".type", format);
            inv.registerTemplateEdition();
            inv.getPlayer().sendMessage(
                    MMOItems.plugin.getPrefix() + "Successfully set the ability to " + ChatColor.GOLD + ability.getName() + ChatColor.GRAY + ".");
            return;
        }

        if (edited.equals("mode")) {
            TriggerType castMode = TriggerType.valueOf(format);

            inv.getEditedSection().set("ability." + configKey + ".mode", castMode.name());
            inv.registerTemplateEdition();
            inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + "Successfully set the trigger to " + ChatColor.GOLD + castMode.getName()
                    + ChatColor.GRAY + ".");
            return;
        }

        new NumericStatFormula(message).fillConfigurationSection(inv.getEditedSection(), "ability." + configKey + "." + edited,
                FormulaSaveOption.NONE);
        inv.registerTemplateEdition();
        inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + ChatColor.GOLD + UtilityMethods.caseOnWords(edited.replace("-", " ")) + ChatColor.GRAY
                + " successfully added.");
    }

    @Override
    public void whenDisplayed(List<String> lore, Optional<RandomAbilityListData> statData) {
        lore.add(ChatColor.GRAY + "Current Abilities: " + ChatColor.GOLD
                + (statData.isPresent() ? statData.get().getAbilities().size() : 0));
        lore.add("");
        lore.add(ChatColor.YELLOW + AltChar.listDash + " Click to edit the item abilities.");
    }

    @NotNull
    @Override
    public AbilityListData getClearStatData() {
        return new AbilityListData();
    }

    @Override
    public void whenLoaded(@NotNull ReadMMOItem mmoitem) {

        // Get tags
        ArrayList<ItemTag> relevantTags = new ArrayList<>();

        if (mmoitem.getNBT().hasTag(getNBTPath()))
            relevantTags.add(ItemTag.getTagAtPath(getNBTPath(), mmoitem.getNBT(), SupportedNBTTagValues.STRING));

        AbilityListData data = getLoadedNBT(relevantTags);

        // Valid?
        if (data != null) {

            // Set
            mmoitem.setData(this, data);
        }
    }

    @Nullable
    @Override
    public AbilityListData getLoadedNBT(@NotNull ArrayList<ItemTag> storedTags) {

        // Get Json Array thing
        ItemTag jsonCompact = ItemTag.getTagAtPath(getNBTPath(), storedTags);

        // Found?
        if (jsonCompact != null) {

            // Attempt to parse
            try {

                // New ablity list data
                AbilityListData list = new AbilityListData();
                JsonArray ar = new JsonParser().parse((String) jsonCompact.getValue()).getAsJsonArray();

                // Convert every object into an ability
                for (JsonElement e : ar) {

                    // For every object
                    if (e.isJsonObject()) {

                        // Get as object
                        JsonObject obj = e.getAsJsonObject();

                        // Add to abilit list
                        list.add(new AbilityData(obj));
                    }
                }

                // Well that mus thave worked aye?
                return list;

            } catch (JsonSyntaxException | IllegalStateException exception) {
                /*
                 * OLD ITEM WHICH MUST BE UPDATED.
                 */
            }
        }

        // Did not work out
        return null;
    }
}
