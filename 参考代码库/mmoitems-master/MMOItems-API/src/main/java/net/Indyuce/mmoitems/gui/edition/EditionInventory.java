package net.Indyuce.mmoitems.gui.edition;

import io.lumine.mythic.lib.api.util.AltChar;
import io.lumine.mythic.lib.api.util.ui.FriendlyFeedbackProvider;
import io.lumine.mythic.lib.gui.Navigator;
import io.lumine.mythic.lib.version.VInventoryView;
import io.lumine.mythic.lib.version.VersionUtils;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.ConfigFile;
import net.Indyuce.mmoitems.api.item.template.MMOItemTemplate;
import net.Indyuce.mmoitems.api.item.template.ModifierNode;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.util.message.FFPMMOItems;
import net.Indyuce.mmoitems.gui.MMOItemsInventory;
import net.Indyuce.mmoitems.stat.data.random.RandomStatData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import io.lumine.mythic.lib.util.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class EditionInventory extends MMOItemsInventory {

    @Nullable
    protected Inventory inventory;

    /**
     * Item template currently being edited. This field is not final as it is
     * refreshed every time the item is edited (after applying a config change,
     * MMOItems updates the registered template and removes the old one)
     */
    protected MMOItemTemplate template;

    /**
     * Config file being edited. It is cached when the edition inventory is
     * opened and can only be accessed through the getEditedSection() method
     */
    private ConfigFile configFile;

    private final boolean displaysBack;

    /**
     * Template modifier being edited, if it is null then the player is directly
     * base item data
     *
     * @deprecated Not being used atm, the item editor only lets the user
     * edit the base item data.
     */
    @Deprecated
    private ModifierNode editedModifier = null;

    private ItemStack cachedItem;
    private int previousPage;

    public EditionInventory(@NotNull Navigator navigator, @NotNull MMOItemTemplate template) {
        this(navigator, template, true);
    }

    public EditionInventory(@NotNull Navigator navigator, @NotNull MMOItemTemplate template, boolean displaysBack) {
        super(navigator);

        this.displaysBack = displaysBack;

        // For logging back to the player
        ffp = new FriendlyFeedbackProvider(FFPMMOItems.get());
        ffp.activatePrefix(true, "Edition");

        // For building the Inventory
        this.template = template;
        this.configFile = template.getType().getConfigFile(); // Update config file
        final VInventoryView open = VersionUtils.getOpen(player);
        if (open.getTopInventory().getHolder() instanceof EditionInventory)
            this.cachedItem = ((EditionInventory) open.getTopInventory().getHolder()).cachedItem;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public abstract void arrangeInventory();

    /**
     * Refreshes the inventory but does not open it again for the player.
     * Has the same clientside effect as {@link #open()} but does not
     * create & open the inventory again.
     */
    public void refreshInventory() {
        Validate.notNull(inventory, "Inventory has never been opened");

        configFile = template.getType().getConfigFile(); // Update config file
        inventory.clear();
        // updateCachedItem();
        addEditionItems();
        arrangeInventory();
    }

    /**
     * Updates and open up the inventory to the player.
     */
    public void open(@Nullable EditionInventory previousInventory) {
        previousPage = previousInventory == null ? 0 : previousInventory.previousPage;
        open();
    }

    @Deprecated
    public void open(int previousPage) {
        this.previousPage = previousPage;
        open();
    }

    /**
     * Updates and open up the inventory to the player.
     */
    @Override
    public void open() {
        if (inventory == null) inventory = Bukkit.createInventory(this, 54, getName());
        configFile = template.getType().getConfigFile(); // Update config file
        addEditionItems();
        arrangeInventory();
        super.open();
    }

    public abstract String getName();

    public MMOItemTemplate getEdited() {
        return template;
    }

    /**
     * @return The currently edited configuration section. It depends on if the
     * player is editing the base item data or editing a modifier. This
     * config section contains item data (either the 'base' config
     * section or the 'stats' section for modifiers).
     */
    public ConfigurationSection getEditedSection() {
        ConfigurationSection config = configFile.getConfig().getConfigurationSection(template.getId());
        Validate.notNull(config, "Could not find config section associated to the template '" + template.getType().getId() + "." + template.getId()
                + "': make sure the config section name is in capital letters");
        return config.getConfigurationSection(editedModifier == null ? "base" : "modifiers." + editedModifier.getId() + ".stats");
    }

    /**
     * Used in edition GUIs to display the current stat data of the edited
     * template.
     *
     * @param stat The stat which data we are looking for
     * @return Optional which contains the corresponding random stat data
     */
    public <R extends RandomStatData<S>, S extends StatData> Optional<R> getEventualStatData(ItemStat<R, S> stat) {

        /*
         * The item data map used to display what the player is currently
         * editing. If he is editing a stat modifier, use the modifier item data
         * map. Otherwise, use the base item data map
         */
        Map<ItemStat, RandomStatData> map = editedModifier != null ? editedModifier.getItemData() : template.getBaseItemData();
        return map.containsKey(stat) ? Optional.of((R) map.get(stat)) : Optional.empty();
    }

    public void registerTemplateEdition() {
        configFile.registerTemplateEdition(template);

        /*
         * After the edition was registered, update the
         * cached itemStack before opening the menu again
         *
         * Some more optimization could be done here by only recalculating the
         * item in the GUI that corresponds to the stat that was edited but it's
         * pretty much useless since the heaviest step is to regenerate the item
         */
        template = MMOItems.plugin.getTemplates().getTemplate(template.getType(), template.getId());
        //editedModifier = editedModifier != null ? template.getModifier(editedModifier.getId()) : null;

        updateCachedItem();
        refreshInventory();
        //open();
    }

    /**
     * Method used when the player gets the item using the chest item so that he
     * can reroll the stats.
     */
    public void updateCachedItem() {
        cachedItem = template.newBuilder(PlayerData.get(getPlayer()).getRPG()).build().newBuilder().buildSilently();
    }

    public ItemStack getCachedItem() {
        if (cachedItem != null)
            return cachedItem;

        updateCachedItem();
        return cachedItem;
    }

    public void addEditionItems() {
        ItemStack get = new ItemStack(Material.CHEST);
        ItemMeta getMeta = get.getItemMeta();
        getMeta.addItemFlags(ItemFlag.values());
        getMeta.setDisplayName(ChatColor.GREEN + AltChar.fourEdgedClub + " Get the Item! " + AltChar.fourEdgedClub);
        List<String> getLore = new ArrayList<>();
        getLore.add(ChatColor.GRAY + "");
        getLore.add(ChatColor.GRAY + "You may also use /mi give " + template.getType().getId() + " " + template.getId());
        getLore.add(ChatColor.GRAY + "");
        getLore.add(ChatColor.YELLOW + AltChar.smallListDash + " Left click to get the item.");
        getLore.add(ChatColor.YELLOW + AltChar.smallListDash + " Right click to reroll its stats.");
        getMeta.setLore(getLore);
        get.setItemMeta(getMeta);

        if (displaysBack) {
            ItemStack back = new ItemStack(Material.BARRIER);
            ItemMeta backMeta = back.getItemMeta();
            backMeta.setDisplayName(ChatColor.GREEN + AltChar.rightArrow + " Back");
            back.setItemMeta(backMeta);

            inventory.setItem(6, back);
        }

        inventory.setItem(2, get);
        inventory.setItem(4, getCachedItem());
    }

    public int getPreviousPage() {
        return previousPage;
    }

    @NotNull
    final FriendlyFeedbackProvider ffp;

    @NotNull
    public FriendlyFeedbackProvider getFFP() {
        return ffp;
    }
}
