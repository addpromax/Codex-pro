package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.MythicLib;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.util.MMOUtils;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.PlayerConsumable;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * When a consumable is eaten, restores saturation.
 *
 * @author Gunging
 */
public class RestoreSaturation extends DoubleStat implements PlayerConsumable {
    public RestoreSaturation() {
        super("RESTORE_SATURATION", Material.GOLDEN_CARROT, "Saturation Restoration", new String[]{"Saturation given when consumed."}, new String[]{"consumable"});
    }

    @Override
    public void onConsume(@NotNull VolatileMMOItem mmo, @NotNull Player player, boolean vanillaEating) {

        final double vanillaSaturation = getSaturationRestored(mmo.getNBT().getItem());
        double saturation = mmo.hasData(ItemStats.RESTORE_SATURATION) ? ((DoubleData) mmo.getData(ItemStats.RESTORE_SATURATION)).getValue() : vanillaSaturation;
        if (vanillaEating) saturation -= vanillaSaturation;

        // Any saturation being provided?
        if (saturation != 0) MMOUtils.saturate(player, saturation);
    }

    private float getSaturationRestored(ItemStack item) {
        try {
            return MythicLib.plugin.getVersion().getWrapper().getSaturationRestored(item);
        } catch (Exception ignored) {

            // If it is not a food item
            return 0;
        }
    }
}
