package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.SupportedNBTTagValues;
import io.lumine.mythic.lib.api.util.AltChar;
import io.lumine.mythic.lib.gson.JsonArray;
import io.lumine.mythic.lib.gson.JsonParser;
import io.lumine.mythic.lib.gson.JsonSyntaxException;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.edition.StatEdition;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.ReadMMOItem;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import net.Indyuce.mmoitems.stat.data.StringListData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import io.lumine.mythic.lib.util.lang3.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@VersionDependant(version = {1, 20, 6})
public class CanBreak extends ItemStat<StringListData, StringListData> {
    public CanBreak() {
        super("CAN_BREAK",
                Material.IRON_PICKAXE,
                "Can Break?",
                new String[]{"The list of materials that this item can break.", "This restriction only applies to adventure mode."},
                new String[]{"tool"});
    }

    @Override
    @SuppressWarnings("unchecked")
    public StringListData whenInitialized(Object object) {
        Validate.isTrue(object instanceof List<?>, "Must specify a string list");
        return new StringListData((List<String>) object);
    }

    @Override
    public void whenClicked(@NotNull EditionInventory inv, @NotNull InventoryClickEvent event) {
        if (event.getAction() == InventoryAction.PICKUP_ALL)
            new StatEdition(inv, this).enable("Write in the chat the name of the material you want to add.");

        if (event.getAction() != InventoryAction.PICKUP_HALF || !inv.getEditedSection().contains(getPath()))
            return;

        List<String> list = inv.getEditedSection().getStringList(getPath());
        if (list.isEmpty()) return;

        String last = list.removeLast();
        inv.getEditedSection().set(getPath(), list);
        inv.registerTemplateEdition();
        inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + "Successfully removed '" + last + "'.");
    }

    @Override
    public void whenInput(@NotNull EditionInventory inv, @NotNull String message, Object... info) {
        List<String> list = inv.getEditedSection().contains(getPath()) ? inv.getEditedSection().getStringList(getPath()) : new ArrayList<>();

        // Validate material
        final Material material = Material.valueOf(UtilityMethods.enumName(message));

        list.add(material.name());
        inv.getEditedSection().set(getPath(), list);
        inv.registerTemplateEdition();
        inv.getPlayer().sendMessage(MMOItems.plugin.getPrefix() + "Material " + material.name() + " successfully added.");
    }

    @Override
    public void whenDisplayed(List<String> lore, Optional<StringListData> statData) {
        if (statData.isPresent()) {
            lore.add(ChatColor.GRAY + "Current Value:");
            statData.get().getList().forEach(str -> lore.add(ChatColor.GRAY + "* " + str));
        } else
            lore.add(ChatColor.GRAY + "Current Value: " + ChatColor.RED + "Cannot mine any item (adventure mode).");

        lore.add("");
        lore.add(ChatColor.YELLOW + AltChar.listDash + " Click to add a new material.");
        lore.add(ChatColor.YELLOW + AltChar.listDash + " Right click to remove the last material.");
    }

    @NotNull
    @Override
    public StringListData getClearStatData() {
        return new StringListData();
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull StringListData data) {
        List<Material> mats = data.getList().stream().map(Material::valueOf).collect(Collectors.toList());
        item.addFutureAction(nbt -> {
            nbt.setCanMine(mats);
        });

        item.addItemTag(getAppliedNBT(data));
    }

    @NotNull
    @Override
    public ArrayList<ItemTag> getAppliedNBT(@NotNull StringListData data) {
        // Build Json Array
        JsonArray array = new JsonArray();

        // For each string in the ids of the data
        for (String sts : data.getList()) {
            array.add(sts);
        }

        // Make returning array
        ArrayList<ItemTag> tags = new ArrayList<>();

        // Add Json Array
        tags.add(new ItemTag(getNBTPath(), array.toString()));

        return tags;
    }

    @Override
    public void whenLoaded(@NotNull ReadMMOItem mmoitem) {
        // FInd relevant tags
        ArrayList<ItemTag> relevantTags = new ArrayList<>();
        if (mmoitem.getNBT().hasTag(getNBTPath()))
            relevantTags.add(ItemTag.getTagAtPath(getNBTPath(), mmoitem.getNBT(), SupportedNBTTagValues.STRING));

        // Generate data
        StatData data = getLoadedNBT(relevantTags);

        if (data != null)
            mmoitem.setData(this, data);
    }

    @Nullable
    @Override
    public StringListData getLoadedNBT(@NotNull ArrayList<ItemTag> storedTags) {
        // Find relevant tag
        ItemTag rTag = ItemTag.getTagAtPath(getNBTPath(), storedTags);

        // Found?
        if (rTag == null)
            // Nope
            return null;

        try {
            // Parse onto Json Array
            JsonArray array = new JsonParser().parse((String) rTag.getValue()).getAsJsonArray();

            // Make and return list
            return new StringListData(array);
        } catch (JsonSyntaxException | IllegalStateException exception) {
            /*
             * OLD ITEM WHICH MUST BE UPDATED.
             */
        }
        return null;
    }
}
