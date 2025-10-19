package com.magicbili.enchantdisassembler;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.java.JavaPlugin;
import com.magicbili.enchantdisassembler.IntegrationAPI;

/**
 * 主插件类
 */
public class EnchantDisassembler extends JavaPlugin {
    private static EnchantDisassembler instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private DisassembleManager disassembleManager;
    private GUIHandler guiHandler;
    private CommandHandler commandHandler;
    private IntegrationAPI integration;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("§a正在启动 EnchantDisassembler...");

        // 检测兼容框架
        integration = new IntegrationAPI(this);
        if (!integration.hasAnyFramework()) {
            getLogger().severe("未检测到 Aiyatsbus 或 EcoEnchants，插件将自动关闭！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        // 输出检测结果
        if (integration.getEco().isAvailable()) {
            getLogger().info("§a已启用 EcoEnchants 兼容模块。");
        }
        if (integration.getAiyatsbus().isAiyatsbusAvailable()) {
            getLogger().info("§a已启用 Aiyatsbus 兼容模块。");
        }

        // 初始化管理器
        configManager = new ConfigManager(this, integration);
        databaseManager = new DatabaseManager(this);
        disassembleManager = new DisassembleManager(this, integration);
        guiHandler = new GUIHandler(this);
        commandHandler = new CommandHandler(this);

        // 初始化数据库
        databaseManager.initialize();

        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(new GUIListener(this), this);

        // 注册命令
        getCommand("disassemble").setExecutor(commandHandler);

        getLogger().info("§aEnchantDisassembler 已成功启用!");
    }

    @Override
    public void onDisable() {
        getLogger().info("§c正在关闭 EnchantDisassembler...");
        
        // 保存所有玩家数据
        if (disassembleManager != null) {
            disassembleManager.saveAllPlayerData();
        }
        
        // 关闭数据库连接
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        
        getLogger().info("§cEnchantDisassembler 已成功禁用!");
    }

    public static EnchantDisassembler getInstance() {
        return instance;
    }

    // Getter方法
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public DisassembleManager getDisassembleManager() {
        return disassembleManager;
    }

    public GUIHandler getGuiHandler() {
        return guiHandler;
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    public IntegrationAPI getIntegration() {
        return integration;
    }

    /**
     * 分解附魔，计算点数 - 暂时使用配置文件
     * @param enchant 附魔
     * @param level   等级
     * @return 分解点数
     */
    public double processEnchantment(Enchantment enchant, int level) {
        return disassembleManager.processEnchantment(enchant, level);
    }

    /**
     * 命令/调试: 列出所有注册的稀有度
     */
    public void listAllRarities() {
        getLogger().info("当前配置的稀有度:");
        for (String rarity : configManager.getRarities()) {
            getLogger().info("Rarity: " + rarity);
        }
    }

    public boolean isDebug() {
        return getConfig().getBoolean("debug", false);
    }

    public void debug(String message) {
        if (isDebug()) {
            getLogger().info("§7[调试] " + message);
        }
    }
}
