package cx.ajneb97.listeners;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.InventoryManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Bukkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {

    private Codex plugin;
    private final Map<UUID, Long> lastCheckTime = new ConcurrentHashMap<>();
    
    public PlayerListener(Codex plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        plugin.getPlayerDataManager().setJoinPlayerData(event.getPlayer());
        
        // 移除玩家加入时的附魔检测
    }
    
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // 在玩家退出时保存数据，确保不丢失
        try {
            if (plugin.getPlayerDataManager().getPlayer(player, false) != null) {
                // 检查玩家数据是否有修改
                if (plugin.getPlayerDataManager().getPlayer(player, false).isModified()) {
                    if (plugin.getDebugManager().isDebugEnabled()) {
                        plugin.getLogger().info("[数据保存] 玩家 " + player.getName() + " 退出时保存数据");
                    }
                    
                    // 使用数据库管理器保存玩家数据
                    plugin.getDatabaseManager().updatePlayer(plugin.getPlayerDataManager().getPlayer(player, false));
                    plugin.getPlayerDataManager().getPlayer(player, false).setModified(false);
                } else if (plugin.getDebugManager().isDebugEnabled()) {
                    plugin.getLogger().info("[数据保存] 玩家 " + player.getName() + " 退出时无需保存（无修改）");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("保存玩家 " + player.getName() + " 退出数据时出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        // 清除玩家的附魔缓存
        plugin.getPlayerDataManager().clearEnchantmentCache(player);
        
        // 最后，从内存中移除玩家数据以释放内存（可选，如果需要内存优化）
        // 注意：这会导致玩家重新加入时需要重新从数据库加载数据
        if (plugin.getConfigsManager().getMainConfigManager().getConfig().getBoolean("memory_management.clean_offline_players", true)) {
            // 延迟5秒后清除，确保所有数据操作都已完成
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getPlayerDataManager().removePlayerByUUID(player.getUniqueId());
                if (plugin.getDebugManager().isDebugEnabled()) {
                    plugin.getLogger().info("[内存管理] 已清除离线玩家数据: " + player.getName());
                }
            }, 100L); // 5秒延迟 (100 ticks = 5 seconds)
        }
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event){
        LivingEntity e = event.getEntity();
        if(e.getKiller() != null){
            String customName = e.getCustomName() != null ? ChatColor.stripColor(e.getCustomName()) : null;
            plugin.getDiscoveryManager().onMobKill(e.getKiller(),e.getType().name(),customName);
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event){
        if(!(event.getEntity() instanceof Player)){
            return;
        }
        Player player = (Player) event.getEntity();
        plugin.getDiscoveryManager().onItemObtain(player,event.getItem().getItemStack());
        
        // 检查物品附魔
        if(Bukkit.getPluginManager().isPluginEnabled("Aiyatsbus") && canCheck(player)) {
            checkEnchantments(player, event.getItem().getItemStack());
        }
    }

    @EventHandler
    public void onFirstCommand(PlayerCommandPreprocessEvent event){
        Player player = event.getPlayer();
        String command = event.getMessage().startsWith("/") ? event.getMessage().substring(1).split(" ")[0] : event.getMessage().split(" ")[0];
        plugin.getDiscoveryManager().onCommandRun(player,command.toLowerCase());
    }
    
    /**
     * 监听物品合成事件，检查合成的物品是否有附魔
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCraftItem(CraftItemEvent event) {
        if (event.isCancelled() || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getRecipe().getResult();
        
        // 检查物品附魔
        if(Bukkit.getPluginManager().isPluginEnabled("Aiyatsbus") && canCheck(player)) {
            checkEnchantments(player, result);
        }
    }
    
    /**
     * 监听铁砧使用事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onAnvilUse(InventoryClickEvent event) {
        if (event.isCancelled() || !(event.getWhoClicked() instanceof Player) || 
            event.getClickedInventory() == null || 
            event.getClickedInventory().getType() != InventoryType.ANVIL) {
            return;
        }
        
        // 只处理结果槽的点击
        if (event.getRawSlot() != 2) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack result = event.getCurrentItem();
        
        if (result != null && canCheck(player)) {
            // 检查物品附魔
            if(Bukkit.getPluginManager().isPluginEnabled("Aiyatsbus")) {
                checkEnchantments(player, result);
            }
        }
    }
    
    /**
     * 监听物品切换事件，用于检查手持物品的附魔
     */
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        
        // 检查物品附魔
        if(Bukkit.getPluginManager().isPluginEnabled("Aiyatsbus") && item != null && canCheck(player)) {
            checkEnchantments(player, item);
        }
    }
    
    /**
     * 监听玩家从容器中获取物品事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        
        // 🐛 调试信息：PlayerListener事件检查
        plugin.getLogger().info("[DEBUG-PLAYER] PlayerListener收到点击事件 - 玩家: " + player.getName() + 
            ", 事件取消状态: " + event.isCancelled());
        
        // 如果事件取消或点击者不是玩家则忽略
        if (event.isCancelled() || !(event.getWhoClicked() instanceof Player)) {
            plugin.getLogger().info("[DEBUG-PLAYER] 事件已取消或不是玩家，跳过处理");
            return;
        }
        
        // 🔧 关键修复：排除Codex GUI界面，防止干扰GUI保护机制
        InventoryManager invManager = plugin.getInventoryManager();
        if (invManager.getInventoryPlayer(player) != null) {
            // 如果玩家正在查看Codex GUI，跳过附魔检测
            plugin.getLogger().info("[DEBUG-PLAYER] ⚠️ 检测到Codex GUI，跳过附魔处理以避免干扰");
            return;
        }
        
        plugin.getLogger().info("[DEBUG-PLAYER] 非Codex GUI，继续附魔检测逻辑");
        
        // 检查是否启用了容器检测功能
        if (plugin.getConfigsManager().getEnchantmentsConfigManager() == null || 
            !plugin.getConfigsManager().getEnchantmentsConfigManager().isDetectFromContainers()) {
            return;
        }
        
        // 只处理需要检测的情况
        if (!Bukkit.getPluginManager().isPluginEnabled("Aiyatsbus") || !canCheck(player)) {
            return;
        }
        
        // 检查是否是支持的容器类型
        if (event.getView().getTopInventory() != null && 
            !isAllowedContainer(event.getView().getTopInventory().getType().name())) {
            return;
        }
        
        // 从箱子取物品的情况
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
            event.getAction() == InventoryAction.PICKUP_ALL || 
            event.getAction() == InventoryAction.PICKUP_HALF || 
            event.getAction() == InventoryAction.PICKUP_ONE || 
            event.getAction() == InventoryAction.PICKUP_SOME) {
            
            // 检查是否是从容器中取物品
            if (event.getClickedInventory() != null && 
                event.getClickedInventory() != player.getInventory()) {
                
                ItemStack item = event.getCurrentItem();
                if (item != null) {
                    // 检查物品附魔
                    checkEnchantments(player, item);
                }
            }
        }
        
        // 检查物品交换（例如用Shift+点击移动物品）
        else if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
            ItemStack cursorItem = event.getCursor();
            if (cursorItem != null) {
                // 检查玩家手中的物品
                checkEnchantments(player, cursorItem);
            }
        }
        
        // 处理快捷栏与物品栏间的拖动
        else if (event.getAction() == InventoryAction.HOTBAR_SWAP || 
                 event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
            
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                if (hotbarItem != null) {
                    // 检查快捷栏物品
                    checkEnchantments(player, hotbarItem);
                }
            }
        }
    }
    
    /**
     * 检查容器类型是否在允许列表中
     * @param containerType 容器类型名称
     * @return 是否允许检测该容器
     */
    private boolean isAllowedContainer(String containerType) {
        if (plugin.getConfigsManager().getEnchantmentsConfigManager() == null) {
            return false;
        }
        
        List<String> allowedTypes = plugin.getConfigsManager().getEnchantmentsConfigManager().getContainerTypes();
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            return true; // 如果列表为空，默认允许所有容器
        }
        
        return allowedTypes.contains(containerType);
    }
    
    /**
     * 检查是否可以进行附魔检测（基于冷却时间）
     * @param player 玩家
     * @return 是否可以检测
     */
    private boolean canCheck(Player player) {
        long now = System.currentTimeMillis();
        long lastCheck = lastCheckTime.getOrDefault(player.getUniqueId(), 0L);
        int cooldown = 500; // 默认冷却时间为500毫秒
        
        if (plugin.getConfigsManager().getEnchantmentsConfigManager() != null) {
            cooldown = plugin.getConfigsManager().getEnchantmentsConfigManager().getCheckCooldown();
        }
        
        if(now - lastCheck > cooldown) {
            lastCheckTime.put(player.getUniqueId(), now);
            return true;
        }
        return false;
    }
    
    /**
     * 检查是否是可以附魔的物品类型
     * @param material 物品材质
     * @return 是否可以附魔
     */
    private boolean isEnchantableItem(Material material) {
        if (material == null) return false;
        String name = material.name();
        // 只处理可能有附魔的物品
        return name.contains("SWORD") || 
               name.contains("AXE") ||
               name.contains("BOW") ||
               name.contains("ARMOR") ||
               name.contains("HELMET") ||
               name.contains("CHESTPLATE") ||
               name.contains("LEGGINGS") ||
               name.contains("BOOTS") ||
               name.contains("TOOL") ||
               name.contains("FISHING_ROD") ||
               name.equals("ENCHANTED_BOOK") ||
               name.equals("TRIDENT") ||
               name.equals("CROSSBOW") ||
               name.equals("SHIELD") ||
               name.equals("ELYTRA") ||
               name.contains("PICKAXE") ||
               name.contains("SHOVEL") ||
               name.contains("HOE");
    }
    
    /**
     * 检查物品上的所有附魔，并触发发现
     * @param player 玩家
     * @param item 物品
     */
    private void checkEnchantments(final Player player, final ItemStack item) {
        // 优化的预检查
        if (item == null || !item.hasItemMeta()) return;
        
        // 检查是否是可能有附魔的物品
        if (!isEnchantableItem(item.getType())) return;
        
        // 检查物品是否有附魔或是附魔书
        boolean isEnchantedBook = item.getItemMeta() instanceof EnchantmentStorageMeta;
        if (!isEnchantedBook && item.getEnchantments().isEmpty()) return;
        
        // 在异步线程中处理
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<Enchantment, Integer> enchantments = new HashMap<>();
                
                // 如果是附魔书，获取存储的附魔
                if (isEnchantedBook) {
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                    enchantments.putAll(meta.getStoredEnchants());
                } 
                // 否则获取物品上的附魔
                else {
                    enchantments.putAll(item.getEnchantments());
                }
                
                // 如果没有附魔，直接返回
                if (enchantments.isEmpty()) return;
                
                // 检测到的新附魔列表
                List<String> newEnchants = new ArrayList<>();
                
                // 处理每个附魔
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    try {
                        Enchantment enchant = entry.getKey();
                        if (enchant == null || enchant.getKey() == null) continue;
                        
                        // 获取完整的附魔ID，包括命名空间
                        String enchantId = enchant.getKey().toString();
                        
                        // 检查玩家是否已经发现过这个附魔
                        if (!plugin.getPlayerDataManager().hasDiscoveredEnchantment(player, enchantId)) {
                            newEnchants.add(enchantId);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("处理单个附魔时出错: " + e.getMessage());
                    }
                }
                
                // 如果有新附魔，回到主线程处理
                if (!newEnchants.isEmpty()) {
                    List<String> finalNewEnchants = newEnchants;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (String enchantId : finalNewEnchants) {
                            plugin.getDiscoveryManager().onEnchantmentDiscover(player, enchantId);
                        }
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().warning("处理附魔检测时出错: " + e.getMessage());
                if (plugin.getDebugManager().isDebugEnabled()) {
                    e.printStackTrace();
                }
            }
        });
    }
}
