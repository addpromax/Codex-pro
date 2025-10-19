package cx.ajneb97.managers;

import cx.ajneb97.Codex;
import cx.ajneb97.model.inventory.BatchItemsInventory;
import cx.ajneb97.model.structure.Category;
import cx.ajneb97.model.structure.DiscoveredOn;
import cx.ajneb97.model.structure.Discovery;
import cx.ajneb97.model.item.CommonItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import cx.ajneb97.managers.CommonItemManager;

/**
 * 管理批量物品添加功能
 */
public class BatchItemsManager {

    private Codex plugin;
    private Map<UUID, BatchItemsInventory> batchInventories;

    public BatchItemsManager(Codex plugin) {
        this.plugin = plugin;
        this.batchInventories = new HashMap<>();
    }

    /**
     * 打开批量添加物品的界面
     * @param player 玩家
     * @param categoryName 指定的分类名称（必须是新分类）
     */
    public void openBatchItemsInventory(Player player, String categoryName) {
        // 检查玩家权限
        if (!player.hasPermission("codex.admin.batch")) {
            String noPermissionMessage = plugin.getMessagesConfig().getString("batchNoPermission");
            player.sendMessage(plugin.getMessagesManager().getColoredMessage(plugin.prefix + noPermissionMessage));
            return;
        }
        
        // 验证分类名是否已存在
        if (plugin.getCategoryManager().getCategory(categoryName) != null) {
            String categoryExistsMessage = plugin.getMessagesConfig().getString("batchCategoryExists")
                    .replace("%category%", categoryName);
            player.sendMessage(plugin.getMessagesManager().getColoredMessage(plugin.prefix + categoryExistsMessage));
            return;
        }

        // 从配置中获取批量添加界面的标题和大小
        String title = plugin.getConfigsManager().getInventoryConfigManager().getBatchInventoryTitle();
        int size = plugin.getConfigsManager().getInventoryConfigManager().getBatchInventorySize();

        // 创建物品栏
        Inventory inventory = Bukkit.createInventory(null, size, plugin.getMessagesManager().getColoredMessage(title));

        // 添加功能按钮
        setupFunctionalButtons(inventory);

        // 创建并存储批量物品界面
        BatchItemsInventory batchItemsInventory = new BatchItemsInventory(player, inventory, title, categoryName);
        batchInventories.put(player.getUniqueId(), batchItemsInventory);

        // 打开界面
        player.openInventory(inventory);
        
        // 发送提示消息
        String openWithCategoryMessage = plugin.getMessagesConfig().getString("batchItemOpenedWithCategory")
                .replace("%category%", categoryName);
        player.sendMessage(plugin.getMessagesManager().getColoredMessage(plugin.prefix + openWithCategoryMessage));
    }

    /**
     * 设置功能按钮
     * @param inventory 物品栏
     */
    private void setupFunctionalButtons(Inventory inventory) {
        // 获取配置信息
        FileConfiguration config = plugin.getConfigsManager().getInventoryConfigManager().getConfig();
        String basePath = "batch_inventory";
        
        // 设置确认按钮
        ItemStack confirmButton = createButtonFromConfig(config, basePath + ".confirm_button");
        int confirmSlot = plugin.getConfigsManager().getInventoryConfigManager().getConfirmButtonSlot();
        inventory.setItem(confirmSlot, confirmButton);
        
        // 设置取消按钮
        ItemStack cancelButton = createButtonFromConfig(config, basePath + ".cancel_button");
        int cancelSlot = plugin.getConfigsManager().getInventoryConfigManager().getCancelButtonSlot();
        inventory.setItem(cancelSlot, cancelButton);
        
        // 设置信息按钮
        if (config.contains(basePath + ".info_button")) {
            ItemStack infoButton = createButtonFromConfig(config, basePath + ".info_button");
            int infoSlot = config.getInt(basePath + ".info_button.slot", 45);
            inventory.setItem(infoSlot, infoButton);
        }
        
        // 设置边框
        if (config.contains(basePath + ".border")) {
            ItemStack borderItem = createButtonFromConfig(config, basePath + ".border");
            List<Integer> borderSlots = config.getIntegerList(basePath + ".border.slots");
            for (int slot : borderSlots) {
                // 只在未设置按钮的位置设置边框
                if (slot != confirmSlot && slot != cancelSlot && 
                    !config.contains(basePath + ".info_button.slot") || 
                    slot != config.getInt(basePath + ".info_button.slot", 45)) {
                    inventory.setItem(slot, borderItem);
                }
            }
        }
    }
    
    /**
     * 从配置创建按钮
     * @param config 配置文件
     * @param path 配置路径
     * @return 按钮物品
     */
    private ItemStack createButtonFromConfig(FileConfiguration config, String path) {
        CommonItemManager itemManager = plugin.getCommonItemManager();
        
        // 创建通用物品
        String id = config.getString(path + ".id", "STONE");
        CommonItem commonItem = new CommonItem(id);
        
        // 设置名称
        String name = config.getString(path + ".name");
        if (name != null) {
            commonItem.setName(name);
        }
        
        // 设置描述
        List<String> lore = config.getStringList(path + ".lore");
        if (!lore.isEmpty()) {
            commonItem.setLore(lore);
        }
        
        // 创建物品
        return itemManager.createItemFromCommonItem(commonItem, null);
    }

    /**
     * 处理玩家放入物品的操作
     * @param player 玩家
     * @param item 物品
     * @param slot 槽位
     * @return 是否成功处理
     */
    public boolean handleItemPlace(Player player, ItemStack item, int slot) {
        BatchItemsInventory inventory = batchInventories.get(player.getUniqueId());
        
        if (inventory == null || !inventory.isOpen()) {
            return false;
        }
        
        // 检查是否是功能区域
        if (isFunctionalSlot(slot)) {
            return false;
        }
        
        // 添加物品到批量处理列表
        inventory.addItem(item.clone());
        inventory.setSlotItem(slot, item.clone());
        return true;
    }

    /**
     * 处理玩家取出物品的操作
     * @param player 玩家
     * @param slot 槽位
     * @return 是否成功处理
     */
    public boolean handleItemTake(Player player, int slot) {
        BatchItemsInventory inventory = batchInventories.get(player.getUniqueId());
        
        if (inventory == null || !inventory.isOpen()) {
            return false;
        }
        
        // 检查是否是功能区域
        if (isFunctionalSlot(slot)) {
            return false;
        }
        
        // 从槽位中移除物品
        inventory.removeSlotItem(slot);
        return true;
    }

    /**
     * 处理功能按钮点击
     * @param player 玩家
     * @param slot 槽位
     * @return 是否成功处理
     */
    public boolean handleFunctionClick(Player player, int slot) {
        BatchItemsInventory inventory = batchInventories.get(player.getUniqueId());
        
        if (inventory == null || !inventory.isOpen()) {
            return false;
        }
        
        // 检查是否是功能区域
        if (!isFunctionalSlot(slot)) {
            return false;
        }

        // 根据不同的功能按钮执行对应操作
        if (isConfirmButton(slot)) {
            processItems(player);
            return true;
        } else if (isCancelButton(slot)) {
            closeInventory(player);
            return true;
        }
        
        return false;
    }

    /**
     * 处理批量添加的物品
     * @param player 玩家
     */
    private void processItems(Player player) {
        BatchItemsInventory inventory = batchInventories.get(player.getUniqueId());
        
        if (inventory == null || inventory.getAddedItems().isEmpty()) {
            String noItemsMessage = plugin.getMessagesConfig().getString("batchNoItems");
            player.sendMessage(plugin.getMessagesManager().getColoredMessage(plugin.prefix + noItemsMessage));
            return;
        }
        
        // 获取指定的分类名称
        String categoryName = inventory.getCategoryName();
        if (categoryName == null || categoryName.isEmpty()) {
            player.sendMessage(plugin.getMessagesManager().getColoredMessage(plugin.prefix + "&c未指定有效的分类名称！"));
            return;
        }
        
        // 再次验证分类名是否已存在
        if (plugin.getCategoryManager().getCategory(categoryName) != null) {
            String categoryExistsMessage = plugin.getMessagesConfig().getString("batchCategoryExists")
                    .replace("%category%", categoryName);
            player.sendMessage(plugin.getMessagesManager().getColoredMessage(plugin.prefix + categoryExistsMessage));
            return;
        }
        
        // 创建新分类
        Category category = createNewCategory(categoryName);
        if (category == null) {
            player.sendMessage(plugin.getMessagesManager().getColoredMessage(plugin.prefix + "&c创建分类失败，请重试！"));
            return;
        }
        
        // 获取所有添加的物品
        List<ItemStack> items = inventory.getAddedItems();
        int totalItems = items.size();
        int processedItems = 0;
        
        // 处理每个物品，生成对应的解锁
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                boolean success = addDiscoveryToCategory(item, category);
                if (success) {
                    processedItems++;
                }
            }
        }
        
        // 保存分类
        plugin.getCategoryManager().addCategory(category);
        
        // 发送处理结果消息
        String resultMessage = plugin.getMessagesConfig().getString("batchItemsProcessedWithCategory")
                .replace("%processed%", String.valueOf(processedItems))
                .replace("%total%", String.valueOf(totalItems))
                .replace("%category%", categoryName);
        player.sendMessage(plugin.getMessagesManager().getColoredMessage(plugin.prefix + resultMessage));
        
        // 关闭界面
        closeInventory(player);
    }

    /**
     * 检测物品类型
     * @param item 物品
     * @return 物品类型 (mmoitems, itemsadder, customenchantments, vanilla)
     */
    private String detectItemType(ItemStack item) {
        // 检查MMOItems物品
        String mmoItemsType = getMMOItemsType(item);
        if (mmoItemsType != null) {
            return "mmoitems";
        }
        
        // 检查ItemsAdder物品
        String itemsAdderId = getItemsAdderId(item);
        if (itemsAdderId != null) {
            return "itemsadder";
        }
        
        // 检查CustomEnchantments物品
        String enchantmentId = getCustomEnchantmentId(item);
        if (enchantmentId != null) {
            return "customenchantments";
        }
        
        // 默认为原版物品
        return "vanilla";
    }

    /**
     * 获取MMOItems物品类型和ID
     * @param item 物品
     * @return MMOItems类型
     */
    private String getMMOItemsType(ItemStack item) {
        // MMOItems API检测
        if (Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            try {
                // 使用反射避免直接依赖MMOItems
                Class<?> mmoItemsClass = Class.forName("net.Indyuce.mmoitems.api.ItemTier");
                if (mmoItemsClass != null) {
                    // 这里只是检测插件是否存在，实际实现需要通过NBT标签获取MMOItems的类型和ID
                    String type = plugin.getNmsManager().getTagStringItem(item, "MMOITEMS_ITEM_TYPE");
                    if (type != null) {
                        return type;
                    }
                }
            } catch (Exception ignored) {
                // 如果无法加载MMOItems类，则忽略异常
            }
        }
        return null;
    }

    /**
     * 获取ItemsAdder物品ID
     * @param item 物品
     * @return ItemsAdder物品ID
     */
    private String getItemsAdderId(ItemStack item) {
        // ItemsAdder API检测
        if (Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            try {
                // 使用NBT标签检查是否为ItemsAdder物品
                String namespace = plugin.getNmsManager().getTagStringItem(item, "ItemsAdder");
                if (namespace != null) {
                    return namespace;
                }
            } catch (Exception ignored) {
                // 如果无法识别ItemsAdder物品，则忽略异常
            }
        }
        return null;
    }

    /**
     * 获取CustomEnchantments物品ID
     * @param item 物品
     * @return 附魔ID
     */
    private String getCustomEnchantmentId(ItemStack item) {
        // CustomEnchantments API检测
        if (Bukkit.getPluginManager().isPluginEnabled("Aiyatsbus")) {
            try {
                // 使用NBT标签检查是否为附魔物品
                String enchantId = plugin.getNmsManager().getTagStringItem(item, "aiyatsbus_enchantment");
                if (enchantId != null) {
                    return enchantId;
                }
            } catch (Exception ignored) {
                // 如果无法识别附魔物品，则忽略异常
            }
        }
        return null;
    }

    /**
     * 创建新的分类
     * @param categoryName 分类名称
     * @return 新创建的分类
     */
    private Category createNewCategory(String categoryName) {
        try {
            Category category = new Category();
            category.setName(categoryName);
            
            // 设置分类物品
            CommonItem categoryItem = new CommonItem("BOOK");
            categoryItem.setName("&e" + formatCategoryName(categoryName));
            List<String> lore = new ArrayList<>();
            lore.add("&7点击查看此分类的物品");
            lore.add("&7发现进度: &e%progress_bar%");
            lore.add("&7已解锁: %unlocked%");
            categoryItem.setLore(lore);
            category.setCategoryItem(categoryItem);
            
            // 设置默认解锁物品
            CommonItem unlockedItem = new CommonItem("LIME_STAINED_GLASS_PANE");
            unlockedItem.setName("&a%name%");
            List<String> unlockedLore = new ArrayList<>();
            unlockedLore.add("&7发现日期: &f%date%");
            unlockedLore.add("&7");
            unlockedLore.add("%description%");
            unlockedItem.setLore(unlockedLore);
            category.setDefaultLevelUnlockedItem(unlockedItem);
            
            // 设置默认未解锁物品
            CommonItem blockedItem = new CommonItem("RED_STAINED_GLASS_PANE");
            blockedItem.setName("&c???");
            List<String> blockedLore = new ArrayList<>();
            blockedLore.add("&7未发现");
            blockedItem.setLore(blockedLore);
            category.setDefaultLevelBlockedItem(blockedItem);
            
            // 初始化发现项列表
            category.setDiscoveries(new ArrayList<>());
            
            return category;
        } catch (Exception e) {
            plugin.getLogger().severe("创建新分类时出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 格式化分类名称
     * @param name 原始名称
     * @return 格式化后的名称
     */
    private String formatCategoryName(String name) {
        if (name == null || name.isEmpty()) {
            return "新分类";
        }
        
        String[] parts = name.split("_");
        StringBuilder formatted = new StringBuilder();
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                formatted.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        
        return formatted.toString().trim();
    }
    
    /**
     * 为物品创建发现项并添加到分类
     * @param item 物品
     * @param category 分类
     * @return 是否成功添加
     */
    private boolean addDiscoveryToCategory(ItemStack item, Category category) {
        // 检测物品类型
        String itemType = detectItemType(item);
        
        // 根据物品类型创建不同的发现项
        if ("mmoitems".equals(itemType)) {
            return addMMOItemDiscovery(item, category);
        } else if ("itemsadder".equals(itemType)) {
            return addItemsAdderDiscovery(item, category);
        } else if ("customenchantments".equals(itemType)) {
            return addCustomEnchantmentDiscovery(item, category);
        } else {
            // 默认使用原版模式
            return addVanillaItemDiscovery(item, category);
        }
    }
    
    /**
     * 添加MMOItems物品发现项
     * @param item 物品
     * @param category 分类
     * @return 是否成功添加
     */
    private boolean addMMOItemDiscovery(ItemStack item, Category category) {
        try {
            String type = plugin.getNmsManager().getTagStringItem(item, "MMOITEMS_ITEM_TYPE");
            String id = plugin.getNmsManager().getTagStringItem(item, "MMOITEMS_ITEM_ID");
            
            if (type == null || id == null) {
                return false;
            }
            
            // 创建发现项ID
            String discoveryId = "mmoitem_" + type.toLowerCase() + "_" + id.toLowerCase();
            
            // 创建新的发现项
            Discovery discovery = new Discovery();
            discovery.setId(discoveryId);
            discovery.setCategoryName(category.getName());
            discovery.setName(item.getItemMeta().getDisplayName());
            
            List<String> description = new ArrayList<>();
            if (item.getItemMeta().hasLore()) {
                description.addAll(item.getItemMeta().getLore());
            } else {
                description.add("&7MMOItems物品: &f" + type + ":" + id);
            }
            discovery.setDescription(description);
            
            // 设置发现条件
            DiscoveredOn discoveredOn = new DiscoveredOn(DiscoveredOn.DiscoveredOnType.ITEM_OBTAIN);
            discoveredOn.setMmoItemsType(type);
            discoveredOn.setMmoItemsId(id);
            discovery.setDiscoveredOn(discoveredOn);
            
            // 添加到分类中
            category.getDiscoveries().add(discovery);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("添加MMOItems发现项时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 添加ItemsAdder物品发现项
     * @param item 物品
     * @param category 分类
     * @return 是否成功添加
     */
    private boolean addItemsAdderDiscovery(ItemStack item, Category category) {
        try {
            String itemsAdderId = getItemsAdderId(item);
            
            if (itemsAdderId == null) {
                return false;
            }
            
            // 创建发现项ID
            String discoveryId = "itemsadder_" + itemsAdderId.toLowerCase().replace(":", "_");
            
            // 创建新的发现项
            Discovery discovery = new Discovery();
            discovery.setId(discoveryId);
            discovery.setCategoryName(category.getName());
            discovery.setName(item.getItemMeta().getDisplayName());
            
            List<String> description = new ArrayList<>();
            if (item.getItemMeta().hasLore()) {
                description.addAll(item.getItemMeta().getLore());
            } else {
                description.add("&7ItemsAdder物品: &f" + itemsAdderId);
            }
            discovery.setDescription(description);
            
            // 设置发现条件
            DiscoveredOn discoveredOn = new DiscoveredOn(DiscoveredOn.DiscoveredOnType.ITEM_OBTAIN);
            discoveredOn.setItemsAdderId(itemsAdderId);
            discovery.setDiscoveredOn(discoveredOn);
            
            // 添加到分类中
            category.getDiscoveries().add(discovery);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("添加ItemsAdder发现项时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 添加附魔物品发现项
     * @param item 物品
     * @param category 分类
     * @return 是否成功添加
     */
    private boolean addCustomEnchantmentDiscovery(ItemStack item, Category category) {
        try {
            String enchantId = getCustomEnchantmentId(item);
            
            if (enchantId == null) {
                return false;
            }
            
            // 创建发现项ID
            String discoveryId = "enchantment_" + enchantId.replace(":", "_").toLowerCase();
            
            // 创建新的发现项
            Discovery discovery = new Discovery();
            discovery.setId(discoveryId);
            discovery.setCategoryName(category.getName());
            
            // 尝试从附魔系统获取名称
            String enchantName = plugin.getEnchantmentManager().getEnchantmentName(enchantId);
            discovery.setName(enchantName != null ? enchantName : "附魔: " + enchantId);
            
            List<String> description = new ArrayList<>();
            String enchantDescription = plugin.getEnchantmentManager().getEnchantmentDescription(enchantId);
            if (enchantDescription != null) {
                description.add(enchantDescription);
            } else {
                description.add("&7附魔ID: &f" + enchantId);
            }
            discovery.setDescription(description);
            
            // 设置发现条件
            DiscoveredOn discoveredOn = new DiscoveredOn(DiscoveredOn.DiscoveredOnType.ENCHANTMENT_DISCOVER);
            discoveredOn.setEnchantmentId(enchantId);
            discovery.setDiscoveredOn(discoveredOn);
            discovery.setEnchantmentId(enchantId);
            
            // 添加到分类中
            category.getDiscoveries().add(discovery);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("添加附魔发现项时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 添加原版物品发现项
     * @param item 物品
     * @param category 分类
     * @return 是否成功添加
     */
    private boolean addVanillaItemDiscovery(ItemStack item, Category category) {
        try {
            String itemType = item.getType().name();
            Integer customModelData = null;
            
            if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                customModelData = item.getItemMeta().getCustomModelData();
            }
            
            // 创建发现项ID
            String itemId = itemType.toLowerCase();
            if (customModelData != null) {
                itemId += "_cmd_" + customModelData;
            }
            String discoveryId = "item_" + itemId;
            
            // 创建新的发现项
            Discovery discovery = new Discovery();
            discovery.setId(discoveryId);
            discovery.setCategoryName(category.getName());
            
            String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName() 
                ? item.getItemMeta().getDisplayName() 
                : formatMaterialName(itemType);
            discovery.setName(displayName);
            
            List<String> description = new ArrayList<>();
            if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                description.addAll(item.getItemMeta().getLore());
            } else {
                description.add("&7物品类型: &f" + formatMaterialName(itemType));
                if (customModelData != null) {
                    description.add("&7自定义模型数据: &f" + customModelData);
                }
            }
            discovery.setDescription(description);
            
            // 设置发现条件
            DiscoveredOn discoveredOn = new DiscoveredOn(DiscoveredOn.DiscoveredOnType.ITEM_OBTAIN);
            discoveredOn.setItemType(itemType);
            if (customModelData != null) {
                discoveredOn.setCustomModelData(customModelData);
            }
            discovery.setDiscoveredOn(discoveredOn);
            
            // 添加到分类中
            category.getDiscoveries().add(discovery);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("添加原版物品发现项时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 格式化物品材质名称
     * @param materialName 材质名称
     * @return 格式化后的名称
     */
    private String formatMaterialName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder formattedName = new StringBuilder();
        
        for (String part : parts) {
            if (!part.isEmpty()) {
                formattedName.append(part.substring(0, 1).toUpperCase()).append(part.substring(1)).append(" ");
            }
        }
        
        return formattedName.toString().trim();
    }

    /**
     * 检查是否是功能按钮槽位
     * @param slot 槽位
     * @return 是否是功能按钮槽位
     */
    private boolean isFunctionalSlot(int slot) {
        // 根据配置确定功能按钮的位置
        // 这里为简单示例，假设最后一行是功能区域
        int size = plugin.getConfigsManager().getInventoryConfigManager().getBatchInventorySize();
        return slot >= (size - 9);
    }

    /**
     * 检查是否是确认按钮
     * @param slot 槽位
     * @return 是否是确认按钮
     */
    private boolean isConfirmButton(int slot) {
        // 根据配置确定确认按钮的位置
        // 这里为简单示例，假设倒数第二个是确认按钮
        int size = plugin.getConfigsManager().getInventoryConfigManager().getBatchInventorySize();
        return slot == (size - 2);
    }

    /**
     * 检查是否是取消按钮
     * @param slot 槽位
     * @return 是否是取消按钮
     */
    private boolean isCancelButton(int slot) {
        // 根据配置确定取消按钮的位置
        // 这里为简单示例，假设倒数第一个是取消按钮
        int size = plugin.getConfigsManager().getInventoryConfigManager().getBatchInventorySize();
        return slot == (size - 1);
    }

    /**
     * 关闭批量物品界面
     * @param player 玩家
     */
    public void closeInventory(Player player) {
        BatchItemsInventory inventory = batchInventories.get(player.getUniqueId());
        
        if (inventory != null) {
            inventory.setOpen(false);
            player.closeInventory();
        }
        
        batchInventories.remove(player.getUniqueId());
    }

    /**
     * 检查玩家是否有打开的批量物品界面
     * @param player 玩家
     * @return 是否有打开的界面
     */
    public boolean hasBatchInventory(Player player) {
        BatchItemsInventory inventory = batchInventories.get(player.getUniqueId());
        return inventory != null && inventory.isOpen();
    }

    /**
     * 获取玩家的批量物品界面
     * @param player 玩家
     * @return 批量物品界面
     */
    public BatchItemsInventory getBatchInventory(Player player) {
        return batchInventories.get(player.getUniqueId());
    }
} 