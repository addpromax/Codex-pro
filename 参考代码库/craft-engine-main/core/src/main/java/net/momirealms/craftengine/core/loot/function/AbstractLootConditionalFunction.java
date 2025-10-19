package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.util.MCUtils;

import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractLootConditionalFunction<T> implements LootFunction<T> {
    protected final List<Condition<LootContext>> predicates;
    private final Predicate<LootContext> compositePredicates;

    public AbstractLootConditionalFunction(List<Condition<LootContext>> predicates) {
        this.predicates = predicates;
        this.compositePredicates = MCUtils.allOf(predicates);
    }

    @Override
    public Item<T> apply(Item<T> item, LootContext lootContext) {
        return this.compositePredicates.test(lootContext) ? this.applyInternal(item, lootContext) : item;
    }

    protected abstract Item<T> applyInternal(Item<T> item, LootContext context);
}
