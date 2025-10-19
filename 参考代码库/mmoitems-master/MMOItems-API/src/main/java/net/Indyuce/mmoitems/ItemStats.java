package net.Indyuce.mmoitems;

import io.lumine.mythic.lib.version.VMaterial;
import net.Indyuce.mmoitems.stat.*;
import net.Indyuce.mmoitems.stat.annotation.DeprecatedStat;
import net.Indyuce.mmoitems.stat.block.*;
import net.Indyuce.mmoitems.stat.type.*;
import org.bukkit.Material;

/**
 * A central file for the plugin since new stats are added ALL the time.
 * It is also much safer to initialize class objects OUTSIDE of the field type class.
 */
@SuppressWarnings("unused")
public class ItemStats {

    // Main Item Stats
    public static final ItemStat
            REVISION_ID = new RevisionID(),
            MATERIAL = new MaterialStat(),
            SKULL_TEXTURE = new SkullTextureStat(),
            ITEM_DAMAGE = new ItemDamage(),
            CUSTOM_MODEL_DATA = new CustomModelDataInt(),
            CUSTOM_MODEL_DATA_STRINGS = new CustomModelDataStrings(),
            CUSTOM_MODEL_DATA_FLOATS = new CustomModelDataFloats(),
            MODEL = new ItemModel(),
            EQUIPPABLE_SLOT = new EquippableSlot(),
            EQUIPPABLE_MODEL = new EquippableModel(),
            MAX_DURABILITY = new MaximumDurability(),
            MAX_ITEM_DAMAGE = new MaxItemDamage(),
            WILL_BREAK = new LostWhenBroken(),
            NAME = new DisplayName(),
            LORE = new Lore(),
            NBT_TAGS = new NBTTags(),
            MAX_STACK_SIZE = new MaxStackSize(),
            CAN_BREAK = new CanBreak(),
            LORE_FORMAT = new LoreFormat(),
            TOOLTIP = new TooltipStat(),
            TOOLTIP_STYLE = new TooltipStyle(),

    // Block Specific Stats
    BLOCK_ID = new BlockID(),
            REQUIRED_POWER = new RequiredPower(),
            REQUIRE_POWER_TO_BREAK = new RequirePowerToBreak(),
            MIN_XP = new MinXP(),
            MAX_XP = new MaxXP(),
            GEN_TEMPLATE = new GenTemplate(),

    // Misc Stats
    DISPLAYED_TYPE = new DisplayedType(),
            ENCHANTS = new Enchants(),
            HIDE_ENCHANTS = new HideEnchants(),
            HIDE_TOOLTIP = new HideTooltip(),
            PERMISSION = new Permission(),
            ITEM_PARTICLES = new ItemParticles(),
            ARROW_PARTICLES = new ArrowParticles(),
            PROJECTILE_PARTICLES = new ProjectileParticles(),

    // Disable Interaction Stats
    DISABLE_INTERACTION = new DisableStat("INTERACTION", VMaterial.GRASS_BLOCK.get(), "Disable Interaction", new String[]{"!block", "all"}, "Disable any unwanted interaction:", "block placement, item use..."),
            DISABLE_CRAFTING = new DisableStat("CRAFTING", Material.CRAFTING_TABLE, "Disable Crafting", "Players can't use this item while crafting."), DISABLE_SMELTING = new DisableStat("SMELTING", Material.FURNACE, "Disable Smelting", "Players can't use this item in furnaces."),
            DISABLE_SMITHING = new DisableStat("SMITHING", Material.DAMAGED_ANVIL, "Disable Smithing", "Players can't smith this item in smithing tables."),
            DISABLE_ENCHANTING = new DisableStat("ENCHANTING", Material.ENCHANTING_TABLE, "Disable Enchanting", new String[]{"!block", "all"}, "Players can't enchant this item."),
            DISABLE_REPAIRING = new DisableStat("REPAIRING", Material.ANVIL, "Disable Repairing", new String[]{"!block", "all"}, "Players can't use this item in anvils."),
            DISABLE_ARROW_SHOOTING = new DisableStat("ARROW_SHOOTING", Material.ARROW, "Disable Arrow Shooting", new Material[]{Material.ARROW}, "Players can't shoot this", "item using a bow."),
            DISABLE_DROP = new DisableStat("DROPING", Material.LAVA_BUCKET, "Disable Item Dropping", new String[0], "Disables the dropping of this item!"),
            DISABLE_ARROW_CONSUMPTION = new DisableStat("ARROW_CONSUMPTION", Material.ARROW, "Disable Arrow Consumption", new String[]{"crossbow"}, "Disable arrow requirement and consumption."),

    // Special item interactions/skills
    LEFT_CLICK_SCRIPT = new ActionLeftClick(),
            RIGHT_CLICK_SCRIPT = new ActionRightClick(),
            DISABLE_ATTACK_PASSIVE = new DisableStat("ATTACK_PASSIVE", Material.BARRIER, "Disable On-Hit Effect", new String[]{"weapon"}, "Disables on-hit effects granted by the item type."),

    // RPG Stats
    REQUIRED_LEVEL = new RequiredLevel(),
            REQUIRED_CLASS = new RequiredClass(),
            ATTACK_DAMAGE = new AttackDamage(),
            ATTACK_SPEED = new AttackSpeed(),
            CRITICAL_STRIKE_CHANCE = new DoubleStat("CRITICAL_STRIKE_CHANCE", Material.NETHER_STAR, "Critical Strike Chance", new String[]{"Critical Strikes deal more damage.", "In % chance."}, new String[]{"!miscellaneous", "!block", "all"}),
            CRITICAL_STRIKE_POWER = new DoubleStat("CRITICAL_STRIKE_POWER", Material.NETHER_STAR, "Critical Strike Power", new String[]{"The extra damage weapon crits deals.", "(Stacks with default value)", "In %."}, new String[]{"!miscellaneous", "!block", "all"}),
            SKILL_CRITICAL_STRIKE_CHANCE = new DoubleStat("SKILL_CRITICAL_STRIKE_CHANCE", Material.NETHER_STAR, "Skill Critical Strike Chance", new String[]{"Increases the chance of dealing skill crits (in %)."}, new String[]{"!miscellaneous", "!block", "all"}),
            SKILL_CRITICAL_STRIKE_POWER = new DoubleStat("SKILL_CRITICAL_STRIKE_POWER", Material.NETHER_STAR, "Skill Critical Strike Power", new String[]{"Extra damage dealt (in %) by skill crits.", "(Stacks with default value)", "In %."}, new String[]{"!miscellaneous", "!block", "all"}),
            BLOCK_POWER = new DoubleStat("BLOCK_POWER", Material.IRON_HELMET, "Block Power", new String[]{"The % of the damage your", "armor/shield can block.", "Default: 25%"}, new String[]{"!miscellaneous", "!block", "all"}),
            BLOCK_RATING = new DoubleStat("BLOCK_RATING", Material.IRON_HELMET, "Block Rating", new String[]{"The chance your piece of armor", "has to block any entity attack."}, new String[]{"!miscellaneous", "!block", "all"}),
            BLOCK_COOLDOWN_REDUCTION = new DoubleStat("BLOCK_COOLDOWN_REDUCTION", Material.IRON_HELMET, "Block Cooldown Reduction", new String[]{"Reduces the blocking cooldown (%)."}, new String[]{"!miscellaneous", "!block", "all"}),
            DODGE_RATING = new DoubleStat("DODGE_RATING", Material.FEATHER, "Dodge Rating", new String[]{"The chance to dodge an attack.", "Dodging completely negates", "the attack damage."}, new String[]{"!miscellaneous", "!block", "all"}),
            DODGE_COOLDOWN_REDUCTION = new DoubleStat("DODGE_COOLDOWN_REDUCTION", Material.FEATHER, "Dodge Cooldown Reduction", new String[]{"Reduces the dodging cooldown (%)."}, new String[]{"!miscellaneous", "!block", "all"}),
            PARRY_RATING = new DoubleStat("PARRY_RATING", Material.BUCKET, "Parry Rating", new String[]{"The chance to parry an attack.", "Parrying negates the damage", "and knocks the attacker back."}, new String[]{"!miscellaneous", "!block", "all"}),
            PARRY_COOLDOWN_REDUCTION = new DoubleStat("PARRY_COOLDOWN_REDUCTION", Material.BUCKET, "Parry Cooldown Reduction", new String[]{"Reduces the parrying cooldown (%)."}, new String[]{"!miscellaneous", "!block", "all"}),
            COOLDOWN_REDUCTION = new DoubleStat("COOLDOWN_REDUCTION", Material.BOOK, "Cooldown Reduction", new String[]{"Reduces cooldowns of item and player skills (%)."}),
            RANGE = new DoubleStat("RANGE", Material.STICK, "Range", new String[]{"The range of your item attacks."}, new String[]{"staff", "whip", "wand", "musket", "gem_stone"}),
            MANA_COST = new ManaCost(),
            STAMINA_COST = new DoubleStat("STAMINA_COST", Material.LIGHT_GRAY_DYE, "Stamina Cost", new String[]{"Stamina spent by your weapon to be used."}, new String[]{"weapon"}),
            ARROW_VELOCITY = new DoubleStat("ARROW_VELOCITY", Material.ARROW, "Arrow Velocity", new String[]{"Determines how far your", "weapon can shoot.", "Default: 1.0"}, new String[]{"gem_stone", "bow", "crossbow"}),
            ARROW_POTION_EFFECTS = new ArrowPotionEffects(),
            PVE_DAMAGE = new DoubleStat("PVE_DAMAGE", Material.PORKCHOP, "PvE Damage", new String[]{"Additional damage against", "non human entities in %."}, new String[]{"equipment", "gem_stone"}),
            PVP_DAMAGE = new DoubleStat("PVP_DAMAGE", Material.SKELETON_SKULL, "PvP Damage", new String[]{"Additional damage", "against players in %."}, new String[]{"equipment", "gem_stone"}),
            BLUNT_POWER = new BluntPower(),
            BLUNT_RATING = new BluntRating(),
            WEAPON_DAMAGE = new DoubleStat("WEAPON_DAMAGE", Material.IRON_SWORD, "Weapon Damage", new String[]{"Additional on-hit weapon damage in %."}),
            SKILL_DAMAGE = new DoubleStat("SKILL_DAMAGE", Material.BOOK, "Skill Damage", new String[]{"Additional ability damage in %."}),
            PROJECTILE_DAMAGE = new DoubleStat("PROJECTILE_DAMAGE", Material.ARROW, "Projectile Damage", new String[]{"Additional skill/weapon projectile damage."}),
            MAGIC_DAMAGE = new DoubleStat("MAGIC_DAMAGE", Material.MAGMA_CREAM, "Magic Damage", new String[]{"Additional magic skill damage in %."}),
            PHYSICAL_DAMAGE = new DoubleStat("PHYSICAL_DAMAGE", Material.IRON_AXE, "Physical Damage", new String[]{"Additional skill/weapon physical damage."}),
            DEFENSE = new DoubleStat("DEFENSE", Material.SHIELD, "Defense", new String[]{"Reduces damage from any source.", "Formula can be set in MMOLib Config."}),
            DAMAGE_REDUCTION = new DoubleStat("DAMAGE_REDUCTION", Material.IRON_CHESTPLATE, "Damage Reduction", new String[]{"Reduces damage from any source.", "In %."}),
            FALL_DAMAGE_REDUCTION = new DoubleStat("FALL_DAMAGE_REDUCTION", Material.FEATHER, "Fall Damage Reduction", new String[]{"Reduces fall damage.", "In %."}),
            PROJECTILE_DAMAGE_REDUCTION = new DoubleStat("PROJECTILE_DAMAGE_REDUCTION", Material.SNOWBALL, "Projectile Damage Reduction", new String[]{"Reduces projectile damage.", "In %."}),
            PHYSICAL_DAMAGE_REDUCTION = new DoubleStat("PHYSICAL_DAMAGE_REDUCTION", Material.LEATHER_CHESTPLATE, "Physical Damage Reduction", new String[]{"Reduces physical damage.", "In %."}),
            FIRE_DAMAGE_REDUCTION = new DoubleStat("FIRE_DAMAGE_REDUCTION", Material.BLAZE_POWDER, "Fire Damage Reduction", new String[]{"Reduces fire damage.", "In %."}),
            MAGIC_DAMAGE_REDUCTION = new DoubleStat("MAGIC_DAMAGE_REDUCTION", Material.POTION, "Magic Damage Reduction", new String[]{"Reduce magic damage dealt by potions.", "In %."}),
            PVE_DAMAGE_REDUCTION = new DoubleStat("PVE_DAMAGE_REDUCTION", Material.PORKCHOP, "PvE Damage Reduction", new String[]{"Reduces damage dealt by mobs.", "In %."}),
            PVP_DAMAGE_REDUCTION = new DoubleStat("PVP_DAMAGE_REDUCTION", Material.SKELETON_SKULL, "PvP Damage Reduction", new String[]{"Reduces damage dealt by players", "In %."}),
            UNDEAD_DAMAGE = new DoubleStat("UNDEAD_DAMAGE", Material.SKELETON_SKULL, "Undead Damage", new String[]{"Deals additional damage to undead.", "In %."}),
            LIFESTEAL = new DoubleStat("LIFESTEAL", Material.REDSTONE, "Lifesteal", new String[]{"Percentage of damage you gain back as", "health when inflicting weapon damage."}),
            SPELL_VAMPIRISM = new DoubleStat("SPELL_VAMPIRISM", Material.REDSTONE, "Spell Vampirism", new String[]{"Percentage of damage you gain back as", "health when inflicting skill damage."}),

    // Extra Stats
    UNBREAKABLE = new Unbreakable(),
            TIER = new ItemTierStat(),
            SET = new ItemSetStat(),
            ARMOR = new Armor(),
            ARMOR_TOUGHNESS = new ArmorToughness(),
            MAX_HEALTH = new MaxHealth(),
            UNSTACKABLE = new Unstackable(),
            MAX_MANA = new DoubleStat("MAX_MANA", Material.LAPIS_LAZULI, "Max Mana", new String[]{"Adds mana to your max mana bar."}),
            KNOCKBACK_RESISTANCE = new KnockbackResistance(),
            MOVEMENT_SPEED = new MovementSpeed(),
            TWO_HANDED = new BooleanStat("TWO_HANDED", Material.IRON_INGOT, "Two Handed", new String[]{"If set to true, a player will be", "significantly slower if holding two", "items, one being Two Handed."}, new String[]{"handheld"}),
            REQUIRED_BIOMES = new RequiredBiomes(),
            DROP_ON_DEATH = new DisableDeathDrop(),
            HIDE_DURABILITY_BAR = new HideDurabilityBar(),
            CAMERA_OVERLAY = new CameraOverlay(),

    // Extra Attributes (1.20.2+)
    MAX_ABSORPTION = new MaxAbsorption(),
            BLOCK_BREAK_SPEED = new BlockBreakSpeed(),
            BLOCK_INTERACTION_RANGE = new BlockInteractionRange(),
            ENTITY_INTERACTION_RANGE = new EntityInteractionRange(),
            FALL_DAMAGE_MULTIPLIER = new FallDamageMultiplier(),
            GRAVITY = new Gravity(),
            JUMP_STRENGTH = new JumpStrength(),
            SAFE_FALL_DISTANCE = new SafeFallDistance(),
            SCALE = new Scale(),
            STEP_HEIGHT = new StepHeight(),
            BURNING_TIME = new BurningTime(),
            EXPLOSION_KNOCKBACK_RESISTANCE = new ExplosionKnockbackResistance(),
            MINING_EFFICIENCY = new MiningEfficiency(),
            MOVEMENT_EFFICIENCY = new MovementEfficiency(),
            OXYGEN_BONUS = new OxygenBonus(),
            SNEAKING_SPEED = new SneakingSpeed(),
            SUBMERGED_MINING_SPEED = new SubmergedMiningSpeed(),
            SWEEPING_DAMAGE_RATIO = new SweepingDamageRatio(),
            WATER_MOVEMENT_EFFICIENCY = new WaterMovementEfficiency(),

    // Permanent Effects
    PERM_EFFECTS = new PermanentEffects(),
            GRANTED_PERMISSIONS = new GrantedPermissions(),

    // Consumable Stats
    RESTORE_HEALTH = new RestoreHealth(),
            RESTORE_FOOD = new RestoreFood(),
            RESTORE_SATURATION = new RestoreSaturation(),
            RESTORE_MANA = new RestoreMana(),
            RESTORE_STAMINA = new RestoreStamina(),
            CAN_IDENTIFY = new CanIdentify(),
            CAN_DECONSTRUCT = new CanDeconstruct(),
            CAN_DESKIN = new CanDeskin(),
            EFFECTS = new Effects(),
            SOULBINDING_CHANCE = new SoulbindingChance(),
            SOULBOUND_BREAK_CHANCE = new SoulbindingBreakChance(),
            SOULBOUND_LEVEL = new SoulboundLevel(),
    //  AUTO_SOULBIND = new BooleanStat("AUTO_SOULBIND", VersionMaterial.ENDER_EYE.toMaterial(), "Auto-Soulbind", new String[]{"Automatically soulbinds this item to", "a player when he acquires it."}, new String[]{"!consumable", "all"}),
    ITEM_COOLDOWN = new DoubleStat("ITEM_COOLDOWN", Material.COOKED_CHICKEN, "Item Cooldown", new String[]{"This cooldown applies for consumables", "as well as for item commands."}, new String[]{"!armor", "!gem_stone", "!block", "all"}),
            COOLDOWN_REFERENCE = new StringStat("COOLDOWN_REFERENCE", Material.CHICKEN, "Cooldown Reference", new String[]{"Two items with the same cooldown reference", "will share their cooldowns. This is useful", "for health or mana pots for example."}, new String[]{"!armor", "!gem_stone", "!block", "all"}),
            VANILLA_EATING_ANIMATION = new VanillaEatingAnimation(),
            CAN_ALWAYS_EAT = new CanAlwaysEat(),
            CONSUME_SECONDS = MMOItems.plugin.getServerAdapter().consumableConsumeSeconds(),
            GEM_COLOR = new GemColor(),
            GEM_UPGRADE_SCALING = new GemUpgradeScaling(),
            ITEM_TYPE_RESTRICTION = new ItemTypeRestriction(),
            MAX_CONSUME = new MaxConsume(),
            SUCCESS_RATE = new SuccessRate(),

    // Crafting Stats
    CRAFTING = new Crafting(),
            CRAFT_PERMISSION = new CraftingPermission(),
    //CRAFT_AMOUNT = new DoubleStat("CRAFTED_AMOUNT", Material.WOODEN_AXE, "Crafted Amount", new String[]{"The stack count for", "this item when crafted."}, new String[0]),

    // Unique Stats
    AUTOSMELT = new BooleanStat("AUTOSMELT", Material.COAL, "Autosmelt", new String[]{"If set to true, your tool will", "automaticaly smelt mined ores."}, new String[]{"tool"}),
            BOUNCING_CRACK = new BooleanStat("BOUNCING_CRACK", Material.COBBLESTONE_WALL, "Bouncing Crack", new String[]{"If set to true, your tool will", "also break nearby blocks."}, new String[]{"tool"}),
            PICKAXE_POWER = new PickaxePower(),
            CUSTOM_SOUNDS = new CustomSounds(),
            ELEMENTS = new Elements(),
            COMMANDS = new Commands(),
    //   STAFF_SPIRIT = new StaffSpiritStat(),
    LUTE_ATTACK_SOUND = new LuteAttackSoundStat(),
            LUTE_ATTACK_EFFECT = new LuteAttackEffectStat(),
            NOTE_WEIGHT = new DoubleStat("NOTE_WEIGHT", Material.MUSIC_DISC_MALL, "Note Weight", new String[]{"Defines how the projectile cast", "by your lute tilts downwards."}, new String[]{"lute"}),
            //REMOVE_ON_CRAFT = new BooleanStat("REMOVE_ON_CRAFT", Material.GLASS_BOTTLE, "Remove on Craft", new String[]{"If the item should be completely", "removed when used in a recipe,", "or if it should become an", "empty bottle or bucket."}, new String[0], Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION, Material.MILK_BUCKET, Material.LAVA_BUCKET, Material.WATER_BUCKET),
            COMPATIBLE_TYPES = new CompatibleTypes(),
            COMPATIBLE_IDS = new CompatibleIds(),
            COMPATIBLE_MATERIALS = new CompatibleMaterials(),
            GEM_SOCKETS = new GemSockets(),
            RANDOM_UNSOCKET = new RandomUnsocket(),
    //todo CAN_UNSOCKET = new CanUnsocket(),
    REPAIR = new RepairPower(),
            REPAIR_PERCENT = new RepairPowerPercent(),
            REPAIR_TYPE = new RepairReference(),
            INEDIBLE = new BooleanStat("INEDIBLE", Material.POISONOUS_POTATO, "Inedible", new String[]{"Players won't be able to right-click this consumable.", "", "No effects of it will take place."}, new String[]{"consumable"}),
            DISABLE_RIGHT_CLICK_CONSUME = new DisableStat("RIGHT_CLICK_CONSUME", Material.BAKED_POTATO, "Infinite Consume", new String[]{"consumable"}, "Players will be able to right-click this consumable", "and benefit from its effects, but it won't be consumed."),
            KNOCKBACK = new DoubleStat("KNOCKBACK", Material.IRON_HORSE_ARMOR, "Knockback", new String[]{"Using this musket will knock", "the user back if positive."}, new String[]{"musket", "gem_stone"}),
            RECOIL = new DoubleStat("RECOIL", Material.IRON_HORSE_ARMOR, "Recoil", new String[]{"Corresponds to the shooting innacuracy."}, new String[]{"musket", "gem_stone"}),
            HANDWORN = new BooleanStat("HANDWORN", Material.STRING, "Handworn", new String[]{"This item can be held along with a", "two-handed weapon in the other hand."}, new String[]{"catalyst", "off_catalyst", "main_catalyst"}),
            AMPHIBIAN = new Amphibian(),

    // Abilities & Upgrading
    ABILITIES = new Abilities(),
            UPGRADE = new UpgradeStat(),
            DOWNGRADE_ON_BREAK = new BooleanStat("BREAK_DOWNGRADE", Material.DAMAGED_ANVIL, "Downgrade when Broken", new String[]{"If this item's durability reaches 0,", "it will be fully repaired but also", "downgraded by one level.", "", "&cIt will only break if it cannot be", "&cdowngraded further", "", "Requires to define an &6Upgrade Template", "Required to define &6Custom Durability"}, new String[]{"equipment"}),
            DOWNGRADE_ON_DEATH = new BooleanStat("DEATH_DOWNGRADE", Material.DAMAGED_ANVIL, "Downgrade on Death", new String[]{"If the wearer of this item dies, it may", "downgrade (based on &6Death Downgrade", "&6Chance &7stat)", "", "Required to define an &6Upgrade Template", "Requires keep-inventory gamerule. "}, new String[]{"equipment"}),
            DOWNGRADE_ON_DEATH_CHANCE = new DoubleStat("DEATH_DOWNGRADE_CHANCE", Material.SKELETON_SKULL, "Death Downgrade Chance", new String[]{"Probability that an item with &cDowngrade ", "&con Death&7 will be downgraded when the", "player dies. ", "", "Exceeding 100% will for sure downgrade", "one item, and roll again to downgrade", "another (with the excess probability).", "&6The same item wont be downgraded twice."}, new String[]{"equipment"}, false),

    // Unique Item Stats
    DYE_COLOR = new DyeColor(),
            HIDE_DYE = new HideDye(),
            TRIM_MATERIAL = new TrimMaterialStat(),
            TRIM_PATTERN = new TrimPatternStat(),
            HIDE_ARMOR_TRIM = new HideTrim(),
            POTION_EFFECTS = new PotionEffects(),
            POTION_COLOR = new PotionColor(),
            SHIELD_PATTERN = new ShieldPatternStat(),
            HIDE_POTION_EFFECTS = new HidePotionEffects(),

    // Internal Stats
    SOULBOUND = new Soulbound(),
            CUSTOM_DURABILITY = new CustomDurability(),
            STORED_TAGS = new StoredTags(),
            ITEM_LEVEL = new ItemLevel(),
            BROWSER_DISPLAY_IDX = new BrowserDisplayIDX();

    /**
     * @deprecated Item damage is now {@link ItemDamage} and
     * custom durability is now {@link CustomDurability}
     */
    @Deprecated
    @DeprecatedStat
    public static final ItemStat DURABILITY = ITEM_DAMAGE;
}
