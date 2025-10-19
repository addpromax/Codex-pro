package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.util.Key;

public abstract class CustomCraftingTableRecipe<T> extends AbstractGroupedRecipe<T> {
    protected final CraftingRecipeCategory category;

    protected CustomCraftingTableRecipe(Key id, CraftingRecipeCategory category, String group, CustomRecipeResult<T> result) {
        super(id, group, result);
        this.category = category;
    }

    public CraftingRecipeCategory category() {
        return category;
    }
}
