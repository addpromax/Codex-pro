package cx.ajneb97.managers;

import cx.ajneb97.Codex;
import cx.ajneb97.model.data.PlayerData;
import cx.ajneb97.model.data.PlayerDataCategory;
import cx.ajneb97.model.data.PlayerDataDiscovery;
import cx.ajneb97.model.structure.Category;
import cx.ajneb97.utils.OtherUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PlayerDataManager {

    private Codex plugin;
    private Map<UUID, PlayerData> players;
    private Map<String,UUID> playerNames;
    private final Map<UUID, Set<String>> discoveredEnchantments = new ConcurrentHashMap<>();
    
    // 实时保存相关
    private final ScheduledExecutorService realtimeSaveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Codex-RealtimeSave");
        t.setDaemon(true);
        return t;
    });
    private final Map<UUID, ScheduledFuture<?>> pendingSaves = new ConcurrentHashMap<>();

    public PlayerDataManager(Codex plugin){
        this.plugin = plugin;
        this.players = new HashMap<>();
        this.playerNames = new HashMap<>();
    }

    public Map<UUID,PlayerData> getPlayers() {
        return players;
    }

    public void setPlayers(Map<UUID,PlayerData> players) {
        this.players = players;
        for(Map.Entry<UUID, PlayerData> entry : players.entrySet()){
            playerNames.put(entry.getValue().getName(),entry.getKey());
        }
    }

    public void addPlayer(PlayerData p){
        players.put(p.getUuid(),p);
        playerNames.put(p.getName(), p.getUuid());
    }

    public PlayerData getPlayer(Player player, boolean create){
        PlayerData playerData = players.get(player.getUniqueId());
        if(playerData == null && create){
            playerData = new PlayerData(player.getUniqueId(),player.getName());
            addPlayer(playerData);
            
            // 确保新创建的玩家数据会被保存到数据库
            plugin.getDatabaseManager().createPlayer(playerData);
            
            if (plugin.getConfigsManager().getMainConfigManager().isShowSaveNotifications()) {
                plugin.getLogger().info("[玩家数据] 已创建新玩家数据: " + player.getName());
            }
        }
        return playerData;
    }

    private void updatePlayerName(String oldName,String newName,UUID uuid){
        if(oldName != null){
            playerNames.remove(oldName);
        }
        playerNames.put(newName,uuid);
    }

    public PlayerData getPlayerByUUID(UUID uuid){
        return players.get(uuid);
    }

    private UUID getPlayerUUID(String name){
        return playerNames.get(name);
    }

    public PlayerData getPlayerByName(String name){
        UUID uuid = getPlayerUUID(name);
        return players.get(uuid);
    }

    public void removePlayerByUUID(UUID uuid){
        players.remove(uuid);
    }

    public void setJoinPlayerData(Player player){
        // 使用新的数据库管理器
        UUID uuid = player.getUniqueId();
        
        plugin.getDatabaseManager().getPlayer(uuid.toString(), playerData -> {
            removePlayerByUUID(uuid); // 移除已存在的数据
            if (playerData != null) {
                addPlayer(playerData);
                
                // 更新玩家名称如果不同
                if (!playerData.getName().equals(player.getName())) {
                    updatePlayerName(playerData.getName(), player.getName(), player.getUniqueId());
                    playerData.setName(player.getName());
                    playerData.setModified(true);
                    plugin.getDatabaseManager().updatePlayerName(playerData);
                }
                
                plugin.getLogger().info("[玩家数据] 已加载玩家数据: " + player.getName());
            } else {
                // 创建新玩家数据
                playerData = new PlayerData(uuid, player.getName());
                addPlayer(playerData);
                
                // 使用数据库管理器保存新玩家
                plugin.getDatabaseManager().createPlayer(playerData);
                playerData.setModified(true);
                
                plugin.getLogger().info("[玩家数据] 已创建新玩家数据: " + player.getName());
            }
        });
    }

    public void addDiscovery(Player player, String categoryName, String discoveryName){
        String normalizedDiscoveryName = toDiscoveryId(discoveryName);
        
        // 先检查是否已经解锁，避免重复处理
        if(hasDiscovery(player, categoryName, normalizedDiscoveryName)) {
            if(plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG][addDiscovery] 玩家 " + player.getName() + " 已经解锁 " + normalizedDiscoveryName + "，跳过添加");
            }
            return;
        }
        
        if(plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG][addDiscovery] 玩家=" + player.getName() + ", 分类=" + categoryName + ", discoveryName=" + discoveryName + ", normalized=" + normalizedDiscoveryName);
        }
        
        PlayerData playerData = getPlayer(player, true); // 确保数据一定存在
        if(playerData == null){
            plugin.getLogger().warning("[ERROR][addDiscovery] 无法为玩家 " + player.getName() + " 创建/获取数据！");
            return;
        }

        PlayerDataCategory playerDataCategory = null;
        for(PlayerDataCategory category : playerData.getCategories()){
            if(category.getName().equals(categoryName)){
                playerDataCategory = category;
                break;
            }
        }

        if(playerDataCategory == null){
            ArrayList<PlayerDataDiscovery> discoveries = new ArrayList<>();
            playerDataCategory = new PlayerDataCategory(categoryName, false, discoveries);
            playerData.getCategories().add(playerDataCategory);
            if(plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG][addDiscovery] 新建分类: " + categoryName);
            }
        }

        // DEBUG: 输出当前已解锁的发现项
        if(plugin.getDebugManager().isDebugEnabled()) {
            List<String> unlocked = new ArrayList<>();
            for(PlayerDataDiscovery d : playerDataCategory.getDiscoveries()) {
                unlocked.add(d.getDiscoveryName());
            }
            plugin.getLogger().info("[DEBUG][addDiscovery] 玩家 " + player.getName() + " 分类 " + categoryName + " 已解锁: " + unlocked);
        }

        String date = OtherUtils.getDate(plugin.getConfigsManager().getMainConfigManager().getDiscoveriesDateFormat());
        PlayerDataDiscovery playerDataDiscovery = new PlayerDataDiscovery(normalizedDiscoveryName, date, System.currentTimeMillis());
        playerDataCategory.getDiscoveries().add(playerDataDiscovery);
        playerData.setModified(true); // 标记数据已修改，确保被保存
        
        // 如果启用实时保存，调度保存任务
        scheduleRealtimeSave(player);
        
        // 如果是附魔发现，添加到缓存
        if(plugin.getConfigsManager().getEnchantmentsConfigManager() != null && 
           categoryName.equals(plugin.getConfigsManager().getEnchantmentsConfigManager().getCategoryName())) {
            String enchantmentId = toEnchantmentId(normalizedDiscoveryName);
            if(plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG][addDiscovery] 调用 addDiscoveredEnchantment, enchantmentId=" + enchantmentId);
            }
            addDiscoveredEnchantment(player, enchantmentId);
        }
    }

    public boolean hasDiscovery(Player player,String categoryName,String discoveryName){
        String normalizedDiscoveryName = toDiscoveryId(discoveryName);
        
        // 快速路径：如果是附魔分类，先检查内存缓存
        if(plugin.getConfigsManager().getEnchantmentsConfigManager() != null && 
           categoryName.equals(plugin.getConfigsManager().getEnchantmentsConfigManager().getCategoryName())) {
            String enchantmentId = toEnchantmentId(normalizedDiscoveryName);
            UUID playerUUID = player.getUniqueId();
            if(discoveredEnchantments.containsKey(playerUUID) && 
               discoveredEnchantments.get(playerUUID).contains(enchantmentId)) {
                if(plugin.getDebugManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG][hasDiscovery] 玩家=" + player.getName() + 
                                           " 从缓存中找到附魔: " + enchantmentId);
                }
                return true;
            }
        }
        
        // 如果不在缓存中，检查玩家数据
        PlayerData playerData = getPlayer(player,false);
        if(plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG][hasDiscovery] 玩家=" + player.getName() + ", 分类=" + categoryName + ", discoveryName=" + discoveryName + ", normalized=" + normalizedDiscoveryName);
        }
        if(playerData == null){
            if(plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG][hasDiscovery] 玩家数据不存在: " + player.getName());
            }
            return false;
        }
        PlayerDataCategory playerDataCategory = playerData.getCategory(categoryName);
        if(plugin.getDebugManager().isDebugEnabled() && playerDataCategory != null) {
            List<String> unlocked = new ArrayList<>();
            for(PlayerDataDiscovery d : playerDataCategory.getDiscoveries()) {
                unlocked.add(d.getDiscoveryName());
            }
            plugin.getLogger().info("[DEBUG][hasDiscovery] 玩家 " + player.getName() + " 分类 " + categoryName + " 已解锁: " + unlocked + "，查询: " + normalizedDiscoveryName);
        }
        boolean result = playerData.hasDiscovery(categoryName,normalizedDiscoveryName);
        if(plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG][hasDiscovery] 返回结果: " + result);
        }
        return result;
    }

    public PlayerDataDiscovery getDiscovery(Player player,String categoryName,String discoveryName){
        String normalizedDiscoveryName = toDiscoveryId(discoveryName);
        PlayerData playerData = getPlayer(player,false);
        if(plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG][getDiscovery] 玩家=" + player.getName() + ", 分类=" + categoryName + ", discoveryName=" + discoveryName + ", normalized=" + normalizedDiscoveryName);
        }
        if(playerData == null){
            if(plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG][getDiscovery] 玩家数据不存在: " + player.getName());
            }
            return null;
        }
        return playerData.getDiscovery(categoryName,normalizedDiscoveryName);
    }

    public int getTotalDiscoveries(Player player,String categoryName){
        PlayerData playerData = getPlayer(player,false);
        if(playerData == null){
            return 0;
        }
        return playerData.getTotalDiscoveries(categoryName);
    }

    public int getTotalDiscoveries(Player player){
        PlayerData playerData = getPlayer(player,false);
        if(playerData == null){
            return 0;
        }
        return playerData.getTotalDiscoveries();
    }

    public int getTotalDiscoveriesPercentage(Player player,String categoryName,int max){
        int totalDiscoveries = getTotalDiscoveries(player,categoryName);
        return OtherUtils.getPercentage(totalDiscoveries,max);
    }

    public int getTotalDiscoveriesPercentage(Player player,int max){
        int totalDiscoveries = getTotalDiscoveries(player);
        return OtherUtils.getPercentage(totalDiscoveries,max);
    }

    public void completeCategory(Player player,String categoryName){
        PlayerData playerData = getPlayer(player, true); // 确保数据一定存在
        if(playerData == null){
            plugin.getLogger().warning("[ERROR][completeCategory] 无法为玩家 " + player.getName() + " 创建/获取数据！");
            return;
        }
        playerData.completeCategory(categoryName);
        playerData.setModified(true); // 标记数据已修改，确保被保存
        
        // 如果启用实时保存，调度保存任务
        scheduleRealtimeSave(player);

        plugin.getDatabaseManager().updateCompletedCategories(player.getUniqueId().toString(),categoryName);
    }

    public boolean hasAllDiscoveries(Player player,String categoryName,int totalDiscoveries){
        PlayerData playerData = getPlayer(player,false);
        if(playerData == null){
            return false;
        }

        return playerData.getTotalDiscoveries(categoryName) >= totalDiscoveries;
    }

    public List<PlayerDataCategory> getCategories(Player player){
        PlayerData playerData = getPlayer(player,false);
        if(playerData == null){
            return new ArrayList<>();
        }

        return playerData.getCategories();
    }

    public boolean hasCompletedCategory(Player player,String categoryName){
        PlayerData playerData = getPlayer(player,false);
        if(playerData == null){
            return false;
        }
        return playerData.isCategoryCompleted(categoryName);
    }

    public boolean hasCompletedCategory(String playerName,String categoryName){
        PlayerData playerData = getPlayerByName(playerName);
        if(playerData == null){
            return false;
        }
        return playerData.isCategoryCompleted(categoryName);
    }

    public void setMillisActionsExecuted(Player player,String categoryName,String discoveryName){
        PlayerData playerData = getPlayer(player,true);
        playerData.setModified(true);
        playerData.setMillisActionsExecuted(categoryName,discoveryName);

        plugin.getDatabaseManager().updateMillisActionsExecuted(player.getUniqueId().toString(),categoryName,discoveryName);
    }

    public long getMillisActionsExecuted(Player player,String categoryName,String discoveryName){
        PlayerData playerData = getPlayer(player,false);
        if(playerData == null){
            return 0;
        }

        return playerData.getMillisActionsExecuted(categoryName,discoveryName);
    }

    public String resetDataPlayer(String playerName, String categoryName, String discoveryName, FileConfiguration messagesConfig){
        PlayerData playerData = null;
        if(!playerName.equals("*")){
            playerData = getPlayerByName(playerName);
            if(playerData == null){
                return messagesConfig.getString("playerDoesNotExist");
            }
        }

        // 验证分类和发现项是否存在
        if(categoryName != null){
            CategoryManager categoryManager = plugin.getCategoryManager();
            Category category = categoryManager.getCategory(categoryName);
            if(category == null){
                return messagesConfig.getString("categoryDoesNotExist");
            }
            if(discoveryName != null && category.getDiscovery(discoveryName) == null){
                return messagesConfig.getString("discoveryDoesNotExist").replace("%category%",categoryName);
            }
        }

        if(categoryName == null && discoveryName == null){
            if(playerName.equals("*")){
                // Reset all data for all players
                for(Map.Entry<UUID, PlayerData> entry : players.entrySet()){
                    entry.getValue().resetData();
                    entry.getValue().setModified(true); // 标记数据已修改
                }
                // 使用数据库管理器重置所有玩家数据
                plugin.getDatabaseManager().resetDataPlayer("*", null, null);
                return messagesConfig.getString("commandResetAllForAllPlayers");
            }else{
                // Reset all data for player
                playerData.resetData();
                playerData.setModified(true); // 标记数据已修改
                // 使用数据库管理器重置特定玩家数据
                plugin.getDatabaseManager().resetDataPlayer(playerData.getUuid().toString(), null, null);
                return messagesConfig.getString("commandResetAllForPlayer").replace("%player%", playerName);
            }
        }

        if(discoveryName == null){
            if(playerName.equals("*")){
                // Reset category for all players
                for(Map.Entry<UUID, PlayerData> entry : players.entrySet()){
                    entry.getValue().resetCategory(categoryName);
                    entry.getValue().setModified(true); // 标记数据已修改
                }
                // 使用数据库管理器重置所有玩家的特定分类
                plugin.getDatabaseManager().resetDataPlayer("*", categoryName, null);
                return messagesConfig.getString("commandResetCategoryForAllPlayers").replace("%category%", categoryName);
            }else{
                // Reset category for player
                playerData.resetCategory(categoryName);
                playerData.setModified(true); // 标记数据已修改
                // 使用数据库管理器重置特定玩家的特定分类
                plugin.getDatabaseManager().resetDataPlayer(playerData.getUuid().toString(), categoryName, null);
                return messagesConfig.getString("commandResetCategoryForPlayer").replace("%player%", playerName)
                        .replace("%category%", categoryName);
            }
        }else{
            if(playerName.equals("*")){
                // Reset discovery for all players
                for(Map.Entry<UUID, PlayerData> entry : players.entrySet()){
                    entry.getValue().resetDiscovery(categoryName, discoveryName);
                    entry.getValue().setModified(true); // 标记数据已修改
                }
                // 使用数据库管理器重置所有玩家的特定发现项
                plugin.getDatabaseManager().resetDataPlayer("*", categoryName, discoveryName);
                return messagesConfig.getString("commandResetDiscoveryForAllPlayers").replace("%category%", categoryName)
                        .replace("%discovery%", discoveryName);
            }else{
                // Reset discovery for player
                playerData.resetDiscovery(categoryName, discoveryName);
                playerData.setModified(true); // 标记数据已修改
                // 使用数据库管理器重置特定玩家的特定发现项
                plugin.getDatabaseManager().resetDataPlayer(playerData.getUuid().toString(), categoryName, discoveryName);
                return messagesConfig.getString("commandResetDiscoveryForPlayer").replace("%player%", playerName)
                        .replace("%category%", categoryName).replace("%discovery%", discoveryName);
            }
        }
    }

    /**
     * 检查玩家是否已发现指定附魔
     * @param player 玩家
     * @param enchantmentId 附魔ID
     * @return 是否已发现
     */
    public boolean hasDiscoveredEnchantment(Player player, String enchantmentId) {
        if(plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG][hasDiscoveredEnchantment] 玩家=" + player.getName() + ", enchantmentId=" + enchantmentId);
        }
        // 使用原始ID格式，不再转换
        String normalizedId = enchantmentId;
        // 先检查内存缓存
        if(discoveredEnchantments.containsKey(player.getUniqueId())) {
            if(discoveredEnchantments.get(player.getUniqueId()).contains(normalizedId)) {
                if(plugin.getDebugManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] 缓存判定 hasDiscoveredEnchantment(" + player.getName() + ", " + normalizedId + ") = true");
                }
                return true;
            }
        }
        
        // 检查玩家数据
        String categoryName = plugin.getConfigsManager().getEnchantmentsConfigManager().getCategoryName();
        boolean hasDiscovery = hasDiscovery(player, categoryName, normalizedId);
        if(plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] 数据判定 hasDiscoveredEnchantment(" + player.getName() + ", " + normalizedId + ") = " + hasDiscovery);
        }
        // 如果找到了，添加到缓存
        if(hasDiscovery) {
            addDiscoveredEnchantment(player, normalizedId);
        }
        
        return hasDiscovery;
    }
    
    /**
     * 将附魔添加到已发现缓存
     * @param player 玩家
     * @param enchantmentId 附魔ID
     */
    public void addDiscoveredEnchantment(Player player, String enchantmentId) {
        if(plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG][addDiscoveredEnchantment] 玩家=" + player.getName() + ", enchantmentId=" + enchantmentId);
        }
        PlayerData playerData = getPlayer(player, true); // 确保数据一定存在
        if(playerData == null){
            plugin.getLogger().warning("[ERROR][addDiscoveredEnchantment] 无法为玩家 " + player.getName() + " 创建/获取数据！");
            return;
        }
        // 使用原始ID格式，不再转换
        String normalizedId = enchantmentId;
        discoveredEnchantments.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(normalizedId);
        playerData.setModified(true); // 标记数据已修改，确保被保存
        
        // 如果启用实时保存，调度保存任务
        scheduleRealtimeSave(player);
        
        if(plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] 已加入缓存: " + normalizedId);
        }
    }
    
    /**
     * 清除玩家的附魔缓存
     * @param player 玩家
     */
    public void clearEnchantmentCache(Player player) {
        discoveredEnchantments.remove(player.getUniqueId());
    }
    
    /**
     * 清除所有玩家的附魔缓存
     */
    public void clearAllEnchantmentCaches() {
        discoveredEnchantments.clear();
    }

    /**
     * 标记玩家数据为已修改状态，确保下次保存时被处理
     * @param uuid 玩家UUID
     */
    public void setPlayerModified(UUID uuid) {
        PlayerData playerData = getPlayerByUUID(uuid);
        if (playerData != null) {
            playerData.setModified(true);
        }
    }

    // 工具方法：附魔ID格式转换 - 现在保持原始格式，不进行转换
    public static String toDiscoveryId(String enchantmentId) {
        if(enchantmentId == null) return null;
        return enchantmentId; // 保持原始格式，不再替换冒号为下划线
    }
    public static String toEnchantmentId(String discoveryId) {
        if(discoveryId == null) return null;
        return discoveryId; // 保持原始格式，下划线格式的旧数据将在迁移时处理
    }
    
    /**
     * 保存所有已标记为修改的玩家数据
     * 在服务器关闭时调用，确保所有数据都被保存
     * @return 保存的数据数量
     */
    public int saveAllModifiedData() {
        return saveAllModifiedData(false);
    }
    
    /**
     * 保存所有已标记为修改的玩家数据
     * @param showDetailedInfo 是否显示详细的诊断信息
     * @return 保存的数据数量
     */
    public int saveAllModifiedData(boolean showDetailedInfo) {
        int savedCount = 0;
        int errorCount = 0;
        int totalPlayerCount = players.size();
        int unmodifiedCount = 0;
        
        // 记录开始保存时间
        long startTime = System.currentTimeMillis();
        
        try {
            if (showDetailedInfo) {
                plugin.getLogger().info("开始保存玩家数据 [总玩家数: " + totalPlayerCount + "]");
                
                // 显示所有玩家的修改状态
                for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
                    PlayerData playerData = entry.getValue();
                    if (playerData.isModified()) {
                        plugin.getLogger().info("  - " + playerData.getName() + ": 需要保存 (已修改)");
                    } else {
                        unmodifiedCount++;
                        if (plugin.getDebugManager().isDebugEnabled()) {
                            plugin.getLogger().info("  - " + playerData.getName() + ": 无需保存 (未修改)");
                        }
                    }
                }
                
                if (unmodifiedCount > 0) {
                    plugin.getLogger().info("共有 " + unmodifiedCount + " 个玩家数据无需保存（未修改或已保存）");
                }
            } else {
                plugin.getLogger().info("开始保存所有已修改的玩家数据...");
            }
            
            // 使用同步方式保存所有已修改的数据，确保在服务器关闭前完成
            for (Map.Entry<UUID, PlayerData> entry : players.entrySet()) {
                PlayerData playerData = entry.getValue();
                if (playerData.isModified()) {
                    try {
                        // 重要：在保存前再次检查数据的有效性
                        if (playerData.getUuid() == null || playerData.getName() == null || playerData.getName().isEmpty()) {
                            plugin.getLogger().warning("跳过保存无效的玩家数据: UUID=" + playerData.getUuid() + ", Name=" + playerData.getName());
                            errorCount++;
                            continue;
                        }
                        
                        // 使用数据库管理器保存数据
                        plugin.getDatabaseManager().updatePlayer(playerData);
                        playerData.setModified(false); // 重置修改标记
                        savedCount++;
                        
                        if (plugin.getConfigsManager().getMainConfigManager().isShowSaveNotifications()) {
                            plugin.getLogger().info("已保存玩家数据: " + playerData.getName());
                        }
                    } catch (Exception e) {
                        errorCount++;
                        plugin.getLogger().warning("保存玩家 " + playerData.getName() + " 数据时出错: " + e.getMessage());
                        e.printStackTrace();
                        
                        // 保存失败时不清除修改标记，以便后续重试或紧急保存
                        // playerData.setModified(true); // 保持修改标记
                    }
                }
            }
            
            long timeTaken = System.currentTimeMillis() - startTime;
            String resultMessage = "已保存 " + savedCount + " 个已修改的玩家数据 (耗时: " + timeTaken + "ms)";
            
            if (totalPlayerCount > 0) {
                resultMessage += " [总玩家: " + totalPlayerCount + ", 未修改: " + unmodifiedCount + "]";
            }
            
            if (errorCount > 0) {
                resultMessage += ", 保存失败: " + errorCount;
            }
            
            plugin.getLogger().info(resultMessage);
            
            // 如果没有保存任何数据但有玩家在线，给出说明
            if (savedCount == 0 && totalPlayerCount > 0) {
                plugin.getLogger().info("提示: 所有在线玩家数据都是最新的，无需保存。这通常意味着定时保存任务工作正常。");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("保存玩家数据过程中发生严重错误: " + e.getMessage());
            e.printStackTrace();
        }
        
        return savedCount;
    }
    
    /**
     * 调度玩家的实时保存任务（防抖动机制）
     * 短时间内的多次修改会被合并，最后一次修改后等待指定时间才保存
     * @param player 玩家
     */
    private void scheduleRealtimeSave(Player player) {
        // 只有在启用实时保存模式时才执行
        if (!plugin.getConfigsManager().getMainConfigManager().isRealtimeSaveMode()) {
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        // 取消之前的保存任务（如果存在）
        ScheduledFuture<?> existingTask = pendingSaves.get(playerUuid);
        if (existingTask != null && !existingTask.isDone()) {
            existingTask.cancel(false);
            if (plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG][实时保存] 取消玩家 " + player.getName() + " 的之前保存任务");
            }
        }
        
        // 调度新的保存任务
        int debounceDelaySeconds = plugin.getConfigsManager().getMainConfigManager().getRealtimeDebounceDelay();
        ScheduledFuture<?> saveTask = realtimeSaveExecutor.schedule(() -> {
            savePlayerDataRealtime(player);
            pendingSaves.remove(playerUuid);
        }, debounceDelaySeconds, TimeUnit.SECONDS);
        
        pendingSaves.put(playerUuid, saveTask);
        
        if (plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG][实时保存] 为玩家 " + player.getName() + " 调度保存任务，延迟: " + debounceDelaySeconds + "秒");
        }
    }
    
    /**
     * 实时保存单个玩家数据
     * @param player 玩家
     */
    private void savePlayerDataRealtime(Player player) {
        try {
            PlayerData playerData = getPlayer(player, false);
            if (playerData == null || !playerData.isModified()) {
                return;
            }
            
            // 验证数据有效性
            if (playerData.getUuid() == null || playerData.getName() == null || playerData.getName().isEmpty()) {
                plugin.getLogger().warning("[实时保存] 跳过保存无效的玩家数据: UUID=" + playerData.getUuid() + ", Name=" + playerData.getName());
                return;
            }
            
            if (plugin.getConfigsManager().getMainConfigManager().isRealtimeShowNotifications()) {
                plugin.getLogger().info("[实时保存] 保存玩家数据: " + player.getName());
            }
            
            // 保存数据
            plugin.getDatabaseManager().updatePlayer(playerData);
            playerData.setModified(false);
            
            if (plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG][实时保存] 成功保存玩家 " + player.getName() + " 的数据");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("[实时保存] 保存玩家 " + player.getName() + " 数据时出错: " + e.getMessage());
            if (plugin.getDebugManager().isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 关闭实时保存执行器
     */
    public void shutdown() {
        if (realtimeSaveExecutor != null && !realtimeSaveExecutor.isShutdown()) {
            // 取消所有待处理的保存任务
            for (ScheduledFuture<?> task : pendingSaves.values()) {
                task.cancel(false);
            }
            pendingSaves.clear();
            
            // 关闭执行器
            realtimeSaveExecutor.shutdown();
            try {
                if (!realtimeSaveExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    realtimeSaveExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                realtimeSaveExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
