package net.momirealms.craftengine.core.pack.conflict.matcher;

import net.momirealms.craftengine.core.pack.conflict.PathContext;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.condition.ConditionFactory;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.util.CharacterUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.nio.file.Path;
import java.util.Map;

public class PathMatcherParentPrefix implements Condition<PathContext> {
    private final String prefix;

    public PathMatcherParentPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean test(PathContext path) {
        Path parent = path.path().getParent();
        if (parent == null) return false;
        String pathStr = CharacterUtils.replaceBackslashWithSlash(parent.toString());
        return pathStr.startsWith(this.prefix);
    }

    @Override
    public Key type() {
        return PathMatchers.PARENT_PATH_PREFIX;
    }

    public static class FactoryImpl implements ConditionFactory<PathContext> {

        @Override
        public Condition<PathContext> create(Map<String, Object> arguments) {
            String prefix = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("prefix"), () -> new LocalizedException("warning.config.conflict_matcher.parent_prefix.missing_prefix"));
            return new PathMatcherParentPrefix(prefix);
        }
    }
}
