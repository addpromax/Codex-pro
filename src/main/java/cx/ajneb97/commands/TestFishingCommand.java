package cx.ajneb97.commands;

import cx.ajneb97.Codex;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * 调试命令：测试钓鱼分组功能
 */
public class TestFishingCommand implements CommandExecutor {
    
    private final Codex plugin;
    
    public TestFishingCommand(Codex plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c这个命令只能由玩家执行！");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("codex.admin")) {
            player.sendMessage("§c你没有权限执行这个命令！");
            return true;
        }
        
        if (args.length == 0) {
            player.sendMessage("§e用法：");
            player.sendMessage("§e/testfishing list - 列出所有鱼类");
            player.sendMessage("§e/testfishing groups <鱼类ID> - 测试特定鱼类的分组概率");
            player.sendMessage("§e/testfishing info <鱼类ID> - 获取鱼类详细信息");
            player.sendMessage("§e/testfishing cache - 查看缓存状态");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
                handleListCommand(player);
                break;
            case "groups":
            case "biomes": // 向后兼容旧命令
                if (args.length < 2) {
                    player.sendMessage("§c请指定鱼类ID：/testfishing groups <鱼类ID>");
                    return true;
                }
                handleGroupsCommand(player, args[1]);
                break;
            case "info":
                if (args.length < 2) {
                    player.sendMessage("§c请指定鱼类ID：/testfishing info <鱼类ID>");
                    return true;
                }
                handleInfoCommand(player, args[1]);
                break;
            case "cache":
                handleCacheCommand(player);
                break;
            default:
                player.sendMessage("§c未知子命令：" + subCommand);
                return true;
        }
        
        return true;
    }
    
    private void handleListCommand(Player player) {
        player.sendMessage("§a正在获取鱼类列表...");
        
        Map<String, Map<String, Object>> allFishData = plugin.getFishingManager().getAllFishData();
        
        if (allFishData.isEmpty()) {
            player.sendMessage("§c未找到任何鱼类数据！请检查CustomFishing是否正常运行。");
            return;
        }
        
        player.sendMessage("§e找到 " + allFishData.size() + " 种鱼类：");
        int count = 0;
        for (String fishId : allFishData.keySet()) {
            if (count >= 10) {
                player.sendMessage("§7... 还有 " + (allFishData.size() - 10) + " 种鱼类");
                break;
            }
            player.sendMessage("§f- " + fishId);
            count++;
        }
    }
    
    private void handleGroupsCommand(Player player, String fishId) {
        player.sendMessage("§a正在获取 " + fishId + " 的分组信息...");
        
        try {
            // 获取CustomFishing实例
            Class<?> customFishingClass = Class.forName("net.momirealms.customfishing.api.BukkitCustomFishingPlugin");
            Object customFishing = customFishingClass.getMethod("getInstance").invoke(null);
            
            if (customFishing == null) {
                player.sendMessage("§cCustomFishing未加载！");
                return;
            }
            
            // 获取LootGroupProbabilityCalculator
            java.lang.reflect.Method getCalculatorMethod = customFishing.getClass().getMethod("getLootGroupProbabilityCalculator");
            Object calculator = getCalculatorMethod.invoke(customFishing);
            
            if (calculator == null) {
                player.sendMessage("§cLootGroupProbabilityCalculator未初始化！");
                return;
            }
            
            // 获取loot所属分组
            java.lang.reflect.Method getLootGroupsMethod = calculator.getClass().getMethod("getLootGroups", String.class);
            @SuppressWarnings("unchecked")
            java.util.List<String> lootGroups = (java.util.List<String>) getLootGroupsMethod.invoke(calculator, fishId);
            
            if (lootGroups == null || lootGroups.isEmpty()) {
                player.sendMessage("§c该物品不属于任何loot分组！");
                return;
            }
            
            player.sendMessage("§e=== " + fishId + " 分组信息 ===");
            player.sendMessage("§b所属分组：§f" + String.join(", ", lootGroups));
            
            // 获取分组概率
            java.lang.reflect.Method calculateMethod = calculator.getClass().getMethod("calculateGroupProbabilities", String.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> groupProbabilities = (Map<String, Object>) calculateMethod.invoke(calculator, fishId);
            
            if (groupProbabilities == null || groupProbabilities.isEmpty()) {
                player.sendMessage("§c未能计算出分组概率！");
                return;
            }
            
            player.sendMessage("§b分组概率：");
            for (Map.Entry<String, Object> entry : groupProbabilities.entrySet()) {
                String groupId = entry.getKey();
                Object groupProbInfo = entry.getValue();
                
                try {
                    java.lang.reflect.Method getProbMethod = groupProbInfo.getClass().getMethod("getProbability");
                    java.lang.reflect.Method getCondMethod = groupProbInfo.getClass().getMethod("getConditionsDescription");
                    
                    double probability = (Double) getProbMethod.invoke(groupProbInfo);
                    String conditions = (String) getCondMethod.invoke(groupProbInfo);
                    
                    String displayName = plugin.getLootGroupConfigManager().getGroupDisplayName(groupId);
                    player.sendMessage(String.format("  §6%s§f: §a%.2f%%", displayName, probability));
                    if (conditions != null && !conditions.isEmpty()) {
                        player.sendMessage(String.format("    §7条件: %s", conditions));
                    }
                } catch (Exception e) {
                    player.sendMessage("  §c" + groupId + ": 获取概率失败");
                }
            }
            
            player.sendMessage("§a概率获取完成！");
            
        } catch (ClassNotFoundException e) {
            player.sendMessage("§cCustomFishing API未找到！请确保CustomFishing插件已安装。");
        } catch (NoSuchMethodException e) {
            player.sendMessage("§cCustomFishing API版本不兼容：" + e.getMessage());
        } catch (Exception e) {
            player.sendMessage("§c获取分组概率时出错：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleInfoCommand(Player player, String fishId) {
        player.sendMessage("§a正在获取 " + fishId + " 的CustomFishing信息...");
        
        Map<String, Object> itemInfo = plugin.getFishingManager().getCustomFishingItemInfo(fishId);
        
        if (itemInfo.isEmpty()) {
            player.sendMessage("§c无法从CustomFishing获取物品信息！");
            return;
        }
        
        String displayName = (String) itemInfo.get("displayName");
        @SuppressWarnings("unchecked")
        java.util.List<String> lore = (java.util.List<String>) itemInfo.get("lore");
        
        player.sendMessage("§e=== " + fishId + " CustomFishing信息 ===");
        
        if (displayName != null) {
            player.sendMessage("§b显示名称：§f" + displayName);
        }
        
        if (lore != null && !lore.isEmpty()) {
            player.sendMessage("§b描述：");
            for (String line : lore) {
                player.sendMessage("§f  " + line);
            }
        }
    }
    
    private void handleCacheCommand(Player player) {
        player.sendMessage("§e=== 系统状态 ===");
        player.sendMessage("§bCustomFishing状态：§f" + (org.bukkit.Bukkit.getPluginManager().getPlugin("CustomFishing") != null ? "已加载" : "未加载"));
        player.sendMessage("§bCodex FishingManager：§f" + (plugin.getFishingManager() != null ? "已加载" : "未加载"));
        player.sendMessage("§bLootGroupConfigManager状态：§f" + (plugin.getLootGroupConfigManager() != null ? "已加载" : "未加载"));
        player.sendMessage("§b调试模式：§f" + (plugin.getDebugManager().isDebugEnabled() ? "已启用" : "已关闭"));
        
        // 测试是否能获取鱼类数据
        try {
            Map<String, Map<String, Object>> allFishData = plugin.getFishingManager().getAllFishData();
            player.sendMessage("§b鱼类数据：§f" + allFishData.size() + " 种鱼类");
            
            // 测试一个示例鱼类
            if (!allFishData.isEmpty()) {
                String firstFishId = allFishData.keySet().iterator().next();
                Map<String, String> detailInfo = plugin.getFishingManager().getFishDetailInfo(firstFishId, player.getLocation(), false);
                player.sendMessage("§b示例鱼类 " + firstFishId + "：§f" + detailInfo.get("biomes"));
            }
        } catch (Exception e) {
            player.sendMessage("§c鱼类数据获取失败：" + e.getMessage());
        }
    }
}
