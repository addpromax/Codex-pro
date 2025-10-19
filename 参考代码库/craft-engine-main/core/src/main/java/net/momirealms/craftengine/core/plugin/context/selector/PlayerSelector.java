package net.momirealms.craftengine.core.plugin.context.selector;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public interface PlayerSelector<CTX extends Context> {

    List<Player> get(CTX context);

    Key type();
}
