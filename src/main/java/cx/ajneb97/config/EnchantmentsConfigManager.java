package cx.ajneb97.config;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.DebugManager;
import cx.ajneb97.model.item.CommonItem;
import cx.ajneb97.model.item.CommonItemSkullData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class EnchantmentsConfigManager {
    
    private Codex plugin;
    private CommonConfig configFile;
    
    private String categoryName;
    private int mainMenuSlot;
    private boolean fetchAiyatsbusInfo;
    private boolean showApiErrorWarnings;
    private int checkCooldown;
    private boolean debug;
    private boolean detectFromContainers;
    private List<String> containerTypes;

    public EnchantmentsConfigManager(Codex plugin) {
        this.plugin = plugin;
        this.configFile = new CommonConfig("enchantments.yml", plugin, null, false);
        this.configFile.registerConfig();
        
        // 确保默认配置项存在
        ensureDefaultConfig();
        loadSettings();
    }
    
    /**
     * 确保默认配置项存在
     */
    private void ensureDefaultConfig() {
        FileConfiguration config = getConfig();
        boolean needsSave = false;
        
        // 检查并设置check_cooldown
        if (!config.isSet("settings.check_cooldown")) {
            config.set("settings.check_cooldown", 500);
            needsSave = true;
        }
        
        // 检查并设置fetch_aiyatsbus_info
        if (!config.isSet("settings.fetch_aiyatsbus_info")) {
            config.set("settings.fetch_aiyatsbus_info", true);
            needsSave = true;
        }
        
        // 检查并设置show_api_error_warnings
        if (!config.isSet("settings.show_api_error_warnings")) {
            config.set("settings.show_api_error_warnings", false);
            needsSave = true;
        }
        
        // 检查并设置debug
        if (!config.isSet("settings.debug")) {
            config.set("settings.debug", false);
            needsSave = true;
        }
        
        // 检查并设置detect_from_containers
        if (!config.isSet("settings.detect_from_containers")) {
            config.set("settings.detect_from_containers", true);
            needsSave = true;
        }
        
        // 检查并设置container_types
        if (!config.isSet("settings.container_types")) {
            List<String> defaultContainerTypes = Arrays.asList("CHEST", "TRAPPED_CHEST", "ENDER_CHEST", 
                                                              "SHULKER_BOX", "BARREL", "DISPENSER", "DROPPER", "HOPPER");
            config.set("settings.container_types", defaultContainerTypes);
            needsSave = true;
        }
        
        // 保存配置
        if (needsSave) {
            try {
                configFile.saveConfig();
            } catch (Exception e) {
                plugin.getLogger().warning("保存附魔配置时出错: " + e.getMessage());
            }
        }
    }

    private void loadSettings(){
        FileConfiguration config = configFile.getConfig();
        categoryName = config.getString("settings.category_name");
        mainMenuSlot = config.getInt("settings.main_menu_slot");
        fetchAiyatsbusInfo = config.getBoolean("settings.fetch_aiyatsbus_info", true);
        checkCooldown = config.getInt("settings.check_cooldown", 500);
        debug = config.getBoolean("settings.debug", false);
        detectFromContainers = config.getBoolean("settings.detect_from_containers", true);
        containerTypes = config.getStringList("settings.container_types");
        if(containerTypes.isEmpty()){
            containerTypes = Arrays.asList("CHEST", "TRAPPED_CHEST", "ENDER_CHEST", 
                                         "SHULKER_BOX", "BARREL", "DISPENSER", "DROPPER", "HOPPER");
        }
    }
    
    /**
     * 重新加载配置
     * @return 是否成功重新加载
     */
    public boolean reload() {
        return configFile.reloadConfig();
    }
    
    /**
     * 获取配置文件
     * @return 配置文件
     */
    public CommonConfig getConfigFile() {
        return configFile;
    }
    
    /**
     * 获取配置
     * @return 配置
     */
    public FileConfiguration getConfig() {
        return configFile.getConfig();
    }
    
    /**
     * 检查附魔图鉴功能是否启用
     * @return 是否启用
     */
    public boolean isEnabled() {
        return getConfig().getBoolean("settings.enabled", true);
    }
    
    /**
     * 检查是否启用调试模式
     * 现在使用统一的DebugManager
     * @return 是否启用调试模式
     */
    public boolean isDebug() {
        DebugManager debugManager = DebugManager.getInstance();
        return debugManager != null ? debugManager.isDebugEnabled() : false;
    }
    
    /**
     * 设置调试模式
     * 现在通过统一的DebugManager设置
     * @param debug 是否启用调试模式
     */
    public void setDebug(boolean debug) {
        DebugManager debugManager = DebugManager.getInstance();
        if (debugManager != null) {
            debugManager.setDebugEnabled(debug);
        } else {
            // 兜底方案，直接写入配置
            getConfig().set("settings.debug", debug);
        }
    }
    
    /**
     * 获取附魔检测冷却时间（毫秒）
     * @return 冷却时间
     */
    public int getCheckCooldown() {
        return getConfig().getInt("settings.check_cooldown", 500);
    }
    
    /**
     * 检查是否自动生成附魔图鉴
     * @return 是否自动生成
     */
    public boolean isAutoGenerate() {
        return getConfig().getBoolean("settings.auto_generate", true);
    }
    
    /**
     * 检查是否从Aiyatsbus获取附魔信息
     * @return 是否获取
     */
    public boolean fetchAiyatsbusInfo() {
        return getConfig().getBoolean("settings.fetch_aiyatsbus_info", true);
    }
    
    /**
     * 检查是否显示API错误警告
     * @return 是否显示API错误警告
     */
    public boolean showApiErrorWarnings() {
        return getConfig().getBoolean("settings.show_api_error_warnings", false);
    }
    
    /**
     * 获取附魔分类名称
     * @return 分类名称
     */
    public String getCategoryName() {
        return getConfig().getString("settings.category_name", "enchantments");
    }
    
    /**
     * 获取附魔分类在主菜单中的位置
     * @return 主菜单位置
     */
    public int getMainMenuSlot() {
        return getConfig().getInt("settings.main_menu_slot", 24);
    }
    
    /**
     * 获取附魔分类物品
     * @return 分类物品
     */
    public CommonItem getCategoryItem() {
        CommonItem item = new CommonItem("ENCHANTED_BOOK");
        ConfigurationSection section = getConfig().getConfigurationSection("category_item");
        if (section != null) {
            item.setId(section.getString("id", "ENCHANTED_BOOK"));
            item.setName(section.getString("name", "&b附魔图鉴"));
            item.setLore(section.getStringList("lore"));
        } else {
            item.setId("ENCHANTED_BOOK");
            item.setName("&b附魔图鉴");
            List<String> lore = new ArrayList<>();
            lore.add("&7发现并收集各种附魔");
            lore.add("&7已解锁: %unlocked% &8[%progress_bar%&8] &8(&7%percentage%&8)");
            item.setLore(lore);
        }
        return item;
    }
    
    /**
     * 获取已解锁的附魔物品
     * @return 已解锁物品
     */
    public CommonItem getUnlockedItem() {
        CommonItem item = new CommonItem("ENCHANTED_BOOK");
        ConfigurationSection section = getConfig().getConfigurationSection("unlocked_item");
        if (section != null) {
            item.setId(section.getString("id", "ENCHANTED_BOOK"));
            item.setName(section.getString("name", "%name%"));
            item.setLore(section.getStringList("lore"));
        } else {
            item.setId("ENCHANTED_BOOK");
            item.setName("%name%");
            List<String> lore = new ArrayList<>();
            lore.add("%description%");
            lore.add("");
            lore.add("&8发现于 %date%");
            item.setLore(lore);
        }
        return item;
    }
    
    /**
     * 获取未解锁的附魔物品
     * @return 未解锁物品
     */
    public CommonItem getLockedItem() {
        CommonItem item = new CommonItem("GRAY_DYE");
        ConfigurationSection section = getConfig().getConfigurationSection("locked_item");
        if (section != null) {
            item.setId(section.getString("id", "GRAY_DYE"));
            item.setName(section.getString("name", "&c??"));
            item.setLore(section.getStringList("lore"));
        } else {
            item.setId("GRAY_DYE");
            item.setName("&c??");
            List<String> lore = new ArrayList<>();
            lore.add("&7你还没有发现这个附魔");
            item.setLore(lore);
        }
        return item;
    }
    
    /**
     * 获取GUI标题
     * @return GUI标题
     */
    public String getGuiTitle() {
        return getConfig().getString("gui.title", "&4Codex &7» &8附魔图鉴");
    }
    
    /**
     * 获取GUI大小
     * @return GUI大小
     */
    public int getGuiSize() {
        int size = getConfig().getInt("gui.size", 54);
        // 确保大小是9的倍数
        if (size % 9 != 0) {
            size = Math.min(54, Math.max(9, (size / 9) * 9));
        }
        return size;
    }
    
    /**
     * 检查是否启用边框
     * @return 是否启用边框
     */
    public boolean isBorderEnabled() {
        return getConfig().getBoolean("gui.border.enabled", true);
    }
    
    /**
     * 获取边框物品
     * @return 边框物品
     */
    public CommonItem getBorderItem() {
        CommonItem item = new CommonItem("PURPLE_STAINED_GLASS_PANE");
        ConfigurationSection section = getConfig().getConfigurationSection("gui.border.item");
        if (section != null) {
            item.setId(section.getString("id", "PURPLE_STAINED_GLASS_PANE"));
            item.setName(section.getString("name", " "));
        } else {
            item.setId("PURPLE_STAINED_GLASS_PANE");
            item.setName(" ");
        }
        return item;
    }
    
    /**
     * 获取每个发现的奖励
     * @return 奖励列表
     */
    public List<String> getPerDiscoveryRewards() {
        return getConfig().getStringList("rewards.per_discovery");
    }
    
    /**
     * 获取全部发现的奖励
     * @return 奖励列表
     */
    public List<String> getAllDiscoveriesRewards() {
        return getConfig().getStringList("rewards.all_discoveries");
    }
    
    /**
     * 检查是否包含原版附魔
     * @return 是否包含
     */
    public boolean includeVanilla() {
        return getConfig().getBoolean("filter.include_vanilla", true);
    }
    
    /**
     * 检查是否包含Aiyatsbus附魔
     * @return 是否包含
     */
    public boolean includeAiyatsbus() {
        return getConfig().getBoolean("filter.include_aiyatsbus", true);
    }
    
    /**
     * 获取排除的附魔列表
     * @return 排除列表
     */
    public List<String> getExcludedEnchantments() {
        return getConfig().getStringList("filter.excluded");
    }
    
    /**
     * 获取特定附魔的自定义描述
     * @param enchantmentId 附魔ID
     * @return 自定义描述，如果没有则返回null
     */
    public List<String> getCustomDescription(String enchantmentId) {
        if (getConfig().isSet("descriptions." + enchantmentId)) {
            return getConfig().getStringList("descriptions." + enchantmentId);
        }
        return null;
    }
    
    /**
     * 获取默认描述模板
     * @return 默认描述模板
     */
    public List<String> getDefaultDescriptionTemplate() {
        return getConfig().getStringList("templates.default_description");
    }
    
    /**
     * 获取每行最多显示的物品数量
     * @return 物品数量
     */
    public int getItemsPerLine() {
        return getConfig().getInt("templates.applicable_items.items_per_line", 3);
    }
    
    /**
     * 获取适用物品显示格式
     * @return 显示格式
     */
    public String getItemFormat() {
        return getConfig().getString("templates.applicable_items.format", "&8- &7%item%");
    }
    
    /**
     * 获取没有适用物品时显示的文本
     * @return 显示文本
     */
    public String getNoItemsText() {
        return getConfig().getString("templates.applicable_items.none", "&8- &7无");
    }
    
    /**
     * 获取适用物品过多时显示的文本
     * @return 显示文本
     */
    public String getTooManyItemsText() {
        return getConfig().getString("templates.applicable_items.too_many", "&8- &7以及更多...");
    }
    
    /**
     * 获取最多显示的物品数量
     * @return 物品数量
     */
    public int getMaxItems() {
        return getConfig().getInt("templates.applicable_items.max_items", 9);
    }
    
    /**
     * 获取每行最多显示的附魔数量
     * @return 附魔数量
     */
    public int getEnchantsPerLine() {
        return getConfig().getInt("templates.conflicts.enchants_per_line", 2);
    }
    
    /**
     * 获取冲突附魔显示格式
     * @return 显示格式
     */
    public String getEnchantFormat() {
        return getConfig().getString("templates.conflicts.format", "&8- &7%enchant%");
    }
    
    /**
     * 获取没有冲突附魔时显示的文本
     * @return 显示文本
     */
    public String getNoConflictsText() {
        return getConfig().getString("templates.conflicts.none", "&8- &7无");
    }
    
    /**
     * 获取冲突附魔过多时显示的文本
     * @return 显示文本
     */
    public String getTooManyConflictsText() {
        return getConfig().getString("templates.conflicts.too_many", "&8- &7以及更多...");
    }
    
    /**
     * 获取最多显示的冲突附魔数量
     * @return 附魔数量
     */
    public int getMaxConflicts() {
        return getConfig().getInt("templates.conflicts.max_conflicts", 6);
    }
    
    /**
     * 获取稀有度显示文本
     * @param rarity 稀有度
     * @return 显示文本
     */
    public String getRarityText(String rarity) {
        if (rarity == null) {
            rarity = "unknown";
        }
        return getConfig().getString("templates.rarity." + rarity.toLowerCase(), "&8未知");
    }
    
    /**
     * 获取边框槽位列表
     * @return 边框槽位
     */
    public List<Integer> getBorderSlots() {
        int size = getGuiSize();
        List<Integer> slots = new ArrayList<>();
        
        // 添加顶部和底部行
        for (int i = 0; i < 9; i++) {
            slots.add(i);
            slots.add(size - 9 + i);
        }
        
        // 添加左右两列
        for (int i = 9; i < size - 9; i += 9) {
            slots.add(i);
            slots.add(i + 8);
        }
        
        return slots;
    }
    
    /**
     * 检查是否启用分页
     * @return 是否启用分页
     */
    public boolean isPaginationEnabled() {
        return getConfig().getBoolean("gui.pagination.enabled", true);
    }
    
    /**
     * 获取每页显示的物品数量
     * @return 每页物品数量
     */
    public int getItemsPerPage() {
        return getConfig().getInt("gui.pagination.items_per_page", 28);
    }
    
    /**
     * 获取上一页按钮的槽位
     * @return 上一页按钮槽位
     */
    public int getPreviousButtonSlot() {
        return getConfig().getInt("gui.pagination.previous_button.slot", 45);
    }
    
    /**
     * 获取上一页按钮的物品
     * @return 上一页按钮物品
     */
    public CommonItem getPreviousButtonItem() {
        CommonItem item = new CommonItem("PLAYER_HEAD");
        ConfigurationSection section = getConfig().getConfigurationSection("gui.pagination.previous_button.item");
        if (section != null) {
            item.setId(section.getString("id", "PLAYER_HEAD"));
            item.setName(section.getString("name", "&7上一页"));
            if (section.isSet("skull_data.texture")) {
                CommonItemSkullData skullData = new CommonItemSkullData(null, section.getString("skull_data.texture"), null);
                item.setSkullData(skullData);
            }
        } else {
            item.setId("PLAYER_HEAD");
            item.setName("&7上一页");
        }
        return item;
    }
    
    /**
     * 获取下一页按钮的槽位
     * @return 下一页按钮槽位
     */
    public int getNextButtonSlot() {
        return getConfig().getInt("gui.pagination.next_button.slot", 53);
    }
    
    /**
     * 获取下一页按钮的物品
     * @return 下一页按钮物品
     */
    public CommonItem getNextButtonItem() {
        CommonItem item = new CommonItem("PLAYER_HEAD");
        ConfigurationSection section = getConfig().getConfigurationSection("gui.pagination.next_button.item");
        if (section != null) {
            item.setId(section.getString("id", "PLAYER_HEAD"));
            item.setName(section.getString("name", "&7下一页"));
            if (section.isSet("skull_data.texture")) {
                CommonItemSkullData skullData = new CommonItemSkullData(null, section.getString("skull_data.texture"), null);
                item.setSkullData(skullData);
            }
        } else {
            item.setId("PLAYER_HEAD");
            item.setName("&7下一页");
        }
        return item;
    }
    
    /**
     * 获取页面信息的槽位
     * @return 页面信息槽位
     */
    public int getPageInfoSlot() {
        return getConfig().getInt("gui.pagination.page_info.slot", 49);
    }
    
    /**
     * 获取页面信息的物品
     * @return 页面信息物品
     */
    public CommonItem getPageInfoItem() {
        CommonItem item = new CommonItem("PAPER");
        ConfigurationSection section = getConfig().getConfigurationSection("gui.pagination.page_info.item");
        if (section != null) {
            item.setId(section.getString("id", "PAPER"));
            item.setName(section.getString("name", "&7第 &e%current_page% &7页，共 &e%total_pages% &7页"));
            item.setLore(section.getStringList("lore"));
        } else {
            item.setId("PAPER");
            item.setName("&7第 &e%current_page% &7页，共 &e%total_pages% &7页");
            List<String> lore = new ArrayList<>();
            lore.add("&7总共 &e%total_items% &7个附魔");
            item.setLore(lore);
        }
        return item;
    }

    public boolean isDetectFromContainers() {
        return detectFromContainers;
    }

    public List<String> getContainerTypes() {
        return containerTypes;
    }
} 