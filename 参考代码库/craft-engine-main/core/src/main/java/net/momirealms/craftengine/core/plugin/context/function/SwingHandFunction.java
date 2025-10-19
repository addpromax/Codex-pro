package net.momirealms.craftengine.core.plugin.context.function;

import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class SwingHandFunction<CTX extends Context> extends AbstractConditionalFunction<CTX> {
    private final Optional<InteractionHand> hand;

    public SwingHandFunction(Optional<InteractionHand> hand, List<Condition<CTX>> predicates) {
        super(predicates);
        this.hand = hand;
    }

    @Override
    public void runInternal(CTX ctx) {
        Optional<Player> cancellable = ctx.getOptionalParameter(DirectContextParameters.PLAYER);
        cancellable.ifPresent(value -> {
            if (this.hand.isPresent()) {
                value.swingHand(this.hand.get());
            } else {
                value.swingHand(ctx.getOptionalParameter(DirectContextParameters.HAND).orElse(InteractionHand.MAIN_HAND));
            }
        });
    }

    @Override
    public Key type() {
        return CommonFunctions.SWING_HAND;
    }

    public static class FactoryImpl<CTX extends Context> extends AbstractFactory<CTX> {

        public FactoryImpl(java.util.function.Function<Map<String, Object>, Condition<CTX>> factory) {
            super(factory);
        }

        @Override
        public Function<CTX> create(Map<String, Object> arguments) {
            Optional<InteractionHand> optionalHand = Optional.ofNullable(arguments.get("hand")).map(it -> InteractionHand.valueOf(it.toString().toUpperCase(Locale.ENGLISH)));
            return new SwingHandFunction<>(optionalHand, getPredicates(arguments));
        }
    }
}
