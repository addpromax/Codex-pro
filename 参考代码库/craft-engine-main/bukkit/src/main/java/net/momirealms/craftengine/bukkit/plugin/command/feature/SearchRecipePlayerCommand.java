package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.recipe.Recipe;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import net.momirealms.craftengine.core.plugin.locale.MessageConstants;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import java.util.List;

public class SearchRecipePlayerCommand extends BukkitCommandFeature<CommandSender> {

    public SearchRecipePlayerCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .senderType(Player.class)
                .handler(context -> {
                    Player player = context.sender();
                    BukkitServerPlayer serverPlayer = plugin().adapt(player);
                    Item<?> item = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
                    if (item == null) {
                        handleFeedback(context, MessageConstants.COMMAND_SEARCH_RECIPE_NO_ITEM);
                        return;
                    }
                    Key itemId = item.id();
                    List<Recipe<Object>> inRecipes = plugin().recipeManager().recipeByResult(itemId);
                    if (!inRecipes.isEmpty()) {
                        plugin().itemBrowserManager().openRecipePage(serverPlayer, null, inRecipes, 0, 0, false);
                    } else {
                        handleFeedback(context, MessageConstants.COMMAND_SEARCH_RECIPE_NOT_FOUND);
                    }
                });
    }

    @Override
    public String getFeatureID() {
        return "search_recipe_player";
    }
}
