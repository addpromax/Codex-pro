package cx.ajneb97.listeners;

import cx.ajneb97.Codex;
import net.momirealms.customfishing.api.event.FishingLootSpawnEvent;
import net.momirealms.customfishing.api.mechanic.loot.Loot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * 监听CustomFishing的钓鱼事件并更新钓鱼图鉴
 */
public class CustomFishingListener implements Listener {
    
    private final Codex plugin;
    
    public CustomFishingListener(Codex plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 监听CustomFishing的战利品生成事件
     * 当玩家钓到物品或实体时更新钓鱼图鉴
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFishingLootSpawn(FishingLootSpawnEvent event) {
        try {
            boolean isDebug = plugin.getDebugManager().isDebugEnabled();
            
            if (isDebug) {
                plugin.getLogger().info("[FISHING-DEBUG] 检测到钓鱼事件: " + event.getClass().getSimpleName());
            }
            
            if (!plugin.getConfigsManager().getFishingConfigManager().isEnabled()) {
                if (isDebug) {
                    plugin.getLogger().info("[FISHING-DEBUG] 钓鱼图鉴功能已禁用，跳过处理");
                }
                return;
            }
            
            // 获取事件数据
            Player player = event.getPlayer();
            Loot loot = event.getLoot();
            String lootId = loot.id();
            
            if (isDebug) {
                plugin.getLogger().info("[FISHING-DEBUG] 玩家: " + player.getName() + " 钓到了: " + lootId + " (类型: " + loot.type() + ")");
            }
            
            // 只处理物品类型的战利品
            if (loot.type() != net.momirealms.customfishing.api.mechanic.loot.LootType.ITEM) {
                if (isDebug) {
                    plugin.getLogger().info("[FISHING-DEBUG] 跳过非物品类型的战利品: " + loot.type());
                }
                return;
            }
            
            // 获取钓鱼分类名称
            String categoryName = plugin.getConfigsManager().getFishingConfigManager().getCategoryName();
            
            // 验证钓鱼分类是否存在
            if (plugin.getCategoryManager().getCategory(categoryName) == null) {
                plugin.getLogger().warning("[FISHING] 钓鱼分类 '" + categoryName + "' 未找到！请检查配置或重新生成分类");
                return;
            }
            
            // 直接触发发现事件，让DiscoveryManager判断是否为新发现
            if (isDebug) {
                plugin.getLogger().info("[FISHING-DEBUG] 尝试触发钓鱼发现: categoryName=" + categoryName + ", discoveryName=" + lootId);
            }
            
            boolean discovered = plugin.getDiscoveryManager().onDiscover(player, categoryName, lootId);
            
            if (isDebug) {
                plugin.getLogger().info("[FISHING-DEBUG] 钓鱼发现结果: " + (discovered ? "✓ 新发现！" : "○ 已知物品"));
            }
            
            // 不记录警告，因为玩家可能钓到不在图鉴中的物品（如垃圾）
            if (!discovered && isDebug) {
                plugin.getLogger().info("[FISHING-DEBUG] 物品 " + lootId + " 未触发新发现（可能已解锁或不在图鉴中）");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[FISHING] 处理CustomFishing钓鱼事件时发生错误: " + e.getMessage());
            if (plugin.getDebugManager().isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 检查CustomFishing插件是否存在并加载所需事件类
     * @return 插件是否成功启用
     */
    public static boolean initialize() {
        if (Bukkit.getPluginManager().getPlugin("CustomFishing") == null) {
            return false;
        }
        
        // 验证CustomFishing事件类是否可用
        try {
            Class.forName("net.momirealms.customfishing.api.event.FishingLootSpawnEvent");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}