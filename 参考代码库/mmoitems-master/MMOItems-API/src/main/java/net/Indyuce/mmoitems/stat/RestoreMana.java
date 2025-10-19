package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.PlayerConsumable;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * When a consumable is eaten, restores mana.
 *
 * @author Gunging
 */
public class RestoreMana extends DoubleStat implements PlayerConsumable {
    public RestoreMana() {
        super("RESTORE_MANA", Material.LAPIS_LAZULI, "Restore Mana", new String[]{"The amount of mana", "your consumable restores."}, new String[]{"consumable"});
    }

    @Override
    public void onConsume(@NotNull VolatileMMOItem mmo, @NotNull Player player, boolean vanillaEating) {
        if (!mmo.hasData(ItemStats.RESTORE_MANA)) return;

        final DoubleData d = (DoubleData) mmo.getData(ItemStats.RESTORE_MANA);
        if (d.getValue() != 0) PlayerData.get(player).getRPG().giveMana(d.getValue());
    }
}