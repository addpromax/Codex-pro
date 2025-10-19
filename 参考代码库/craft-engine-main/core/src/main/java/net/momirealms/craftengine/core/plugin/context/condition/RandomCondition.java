package net.momirealms.craftengine.core.plugin.context.condition;

import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.RandomUtils;

import java.util.Map;
import java.util.Optional;

public class RandomCondition<CTX extends Context> implements Condition<CTX> {
    private final NumberProvider chance;
    private final boolean previous;

    public RandomCondition(NumberProvider chance, boolean previous) {
        this.chance = chance;
        this.previous = previous;
    }

    @Override
    public Key type() {
        return CommonConditions.RANDOM;
    }

    @Override
    public boolean test(CTX ctx) {
        if (this.previous) {
            Optional<Double> random = ctx.getOptionalParameter(DirectContextParameters.LAST_RANDOM);
            return random.map(d -> d < this.chance.getFloat(ctx))
                    .orElseGet(() -> RandomUtils.generateRandomFloat(0, 1) < this.chance.getFloat(ctx));
        } else {
            Optional<Double> random = ctx.getOptionalParameter(DirectContextParameters.RANDOM);
            return random.map(d -> d < this.chance.getFloat(ctx))
                    .orElseGet(() -> RandomUtils.generateRandomFloat(0, 1) < this.chance.getFloat(ctx));
        }
    }

    public static class FactoryImpl<CTX extends Context> implements ConditionFactory<CTX> {

        @Override
        public Condition<CTX> create(Map<String, Object> arguments) {
            NumberProvider provider = NumberProviders.fromObject(arguments.getOrDefault("value", 0.5f));
            boolean useLastRandom = Boolean.parseBoolean(arguments.getOrDefault("use-last", "false").toString());
            return new RandomCondition<>(provider, useLastRandom);
        }
    }
}
