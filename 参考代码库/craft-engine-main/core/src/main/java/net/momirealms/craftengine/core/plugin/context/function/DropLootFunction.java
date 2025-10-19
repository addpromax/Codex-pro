package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DropLootFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final NumberProvider x;
    private final NumberProvider y;
    private final NumberProvider z;
    private final LootTable<?> lootTable;

    public DropLootFunction(NumberProvider x, NumberProvider y, NumberProvider z, LootTable<?> lootTable, List<Condition<CTX>> predicates) {
        super(predicates);
        this.x = x;
        this.y = y;
        this.z = z;
        this.lootTable = lootTable;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<WorldPosition> optionalWorldPosition = ctx.getOptionalParameter(DirectContextParameters.POSITION);
        if (optionalWorldPosition.isPresent()) {
            World world = optionalWorldPosition.get().world();
            WorldPosition position = new WorldPosition(world, x.getDouble(ctx), y.getDouble(ctx), z.getDouble(ctx));
            Player player = ctx.getOptionalParameter(DirectContextParameters.PLAYER).orElse(null);
            List<? extends Item<?>> items = lootTable.getRandomItems(ctx.contexts(), world, player);
            for (Item<?> item : items) {
                world.dropItemNaturally(position, item);
            }
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.DROP_LOOT;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            NumberProvider x = NumberProviders.fromObject(arguments.getOrDefault("x", "<arg:position.x>"));
            NumberProvider y = NumberProviders.fromObject(arguments.getOrDefault("y", "<arg:position.y>"));
            NumberProvider z = NumberProviders.fromObject(arguments.getOrDefault("z", "<arg:position.z>"));
            LootTable<?> loots = LootTable.fromMap(MiscUtils.castToMap(arguments.get("loot"), true));
            return new DropLootFunction<>(x, y, z, loots, getPredicates(arguments));
        }
    }
}
