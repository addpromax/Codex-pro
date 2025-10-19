package com.magicbili.enchantdisassembler;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GUIHandler implements InventoryHolder {

    private final EnchantDisassembler plugin;
    private final Map<UUID, Inventory> openInventories = new ConcurrentHashMap<>();
    private final Map<UUID, InventoryView> inventoryViews = new ConcurrentHashMap<>();

    public GUIHandler(EnchantDisassembler plugin) {
        this.plugin = plugin;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public boolean isDisassembleGUI(InventoryView view) {
        return openInventories.containsValue(view.getTopInventory());
    }

    public void removeOpenInventory(Player player) {
        openInventories.remove(player.getUniqueId());
        inventoryViews.remove(player.getUniqueId());
    }

    public Inventory getPlayerInventory(Player player) {
        return openInventories.get(player.getUniqueId());
    }

    public void openDisassembleGUI(Player player) {
        // 检查世界是否禁用
        FileConfiguration config = plugin.getConfigManager().getConfig();
        List<String> disabledWorlds = config.getStringList("disabled-worlds");
        if (disabledWorlds != null && disabledWorlds.contains(player.getWorld().getName())) {
            player.sendMessage(plugin.getConfigManager().getMessage("disabled-in-world"));
            return;
        }

        // 加载玩家数据
        plugin.getDisassembleManager().loadPlayerData(player);

        // 创建GUI
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig();
        String title = plugin.getConfigManager().parseMiniMessage(guiConfig.getString("title", "&6附魔分解"));
        int size = guiConfig.getInt("size", 54);

        Inventory gui = Bukkit.createInventory(this, size, title);
        openInventories.put(player.getUniqueId(), gui);

        // 填充背景
        fillBackground(gui, guiConfig);

        // 填充按钮
        fillButtons(gui, guiConfig);

        // 填充池子显示
        fillPools(gui, guiConfig, player);

        // 打开GUI
        player.openInventory(gui);
        inventoryViews.put(player.getUniqueId(), player.getOpenInventory());
    }

    public void updateGUI(Player player) {
        Inventory gui = openInventories.get(player.getUniqueId());
        if (gui == null) return;

        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig();

        // 更新池子显示
        fillPools(gui, guiConfig, player);

        // 更新按钮
        fillButtons(gui, guiConfig);

        // 刷新GUI
        player.updateInventory();
    }

    private void fillBackground(Inventory gui, FileConfiguration guiConfig) {
        ConfigurationSection background = guiConfig.getConfigurationSection("background");
        if (background == null) return;

        if(!background.getBoolean("enabled", true)) return;

        // 需要跳过的槽位（输入槽、按钮等）
        java.util.Set<Integer> skipSlots = new java.util.HashSet<>();

        // 输入槽位
        ConfigurationSection inputSec = guiConfig.getConfigurationSection("input");
        if(inputSec!=null){
            skipSlots.addAll(inputSec.getIntegerList("slots"));
        }

        // 按钮槽位
        ConfigurationSection btnDestroy = guiConfig.getConfigurationSection("buttons.destroy");
        if(btnDestroy!=null) skipSlots.add(btnDestroy.getInt("slot", -1));
        ConfigurationSection btnReturn = guiConfig.getConfigurationSection("buttons.return");
        if(btnReturn!=null) skipSlots.add(btnReturn.getInt("slot", -1));

        String materialName = background.getString("material", "BLACK_STAINED_GLASS_PANE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.BLACK_STAINED_GLASS_PANE;

        String name = ChatColor.translateAlternateColorCodes('&',
                background.getString("name", " "));
        List<String> lore = background.getStringList("lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .toList();

        ItemStack backgroundItem = new ItemStack(material);
        ItemMeta meta = backgroundItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            backgroundItem.setItemMeta(meta);
        }

        // 填充整个背景
        for (int i = 0; i < gui.getSize(); i++) {
            if(!skipSlots.contains(i)){
            gui.setItem(i, backgroundItem);
            }
        }
    }

    private void fillButtons(Inventory gui, FileConfiguration guiConfig) {
        // 分解按钮
        fillButton(gui, guiConfig, "buttons.destroy");

        // 返还按钮
        fillButton(gui, guiConfig, "buttons.return");

        // 输入槽位占位符
        ConfigurationSection input = guiConfig.getConfigurationSection("input");
        if (input != null) {
            List<Integer> slots = input.getIntegerList("slots");
            ConfigurationSection itemConfig = input.getConfigurationSection("item");

            if (itemConfig != null) {
                String materialName = itemConfig.getString("material", "AIR");
                Material material = Material.matchMaterial(materialName);
                if (material == null) material = Material.AIR;

                String name = ChatColor.translateAlternateColorCodes('&',
                        itemConfig.getString("name", "&e放入需要分解的物品"));
                List<String> lore = itemConfig.getStringList("lore").stream()
                        .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                        .toList();

                ItemStack placeholder = new ItemStack(material);
                ItemMeta meta = placeholder.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(name);
                    meta.setLore(lore);
                    placeholder.setItemMeta(meta);
                }

                for (int slot : slots) {
                    // 只在槽位为空时放置占位符
                    if (slot < gui.getSize() && (gui.getItem(slot) == null || gui.getItem(slot).getType() == Material.AIR)) {
                        gui.setItem(slot, placeholder);
                    }
                }
            }
        }
    }

    private void fillButton(Inventory gui, FileConfiguration guiConfig, String path) {
        ConfigurationSection buttonConfig = guiConfig.getConfigurationSection(path);
        if (buttonConfig == null) return;

        int slot = buttonConfig.getInt("slot", -1);
        if (slot < 0 || slot >= gui.getSize()) return;

        String materialName = buttonConfig.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.STONE;

        String name = ChatColor.translateAlternateColorCodes('&',
                buttonConfig.getString("name", "&c按钮"));
        List<String> lore = buttonConfig.getStringList("lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .toList();

        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            button.setItemMeta(meta);
        }

        gui.setItem(slot, button);
    }

    private void fillPools(Inventory gui, FileConfiguration guiConfig, Player player) {
        // 直接使用 gui.yml 中定义的 pools 节点，避免与 Aiyatsbus 稀有度名称不一致
        Set<String> rarities = new java.util.HashSet<>();
        ConfigurationSection poolsSection = guiConfig.getConfigurationSection("pools");
        if (poolsSection != null) {
            rarities.addAll(poolsSection.getKeys(false));
        }
        rarities.addAll(plugin.getConfigManager().getRarities());
        DisassembleManager disassembleManager = plugin.getDisassembleManager();
        ConfigManager configManager = plugin.getConfigManager();

        for (String rarity : rarities) {
            ConfigurationSection poolConfig = guiConfig.getConfigurationSection("pools." + rarity);
            if (poolConfig == null) continue;

            int slot = poolConfig.getInt("slot", -1);
            if (slot < 0 || slot >= gui.getSize()) continue;

            // 获取点数
            int count = disassembleManager.getPlayerPools(player.getUniqueId()).getOrDefault(rarity, 0);
            int overflow = disassembleManager.getOverflowPools(player.getUniqueId()).getOrDefault(rarity, 0);
            int maxPool = configManager.getMaxPool(rarity);

            String materialName = poolConfig.getString("material", "WHITE_STAINED_GLASS_PANE");
            Material material = Material.matchMaterial(materialName);
            if (material == null) material = Material.WHITE_STAINED_GLASS_PANE;

            String name = ChatColor.translateAlternateColorCodes('&',
                    poolConfig.getString("name", "&f普通附魔池")
                            .replace("%rarity%", rarity));

            List<String> lore = new ArrayList<>();
            for (String line : poolConfig.getStringList("lore")) {
                line = line.replace("%count%", String.valueOf(count))
                        .replace("%overflow%", String.valueOf(overflow))
                        .replace("%max%", String.valueOf(maxPool))
                        .replace("%rarity%", rarity)
                        .replace("%progress%", count + "/" + maxPool);
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }

            // 如果池子已满，追加警告信息
            if (count >= maxPool) {
                for (String warn : guiConfig.getStringList("pool-full-lore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', warn));
                }
            }

            ItemStack poolItem = new ItemStack(material);
            ItemMeta meta = poolItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(lore);
                poolItem.setItemMeta(meta);
            }

            gui.setItem(slot, poolItem);
        }
    }
}
