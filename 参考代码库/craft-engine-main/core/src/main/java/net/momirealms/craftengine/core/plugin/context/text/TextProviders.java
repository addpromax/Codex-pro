package net.momirealms.craftengine.core.plugin.context.text;

import net.momirealms.craftengine.core.util.Key;

public class TextProviders {
    public static final Key PLAIN = Key.of("craftengine:plain");
    public static final Key TAG = Key.of("craftengine:tag");

    public static TextProvider fromString(String string) {
        if (!string.contains("<") || !string.contains(">")) {
            return PlainTextProvider.of(string);
        }
        return TagTextProvider.of(string);
    }
}
