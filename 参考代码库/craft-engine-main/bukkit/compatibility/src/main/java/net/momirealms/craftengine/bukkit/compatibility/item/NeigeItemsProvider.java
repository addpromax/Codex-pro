package net.momirealms.craftengine.bukkit.compatibility.item;

import net.momirealms.craftengine.core.item.ExternalItemProvider;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pers.neige.neigeitems.manager.ItemManager;

import java.util.Optional;

public class NeigeItemsProvider implements ExternalItemProvider<ItemStack> {

    @Override
    public String plugin() {
        return "NeigeItems";
    }

    @Override
    public ItemStack build(String id, ItemBuildContext context) {
        return ItemManager.INSTANCE.getItemStack(id, Optional.ofNullable(context.player()).map(it -> (Player) it.platformPlayer()).orElse(null));
    }
}
