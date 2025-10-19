package net.Indyuce.mmoitems.gui.edition.recipe.registry.burninglegacy;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.version.VMaterial;
import net.Indyuce.mmoitems.manager.RecipeManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Deprecated
public enum CraftingType {
    //SHAPED(21, "The C. Table Recipe (Shaped) for this item", VersionMaterial.CRAFTING_TABLE, null),
    //SHAPELESS(22, "The C. Table Recipe (Shapeless) for this item", VersionMaterial.CRAFTING_TABLE, null),
    FURNACE(23, "The Furnace Recipe for this item", Material.FURNACE, RecipeManager.BurningRecipeType.FURNACE),
    BLAST(29, "The Blast Furnace Recipe for this item", VMaterial.BLAST_FURNACE, RecipeManager.BurningRecipeType.BLAST, 1, 14),
    SMOKER(30, "The Smoker Recipe for this item", VMaterial.SMOKER, RecipeManager.BurningRecipeType.SMOKER, 1, 14),
    CAMPFIRE(32, "The Campfire Recipe for this item", VMaterial.CAMPFIRE, RecipeManager.BurningRecipeType.CAMPFIRE, 1, 14);
    //SMITHING(33, "The Smithing Recipe for this item", VersionMaterial.SMITHING_TABLE, null, 1, 15);

    private final int slot;
    private final String lore;
    private final Material material;
    private final int[] mustBeHigher;
    private final RecipeManager.BurningRecipeType burning;

    private CraftingType(int slot, String lore, VMaterial material, @Nullable RecipeManager.BurningRecipeType burn, int... mustBeHigher) {
        this(slot, lore, material.get(), burn, mustBeHigher);
    }

    private CraftingType(int slot, String lore, Material material, @Nullable RecipeManager.BurningRecipeType burn, int... mustBeHigher) {
        this.slot = slot;
        this.lore = lore;
        this.material = material;
        this.mustBeHigher = mustBeHigher;
        this.burning = burn;
    }

    public ItemStack getItem() {
        return new ItemStack(material);
    }

    public int getSlot() {
        return slot;
    }

    public String getName() {
        return UtilityMethods.caseOnWords(name().toLowerCase());
    }

    public String getLore() {
        return lore;
    }

    public RecipeManager.BurningRecipeType getBurningType() {
        return burning;
    }

    public boolean shouldAdd() {
        return mustBeHigher.length == 0 || MythicLib.plugin.getVersion().isStrictlyHigher(mustBeHigher);
    }

    public static CraftingType getBySlot(int slot) {
        for (CraftingType type : values())
            if (type.getSlot() == slot) return type;
        return null;
    }
}