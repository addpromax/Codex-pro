package net.Indyuce.mmoitems.gui.edition;

import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.util.AltChar;
import io.lumine.mythic.lib.gui.Navigator;
import io.lumine.mythic.lib.player.particle.ParticleEffectType;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.edition.StatEdition;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ParticlesEdition extends EditionInventory {
    private static final NamespacedKey PATTERN_MODIFIED_KEY = new NamespacedKey(MMOItems.plugin, "PatternModifierId");

    public ParticlesEdition(Navigator navigator, MMOItemTemplate template) {
        super(navigator, template);
    }

    @Override
    public String getName() {
        return "Particles: " + template.getId();
    }

    @Override
    public void arrangeInventory() {
        int[] slots = {37, 38, 39, 40, 41, 42, 43};
        int n = 0;

        @Nullable ParticleEffectType particleType = null;
        try {
            particleType = ParticleEffectType.get(getEditedSection().getString("item-particles.type"));
        } catch (Exception ignored) {
        }

        ItemStack particleTypeItem = new ItemStack(Material.PINK_STAINED_GLASS);
        ItemMeta particleTypeItemMeta = particleTypeItem.getItemMeta();
        particleTypeItemMeta.setDisplayName(ChatColor.GREEN + "Particle Pattern");
        List<String> particleTypeItemLore = new ArrayList<>();
        particleTypeItemLore.add(ChatColor.GRAY + "The particle pattern defines how");
        particleTypeItemLore.add(ChatColor.GRAY + "particles behave, what pattern they follow");
        particleTypeItemLore.add(ChatColor.GRAY + "when displayed or what shape they form.");
        particleTypeItemLore.add("");
        particleTypeItemLore.add(ChatColor.GRAY + "Current Value: "
                + (particleType == null ? ChatColor.RED + "No type selected." : ChatColor.GOLD + particleType.getName()));
        if (particleType != null) {
            particleTypeItemLore.add("" + ChatColor.GRAY + ChatColor.ITALIC + particleType.getDescription());
        }
        particleTypeItemLore.add("");
        particleTypeItemLore.add(ChatColor.YELLOW + AltChar.listDash + " Click to change this value.");
        particleTypeItemLore.add(ChatColor.YELLOW + AltChar.listDash + " Right click to change this value.");
        particleTypeItemMeta.setLore(particleTypeItemLore);
        particleTypeItem.setItemMeta(particleTypeItemMeta);

        if (particleType != null) {
            ConfigurationSection psection = getEditedSection().getConfigurationSection("item-particles");
            for (String modifier : particleType.getModifiers()) {
                final ItemStack modifierItem = new ItemStack(Material.GRAY_DYE);
                ItemMeta modifierItemMeta = modifierItem.getItemMeta();
                modifierItemMeta.setDisplayName(ChatColor.GREEN + UtilityMethods.caseOnWords(modifier.toLowerCase().replace("-", " ")));
                List<String> modifierItemLore = new ArrayList<>();
                modifierItemLore.add("" + ChatColor.GRAY + ChatColor.ITALIC + "This is a pattern modifier.");
                modifierItemLore.add("" + ChatColor.GRAY + ChatColor.ITALIC + "Changing this value will slightly");
                modifierItemLore.add("" + ChatColor.GRAY + ChatColor.ITALIC + "customize the particle pattern.");
                modifierItemLore.add("");
                modifierItemLore.add(ChatColor.GRAY + "Current Value: " + ChatColor.GOLD
                        + (psection.contains(modifier) ? psection.getDouble(modifier) : particleType.getDefaultModifierValue(modifier)));
                modifierItemMeta.setLore(modifierItemLore);
                modifierItemMeta.getPersistentDataContainer().set(PATTERN_MODIFIED_KEY, PersistentDataType.STRING, modifier);
                modifierItem.setItemMeta(modifierItemMeta);

                inventory.setItem(slots[n++], modifierItem);
            }
        }

        @Nullable Particle particle = null;
        try {
            particle = Particle.valueOf(getEditedSection().getString("item-particles.particle"));
        } catch (Exception ignored) {
        }

        ItemStack particleItem = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta particleItemMeta = particleItem.getItemMeta();
        particleItemMeta.setDisplayName(ChatColor.GREEN + "Particle");
        List<String> particleItemLore = new ArrayList<>();
        particleItemLore.add(ChatColor.GRAY + "Defines what particle is used");
        particleItemLore.add(ChatColor.GRAY + "in the particle effect.");
        particleItemLore.add("");
        particleItemLore.add(ChatColor.GRAY + "Current Value: " + (particle == null ? ChatColor.RED + "No particle selected."
                : ChatColor.GOLD + UtilityMethods.caseOnWords(particle.name().toLowerCase().replace("_", " "))));
        particleItemLore.add("");
        particleItemLore.add(ChatColor.YELLOW + AltChar.listDash + " Click to change this value.");
        particleItemLore.add(ChatColor.YELLOW + AltChar.listDash + " Right click to change this value.");
        particleItemMeta.setLore(particleItemLore);
        particleItem.setItemMeta(particleItemMeta);

        if (particle != null && MMOUtils.isColorable(particle)) {
            int red = getEditedSection().getInt("item-particles.color.red");
            int green = getEditedSection().getInt("item-particles.color.green");
            int blue = getEditedSection().getInt("item-particles.color.blue");

            ItemStack colorItem = new ItemStack(Material.RED_DYE);
            ItemMeta colorItemMeta = colorItem.getItemMeta();
            colorItemMeta.setDisplayName(ChatColor.GREEN + "Particle Color");
            List<String> colorItemLore = new ArrayList<>();
            colorItemLore.add(ChatColor.GRAY + "The RGB color of your particle.");
            colorItemLore.add("");
            colorItemLore.add(ChatColor.GRAY + "Current Value (R-G-B):");
            colorItemLore.add("" + ChatColor.RED + ChatColor.BOLD + red + ChatColor.GRAY + " - " + ChatColor.GREEN + ChatColor.BOLD + green
                    + ChatColor.GRAY + " - " + ChatColor.BLUE + ChatColor.BOLD + blue);
            colorItemLore.add("");
            colorItemLore.add(ChatColor.YELLOW + AltChar.listDash + " Click to change this value.");
            colorItemLore.add(ChatColor.YELLOW + AltChar.listDash + " Right click to change this value.");
            colorItemMeta.setLore(colorItemLore);
            colorItem.setItemMeta(colorItemMeta);

            inventory.setItem(25, colorItem);
        }

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        glassMeta.setDisplayName(ChatColor.RED + "- No Modifier -");
        glass.setItemMeta(glassMeta);

        while (n < slots.length)
            inventory.setItem(slots[n++], glass);

        inventory.setItem(21, particleTypeItem);
        inventory.setItem(23, particleItem);
    }

    @Override
    public void whenClicked(InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();

        event.setCancelled(true);
        if (event.getInventory() != event.getClickedInventory() || !MMOUtils.isMetaItem(item, false))
            return;

        if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Particle")) {
            if (event.getAction() == InventoryAction.PICKUP_ALL)
                new StatEdition(this, ItemStats.ITEM_PARTICLES, "particle").enable("Write in the chat the particle you want.");

            if (event.getAction() == InventoryAction.PICKUP_HALF) {
                if (getEditedSection().contains("item-particles.particle")) {
                    getEditedSection().set("item-particles.particle", null);
                    registerTemplateEdition();
                    player.sendMessage(MMOItems.plugin.getPrefix() + "Successfully reset the particle.");
                }
            }
        }

        if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Particle Color")) {
            if (event.getAction() == InventoryAction.PICKUP_ALL)
                new StatEdition(this, ItemStats.ITEM_PARTICLES, "particle-color").enable("Write in the chat the RGB color you want.",
                        ChatColor.AQUA + "Format: [RED] [GREEN] [BLUE]");

            if (event.getAction() == InventoryAction.PICKUP_HALF) {
                if (getEditedSection().contains("item-particles.color")) {
                    getEditedSection().set("item-particles.color", null);
                    registerTemplateEdition();
                    player.sendMessage(MMOItems.plugin.getPrefix() + "Successfully reset the particle color.");
                }
            }
        }

        if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Particle Pattern")) {
            if (event.getAction() == InventoryAction.PICKUP_ALL) {
                new StatEdition(this, ItemStats.ITEM_PARTICLES, "particle-type").enable("Write in the chat the particle type you want.");
                player.sendMessage("");
                player.sendMessage("" + ChatColor.GREEN + ChatColor.BOLD + "Available Particles Patterns");
                for (ParticleEffectType type : ParticleEffectType.getAll())
                    player.sendMessage("* " + ChatColor.GREEN + type.getId());
            }

            if (event.getAction() == InventoryAction.PICKUP_HALF) {
                if (getEditedSection().contains("item-particles.type")) {
                    getEditedSection().set("item-particles.type", null);

                    // reset other modifiers
                    for (String key : getEditedSection().getConfigurationSection("item-particles").getKeys(false))
                        if (!key.equals("particle"))
                            getEditedSection().set("item-particles." + key, null);

                    registerTemplateEdition();
                    player.sendMessage(MMOItems.plugin.getPrefix() + "Successfully reset the particle pattern.");
                }
            }
        }

        final String tag = item.getItemMeta().getPersistentDataContainer().get(PATTERN_MODIFIED_KEY, PersistentDataType.STRING);
        if (tag == null || tag.equals("")) return;

        if (event.getAction() == InventoryAction.PICKUP_ALL)
            new StatEdition(this, ItemStats.ITEM_PARTICLES, tag).enable("Write in the chat the value you want.");

        if (event.getAction() == InventoryAction.PICKUP_HALF) {
            if (getEditedSection().contains("item-particles." + tag)) {
                getEditedSection().set("item-particles." + tag, null);
                registerTemplateEdition();
                player.sendMessage(MMOItems.plugin.getPrefix() + "Successfully reset " + ChatColor.GOLD + tag + ChatColor.GRAY + ".");
            }
        }
    }
}