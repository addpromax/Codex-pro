package cx.ajneb97.config;

import cx.ajneb97.Codex;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigsManager {

	private Codex plugin;
	private MainConfigManager mainConfigManager;
	private MessagesConfigManager messagesConfigManager;
    private InventoryConfigManager inventoryConfigManager;
	private PlayersConfigManager playersConfigManager;
    private CategoriesConfigManager categoriesConfigManager; // 使用CategoriesConfigManager替代DataFolderConfigManager
    private EnchantmentsConfigManager enchantmentsConfigManager;
    private FishingConfigManager fishingConfigManager;
    private CategoryPaginationConfig categoryPaginationConfig;
	
    public ConfigsManager(Codex plugin){
		this.plugin = plugin;
		this.mainConfigManager = new MainConfigManager(plugin);
		this.messagesConfigManager = new MessagesConfigManager(plugin);
		this.inventoryConfigManager = new InventoryConfigManager(plugin);
        this.playersConfigManager = new PlayersConfigManager(plugin, "players");
        this.categoriesConfigManager = new CategoriesConfigManager(plugin, "categories");
        this.enchantmentsConfigManager = new EnchantmentsConfigManager(plugin);
        this.fishingConfigManager = new FishingConfigManager(plugin);
        this.categoryPaginationConfig = new CategoryPaginationConfig(plugin);
	}
	
    public boolean reload(){
        boolean isDebug = mainConfigManager.isDebug();
        
        if (isDebug) {
            plugin.getLogger().info("========== 开始重新加载配置 ==========");
        }
        
        if (isDebug) {
            plugin.getLogger().info("重新加载主配置文件...");
        }
        if(!mainConfigManager.reloadConfig()){
            plugin.getLogger().severe("主配置文件重新加载失败!");
            return false;
        }
        if (isDebug) {
            plugin.getLogger().info("主配置文件重新加载成功");
        }
        
        // 重新加载DebugManager的配置，确保debug状态被正确更新
        plugin.getDebugManager().reload();
        if (plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("Debug模式状态已更新");
        }
        
        if (isDebug) {
            plugin.getLogger().info("重新加载消息配置文件...");
        }
        if(!messagesConfigManager.reloadConfig()){
            plugin.getLogger().severe("消息配置文件重新加载失败!");
            return false;
        }
        if (isDebug) {
            plugin.getLogger().info("消息配置文件重新加载成功");
        }
        
        if (isDebug) {
            plugin.getLogger().info("重新加载界面配置文件...");
        }
        if(!inventoryConfigManager.reloadConfig()){
            plugin.getLogger().severe("界面配置文件重新加载失败!");
            return false;
        }
        if (isDebug) {
            plugin.getLogger().info("界面配置文件重新加载成功");
        }
        
        if (isDebug) {
            plugin.getLogger().info("重新加载分类配置文件...");
        }
        try {
            categoriesConfigManager.configure();
            if (isDebug) {
                plugin.getLogger().info("分类配置文件重新加载成功");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("分类配置文件重新加载失败: " + e.getMessage());
            if (isDebug) {
                e.printStackTrace();
            }
            return false;
        }
        
        if (isDebug) {
            plugin.getLogger().info("重新加载附魔配置文件...");
        }
        if(!enchantmentsConfigManager.reload()){
            plugin.getLogger().severe("附魔配置文件重新加载失败!");
            return false;
        }
        if (isDebug) {
            plugin.getLogger().info("附魔配置文件重新加载成功");
        }
        
        if (isDebug) {
            plugin.getLogger().info("重新加载钓鱼配置文件...");
        }
        if(!fishingConfigManager.reload()){
            plugin.getLogger().severe("钓鱼配置文件重新加载失败!");
            return false;
        }
        if (isDebug) {
            plugin.getLogger().info("钓鱼配置文件重新加载成功");
        }
        
        if (isDebug) {
            plugin.getLogger().info("清空玩家数据缓存...");
        }
        plugin.getPlayerDataManager().getPlayers().clear();
        
        if (isDebug) {
            plugin.getLogger().info("重新加载玩家配置文件...");
        }
        playersConfigManager.loadConfigs();
        if (isDebug) {
            plugin.getLogger().info("玩家配置文件重新加载成功");
            plugin.getLogger().info("========== 配置重新加载完成 ==========");
        }
        
        return true;
    }

    public void configure(){
		mainConfigManager.configure();
        messagesConfigManager.configure();
        inventoryConfigManager.configure();
		categoriesConfigManager.configure();
        
        // 确保enchantments.yml配置文件被正确注册和加载
        if (enchantmentsConfigManager.getConfigFile().isFirstTime()) {
            // 如果是第一次创建，保存默认配置
            saveDefaultEnchantmentsConfig();
        }
        enchantmentsConfigManager.reload(); // 重新加载配置
        
        // 确保fishing.yml配置文件被正确注册和加载
        if (fishingConfigManager.getConfigFile().isFirstTime()) {
            // 如果是第一次创建，保存默认配置
            saveDefaultFishingConfig();
        }
        fishingConfigManager.reload(); // 重新加载配置
        
        // 加载分类分页配置
        categoryPaginationConfig.loadAllPaginations();
        
        playersConfigManager.loadConfigs(); // 加载玩家数据
	}
    
    /**
     * 保存默认的fishing.yml配置文件
     */
    private void saveDefaultFishingConfig() {
        try {
            FileConfiguration config = fishingConfigManager.getConfig();
            
            // 基本设置
            config.set("settings.enabled", true);
            config.set("settings.auto_generate", true);
            config.set("settings.category_name", "fishing");
            config.set("settings.excluded_fish", new java.util.ArrayList<>());
            
            // Loot分组显示名称
            config.set("loot_group_names.ocean_fish", "海水组");
            config.set("loot_group_names.river_fish", "淡水组");
            config.set("loot_group_names.lava_fish", "岩浆组");
            config.set("loot_group_names.loots_in_water", "全部水域");
            config.set("loot_group_names.loots_in_lava", "全部岩浆");
            
            // 稀有度映射
            config.set("rarity_mapping.iridium_star", "传奇");
            config.set("rarity_mapping.iridium", "传奇");
            config.set("rarity_mapping.golden_star", "史诗");
            config.set("rarity_mapping.golden", "史诗");
            config.set("rarity_mapping.silver_star", "稀有");
            config.set("rarity_mapping.silver", "稀有");
            config.set("rarity_mapping.no_star", "普通");
            config.set("rarity_mapping.legendary", "传奇");
            config.set("rarity_mapping.epic", "史诗");
            config.set("rarity_mapping.rare", "稀有");
            config.set("rarity_mapping.uncommon", "不常见");
            config.set("rarity_mapping.common", "普通");
            
            // 环境映射
            config.set("environment_mapping.lava", "岩浆中");
            config.set("environment_mapping.lava_fish", "岩浆中");
            config.set("environment_mapping.water", "水中");
            config.set("environment_mapping.ocean", "水中");
            config.set("environment_mapping.river", "水中");
            config.set("environment_mapping.lake", "水中");
            
            // 显示配置
            config.set("display.category_item.id", "FISHING_ROD");
            config.set("display.category_item.name", "&b钓鱼图鉴");
            config.set("display.category_item.lore", java.util.Arrays.asList(
                "&7记录所有可钓到的鱼类",
                "&7发现进度: &e%progress_bar%",
                "&7已解锁: %unlocked%"
            ));
            
            config.set("display.discovery_unlocked.id", "COD");
            config.set("display.discovery_unlocked.name", "%name%");
            config.set("display.discovery_unlocked.lore", java.util.Arrays.asList(
                "%description%",
                "",
                "#7289da发现于: #ffffff%date%",
                "#7289da捕获数量: #ffffff%amount%条",
                "#7289da最大尺寸: #ffffff%max_size%厘米",
                "",
                "#7289da可钓生物群系:",
                "#ffffff%biomes%",
                "#7289da稀有度: #ffffff%rarity%"
            ));
            
            config.set("display.discovery_blocked.id", "BARRIER");
            config.set("display.discovery_blocked.name", "#8c8c8c未知鱼类");
            config.set("display.discovery_blocked.lore", java.util.Arrays.asList(
                "#8c8c8c继续钓鱼以发现这种鱼!",
                "",
                "#7289da可钓生物群系:",
                "#ffffff%biomes%",
                "#7289da稀有度: #ffffff%rarity%",
                "#7289da钓鱼环境: #ffffff%environment%"
            ));
            
            // 奖励配置
            config.set("rewards.per_discovery", java.util.Arrays.asList(
                "centered_message: #6bcbfe&m00                                                 00",
                "centered_message: ",
                "centered_message: #eeeeee&lCODEX UPDATED",
                "centered_message: &7钓鱼图鉴: %name%",
                "centered_message: ",
                "centered_message: #6bcbfe&m00                                                 00",
                "sound: BLOCK_ANVIL_USE",
                "experience: 50"
            ));
            
            config.set("rewards.all_discoveries", java.util.Arrays.asList(
                "centered_message: #6bcbfe&m00                                                 00",
                "centered_message: ",
                "centered_message: #eeeeee&l钓鱼大师!",
                "centered_message: &7你已经发现了所有鱼类!",
                "centered_message: ",
                "centered_message: #6bcbfe&m00                                                 00",
                "sound: UI_TOAST_CHALLENGE_COMPLETE",
                "experience: 500"
            ));
            
            // 保存配置
            fishingConfigManager.getConfigFile().saveConfig();
            plugin.getLogger().info("[FISHING] 默认fishing.yml配置文件创建成功");
        } catch (Exception e) {
            plugin.getLogger().severe("无法创建默认的fishing.yml配置文件: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 保存默认的enchantments.yml配置文件
     */
    private void saveDefaultEnchantmentsConfig() {
        try {
            FileConfiguration config = enchantmentsConfigManager.getConfig();
            
            // 基本设置
            config.set("settings.enabled", true);
            config.set("settings.auto_generate", true);
            config.set("settings.category_name", "enchantments");
            config.set("settings.main_menu_slot", 24);
            config.set("settings.fetch_aiyatsbus_info", true);
            
            // 附魔分类物品设置
            config.set("category_item.id", "ENCHANTED_BOOK");
            config.set("category_item.name", "&b附魔图鉴");
            config.set("category_item.lore", java.util.Arrays.asList(
                "&7发现并收集各种附魔", 
                "&7了解它们的特性和用途", 
                "", 
                "&7已解锁: %unlocked% &8[%progress_bar%&8] &8(&7%percentage%&8)"
            ));
            
            // 已解锁的附魔物品设置
            config.set("unlocked_item.id", "ENCHANTED_BOOK");
            config.set("unlocked_item.name", "%name%");
            config.set("unlocked_item.lore", java.util.Arrays.asList(
                "&b&l%enchant_name% &7(最高等级: &e%max_level%&7)",
                "&8&o%namespace%:%key%",
                "",
                "%description%",
                "",
                "&7适用物品:",
                "%applicable_items%",
                "",
                "&7冲突附魔:",
                "%conflicts%",
                "",
                "&7稀有度: %rarity%",
                "&8发现于 %date%"
            ));
            
            // 未解锁的附魔物品设置
            config.set("locked_item.id", "GRAY_DYE");
            config.set("locked_item.name", "&c??");
            config.set("locked_item.lore", java.util.Arrays.asList(
                "&7你还没有发现这个附魔",
                "&7继续探索世界以解锁更多附魔"
            ));
            
            // 附魔图鉴GUI设置
            config.set("gui.title", "&4Codex &7» &8附魔图鉴");
            config.set("gui.size", 54);
            config.set("gui.border.enabled", true);
            config.set("gui.border.item.id", "PURPLE_STAINED_GLASS_PANE");
            config.set("gui.border.item.name", " ");
            
            // 分页设置
            config.set("gui.pagination.enabled", true);
            config.set("gui.pagination.items_per_page", 28);
            config.set("gui.pagination.previous_button.slot", 45);
            config.set("gui.pagination.previous_button.item.id", "PLAYER_HEAD");
            config.set("gui.pagination.previous_button.item.name", "&7上一页");
            config.set("gui.pagination.previous_button.item.skull_data.texture", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzdhZWU5YTc1YmYwZGY3ODk3MTgzMDE1Y2NhMGIyYTdkNzU1YzYzMzg4ZmYwMTc1MmQ1ZjQ0MTlmYzY0NSJ9fX0=");
            config.set("gui.pagination.next_button.slot", 53);
            config.set("gui.pagination.next_button.item.id", "PLAYER_HEAD");
            config.set("gui.pagination.next_button.item.name", "&7下一页");
            config.set("gui.pagination.next_button.item.skull_data.texture", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjgyYWQxYjljYjRkZDIxMjU5YzBkNzVhYTMxNWZmMzg5YzNjZWY3NTJiZTM5NDkzMzgxNjRiYWM4NGE5NmUifX19");
            config.set("gui.pagination.page_info.slot", 49);
            config.set("gui.pagination.page_info.item.id", "PAPER");
            config.set("gui.pagination.page_info.item.name", "&7第 &e%current_page% &7页，共 &e%total_pages% &7页");
            config.set("gui.pagination.page_info.item.lore", java.util.Arrays.asList("&7总共 &e%total_items% &7个附魔"));
            
            // 附魔发现奖励
            config.set("rewards.per_discovery", java.util.Arrays.asList(
                "message: &a你发现了一个新的附魔: &b%name%&a!",
                "title: 20;60;20;&b附魔图鉴;&a发现新附魔: &b%name%",
                "playsound: ENTITY_PLAYER_LEVELUP;1;1"
            ));
            config.set("rewards.all_discoveries", java.util.Arrays.asList(
                "message: &a恭喜你发现了所有附魔!",
                "title: 20;100;20;&b附魔大师;&a你已收集所有附魔!",
                "playsound: UI_TOAST_CHALLENGE_COMPLETE;1;1",
                "give_exp: 1000"
            ));
            
            // 附魔过滤设置
            config.set("filter.include_vanilla", true);
            config.set("filter.include_aiyatsbus", true);
            config.set("filter.excluded", java.util.Arrays.asList("minecraft:mending"));
            
            // 附魔描述模板
            config.set("templates.default_description", java.util.Arrays.asList(
                "&7%aiyatsbus_description%",
                "&7最大等级: &e%max_level%"
            ));
            config.set("templates.applicable_items.items_per_line", 3);
            config.set("templates.applicable_items.format", "&8- &7%item%");
            config.set("templates.applicable_items.none", "&8- &7无");
            config.set("templates.applicable_items.too_many", "&8- &7以及更多...");
            config.set("templates.applicable_items.max_items", 9);
            config.set("templates.conflicts.enchants_per_line", 2);
            config.set("templates.conflicts.format", "&8- &7%enchant%");
            config.set("templates.conflicts.none", "&8- &7无");
            config.set("templates.conflicts.too_many", "&8- &7以及更多...");
            config.set("templates.conflicts.max_conflicts", 6);
            config.set("templates.rarity.common", "&7普通");
            config.set("templates.rarity.uncommon", "&a罕见");
            config.set("templates.rarity.rare", "&9稀有");
            config.set("templates.rarity.very_rare", "&5非常稀有");
            config.set("templates.rarity.unknown", "&8未知");
            
            // 保存配置
            enchantmentsConfigManager.getConfigFile().saveConfig();
        } catch (Exception e) {
            plugin.getLogger().severe("无法创建默认的enchantments.yml配置文件: " + e.getMessage());
            e.printStackTrace();
        }
    }

	public MainConfigManager getMainConfigManager() {
		return mainConfigManager;
	}

	public MessagesConfigManager getMessagesConfigManager() {
		return messagesConfigManager;
	}

    public InventoryConfigManager getInventoryConfigManager() {
        return inventoryConfigManager;
	}

	public PlayersConfigManager getPlayersConfigManager() {
		return playersConfigManager;
	}

    public CategoriesConfigManager getCategoriesConfigManager() {
        return categoriesConfigManager;
		}
    
        public EnchantmentsConfigManager getEnchantmentsConfigManager() {
        return enchantmentsConfigManager;
    }
    
    public FishingConfigManager getFishingConfigManager() {
        return fishingConfigManager;
    }
    
    public CategoryPaginationConfig getCategoryPaginationConfig() {
        return categoryPaginationConfig;
    }
}
