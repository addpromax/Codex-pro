package cx.ajneb97.managers;

import cx.ajneb97.Codex;
import cx.ajneb97.model.data.PlayerDataCategory;
import cx.ajneb97.model.data.PlayerDataDiscovery;
import cx.ajneb97.model.enchantment.EnchantmentInfo;
import cx.ajneb97.model.internal.CommonVariable;
import cx.ajneb97.model.structure.Category;
import cx.ajneb97.model.structure.DiscoveredOn;
import cx.ajneb97.model.structure.Discovery;
import cx.ajneb97.utils.ActionUtils;
import cx.ajneb97.utils.AiyatsbusUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import java.util.concurrent.ConcurrentHashMap;

import java.util.*;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import cx.ajneb97.managers.MessagesManager;

public class DiscoveryManager {
    private Codex plugin;
    private final Map<DiscoveredOn.DiscoveredOnType,List<Discovery>> discoveryCache = new ConcurrentHashMap<>();
    public static final String CRAFT_ENGINE_ID_KEY = "craftengine:id";
    private final boolean itemsAdderPresent = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
    private final boolean mmoItemsPresent = Bukkit.getPluginManager().getPlugin("MMOItems") != null;
    private final boolean aiyatsbusPresent = Bukkit.getPluginManager().getPlugin("Aiyatsbus") != null;

    public DiscoveryManager(Codex plugin){
        this.plugin = plugin;
        rebuildCache();
    }

    /**
     * 重建触发器 -> discoveries 缓存，提升查询效率
     */
    public void rebuildCache(){
        discoveryCache.clear();
        ArrayList<Category> categories = plugin.getCategoryManager().getCategories();
        for(Category category : categories){
            for(Discovery discovery : category.getDiscoveries()){
                DiscoveredOn discoveredOn = discovery.getDiscoveredOn();
                if(discoveredOn == null){
                    continue;
                }
                discoveryCache.computeIfAbsent(discoveredOn.getType(),k->new ArrayList<>()).add(discovery);
            }
        }
    }

    private List<Discovery> getPossibleDiscoveries(DiscoveredOn.DiscoveredOnType type){
        return discoveryCache.getOrDefault(type, Collections.emptyList());
    }

    private ArrayList<Discovery> getNotFoundDiscoveries(ArrayList<PlayerDataCategory> foundDiscoveries){
        ArrayList<Discovery> notFoundDiscoveries = new ArrayList<>();
        ArrayList<Category> categories = plugin.getCategoryManager().getCategories();

        Set<String> foundSet = new HashSet<>();
        for (PlayerDataCategory c : foundDiscoveries) {
            for (PlayerDataDiscovery d : c.getDiscoveries()) {
                foundSet.add(c.getName() + ":" + d.getDiscoveryName());
            }
        }

        for (Category category : categories) {
            String categoryName = category.getName();
            for (Discovery discovery : category.getDiscoveries()) {
                String key = categoryName + ":" + discovery.getId();
                if (!foundSet.contains(key)) {
                    notFoundDiscoveries.add(discovery);
                }
            }
        }

        return notFoundDiscoveries;
    }

    public void onMobKill(Player player, String mobType, String mobName){
        List<Discovery> discoveries = getPossibleDiscoveries(DiscoveredOn.DiscoveredOnType.MOB_KILL);
        for(Discovery discovery : discoveries){
            DiscoveredOn discoveredOn = discovery.getDiscoveredOn();
            String discoveryMobName = discoveredOn.getMobName();
            String discoveryMobType = discoveredOn.getMobType();
            if(discoveryMobType != null && !discoveryMobType.equals(mobType)){
                continue;
            }
            if(discoveryMobName != null && !discoveryMobName.equals(mobName)){
                continue;
            }

            onDiscover(player,discovery.getCategoryName(),discovery.getId());

            return;
        }
    }

    public void onMythicMobKill(Player player, String mythicMobType){
        List<Discovery> discoveries = getPossibleDiscoveries(DiscoveredOn.DiscoveredOnType.MYTHIC_MOB_KILL);
        onPluginMobKill(player,mythicMobType,discoveries);
    }

    public void onEliteMobKill(Player player, String eliteMobType){
        List<Discovery> discoveries = getPossibleDiscoveries(DiscoveredOn.DiscoveredOnType.ELITE_MOB_KILL);
        onPluginMobKill(player,eliteMobType.replace(".yml",""),discoveries);
    }

    private void onPluginMobKill(Player player, String mobType, List<Discovery> discoveries){
        for(Discovery discovery : discoveries){
            DiscoveredOn discoveredOn = discovery.getDiscoveredOn();
            String discoveryMobType = discoveredOn.getMobType();
            if(discoveryMobType != null){
                String[] sep = discoveryMobType.split(";");
                if(Arrays.stream(sep).noneMatch(mobType::equals)){
                    continue;
                }
            }

            onDiscover(player,discovery.getCategoryName(),discovery.getId());

            return;
        }
    }

    public void onWorldGuardRegionEnter(Player player, String regionName){
        List<Discovery> discoveries = getPossibleDiscoveries(DiscoveredOn.DiscoveredOnType.WORLDGUARD_REGION);
        for(Discovery discovery : discoveries){
            DiscoveredOn discoveredOn = discovery.getDiscoveredOn();
            String discoveryRegionName = discoveredOn.getRegionName();
            if(discoveryRegionName != null && !discoveryRegionName.equals(regionName)){
                continue;
            }

            onDiscover(player,discovery.getCategoryName(),discovery.getId());

            return;
        }
    }

    // New trigger: item obtain
    public void onItemObtain(Player player, ItemStack item){
        if(item == null){
            return;
        }
        String itemType = item.getType().name();
        ItemMeta meta = item.getItemMeta();

        List<Discovery> discoveries = getPossibleDiscoveries(DiscoveredOn.DiscoveredOnType.ITEM_OBTAIN);
        for(Discovery discovery : discoveries){
            DiscoveredOn discoveredOn = discovery.getDiscoveredOn();

            // Check item type
            String discoveryItemType = discoveredOn.getItemType();
            if(discoveryItemType != null){
                String[] sep = discoveryItemType.split(";");
                if(Arrays.stream(sep).noneMatch(itemType::equals)){
                    continue;
                }
            }

            // Check custom model data
            Integer cmd = discoveredOn.getCustomModelData();
            if(cmd != null){
                if(meta == null || !meta.hasCustomModelData() || meta.getCustomModelData() != cmd){
                    continue;
                }
            }

            // Check components / persistent data
            String components = discoveredOn.getComponents();
            if(components != null && !components.isEmpty()){
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                boolean allMatch = true;
                String[] keys = components.split(";");
                for(String keyStr : keys){
                    String trimmed = keyStr.trim();
                    if(trimmed.isEmpty()) continue;
                    NamespacedKey key = NamespacedKey.fromString(trimmed);
                    if(key == null || !pdc.has(key, PersistentDataType.STRING)){
                        allMatch = false;
                        break;
                    }
                }
                if(!allMatch){
                    continue;
                }
            }

            // Check CraftEngine ID
            String craftId = discoveredOn.getCraftEngineId();
            if(craftId != null){
                String itemCraftId;
                if(meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(plugin, CRAFT_ENGINE_ID_KEY), PersistentDataType.STRING)){
                    itemCraftId = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, CRAFT_ENGINE_ID_KEY), PersistentDataType.STRING);
                }else{
                    itemCraftId = plugin.getNmsManager().getTagStringItem(item, CRAFT_ENGINE_ID_KEY);
                }
                if(itemCraftId == null || !itemCraftId.equals(craftId)){
                    continue;
                }
            }

            // Check ItemsAdder ID
            String iaId = discoveredOn.getItemsAdderId();
            if(iaId != null){
                String itemIaId = getItemsAdderId(item);
                if(itemIaId == null || !itemIaId.equalsIgnoreCase(iaId)){
                    continue;
                }
            }

            // Check MMOItems
            if(discoveredOn.getMmoItemsId()!=null || discoveredOn.getMmoItemsType()!=null){
                String id = getMmoItemsId(item);
                String type = getMmoItemsType(item);
                if(discoveredOn.getMmoItemsId()!=null && (id==null || !id.equalsIgnoreCase(discoveredOn.getMmoItemsId()))){
                    continue;
                }
                if(discoveredOn.getMmoItemsType()!=null && (type==null || !type.equalsIgnoreCase(discoveredOn.getMmoItemsType()))){
                    continue;
                }
            }

            // CustomFishing ID 检查（注意：钓鱼分类不走这个流程，而是通过CustomFishingListener直接触发）
            String customFishingId = discoveredOn.getCustomFishingId();
            if(customFishingId != null){
                plugin.getLogger().warning("检测到通过onItemObtain处理CustomFishing物品，这通常不应该发生！物品类型: " + item.getType() + ", 期望CustomFishingId: " + customFishingId);
                continue; // 跳过，因为钓鱼应该通过CustomFishingListener处理
            }

            onDiscover(player,discovery.getCategoryName(),discovery.getId());
            return;
        }
    }

    // New trigger: command run
    public void onCommandRun(Player player, String command){
        List<Discovery> discoveries = getPossibleDiscoveries(DiscoveredOn.DiscoveredOnType.COMMAND_RUN);
        for(Discovery discovery : discoveries){
            DiscoveredOn discoveredOn = discovery.getDiscoveredOn();
            String discoveryCommand = discoveredOn.getCommand();
            if(discoveryCommand != null){
                String[] sep = discoveryCommand.split(";");
                if(Arrays.stream(sep).noneMatch(c -> c.equalsIgnoreCase(command))){
                    continue;
                }
            }

            onDiscover(player,discovery.getCategoryName(),discovery.getId());

            return;
        }
    }
    
    /**
     * 玩家发现附魔触发器
     * 
     * @param player 玩家
     * @param enchantmentId 附魔ID，格式为 namespace:key，例如 minecraft:sharpness 或 aiyatsbus:telekinesis
     */
    public void onEnchantmentDiscover(Player player, String enchantmentId) {
        if(enchantmentId == null) return;
        // 使用原始格式，不再转换为下划线
        String discoveryId = enchantmentId;
        // 检查玩家是否已经发现过这个附魔（统一用原始格式）
        if(plugin.getPlayerDataManager().hasDiscoveredEnchantment(player, discoveryId)) {
            return;
        }
        // 从Aiyatsbus获取附魔中文名称
        String enchantName = AiyatsbusUtils.getEnchantmentName(plugin, enchantmentId, enchantmentId);
        // 调试输出
        if(plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] 处理附魔发现: " + enchantName + " (ID: " + enchantmentId + ", 使用ID: " + discoveryId + ") 玩家: " + player.getName());
        }
        // 直接搜索特定分类中的发现项
        String enchantCatName = plugin.getConfigsManager().getEnchantmentsConfigManager().getCategoryName();
        Category enchantCategory = plugin.getCategoryManager().getCategory(enchantCatName);
        if(enchantCategory != null) {
            Discovery directMatch = enchantCategory.getDiscovery(discoveryId);
            if(directMatch != null) {
                if(plugin.getDebugManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG] 找到精确匹配的附魔发现项: " + enchantName);
                }
                if(aiyatsbusPresent && !enchantName.equals(enchantmentId)) {
                    directMatch.setName("&b" + enchantName);
                    if(plugin.getDebugManager().isDebugEnabled()) {
                        plugin.getLogger().info("[DEBUG] 更新附魔发现项名称为: " + enchantName);
                    }
                }
                onDiscover(player, enchantCatName, discoveryId);
                return;
            } else if(plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG] 未找到精确匹配的附魔发现项: " + discoveryId);
            }
        }
        // 如果直接匹配失败，则使用原有逻辑搜索所有可能的发现项
        List<Discovery> discoveries = getPossibleDiscoveries(DiscoveredOn.DiscoveredOnType.ENCHANTMENT_DISCOVER);
        if(discoveries.isEmpty() && plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG] 没有找到附魔发现项，可能需要重建缓存或检查分类配置");
        }
        for(Discovery discovery : discoveries) {
            DiscoveredOn discoveredOn = discovery.getDiscoveredOn();
            String discoveryEnchantmentId = discoveredOn.getEnchantmentId();
            if(discoveryEnchantmentId != null) {
                if(!discoveryId.equalsIgnoreCase(discoveryEnchantmentId)) {
                    continue;
                }
                onDiscover(player, discovery.getCategoryName(), discovery.getId());
                return;
            }
        }
        
        // 如果没有找到匹配的发现项，且启用了自动生成，则尝试创建一个
        if(plugin.getConfigsManager().getEnchantmentsConfigManager().isAutoGenerate()) {
            String enchantCategoryName = plugin.getConfigsManager().getEnchantmentsConfigManager().getCategoryName();
            Category category = plugin.getCategoryManager().getCategory(enchantCategoryName);
            
            if(category != null) {
                // 检查是否已经存在相同ID的发现项
                String autoDiscoveryId = enchantmentId; // 使用原始格式
                if(category.getDiscovery(autoDiscoveryId) == null) {
                    if(plugin.getDebugManager().isDebugEnabled()) {
                        plugin.getLogger().info("[DEBUG] 正在为附魔 " + enchantmentId + " 创建新的发现项，ID: " + autoDiscoveryId);
                    }
                    // 获取附魔信息（用于描述和其他属性）
                    EnchantmentInfo info = plugin.getEnchantmentInfoManager().getEnchantmentInfo(enchantmentId);
                    // 创建新的发现项
                    Discovery newDiscovery = new Discovery();
                    newDiscovery.setId(autoDiscoveryId);
                    newDiscovery.setName("&b" + enchantName);
                    newDiscovery.setCategoryName(enchantCategoryName);
                    // 设置发现触发条件
                    DiscoveredOn discoveredOn = new DiscoveredOn(DiscoveredOn.DiscoveredOnType.ENCHANTMENT_DISCOVER);
                    discoveredOn.setEnchantmentId(enchantmentId); // 保存原始ID，带冒号
                    newDiscovery.setDiscoveredOn(discoveredOn);
                    // 获取描述
                    List<String> description = plugin.getConfigsManager().getEnchantmentsConfigManager().getCustomDescription(enchantmentId);
                    if(description == null) {
                        description = plugin.getEnchantmentInfoManager().processDescription(
                            info, plugin.getConfigsManager().getEnchantmentsConfigManager().getDefaultDescriptionTemplate());
                    }
                    newDiscovery.setDescription(description);
                    // 添加到分类
                    category.getDiscoveries().add(newDiscovery);
                    // 重建缓存
                    rebuildCache();
                    // 触发发现
                    onDiscover(player, enchantCategoryName, autoDiscoveryId);
                }
            }
        }
    }

    public boolean onDiscover(Player player,String categoryName,String discoveryName){
        boolean isDebug = plugin.getDebugManager().isDebugEnabled();
        
        if(isDebug) {
            plugin.getLogger().info("[DEBUG] onDiscover 参数: player=" + player.getName() + ", categoryName=" + categoryName + ", discoveryName=" + discoveryName);
        }
        
        Category category = plugin.getCategoryManager().getCategory(categoryName);
        if(isDebug) {
            plugin.getLogger().info("[DEBUG] onDiscover category: " + (category == null ? "null" : "存在"));
            if(category == null) {
                plugin.getLogger().info("[DEBUG] 分类不存在，尝试查看所有分类...");
                ArrayList<Category> allCategories = plugin.getCategoryManager().getCategories();
                plugin.getLogger().info("[DEBUG] 当前已有分类数量: " + allCategories.size());
                for(Category cat : allCategories) {
                    plugin.getLogger().info("[DEBUG] 现有分类: " + cat.getName());
                }
            }
        }
        
        Discovery discovery = category != null ? category.getDiscovery(discoveryName) : null;
        if(isDebug) {
            plugin.getLogger().info("[DEBUG] onDiscover discovery: " + (discovery == null ? "null" : "存在"));
            if(discovery == null && category != null) {
                plugin.getLogger().info("[DEBUG] 分类存在但发现项不存在，分类中的发现项数量: " + category.getDiscoveries().size());
                int maxShow = Math.min(10, category.getDiscoveries().size()); // 最多显示10个
                for(int i = 0; i < maxShow; i++) {
                    Discovery d = category.getDiscoveries().get(i);
                    plugin.getLogger().info("[DEBUG] 现有发现项[" + i + "]: " + d.getId());
                }
                if(category.getDiscoveries().size() > 10) {
                    plugin.getLogger().info("[DEBUG] ... 还有 " + (category.getDiscoveries().size() - 10) + " 个发现项");
                }
            }
        }

        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        if(playerDataManager.hasDiscovery(player,categoryName,discoveryName)){
            return false;
        }
        playerDataManager.addDiscovery(player,categoryName,discoveryName);

        // Rewards
        ArrayList<CommonVariable> variables = new ArrayList<>();
        variables.add(new CommonVariable("%name%",discovery.getName()));
        boolean completed = playerDataManager.hasAllDiscoveries(player, categoryName, category.getDiscoveries().size()) &&
                !playerDataManager.hasCompletedCategory(player, category.getName());
        List<String> rewards = category.getDefaultRewardsPerDiscovery();
        if(discovery.getCustomRewards() != null){
            rewards = discovery.getCustomRewards();
        }

        if(rewards != null){
            for(String action : rewards){
                ActionUtils.executeAction(player,action,plugin,variables);
            }
        }

        if(completed){
            playerDataManager.completeCategory(player,categoryName);
            rewards = category.getDefaultRewardsAllDiscoveries();
            if(rewards != null){
                for(String action : rewards){
                    ActionUtils.executeAction(player,action,plugin,variables);
                }
            }
        }
        
        // 发送发现通知
        sendDiscoveryNotification(player, categoryName, discoveryName, discovery);

        return true;
    }
    
    /**
     * 发送发现通知
     * @param player 玩家
     * @param categoryName 分类名称
     * @param discoveryName 发现项ID
     * @param discovery 发现项对象
     */
    public void sendDiscoveryNotification(Player player, String categoryName, String discoveryName, Discovery discovery) {
        // 检查是否启用通知
        if (!isNotificationEnabled(categoryName)) {
            return;
        }
        
        // 获取发现项的名称
        String name = discovery.getName();
        if (name == null || name.isEmpty()) {
            name = discoveryName;
        }
        
        // 从messages.yml获取发现消息模板
        FileConfiguration messagesConfig = plugin.getMessagesConfig();
        String messageKey = "discovery." + categoryName + ".notificationMessage";
        String defaultTemplate = "&aPlayer &7%player% &aunlocked discovery &7%discovery% &aon category &7%category%&a!";
        String messageTemplate = messagesConfig.getString(messageKey, 
            messagesConfig.getString("playerUnlockDiscovery", defaultTemplate));
        
        // 替换变量
        String message = messageTemplate
            .replace("%player%", player.getName())
            .replace("%discovery%", name)
            .replace("%category%", categoryName);
        
        // 发送消息 - 钓鱼分类使用CustomFishing颜色处理，其他分类使用普通颜色处理
        String finalMessage;
        if (categoryName.equals("fishing")) {
            finalMessage = MessagesManager.getCustomFishingColoredMessage(plugin.prefix + message);
        } else {
            finalMessage = MessagesManager.getColoredMessage(plugin.prefix + message);
        }
        player.sendMessage(finalMessage);
        
        // 获取声音配置
        String soundName = messagesConfig.getString("discovery." + categoryName + ".sound", "ENTITY_PLAYER_LEVELUP");
        float volume = (float) messagesConfig.getDouble("discovery." + categoryName + ".volume", 0.5);
        float pitch = (float) messagesConfig.getDouble("discovery." + categoryName + ".pitch", 1.0);
        
        // 播放声音
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            // 如果声音无效，尝试使用默认声音
            try {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.0f);
            } catch (Exception ex) {
                // 忽略声音错误
            }
        }
    }
    
    /**
     * 检查是否为指定分类启用了通知
     * @param categoryName 分类名称
     * @return 是否启用通知
     */
    private boolean isNotificationEnabled(String categoryName) {
        // 默认启用所有通知
        FileConfiguration config = plugin.getConfigsManager().getMainConfigManager().getConfig();
        
        // 先检查特定分类的设置
        String specificPath = "notifications." + categoryName + ".enabled";
        if (config.contains(specificPath)) {
            return config.getBoolean(specificPath, true);
        }
        
        // 否则返回全局设置
        return config.getBoolean("notifications.enabled", true);
    }

    private String getItemsAdderId(ItemStack item){
        if(!itemsAdderPresent){
            return null;
        }
        try{
            Class<?> customStackClz = Class.forName("dev.lone.itemsadder.api.CustomStack");
            java.lang.reflect.Method byItem = customStackClz.getMethod("byItemStack", ItemStack.class);
            Object cs = byItem.invoke(null,item);
            if(cs == null){
                return null;
            }
            java.lang.reflect.Method getName = customStackClz.getMethod("getName");
            return (String)getName.invoke(cs);
        }catch(Exception e){
            return null;
        }
    }

    private String getMmoItemsId(ItemStack item){
        if(!mmoItemsPresent){return null;}
        // Try PDC keys first
        ItemMeta meta = item.getItemMeta();
        if(meta!=null){
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            NamespacedKey idKey = new NamespacedKey("mmoitems","item_id");
            if(pdc.has(idKey,PersistentDataType.STRING)){
                return pdc.get(idKey,PersistentDataType.STRING);
            }
        }
        // Fallback NBT
        return plugin.getNmsManager().getTagStringItem(item,"MMOITEMS_ITEM_ID");
    }

    private String getMmoItemsType(ItemStack item){
        if(!mmoItemsPresent){return null;}
        ItemMeta meta = item.getItemMeta();
        if(meta!=null){
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            NamespacedKey typeKey = new NamespacedKey("mmoitems","item_type");
            if(pdc.has(typeKey,PersistentDataType.STRING)){
                return pdc.get(typeKey,PersistentDataType.STRING);
            }
        }
        return plugin.getNmsManager().getTagStringItem(item,"MMOITEMS_ITEM_TYPE");
    }


    // 工具方法：附魔ID格式转换 - 现在保持原始格式，不进行转换
    public static String toDiscoveryId(String enchantmentId) {
        if(enchantmentId == null) return null;
        return enchantmentId; // 保持原始格式，不再替换冒号为下划线
    }
    public static String toEnchantmentId(String discoveryId) {
        if(discoveryId == null) return null;
        return discoveryId; // 保持原始格式
    }
}
