package net.momirealms.craftengine.core.item.modifier;

import net.momirealms.craftengine.core.item.ComponentKeys;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.NetworkItemHandler;
import net.momirealms.craftengine.core.util.AdventureHelper;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.Tag;

import java.util.List;

public class LoreModifier<I> implements ItemDataModifier<I> {
    private final List<String> argument;

    public LoreModifier(List<String> argument) {
        this.argument = argument;
    }

    @Override
    public String name() {
        return "lore";
    }

    @Override
    public Item<I> apply(Item<I> item, ItemBuildContext context) {
        item.loreComponent(this.argument.stream().map(it -> AdventureHelper.miniMessage().deserialize(it, context.tagResolvers())).toList());
        return item;
    }

    @Override
    public Item<I> prepareNetworkItem(Item<I> item, ItemBuildContext context, CompoundTag networkData) {
        if (VersionHelper.isOrAbove1_20_5()) {
            Tag previous = item.getNBTComponent(ComponentKeys.LORE);
            if (previous != null) {
                networkData.put(ComponentKeys.LORE.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put(ComponentKeys.LORE.asString(), NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        } else {
            Tag previous = item.getNBTTag("display", "Lore");
            if (previous != null) {
                networkData.put("display.Lore", NetworkItemHandler.pack(NetworkItemHandler.Operation.ADD, previous));
            } else {
                networkData.put("display.Lore", NetworkItemHandler.pack(NetworkItemHandler.Operation.REMOVE));
            }
        }
        return item;
    }
}
