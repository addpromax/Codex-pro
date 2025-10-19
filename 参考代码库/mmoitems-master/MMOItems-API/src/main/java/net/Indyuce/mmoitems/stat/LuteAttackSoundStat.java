package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.type.GemStoneStat;
import net.Indyuce.mmoitems.stat.type.StringStat;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class LuteAttackSoundStat extends StringStat implements GemStoneStat {
	public LuteAttackSoundStat() {
		super("LUTE_ATTACK_SOUND", Material.GOLDEN_HORSE_ARMOR, "Lute Attack Sound", new String[] { "The sound played when", "basic attacking with this lute." }, new String[] { "lute" });
	}

	@Override
	public void whenApplied(@NotNull ItemStackBuilder item, @NotNull StringData data) {
		item.addItemTag(new ItemTag("MMOITEMS_LUTE_ATTACK_SOUND", data.toString().toUpperCase().replace("-", "_").replace(" ", "_")));
	}
}
