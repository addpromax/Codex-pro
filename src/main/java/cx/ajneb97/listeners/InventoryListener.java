package cx.ajneb97.listeners;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.InventoryManager;
import cx.ajneb97.managers.BatchItemsManager;
import cx.ajneb97.model.inventory.InventoryPlayer;
import cx.ajneb97.utils.InventoryUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class InventoryListener implements Listener {

    private Codex plugin;
    public InventoryListener(Codex plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void closeInventory(InventoryCloseEvent event){
        Player player = (Player) event.getPlayer();
        plugin.getInventoryManager().removeInventoryPlayer(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        
        // 🐛 调试信息1：基本事件信息
        plugin.getLogger().info("[DEBUG-GUI] InventoryListener收到点击事件 - 玩家: " + player.getName() + 
            ", 点击类型: " + e.getClick() + ", 动作: " + e.getAction());
        
        if (e.getClickedInventory() == null) {
            plugin.getLogger().info("[DEBUG-GUI] 点击的界面为null，跳过处理");
            return;
        }
        
        plugin.getLogger().info("[DEBUG-GUI] 点击界面: " + e.getClickedInventory().getType() + 
            ", 界面大小: " + e.getClickedInventory().getSize());
        
        InventoryManager invManager = plugin.getInventoryManager();
        InventoryPlayer inventoryPlayer = invManager.getInventoryPlayer(player);
        
        // 🐛 调试信息2：InventoryPlayer检测
        plugin.getLogger().info("[DEBUG-GUI] InventoryPlayer检测结果: " + 
            (inventoryPlayer != null ? ("存在，界面名: " + inventoryPlayer.getInventoryName()) : "不存在"));
        
        // 🔧 修复：如果是Codex GUI，优先处理并无条件取消事件
        if(inventoryPlayer != null) {
            // 🐛 调试信息3：GUI保护激活
            plugin.getLogger().info("[DEBUG-GUI] ✅ Codex GUI保护激活 - 取消事件，界面: " + inventoryPlayer.getInventoryName());
            
            // 关键修复：无论什么情况都要取消事件，防止拿出物品
            e.setCancelled(true);
            
            plugin.getLogger().info("[DEBUG-GUI] 事件已取消，isCancelled: " + e.isCancelled());
            
            if(e.getCurrentItem() == null || e.getClickedInventory() == null){
                plugin.getLogger().info("[DEBUG-GUI] 当前物品或界面为null，跳过按钮处理");
                return;
            }

            plugin.getLogger().info("[DEBUG-GUI] 检查按钮点击 - 当前物品: " + 
                (e.getCurrentItem().hasItemMeta() ? e.getCurrentItem().getItemMeta().getDisplayName() : "无名称"));

            if(e.getClickedInventory().equals(InventoryUtils.getTopInventory(player))) {
                plugin.getLogger().info("[DEBUG-GUI] 🎯 触发按钮处理逻辑");
                invManager.clickInventory(inventoryPlayer,e.getCurrentItem(),e.getClick());
            } else {
                plugin.getLogger().info("[DEBUG-GUI] 点击的不是顶层界面，跳过按钮处理");
            }
            return; // 处理完Codex GUI后退出
        }
        
        // 🐛 调试信息4：非Codex GUI处理
        plugin.getLogger().info("[DEBUG-GUI] 非Codex GUI，检查其他处理器");
        
        // 处理批量添加物品界面的点击（只有不是Codex GUI时才处理）
        BatchItemsManager batchItemsManager = plugin.getBatchItemsManager();
        if (batchItemsManager.hasBatchInventory(player)) {
            plugin.getLogger().info("[DEBUG-GUI] 检测到BatchItemsManager界面");
            handleBatchInventoryClick(e, player, batchItemsManager);
            return;
        }
        
        plugin.getLogger().info("[DEBUG-GUI] 无特殊界面处理，事件继续传递");
    }
    
    /**
     * 处理批量添加物品界面的点击
     * @param e 事件
     * @param player 玩家
     * @param batchItemsManager 批量物品管理器
     */
    private void handleBatchInventoryClick(InventoryClickEvent e, Player player, BatchItemsManager batchItemsManager) {
        int slot = e.getRawSlot();
        ItemStack currentItem = e.getCurrentItem();
        
        // 处理功能按钮点击
        if (batchItemsManager.handleFunctionClick(player, slot)) {
            e.setCancelled(true);
            return;
        }
        
        // 允许玩家在界面中放置和取出物品
        if (e.getAction() == InventoryAction.PLACE_ALL || 
            e.getAction() == InventoryAction.PLACE_ONE || 
            e.getAction() == InventoryAction.PLACE_SOME) {
            // 玩家放入物品，记录它
            if (e.getCursor() != null && !e.getCursor().getType().equals(Material.AIR)) {
                batchItemsManager.handleItemPlace(player, e.getCursor(), slot);
            }
        } else if (e.getAction() == InventoryAction.PICKUP_ALL || 
                   e.getAction() == InventoryAction.PICKUP_HALF || 
                   e.getAction() == InventoryAction.PICKUP_ONE || 
                   e.getAction() == InventoryAction.PICKUP_SOME) {
            // 玩家取出物品，移除记录
            if (currentItem != null && !currentItem.getType().equals(Material.AIR)) {
                batchItemsManager.handleItemTake(player, slot);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        
        // 处理批量添加物品界面的关闭
        BatchItemsManager batchItemsManager = plugin.getBatchItemsManager();
        if (batchItemsManager.hasBatchInventory(player)) {
            batchItemsManager.closeInventory(player);
            return;
        }
        
        plugin.getInventoryManager().removeInventoryPlayer(player);
    }
}
