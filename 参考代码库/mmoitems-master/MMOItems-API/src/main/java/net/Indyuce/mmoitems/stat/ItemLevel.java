package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.SupportedNBTTagValues;
import io.lumine.mythic.lib.util.lang3.NotImplementedException;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.ReadMMOItem;
import net.Indyuce.mmoitems.api.util.NumericStatFormula;
import net.Indyuce.mmoitems.gui.edition.EditionInventory;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.InternalStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemLevel extends ItemStat<NumericStatFormula, DoubleData> implements InternalStat {
	public ItemLevel() {
		super("ITEM_LEVEL", Material.EXPERIENCE_BOTTLE, "Item Level", new String[] { "The item level" }, new String[0]);
	}

	@Override
	public void whenApplied(@NotNull ItemStackBuilder item, @NotNull DoubleData data) { item.addItemTag(getAppliedNBT(data)); }

	@Nullable
	@Override
	public NumericStatFormula whenInitialized(Object object) {
		throw new NotImplementedException();
	}

	@Override
	public void whenClicked(@NotNull EditionInventory inv, @NotNull InventoryClickEvent event) {
		throw new NotImplementedException();
	}

	@Override
	public void whenInput(@NotNull EditionInventory inv, @NotNull String message, Object... info) {
		throw new NotImplementedException();
	}

	@Override
	public void whenDisplayed(List<String> lore, Optional<NumericStatFormula> statData) {
		throw new NotImplementedException();
	}

	@NotNull
	@Override
	public ArrayList<ItemTag> getAppliedNBT(@NotNull DoubleData data) {

		// Array
		ArrayList<ItemTag> ret = new ArrayList<>();

		// Add
		ret.add(new ItemTag(getNBTPath(), data.getValue()));

		return ret;
	}

	@Override
	public void whenLoaded(@NotNull ReadMMOItem mmoitem) {

		// Find relevant tags
		ArrayList<ItemTag> relevantTags = new ArrayList<>();
		if (mmoitem.getNBT().hasTag(getNBTPath()))
			relevantTags.add(ItemTag.getTagAtPath(getNBTPath(), mmoitem.getNBT(), SupportedNBTTagValues.DOUBLE));

		// Build StatData
		StatData data = getLoadedNBT(relevantTags);

		if (data != null) { mmoitem.setData(this, data); }
	}

	@Nullable
	@Override
	public DoubleData getLoadedNBT(@NotNull ArrayList<ItemTag> storedTags) {

		// Get tag
		ItemTag lTag = ItemTag.getTagAtPath(getNBTPath(), storedTags);

		// Found?
		if (lTag != null) {

			// Thats it
			return new DoubleData((double) lTag.getValue());
		}

		// Nope
		return null;
	}

	@NotNull
	@Override
	public DoubleData getClearStatData() {
		return new DoubleData(0D);
	}
}
