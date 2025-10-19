package com.magicbili.enchantdisassembler;

import org.bukkit.Bukkit;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.magicbili.enchantdisassembler.IntegrationAPI;

/**
 * 分解管理器，负责处理物品分解和玩家数据管理
 */
public class DisassembleManager {
    private final EnchantDisassembler plugin;
    private final IntegrationAPI integration;
    private final Map<UUID, Map<String, Integer>> playerPools = new HashMap<>();
    private final Map<UUID, Map<String, Integer>> overflowPools = new HashMap<>();
    private final Map<UUID, Map<Integer, ItemStack>> playerItems = new HashMap<>();

    public DisassembleManager(EnchantDisassembler plugin, IntegrationAPI integration) {
        this.plugin = plugin;
        this.integration = integration;
    }

    /**
     * 添加物品到分解池
     * @param player 玩家
     * @param item 物品
     * @return 是否成功添加
     */
    public boolean addItem(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-enchants"));
            return false;
        }

        // 根据物品类型选择附魔来源
        Map<Enchantment, Integer> enchantMap;
        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta meta) {
            enchantMap = meta.getStoredEnchants();
        } else {
            enchantMap = item.getItemMeta().getEnchants();
        }

        if (enchantMap.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-enchants"));
            return false;
        }

        // 检查是否包含诅咒附魔，若包含则拦截
        for (Map.Entry<Enchantment, Integer> entry : enchantMap.entrySet()) {
            String rarityCheck = plugin.getConfigManager().translateRarity(integration.getEnchantRarity(entry.getKey()));
            if ("curse".equalsIgnoreCase(rarityCheck)) {
                player.sendMessage(plugin.getConfigManager().getMessage("curse-enchant"));
                return false;
            }
        }

        UUID playerId = player.getUniqueId();
        Map<String, Integer> pools = playerPools.computeIfAbsent(playerId, k -> new HashMap<>());
        Map<String, Integer> overflow = overflowPools.computeIfAbsent(playerId, k -> new HashMap<>());
        Map<Integer, ItemStack> items = playerItems.computeIfAbsent(playerId, k -> new HashMap<>());

        // 按附魔稀有度分组累计点数 (点数 = 附魔等级)
        Map<String, Integer> addMap = new java.util.HashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : enchantMap.entrySet()) {
            String rarity = integration.getEnchantRarity(entry.getKey());
            rarity = plugin.getConfigManager().translateRarity(rarity);
            int level = entry.getValue();
            addMap.merge(rarity, level, Integer::sum);
        }

        // Debug: log each enchant and its determined rarity
        if (plugin.isDebug()) {
            enchantMap.forEach((ench, lvl) -> {
                String r = plugin.getConfigManager().translateRarity(integration.getEnchantRarity(ench));
                plugin.debug("附魔 " + ench.getKey() + " 等级 " + lvl + " -> 稀有度 " + r);
            });
        }

        for (Map.Entry<String, Integer> addEntry : addMap.entrySet()) {
            String rarity = addEntry.getKey();
            int pointsToAdd = addEntry.getValue();

            int maxPool = plugin.getConfigManager().getMaxPool(rarity);
            int currentPool = pools.getOrDefault(rarity, 0);

            if (currentPool >= maxPool) {
                overflow.put(rarity, overflow.getOrDefault(rarity, 0) + pointsToAdd);
            } else {
                int newTotal = currentPool + pointsToAdd;
                if (newTotal > maxPool) {
                    int overflowPts = newTotal - maxPool;
                    pools.put(rarity, maxPool);
                    overflow.put(rarity, overflow.getOrDefault(rarity, 0) + overflowPts);
                } else {
                    pools.put(rarity, newTotal);
                }
            }

            // 同步到数据库，跨服共享
            plugin.getDatabaseManager().addPoints(playerId, rarity, pointsToAdd);

            plugin.debug("为玩家 " + player.getName() + " 的 " + rarity + " 池添加 " + pointsToAdd + " 点 (当前 " + pools.getOrDefault(rarity,0) + "，溢出 " + overflow.getOrDefault(rarity,0) + ")");
        }

        // 保存物品
        int slot = getNextEmptySlot(items);
        items.put(slot, item.clone());

        plugin.debug("玩家 " + player.getName() + " 正在添加物品 " + item.getType() + "，包含 " + enchantMap.size() + " 个附魔");
        player.sendMessage(plugin.getConfigManager().getMessage("item-added"));
        return true;
    }

    /**
     * 销毁所有物品
     * @param player 玩家
     */
    public void destroyAllItems(Player player) {
        UUID playerId = player.getUniqueId();
        Map<Integer, ItemStack> items = playerItems.get(playerId);
        
        if (items == null || items.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-items"));
            return;
        }

        // 清空物品
        items.clear();
        player.sendMessage(plugin.getConfigManager().getMessage("destroy-success"));
    }

    /**
     * 返还所有物品
     * @param player 玩家
     */
    public void returnItems(Player player) {
        UUID playerId = player.getUniqueId();
        Map<Integer, ItemStack> items = playerItems.get(playerId);
        
        if (items == null || items.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-items"));
            return;
        }

        Map<String,Integer> pools = playerPools.getOrDefault(playerId, new HashMap<>());
        Map<String,Integer> overflow = overflowPools.getOrDefault(playerId, new HashMap<>());

        for (ItemStack item : items.values()) {
            if (item == null) continue;

            Map<Enchantment,Integer> enchMap;
            if(item.getItemMeta() instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta meta){
                enchMap = meta.getStoredEnchants();
            } else {
                enchMap = item.getItemMeta().getEnchants();
            }

            for(Map.Entry<Enchantment,Integer> ent: enchMap.entrySet()){
                String rarity = plugin.getConfigManager().translateRarity(integration.getEnchantRarity(ent.getKey()));
                int pts = ent.getValue();

                int poolVal = pools.getOrDefault(rarity,0);
                if(poolVal>=pts){
                    pools.put(rarity,poolVal-pts);
                    // 数据库扣除
                    plugin.getDatabaseManager().deductPoints(playerId, rarity, pts);
                } else {
                    pools.put(rarity,0);
                    int leftover = pts - poolVal;
                    int overVal = overflow.getOrDefault(rarity,0);
                    overflow.put(rarity, Math.max(0, overVal-leftover));
                    int deduct = Math.min(pts, poolVal + overVal);
                    if(deduct>0){
                        plugin.getDatabaseManager().deductPoints(playerId, rarity, deduct);
                    }
                }
            }

            // 返还物品给玩家
            player.getInventory().addItem(item);
        }

        items.clear();
        plugin.debug("玩家 " + player.getName() + " 返还物品，已扣除对应点数");
        player.sendMessage(plugin.getConfigManager().getMessage("return-success"));
    }

    /**
     * 领取稀有度奖励
     * @param player 玩家
     * @param rarity 稀有度
     */
    public void claimReward(Player player, String rarity) {
        UUID playerId = player.getUniqueId();
        Map<String, Integer> pools = playerPools.get(playerId);
        Map<String, Integer> overflow = overflowPools.get(playerId);

        int current = pools != null ? pools.getOrDefault(rarity, 0) : 0;
        // 双保险：以数据库中的值为准（避免跨服不一致）
        int dbCurrent = plugin.getDatabaseManager().getPoints(playerId, rarity);
        if(dbCurrent>current) current = dbCurrent;

        int maxPool = plugin.getConfigManager().getMaxPool(rarity);

        // 若未达到满池，则拒绝领取
        if (current < maxPool) {
            player.sendMessage(plugin.getConfigManager().getMessage("claim-failed"));
            return;
        }

        // 执行奖励命令或物品
        List<String> cmds = plugin.getConfig().getStringList("reward-commands." + rarity);
        if (!cmds.isEmpty()) {
            for (String raw : cmds) {
                String cmd = raw.replace("%player%", player.getName());
                // PlaceholderAPI 支持
                if (plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    try {
                        Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                        java.lang.reflect.Method method = clazz.getMethod("setPlaceholders", org.bukkit.entity.Player.class, String.class);
                        cmd = (String) method.invoke(null, player, cmd);
                    } catch (Exception ignored) {
                    }
                }
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
            }
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("reward-not-set"));
        }

        // 清空该稀有度的池子（内存 & 数据库）
        if(pools!=null) pools.put(rarity,0);
        if(overflow!=null) overflow.put(rarity,0);
        plugin.getDatabaseManager().resetPoints(playerId, rarity);

        player.sendMessage(plugin.getConfigManager().getMessage("claim-success"));

        // 领取后清空已提交的物品，防止返还
        Map<Integer, ItemStack> items = playerItems.get(playerId);
        if(items!=null){
            items.clear();
        }
    }

    /**
     * 加载玩家数据
     * @param player 玩家
     */
    public void loadPlayerData(Player player) {
        // 从数据库加载玩家点数到内存
        java.util.Map<String,Integer> dbPoints = plugin.getDatabaseManager().getAllPoints(player.getUniqueId());
        if(dbPoints.isEmpty()) return;
        UUID uid = player.getUniqueId();
        Map<String,Integer> pools = playerPools.computeIfAbsent(uid,k->new java.util.HashMap<>());
        pools.clear();
        pools.putAll(dbPoints);
    }

    /**
     * 保存玩家数据
     * @param player 玩家
     */
    public void savePlayerData(Player player) {
        // 数据实时写入，无需额外保存；仅做内存落库保障
        Map<String,Integer> pools = getPlayerPools(player.getUniqueId());
        if(pools!=null){
            for(java.util.Map.Entry<String,Integer> e : pools.entrySet()){
                plugin.getDatabaseManager().addPoints(player.getUniqueId(), e.getKey(), 0); // ensure row exists
            }
        }
    }

    /**
     * 保存所有玩家数据
     */
    public void saveAllPlayerData() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerData(player);
        }
    }

    /**
     * 获取玩家池子数据
     * @param playerId 玩家ID
     * @return 池子数据
     */
    public Map<String, Integer> getPlayerPools(UUID playerId) {
        return playerPools.getOrDefault(playerId, new HashMap<>());
    }

    /**
     * 获取玩家溢出池数据
     * @param playerId 玩家ID
     * @return 溢出池数据
     */
    public Map<String, Integer> getOverflowPools(UUID playerId) {
        return overflowPools.getOrDefault(playerId, new HashMap<>());
    }

    /**
     * 分解附魔，计算点数 - 使用AiyatsbusIntegration
     * @param enchant 附魔
     * @param level   等级
     * @return 分解点数
     */
    public double processEnchantment(Enchantment enchant, int level) {
        // 点数 = 附魔等级
        return level;
    }

    /**
     * 获取物品的稀有度 - 使用AiyatsbusIntegration
     * @param item 物品
     * @return 稀有度
     */
    @SuppressWarnings("unused")
    private String getEnchantRarity(ItemStack item) {
        return "common";
    }

    /**
     * 获取下一个空槽位
     * @param items 物品映射
     * @return 槽位索引
     */
    private int getNextEmptySlot(Map<Integer, ItemStack> items) {
        int slot = 0;
        while (items.containsKey(slot)) {
            slot++;
        }
        return slot;
    }

    /**
     * 获取Aiyatsbus集成实例
     * @return Aiyatsbus集成实例
     */
    public IntegrationAPI getIntegrationAPI() {
        return integration;
    }
}
