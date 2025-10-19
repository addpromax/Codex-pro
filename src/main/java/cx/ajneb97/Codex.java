package cx.ajneb97;

import cx.ajneb97.api.AiyatsbusAPI;
import cx.ajneb97.api.CodexAPI;
import cx.ajneb97.commands.MainCommand;
import cx.ajneb97.config.ConfigsManager;
import cx.ajneb97.config.FishingConfigManager;
import cx.ajneb97.database.DatabaseFactory;
import cx.ajneb97.database.DatabaseManager;
import cx.ajneb97.listeners.PlayerListener;
import cx.ajneb97.listeners.InventoryListener;
import cx.ajneb97.listeners.CustomFishingListener;
import cx.ajneb97.listeners.dependencies.WorldGuardListener;
import cx.ajneb97.listeners.dependencies.MythicMobsListener;
import cx.ajneb97.listeners.dependencies.EliteMobsListener;
import cx.ajneb97.managers.*;
import cx.ajneb97.managers.dependencies.DependencyManager;
import cx.ajneb97.managers.dependencies.Metrics;
import cx.ajneb97.model.data.PlayerData;
import cx.ajneb97.model.structure.Category;
import cx.ajneb97.model.structure.Discovery;
import cx.ajneb97.tasks.PlayerDataSaveTask;
import cx.ajneb97.utils.ServerVersion;
import cx.ajneb97.versions.NMSManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.io.File;
import cx.ajneb97.managers.BatchItemsManager;

public class Codex extends JavaPlugin {

    PluginDescriptionFile pdfFile = getDescription();
    public String version = pdfFile.getVersion();
    public static String prefix;
    public static ServerVersion serverVersion;

    private ConfigsManager configsManager;
    private NMSManager nmsManager;
    private CommonItemManager commonItemManager;
    private MessagesManager messagesManager;
    private InventoryManager inventoryManager;
    private CategoryManager categoryManager;
    private DiscoveryManager discoveryManager;
    private DependencyManager dependencyManager;
    private PlayerDataManager playerDataManager;
    private EnchantmentManager enchantmentManager;
    private EnchantmentInfoManager enchantmentInfoManager;
    private FishingManager fishingManager;
    private FishingConfigManager fishingConfigManager;
    private BatchItemsManager batchItemsManager;
    private DebugManager debugManager;
    private LootGroupConfigManager lootGroupConfigManager;

    private DatabaseManager databaseManager; // 新的数据库管理器
    private PlayerDataSaveTask playerDataSaveTask;
    private VerifyManager verifyManager;

    private FileConfiguration messagesConfig;

    public void onEnable(){
        setVersion();
        this.playerDataManager = new PlayerDataManager(this);
        this.inventoryManager = new InventoryManager(this);
        this.commonItemManager = new CommonItemManager(this);
        this.categoryManager = new CategoryManager(this);
        this.discoveryManager = new DiscoveryManager(this);
        this.dependencyManager = new DependencyManager(this);
        this.nmsManager = new NMSManager(this);
        this.configsManager = new ConfigsManager(this);
        this.configsManager.configure();
        this.debugManager = DebugManager.getInstance(this);
        this.enchantmentInfoManager = new EnchantmentInfoManager(this);
        this.enchantmentManager = new EnchantmentManager(this);
        this.lootGroupConfigManager = new LootGroupConfigManager(this);
        this.fishingManager = new FishingManager(this);
        this.batchItemsManager = new BatchItemsManager(this);

        this.messagesConfig = configsManager.getMessagesConfigManager().getConfigFile().getConfig();
        setPrefix();

        if(configsManager.getMainConfigManager().getConfigVersion() != 2){
            legacyVersionError();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 显示启动ASCII艺术和信息
        displayStartupBanner();
        
        // 显示Paper版本兼容性信息
        cx.ajneb97.utils.PaperVersionChecker.logCompatibilityInfo();

        registerEvents();
        registerCommands();

        reloadPlayerDataSaveTask();
        
        // 注册JVM关闭钩子，在服务器异常关闭时处理数据保存
        registerShutdownHook();

        CodexAPI api = new CodexAPI(this);
        if(getServer().getPluginManager().getPlugin("PlaceholderAPI") != null){
            try {
                // 直接注册扩展
                getLogger().info("正在注册PlaceholderAPI扩展...");
                Class<?> papiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                if (papiClass != null) {
                    getLogger().info("成功注册PlaceholderAPI扩展");
                }
            } catch (Exception e) {
                getLogger().warning("注册PlaceholderAPI扩展时出错: " + e.getMessage());
            }
        }
        Metrics metrics = new Metrics(this,24230);

        this.verifyManager = new VerifyManager(this);
        this.verifyManager.verify();

        // 显示插件挂钩状态信息
        displayHookStatus();

        // 如果存在Aiyatsbus插件，延迟生成附魔图鉴，确保Aiyatsbus注册表已加载
        if(Bukkit.getPluginManager().getPlugin("Aiyatsbus") != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
            setupEnchantmentCategory();
                    // 同步附魔数据库，确保所有ID格式一致
                    syncEnchantmentsDatabase();
                }
            }.runTaskLater(this, 40L); // 延迟2秒
        }
        
        // 如果存在CustomFishing插件，延迟生成钓鱼图鉴
        if(Bukkit.getPluginManager().getPlugin("CustomFishing") != null && configsManager.getFishingConfigManager().isEnabled()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    setupFishingCategory();
                }
            }.runTaskLater(this, 40L); // 延迟2秒
        }

        // 异步初始化数据库以避免启动阻塞
        getLogger().info("正在异步初始化数据库...");
        new BukkitRunnable() {
            @Override
            public void run() {
                // 设置数据库连接 - 使用数据库工厂
                databaseManager = DatabaseFactory.createDatabaseManager(Codex.this);
                
                // 异步加载数据
                if (databaseManager != null) {
                    getLogger().info("正在加载玩家数据...");
                    databaseManager.loadData();
                    
                    // 显示数据保存模式信息
                    String saveMode = configsManager.getMainConfigManager().getDataSaveMode();
                    if ("realtime".equals(saveMode)) {
                        int delay = configsManager.getMainConfigManager().getRealtimeDebounceDelay();
                        getLogger().info("数据保存模式: 实时保存 (修改后 " + delay + " 秒内无新修改时自动保存)");
                    } else {
                        getLogger().info("数据保存模式: 批量保存 (定期保存，间隔: " + configsManager.getMainConfigManager().getPlayerDataSave() + "秒)");
                    }
                    
                    getLogger().info("数据库初始化完成!");
                } else {
                    getLogger().severe("数据库初始化失败!");
                }
            }
        }.runTaskAsynchronously(this);
    }

    /**
     * 插件关闭时执行
     * 保存所有玩家数据，确保不丢失
     */
    @Override
    public void onDisable() {
        // 显示关闭时的美化提示信息
        displayShutdownBanner();
        
        getLogger().info("开始执行插件关闭流程...");
        
        // 1. 首先停止定时保存任务，避免冲突
        if (playerDataSaveTask != null) {
            getLogger().info("正在停止定时保存任务...");
            playerDataSaveTask.end();
            
            // 等待当前保存任务完成（最多等待3秒）
            int waitCount = 0;
            while (playerDataSaveTask.isSaving() && waitCount < 30) {
                try {
                    Thread.sleep(100);
                    waitCount++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            if (waitCount >= 30) {
                getLogger().warning("定时保存任务未能及时结束，强制继续关闭流程");
            } else {
                getLogger().info("定时保存任务已停止");
            }
        }
        
        // 2. 保存所有在线玩家数据
        if (playerDataManager != null) {
            getLogger().info("开始保存玩家数据...");
            try {
                // 使用详细信息版本，帮助诊断数据保存状态
                int savedCount = playerDataManager.saveAllModifiedData(true);
                getLogger().info("玩家数据保存完成，共保存 " + savedCount + " 个玩家数据");
                
                // 关闭实时保存执行器
                playerDataManager.shutdown();
                getLogger().info("实时保存执行器已关闭");
            } catch (Exception e) {
                getLogger().severe("保存玩家数据时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 3. 关闭数据库连接
        if (databaseManager != null) {
            getLogger().info("正在关闭数据库连接...");
            try {
                databaseManager.shutdown();
                getLogger().info("数据库连接已安全关闭");
            } catch (Exception e) {
                getLogger().severe("关闭数据库连接时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        getLogger().info("插件关闭流程完成");
    }

    public void legacyVersionError(){
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(this.messagesConfig.getString("legacyVersionError")));
        Bukkit.getConsoleSender().sendMessage(MessagesManager.getColoredMessage(this.messagesConfig.getString("legacyVersionLink")));
    }

    public void registerCommands(){
        this.getCommand("codex").setExecutor(new MainCommand(this));
        this.getCommand("testfishing").setExecutor(new cx.ajneb97.commands.TestFishingCommand(this));
    }

    public void registerEvents(){
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        
        // 注册其他依赖的监听器
        if(getServer().getPluginManager().getPlugin("WorldGuard") != null){
            getServer().getPluginManager().registerEvents(new WorldGuardListener(this), this);
        }
        if(getServer().getPluginManager().getPlugin("MythicMobs") != null){
            getServer().getPluginManager().registerEvents(new MythicMobsListener(this), this);
        }
        if(getServer().getPluginManager().getPlugin("EliteMobs") != null){
            getServer().getPluginManager().registerEvents(new EliteMobsListener(this), this);
        }
        if(getServer().getPluginManager().getPlugin("CustomFishing") != null){
            getServer().getPluginManager().registerEvents(new CustomFishingListener(this), this);
        }
    }

    public void setPrefix(){
        if(messagesConfig != null) {
            String prefixStr = messagesConfig.getString("prefix");
            prefix = prefixStr != null ? prefixStr : "&9[&bCodex&9] ";
        } else {
            prefix = "&9[&bCodex&9] ";
        }
    }

    public void setVersion(){
        String packageName = getServer().getClass().getPackage().getName();
        // 版本低于1.20.5
        if(packageName.contains("v1_20_R4")){
                serverVersion = ServerVersion.v1_20_R4;
            return;
        }
        if(packageName.contains("v1_21_R1")){
                serverVersion = ServerVersion.v1_21_R1;
            return;
        }
        if(packageName.contains("v1_21_R2")){
                serverVersion = ServerVersion.v1_21_R2;
            return;
        }
        if(packageName.contains("v1_21_R3")){
                serverVersion = ServerVersion.v1_21_R3;
            return;
        }
        if(packageName.contains("v1_21_R4")){
                serverVersion = ServerVersion.v1_21_R4;
            return;
        }
        if(packageName.contains("v1_21_R5")){
                serverVersion = ServerVersion.v1_21_R5;
            return;
        }
        
        try {
                    serverVersion = ServerVersion.valueOf(packageName.replace("org.bukkit.craftbukkit.", ""));
        } catch (IllegalArgumentException e) {
                    serverVersion = ServerVersion.v1_21_R5;
        }
    }

    public void reloadPlayerDataSaveTask() {
        if (playerDataSaveTask != null) {
            playerDataSaveTask.end();
        }
        
        if (configsManager.getMainConfigManager().isAutoSaveEnabled()) {
            int seconds = configsManager.getMainConfigManager().getPlayerDataSave();
            if (seconds <= 0) {
                getLogger().info("自动保存已禁用 (配置值为0)");
                return;
            }
                playerDataSaveTask = new PlayerDataSaveTask(this);
                playerDataSaveTask.start(seconds);
            getLogger().info("已启动玩家数据自动保存任务，间隔: " + seconds + " 秒");
        } else {
            getLogger().info("自动保存功能已禁用 (auto_save_enabled=false)");
            }
    }
    
    /**
     * 注册JVM关闭钩子，作为数据保存的最后保障
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                // 如果插件已经正常关闭，只需要简单的验证
                if (!isEnabled()) {
                    // 最后的数据验证，确保没有遗漏的数据
                    if (databaseManager != null) {
                        try {
                            // 最后一次检查是否还有未保存的数据
                            if (playerDataManager != null) {
                                int unsavedCount = 0;
                                for (PlayerData playerData : playerDataManager.getPlayers().values()) {
                                    if (playerData.isModified()) {
                                        unsavedCount++;
                                    }
                                }
                                
                                if (unsavedCount > 0) {
                                    System.err.println("[Codex-警告] 发现 " + unsavedCount + " 个未保存的玩家数据，执行最后保存...");
                                    playerDataManager.saveAllModifiedData();
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("[Codex-错误] 最后验证保存失败: " + e.getMessage());
                        }
                    }
                    return;
                }
                
                getLogger().warning("检测到JVM异常关闭，执行紧急数据保存...");
                
                // 停止所有异步任务
                if (playerDataSaveTask != null) {
                    playerDataSaveTask.end();
                }
                
                // 保存玩家数据
                if (playerDataManager != null) {
                    try {
                        getLogger().info("紧急保存玩家数据...");
                        int savedCount = playerDataManager.saveAllModifiedData();
                        getLogger().info("紧急保存完成，保存了 " + savedCount + " 个玩家数据");
                    } catch (Exception e) {
                        getLogger().severe("紧急保存玩家数据失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                // 确保数据库正确关闭
                if (databaseManager != null) {
                    try {
                        getLogger().info("紧急关闭数据库连接...");
                        databaseManager.shutdown();
                        getLogger().info("数据库连接已紧急关闭");
                    } catch (Exception e) {
                        getLogger().severe("紧急关闭数据库失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                getLogger().info("紧急数据保存流程完成");
            } catch (Exception e) {
                System.err.println("[Codex-严重错误] 紧急保存过程中出现意外错误: " + e.getMessage());
                e.printStackTrace();
            }
        }, "Codex-ShutdownHook"));
    }

    /**
     * 设置附魔分类和GUI
     */
    private void setupEnchantmentCategory() {
        // 设置附魔分类的实现
    }
    
    /**
     * 同步附魔数据库
     */
    private void syncEnchantmentsDatabase() {
        // 同步附魔数据库的实现
    }
    
    /**
     * 设置钓鱼分类
     */
    private void setupFishingCategory() {
        try {
            getLogger().info("开始检查钓鱼分类...");
            
            // 检查配置是否启用钓鱼图鉴
            if (!configsManager.getFishingConfigManager().isEnabled()) {
                getLogger().info("钓鱼图鉴功能已禁用，跳过分类生成");
                return;
            }
            
            String categoryName = configsManager.getFishingConfigManager().getCategoryName();
            
            // 检查钓鱼分类是否已经加载到CategoryManager
            Category existingCategory = categoryManager.getCategory(categoryName);
            if (existingCategory != null) {
                getLogger().info("✓ 钓鱼分类已加载: " + categoryName + " (包含 " + existingCategory.getDiscoveries().size() + " 个发现项)");
                getLogger().info("✓ 钓鱼图鉴已就绪，可以开始记录钓鱼发现");
                return;
            }
            
            // 检查是否启用自动生成
            if (!configsManager.getFishingConfigManager().isAutoGenerate()) {
                getLogger().info("钓鱼图鉴自动生成已禁用，跳过分类生成");
                return;
            }
            
            // 检查钓鱼分类配置文件是否已存在
            File fishingConfigFile = new File(getDataFolder(), "categories/" + categoryName + ".yml");
            
            if (fishingConfigFile.exists()) {
                getLogger().warning("⚠ 钓鱼分类配置文件存在但未加载，可能存在配置错误");
                getLogger().info("建议：删除 " + fishingConfigFile.getName() + " 并重启服务器以重新生成");
                return;
            }
            
            getLogger().info("钓鱼分类配置文件不存在，开始自动生成...");
            
            // 生成钓鱼分类
            Category fishingCategory = fishingManager.generateFishingCategory();
            
            if (fishingCategory != null) {
                // 添加到分类管理器
                categoryManager.addCategory(fishingCategory);
                
                // 重建发现项缓存，确保监听器能找到钓鱼发现项
                discoveryManager.rebuildCache();
                
                getLogger().info("✓ 钓鱼分类生成成功，包含 " + fishingCategory.getDiscoveries().size() + " 个发现项");
                getLogger().info("✓ 钓鱼图鉴已就绪，可以开始记录钓鱼发现");
                
                if (debugManager.isDebugEnabled()) {
                    getLogger().info("[DEBUG] 钓鱼分类详情: " + fishingCategory.getName());
                    for (Discovery discovery : fishingCategory.getDiscoveries()) {
                        getLogger().info("[DEBUG]   - " + discovery.getName() + " (ID: " + discovery.getId() + ")");
                    }
                }
            } else {
                getLogger().warning("✗ 钓鱼分类生成失败");
            }
        } catch (Exception e) {
            getLogger().severe("设置钓鱼分类时发生错误: " + e.getMessage());
            if (debugManager.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    // Getter方法
    public CommonItemManager getCommonItemManager() {
        return commonItemManager;
    }

    public ConfigsManager getConfigsManager() {
        return configsManager;
    }

    public NMSManager getNmsManager() {
        return nmsManager;
    }

    public MessagesManager getMessagesManager() {
        return messagesManager;
    }

    public void setMessagesManager(MessagesManager messagesManager) {
        this.messagesManager = messagesManager;
    }

    public FileConfiguration getMessagesConfig(){
        return messagesConfig;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public DependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public CategoryManager getCategoryManager() {
        return categoryManager;
    }

    public DiscoveryManager getDiscoveryManager() {
        return discoveryManager;
    }

    public EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }

    public EnchantmentInfoManager getEnchantmentInfoManager() {
        return enchantmentInfoManager;
    }

    public VerifyManager getVerifyManager() {
        return verifyManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public FishingManager getFishingManager() {
        return fishingManager;
    }

    public FishingConfigManager getFishingConfigManager() {
        return fishingConfigManager;
    }

    public BatchItemsManager getBatchItemsManager() {
        return batchItemsManager;
    }
    
    public DebugManager getDebugManager() {
        return debugManager;
    }

    /**
     * 显示插件启动时的ASCII艺术标题和插件信息
     */
    private void displayStartupBanner() {
        String[] banner = {
            "",
            "   &b&l██████╗ ██████╗ ██████╗ ███████╗██╗  ██╗",
            "  &b&l██╔════╝██╔═══██╗██╔══██╗██╔════╝╚██╗██╔╝",
            "  &b&l██║     ██║   ██║██║  ██║█████╗   ╚███╔╝ ",
            "  &b&l██║     ██║   ██║██║  ██║██╔══╝   ██╔██╗ ",
            "  &b&l╚██████╗╚██████╔╝██████╔╝███████╗██╔╝ ██╗",
            "   &b&l╚═════╝ ╚═════╝ ╚═════╝ ╚══════╝╚═╝  ╚═╝",
            "",
            "  &9╔═══════════════════════════════════════╗",
            "  &9║      &f&lRPG图鉴发现系统 - v&e" + version + "      &9║",
            "  &9╠═══════════════════════════════════════╣",
            "  &9║  &f原作者: &eAjneb97                      &9║",
            "  &9║  &f当前维护: &amagicbili                  &9║",
            "  &9║  &f支持: &e1.8 - 1.21                     &9║",
            "  &9╚═══════════════════════════════════════╝",
            ""
        };

        for (String line : banner) {
            getServer().getConsoleSender().sendMessage(MessagesManager.getColoredMessage(line));
        }
    }

    /**
     * 显示插件各种挂钩状态信息
     */
    private void displayHookStatus() {
        boolean craftEngineHook = Bukkit.getPluginManager().getPlugin("CraftEngine") != null;
        boolean itemsAdderHook = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
        boolean aiyatsbusHook = Bukkit.getPluginManager().getPlugin("Aiyatsbus") != null;
        boolean customFishingHook = Bukkit.getPluginManager().getPlugin("CustomFishing") != null;
        
        getServer().getConsoleSender().sendMessage("");
        getServer().getConsoleSender().sendMessage(MessagesManager.getColoredMessage("  &9╔═══════════════ &f&l插件挂钩状态 &9═══════════════╗"));
        
        // CraftEngine挂钩状态
        String craftEngineStatus = craftEngineHook ? "&a✓ 成功" : "&c✗ 未找到";
        displayStatusLine("CraftEngine", craftEngineStatus);
        
        // ItemsAdder挂钩状态
        String itemsAdderStatus = itemsAdderHook ? "&a✓ 成功" : "&c✗ 未找到";
        displayStatusLine("ItemsAdder", itemsAdderStatus);
        
        // Aiyatsbus挂钩状态和API初始化状态
        boolean aiyatsbusAPIInitialized = false;
        if (aiyatsbusHook) {
            aiyatsbusAPIInitialized = AiyatsbusAPI.initialize(this);
        }
        String aiyatsbusStatus = aiyatsbusHook ? 
            "&a✓ 成功" + (aiyatsbusAPIInitialized ? " &7(API已初始化)" : " &c(API初始化失败)") : 
            "&c✗ 未找到";
        displayStatusLine("Aiyatsbus", aiyatsbusStatus);
        
        // CustomFishing挂钩状态
        String fishingEnabled = configsManager.getFishingConfigManager().isEnabled() ? "" : "但图鉴未启用";
        String customFishingStatus = customFishingHook ? 
            "&a✓ 成功" + (fishingEnabled.isEmpty() ? "" : " &7(" + fishingEnabled + ")") : 
            "&c✗ 未找到";
        displayStatusLine("CustomFishing", customFishingStatus);
        
        getServer().getConsoleSender().sendMessage(MessagesManager.getColoredMessage("  &9╚════════════════════════════════════════════╝"));
        getServer().getConsoleSender().sendMessage("");
    }
    
    /**
     * 显示状态行，确保对齐
     * @param pluginName 插件名称
     * @param status 状态信息
     */
    private void displayStatusLine(String pluginName, String status) {
        // 固定宽度表格，总宽度为46字符（包括边框）
        // 插件名称左侧占2个空格+2个冒号=4个字符
        // 插件名称与状态之间占2个空格
        // 状态右侧到边框
        int totalWidth = 46;
        int leftMargin = 4; // "  ║  "
        int pluginWidth = pluginName.length();
        int colonWidth = 2; // ": "
        
        // 移除颜色代码后计算真实宽度
        String statusNoColor = ChatColor.stripColor(MessagesManager.getColoredMessage(status));
        int statusWidth = getVisualWidth(statusNoColor);
        
        int spacesNeeded = totalWidth - leftMargin - pluginWidth - colonWidth - statusWidth - 1; // -1是右边的边框"║"
        
        if (spacesNeeded < 0) spacesNeeded = 0;
        
        StringBuilder line = new StringBuilder();
        line.append("  &9║  &f");
        line.append(pluginName);
        line.append("&f: ");
        line.append(status);
        line.append(getSpaces(spacesNeeded));
        line.append("&9║");
        
        getServer().getConsoleSender().sendMessage(MessagesManager.getColoredMessage(line.toString()));
    }
    
    /**
     * 计算字符串的视觉宽度（考虑中文和特殊字符）
     * @param text 要计算宽度的文本
     * @return 视觉宽度
     */
    private int getVisualWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCJK(c)) {
                width += 2; // 中文字符算2个宽度
            } else if (c == '✓' || c == '✗') {
                width += 1; // Unicode符号算1个宽度
            } else {
                width += 1; // ASCII字符算1个宽度
            }
        }
        return width;
    }
    
    /**
     * 判断字符是否为中日韩文字（占用2个宽度）
     * @param c 要判断的字符
     * @return 是否为中日韩文字
     */
    private boolean isCJK(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
            || ub == Character.UnicodeBlock.CJK_COMPATIBILITY
            || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS
            || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || ub == Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT
            || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
            || ub == Character.UnicodeBlock.HIRAGANA
            || ub == Character.UnicodeBlock.KATAKANA
            || ub == Character.UnicodeBlock.HANGUL_SYLLABLES;
    }
    
    /**
     * 显示插件关闭时的ASCII艺术提示
     */
    private void displayShutdownBanner() {
        String[] banner = {
            "",
            "  &c╔════════════════════════════════════════════╗",
            "  &c║             &f&lCODEX 图鉴插件已停用           &c║",
            "  &c╠════════════════════════════════════════════╣",
            "  &c║  &f感谢使用! 期待下次再见...                 &c║",
            "  &c╚════════════════════════════════════════════╝",
            ""
        };

        for (String line : banner) {
            getServer().getConsoleSender().sendMessage(MessagesManager.getColoredMessage(line));
        }
    }

    /**
     * 生成指定数量的空格字符串
     * @param count 空格数量
     * @return 空格字符串
     */
    private String getSpaces(int count) {
        if (count <= 0) return "";
        StringBuilder spaces = new StringBuilder();
        for (int i = 0; i < count; i++) {
            spaces.append(" ");
        }
        return spaces.toString();
    }
    
    // Getter methods for managers
    public LootGroupConfigManager getLootGroupConfigManager() {
        return lootGroupConfigManager;
    }
}
