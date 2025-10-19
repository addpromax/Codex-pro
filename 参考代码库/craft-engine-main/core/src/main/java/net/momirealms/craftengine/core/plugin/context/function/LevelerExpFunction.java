package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.context.*;
import net.momirealms.craftengine.core.plugin.context.number.NumberProvider;
import net.momirealms.craftengine.core.plugin.context.number.NumberProviders;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelector;
import net.momirealms.craftengine.core.plugin.context.selector.PlayerSelectors;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.List;
import java.util.Map;

public class LevelerExpFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final PlayerSelector<CTX> selector;
    private final NumberProvider count;
    private final String leveler;
    private final String plugin;

    public LevelerExpFunction(NumberProvider count, String leveler, String plugin, PlayerSelector<CTX> selector, List<Condition<CTX>> predicates) {
        super(predicates);
        this.count = count;
        this.leveler = leveler;
        this.plugin = plugin;
        this.selector = selector;
    }

    @Override
    public void runInternal(CTX ctx) {
        if (this.selector == null) {
            ctx.getOptionalParameter(DirectContextParameters.PLAYER).ifPresent(it -> {
                CraftEngine.instance().compatibilityManager().addLevelerExp(it, this.plugin, this.leveler, this.count.getDouble(ctx));
            });
        } else {
            for (Player target : this.selector.get(ctx)) {
                RelationalContext relationalContext = ViewerContext.of(ctx, PlayerOptionalContext.of(target, ContextHolder.EMPTY));
                CraftEngine.instance().compatibilityManager().addLevelerExp(target, this.plugin, this.leveler, this.count.getDouble(relationalContext));
            }
        }
    }

    @Override
    public Key type() {
        return CommonFunctions.LEVELER_EXP;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            Object count = ResourceConfigUtils.requireNonNullOrThrow(arguments.get("count"), "warning.config.function.leveler_exp.missing_count");
            String leveler = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("leveler"), "warning.config.function.leveler_exp.missing_leveler");
            String plugin = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("plugin"), "warning.config.function.leveler_exp.missing_plugin");
            return new LevelerExpFunction<>(NumberProviders.fromObject(count), leveler, plugin, PlayerSelectors.fromObject(arguments.get("target"), conditionFactory()), getPredicates(arguments));
        }
    }
}
