package net.Indyuce.mmoitems.stat;

import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.api.item.mmoitem.ReadMMOItem;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.type.ChooseStat;
import net.Indyuce.mmoitems.stat.type.GemStoneStat;
import net.Indyuce.mmoitems.util.StatChoice;
import net.Indyuce.mmoitems.stat.annotation.VersionDependant;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Jules
 */
@VersionDependant(version = {1, 20})
public class TrimPatternStat extends ChooseStat implements GemStoneStat {
    public TrimPatternStat() {
        super("TRIM_PATTERN", Material.LEATHER_CHESTPLATE, "Trim Pattern", new String[]{"Pattern of trimmed armor."}, new String[]{"armor", "skin"});

        // Version dependency
        if (!isEnabled()) return;

        for (TrimPattern mat : Registry.TRIM_PATTERN)
            addChoices(new StatChoice(mat.getKey().toString()));
    }

    public static NamespacedKey fixNamespacedKey(String str) {
        if (!str.contains(":")) return NamespacedKey.minecraft(str);
        return NamespacedKey.fromString(str);
    }

    @Nullable
    @Override
    public StatChoice getChoice(String id) {
        return super.getChoice(fixNamespacedKey(id).toString());
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull StringData data) {
        if (!(item.getMeta() instanceof ArmorMeta)) return;

        @Nullable TrimPattern pattern = Registry.TRIM_PATTERN.get(fixNamespacedKey(data.toString()));
        if (pattern == null) return;

        final ArmorMeta meta = (ArmorMeta) item.getMeta();
        final ArmorTrim currentTrim = meta.hasTrim() ? meta.getTrim() : new ArmorTrim(TrimMaterial.AMETHYST, TrimPattern.COAST);
        meta.setTrim(new ArmorTrim(currentTrim.getMaterial(), pattern));
    }

    @Override
    public void whenLoaded(@NotNull ReadMMOItem mmoitem) {
        if (!(mmoitem.getNBT().getItem().getItemMeta() instanceof ArmorMeta)) return;
        final ArmorMeta meta = (ArmorMeta) mmoitem.getNBT().getItem().getItemMeta();
        if (!meta.hasTrim()) return;
        mmoitem.setData(this, new StringData(meta.getTrim().getPattern().getKey().toString()));
    }
}
