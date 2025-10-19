package net.Indyuce.mmoitems.manager;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.ConfigFile;
import net.Indyuce.mmoitems.api.ItemSet;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class SetManager implements Reloadable {
    private final Map<String, ItemSet> itemSets = new HashMap<>();

    public SetManager() {
        reload();
    }

    public void reload() {
        itemSets.clear();

        ConfigFile config = new ConfigFile("item-sets");
        for (String id : config.getConfig().getKeys(false))
            try {
                final ConfigurationSection section = config.getConfig().getConfigurationSection(id);
                if (section == null)
                    throw new IllegalStateException(String.format("Item set '%s' is not a valid configuration section.", id));
                itemSets.put(id, new ItemSet(section));
            } catch (IllegalArgumentException exception) {
                MMOItems.plugin.getLogger().log(Level.SEVERE, String.format("Could not load item set '%s': %s", id, exception.getMessage()));
            }
    }

    public void register(ItemSet set) {
        itemSets.put(set.getId(), set);
    }

    public boolean has(String id) {
        return itemSets.containsKey(id);
    }

    public Collection<ItemSet> getAll() {
        return itemSets.values();
    }

    @Nullable
    public ItemSet get(String id) {
        return itemSets.get(id);
    }
}
