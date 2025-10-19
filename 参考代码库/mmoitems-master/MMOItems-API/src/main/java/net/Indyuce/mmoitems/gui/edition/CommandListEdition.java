package net.Indyuce.mmoitems.gui.edition;

import io.lumine.mythic.lib.api.util.AltChar;
import io.lumine.mythic.lib.gui.Navigator;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.edition.StatEdition;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.util.MMOUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class CommandListEdition extends EditionInventory {
	private static final int[] slots = { 19, 20, 21, 22, 23, 24, 25, 28, 29, 33, 34, 37, 38, 42, 43 };
	private static final NamespacedKey CONFIG_KEY = new NamespacedKey(MMOItems.plugin, "ConfigKey");

	public CommandListEdition(Navigator navigator, MMOItemTemplate template) {
		super(navigator, template);
	}

	@Override
	public String getName() {
		return "Command List";
	}

	@Override
	public void arrangeInventory() {
		int n = 0;

		if (getEditedSection().contains("commands"))
			for (String key : getEditedSection().getConfigurationSection("commands").getKeys(false)) {

				String format = getEditedSection().getString("commands." + key + ".format");
				double delay = getEditedSection().getDouble("commands." + key + ".delay");
				boolean console = getEditedSection().getBoolean("commands." + key + ".console"),
						op = getEditedSection().getBoolean("commands." + key + ".op");

				final ItemStack item = new ItemStack(Material.COMPARATOR);
				ItemMeta itemMeta = item.getItemMeta();
				itemMeta.setDisplayName(format == null || format.equals("") ? ChatColor.RED + "No Format" : ChatColor.GREEN + format);
				List<String> itemLore = new ArrayList<>();
				itemLore.add("");
				itemLore.add(ChatColor.GRAY + "Command Delay: " + ChatColor.RED + delay);
				itemLore.add(ChatColor.GRAY + "Sent by Console: " + ChatColor.RED + console);
				itemLore.add(ChatColor.GRAY + "Sent w/ OP perms: " + ChatColor.RED + op);
				itemLore.add("");
				itemLore.add(ChatColor.YELLOW + AltChar.listDash + " Right click to remove.");
				itemMeta.setLore(itemLore);
				itemMeta.getPersistentDataContainer().set(CONFIG_KEY, PersistentDataType.STRING, key);
				item.setItemMeta(itemMeta);

				inventory.setItem(slots[n++], item);
			}

		ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta glassMeta = glass.getItemMeta();
		glassMeta.setDisplayName(ChatColor.RED + "- No Command -");
		glass.setItemMeta(glassMeta);

		ItemStack add = new ItemStack(Material.WRITABLE_BOOK);
		ItemMeta addMeta = add.getItemMeta();
		addMeta.setDisplayName(ChatColor.GREEN + "Register a command...");
		add.setItemMeta(addMeta);

		inventory.setItem(40, add);
		while (n < slots.length)
			inventory.setItem(slots[n++], glass);
	}

	@Override
	public void whenClicked(InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();

		event.setCancelled(true);
		if (event.getInventory() != event.getClickedInventory() || !MMOUtils.isMetaItem(item, false))
			return;

		if (item.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "Register a command...")) {
			new StatEdition(this, ItemStats.COMMANDS).enable("Write in the chat the command you want to add.", "", "To add a delay, use " + ChatColor.RED + "-d:<delay>",
					"To make the command cast itself w/ console, use " + ChatColor.RED + "-c", "To make the command cast w/ OP perms, use " + ChatColor.RED + "-op", "",
					ChatColor.YELLOW + "Ex: -d:10.3 -op bc Hello, this is a test command.");
			return;
		}

        final String tag = item.getItemMeta().getPersistentDataContainer().get(CONFIG_KEY, PersistentDataType.STRING);
        if (tag == null || tag.equals("")) return;

		if (event.getAction() == InventoryAction.PICKUP_HALF) {
			if (getEditedSection().contains("commands") && getEditedSection().getConfigurationSection("commands").contains(tag)) {
				getEditedSection().set("commands." + tag, null);
				registerTemplateEdition();
				player.sendMessage(MMOItems.plugin.getPrefix() + "Successfully removed " + ChatColor.GOLD + tag + ChatColor.DARK_GRAY
						+ " (Internal ID)" + ChatColor.GRAY + ".");
			}
		}
	}
}