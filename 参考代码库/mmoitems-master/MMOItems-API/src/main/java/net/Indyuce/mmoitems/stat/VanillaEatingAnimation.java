package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class
VanillaEatingAnimation extends BooleanStat {
	public VanillaEatingAnimation() {
		super("VANILLA_EATING", Material.COOKED_BEEF, "Vanilla Eating Animation", new String[] { "When enabled, players have to wait", "for the vanilla eating animation", "in order to eat the consumable.", "", "Only works on items that", "can normally be eaten.", "Recommended in 1.21.4+"}, new String[] { "consumable" });
	}

	@Override
	public void whenApplied(@NotNull ItemStackBuilder item, @NotNull BooleanData data) {
		if (data.isEnabled())
			item.addItemTag(new ItemTag("MMOITEMS_VANILLA_EATING", true));
	}
}
