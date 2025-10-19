package net.momirealms.craftengine.core.plugin.config.template;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.data.EvaluationValue;
import net.momirealms.craftengine.core.util.Key;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ExpressionTemplateArgument implements TemplateArgument {
    public static final Factory FACTORY = new Factory();
    private final TemplateManager.ArgumentString expression;
    private final ValueType valueType;

    protected ExpressionTemplateArgument(String expression, ValueType valueType) {
        this.expression = TemplateManager.preParse(expression);
        this.valueType = valueType;
    }

    @Override
    public Object get(Map<String, TemplateArgument> arguments) {
        String expression = Optional.ofNullable(this.expression.get(arguments)).map(String::valueOf).orElse(null);
        if (expression == null) return null;
        try {
            return this.valueType.formatter().apply(new Expression(expression).evaluate());
        } catch (Exception e) {
            throw new RuntimeException("Failed to process expression argument: " + this.expression, e);
        }
    }

    @Override
    public Key type() {
        return TemplateArguments.EXPRESSION;
    }

    protected enum ValueType {
        INT(e -> e.getNumberValue().intValue()),
        LONG(e -> e.getNumberValue().longValue()),
        SHORT(e -> e.getNumberValue().shortValueExact()),
        DOUBLE(e -> e.getNumberValue().doubleValue()),
        FLOAT(e -> e.getNumberValue().floatValue()),
        BOOLEAN(EvaluationValue::getBooleanValue),;

        private final Function<EvaluationValue, Object> formatter;

        ValueType(Function<EvaluationValue, Object> formatter) {
            this.formatter = formatter;
        }

        public Function<EvaluationValue, Object> formatter() {
            return this.formatter;
        }
    }

    public static class Factory implements TemplateArgumentFactory {
        @Override
        public TemplateArgument create(Map<String, Object> arguments) {
            return new ExpressionTemplateArgument(
                    arguments.getOrDefault("expression", "").toString(),
                    ValueType.valueOf(arguments.getOrDefault("value-type", "double").toString().toUpperCase(Locale.ENGLISH))
            );
        }
    }
}
