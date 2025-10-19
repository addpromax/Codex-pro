package net.Indyuce.mmoitems.stat;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.util.SmartGive;
import io.lumine.mythic.lib.gson.JsonObject;
import io.lumine.mythic.lib.util.lang3.Validate;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.interaction.Consumable;
import net.Indyuce.mmoitems.api.interaction.ItemSkin;
import net.Indyuce.mmoitems.api.item.mmoitem.MMOItem;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.stat.data.ParticleData;
import net.Indyuce.mmoitems.stat.data.SkullTextureData;
import net.Indyuce.mmoitems.stat.type.BooleanStat;
import net.Indyuce.mmoitems.stat.type.ConsumableItemInteraction;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.logging.Level;

public class CanDeskin extends BooleanStat implements ConsumableItemInteraction {
    public CanDeskin() {
        super("CAN_DESKIN", Material.LEATHER, "Can Deskin?",
                new String[]{"Players can deskin their item", "and get their skin back", "from the item."}, new String[]{"consumable"});
    }

    @Override
    public boolean handleConsumableEffect(@NotNull InventoryClickEvent event, @NotNull PlayerData playerData, @NotNull Consumable consumable, @NotNull NBTItem target, Type targetType) {
        final String skinId = target.getString(ItemSkin.SKIN_ID_TAG);
        Player player = playerData.getPlayer();

        if (consumable.getNBTItem().getBoolean("MMOITEMS_CAN_DESKIN") && !skinId.isEmpty()) {

            // Set target item to default skin
            String targetItemId = target.getString("MMOITEMS_ITEM_ID");
            target.removeTag(ItemSkin.SKIN_ID_TAG);

            MMOItemTemplate targetTemplate = MMOItems.plugin.getTemplates().getTemplateOrThrow(targetType, targetItemId);
            MMOItem originalMmoitem = targetTemplate.newBuilder(playerData.getRPG()).build();
            ItemStack originalItem = targetTemplate.newBuilder(playerData.getRPG()).build().newBuilder().build();

            if (originalMmoitem.hasData(ItemStats.ITEM_PARTICLES)) {
                JsonObject itemParticles = ((ParticleData) originalMmoitem.getData(ItemStats.ITEM_PARTICLES)).toJson();
                target.addTag(new ItemTag("MMOITEMS_ITEM_PARTICLES", itemParticles.toString()));
            } else
                target.removeTag("MMOITEMS_ITEM_PARTICLES");

            ItemStack targetItem = target.toItem();
            ItemMeta targetItemMeta = targetItem.getItemMeta();
            ItemMeta originalItemMeta = originalItem.getItemMeta();

            // Custom model data
            final Integer originalCustomModelData = originalItemMeta.hasCustomModelData() ? originalItemMeta.getCustomModelData() : null;
            targetItemMeta.setCustomModelData(originalCustomModelData);

            // TODO SkinStat. implement this with MI7

            // unbreakable
            if (targetItemMeta.isUnbreakable()) {
                targetItemMeta.setUnbreakable(originalItemMeta.isUnbreakable());
                if (targetItemMeta instanceof Damageable && originalItemMeta instanceof Damageable)
                    ((Damageable) targetItemMeta).setDamage(((Damageable) originalItemMeta).getDamage());
            }

            // leather armor color
            if (targetItemMeta instanceof LeatherArmorMeta && originalItemMeta instanceof LeatherArmorMeta)
                ((LeatherArmorMeta) targetItemMeta).setColor(((LeatherArmorMeta) originalItemMeta).getColor());

            // armor trim
            if (MythicLib.plugin.getVersion().isAbove(1, 20))
                if (targetItemMeta instanceof ArmorMeta && originalItemMeta instanceof ArmorMeta) {
                    ((ArmorMeta) targetItemMeta).setTrim(((ArmorMeta) originalItemMeta).getTrim());
                    if (originalItemMeta.hasItemFlag(ItemFlag.HIDE_ARMOR_TRIM))
                        targetItemMeta.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
                    else targetItemMeta.removeItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
                }

            // Equippable model
            if (MythicLib.plugin.getVersion().isAbove(1, 21, 2)) {
                if (targetItemMeta.hasEquippable()) {
                    targetItemMeta.setEquippable(originalItemMeta.getEquippable());
                }
            }

            // custom model data component
            if (MythicLib.plugin.getVersion().isAbove(1, 21, 4)) {
                targetItemMeta.setCustomModelDataComponent(originalItemMeta.getCustomModelDataComponent());
            }

            // item model
            if (MythicLib.plugin.getVersion().isAbove(1, 21, 2)) {
                targetItemMeta.setItemModel(originalItemMeta.getItemModel());
            }

            // TODO wtf is this
            if (target.hasTag("SkullOwner") && (targetItem.getType() == Material.PLAYER_HEAD)
                    && (originalItem.getType() == Material.PLAYER_HEAD))
                MythicLib.plugin.getVersion().getWrapper().setProfile((SkullMeta) targetItemMeta,
                        ((SkullTextureData) originalMmoitem.getData(ItemStats.SKULL_TEXTURE)).getGameProfile());

            // Update un-skined item
            final ItemStack updated = target.getItem();
            updated.setItemMeta(targetItemMeta);
            updated.setType(originalItem.getType());

            // Give back the skin item
            try {

                /*
                 * Try to find the skin item. This code is for backwards compatibility as
                 * cases with SKIN subtypes were not handled in the past, inducing an
                 * unfixable data loss for item skins applied onto items
                 */
                @Deprecated final String skinTypeId = target.getString(ItemSkin.SKIN_TYPE_TAG);
                final Type type = Objects.requireNonNullElse(Type.get(skinTypeId), Type.SKIN);
                final MMOItemTemplate template = MMOItems.plugin.getTemplates().getTemplateOrThrow(type, skinId);
                Validate.notNull(template, "Could not find item template of applied skin, type used " + type.getName());

                // Item found, giving it to the player
                final MMOItem mmoitem = template.newBuilder(playerData.getRPG()).build();
                new SmartGive(player).give(mmoitem.newBuilder().build());
            } catch (Exception exception) {
                MMOItems.plugin.getLogger().log(Level.SEVERE, "Could not retrieve item skin with ID '" + skinId + "' for player " + playerData.getUniqueId());
                // No luck :(
            }

            Message.SKIN_REMOVED.format(ChatColor.YELLOW, "#item#", MMOUtils.getDisplayName(targetItem)).send(player);
            return true;
        }
        return false;
    }
}
