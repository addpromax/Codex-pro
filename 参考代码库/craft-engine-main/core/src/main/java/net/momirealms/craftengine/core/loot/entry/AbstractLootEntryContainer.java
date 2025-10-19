package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MCUtils;

import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractLootEntryContainer<T> implements LootEntryContainer<T>, Predicate<LootContext> {
    protected final List<Condition<LootContext>> conditions;
    private final Predicate<LootContext> compositeCondition;

    protected AbstractLootEntryContainer(List<Condition<LootContext>> conditions) {
        this.conditions = conditions;
        this.compositeCondition = MCUtils.allOf(conditions);
    }

    @Override
    public final boolean test(LootContext context) {
        return this.compositeCondition.test(context);
    }

    public abstract Key type();
}