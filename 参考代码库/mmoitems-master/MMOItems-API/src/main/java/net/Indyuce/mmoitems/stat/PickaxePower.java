package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.util.NumericStatFormula;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import io.lumine.mythic.lib.util.lang3.Validate;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class PickaxePower extends DoubleStat {
    public PickaxePower() {
        super("PICKAXE_POWER", Material.IRON_PICKAXE, "Pickaxe Power", new String[]{"The breaking strength of the", "item when mining custom blocks."}, new String[]{"tool"});
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull DoubleData data) {
        int pickPower = (int) data.getValue();

        item.addItemTag(new ItemTag("MMOITEMS_PICKAXE_POWER", pickPower));
        item.getLore().insert("pickaxe-power", DoubleStat.formatPath(getPath(), getGeneralStatFormat(), true, false, pickPower));
    }

    @Override
    public void whenPreviewed(@NotNull ItemStackBuilder item, @NotNull DoubleData currentData, @NotNull NumericStatFormula templateData) throws IllegalArgumentException {
        Validate.isTrue(currentData instanceof DoubleData, "Current Data is not Double Data");
        Validate.isTrue(templateData instanceof NumericStatFormula, "Template Data is not Numeric Stat Formula");

        // Get Value
        double techMinimum = templateData.calculate(0, NumericStatFormula.FormulaInputType.LOWER_BOUND);
        double techMaximum = templateData.calculate(0, NumericStatFormula.FormulaInputType.UPPER_BOUND);

        // Cancel if it its NEGATIVE and this doesn't support negative stats.
        if (techMaximum < 0 && !handleNegativeStats()) {
            return;
        }
        if (techMinimum < 0 && !handleNegativeStats()) {
            techMinimum = 0;
        }
        if (techMinimum <  templateData.getBase() -  templateData.getMaxSpread()) {
            techMinimum = templateData.getBase() - templateData.getMaxSpread();
        }
        if (techMaximum > templateData.getBase() +  templateData.getMaxSpread()) {
            techMaximum = templateData.getBase() +  templateData.getMaxSpread();
        }

        // Add NBT Path
        item.addItemTag(new ItemTag("MMOITEMS_PICKAXE_POWER", currentData.getValue()));

        // Display if not ZERO
        if (techMinimum != 0 || techMaximum != 0) {
            String builtRange = DoubleStat.formatPath(getPath(), getGeneralStatFormat(), true, false, Math.floor(techMinimum), Math.floor(techMaximum));
            item.getLore().insert("pickaxe-power", builtRange);
        }
    }
}
