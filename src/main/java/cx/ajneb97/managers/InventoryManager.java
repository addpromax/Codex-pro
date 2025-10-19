package cx.ajneb97.managers;

import cx.ajneb97.Codex;
import cx.ajneb97.config.CategoryPaginationConfig;
import cx.ajneb97.model.data.PlayerDataDiscovery;
import cx.ajneb97.model.internal.CommonVariable;
import cx.ajneb97.model.inventory.CommonInventory;
import cx.ajneb97.model.inventory.CommonInventoryItem;
import cx.ajneb97.model.inventory.InventoryPlayer;
import cx.ajneb97.model.item.CommonItem;
import cx.ajneb97.model.structure.Category;
import cx.ajneb97.model.structure.Discovery;
import cx.ajneb97.utils.ActionUtils;
import cx.ajneb97.utils.ItemUtils;
import cx.ajneb97.utils.OtherUtils;
import cx.ajneb97.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InventoryManager {

    private Codex plugin;
    private ArrayList<CommonInventory> inventories;
    private ArrayList<InventoryPlayer> players;

    public InventoryManager(Codex plugin){
        this.plugin = plugin;
        this.inventories = new ArrayList<>();
        this.players = new ArrayList<>();
    }

    public ArrayList<CommonInventory> getInventories() {
        return inventories;
    }

    public void setInventories(ArrayList<CommonInventory> inventories) {
        this.inventories = inventories;
    }

    public ArrayList<InventoryPlayer> getPlayers() {
        return players;
    }

    public CommonInventory getInventory(String name){
        for(CommonInventory inventory : inventories){
            if(inventory.getName().equals(name)){
                return inventory;
            }
        }
        return null;
    }

    public InventoryPlayer getInventoryPlayer(Player player){
        if (plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG-GET] 查找InventoryPlayer - 玩家: " + player.getName() + 
                ", 管理器中总玩家数: " + players.size());
        }
        
        for(InventoryPlayer inventoryPlayer : players){
            if (plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG-GET] 检查玩家: " + inventoryPlayer.getPlayer().getName() + 
                    ", 界面: " + inventoryPlayer.getInventoryName());
            }
            if(inventoryPlayer.getPlayer().equals(player)){
                if (plugin.getDebugManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG-GET] ✅ 找到匹配的InventoryPlayer");
                }
                return inventoryPlayer;
            }
        }
        if (plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG-GET] ❌ 未找到InventoryPlayer");
        }
        return null;
    }

    public void removeInventoryPlayer(Player player){
        players.removeIf(p -> p.getPlayer().equals(player));
    }

    public void openInventory(InventoryPlayer inventoryPlayer){
        CommonInventory inventory = getInventory(inventoryPlayer.getInventoryName());

        // 检查inventory是否为null
        if (inventory == null) {
            plugin.getLogger().warning("无法找到物品栏配置: " + inventoryPlayer.getInventoryName());
            // 如果找不到该物品栏配置，打开主物品栏
            inventoryPlayer.setInventoryName("main");
            inventory = getInventory("main");
            
            // 如果连主物品栏都没有，则无法继续
            if (inventory == null) {
                plugin.getLogger().severe("无法找到主物品栏配置！");
                return;
            }
        }

        String title = inventory.getTitle();
        Inventory inv = Bukkit.createInventory(null,inventory.getSlots(), MessagesManager.getColoredMessage(title));

        List<CommonInventoryItem> items = inventory.getItems();
        CommonItemManager commonItemManager = plugin.getCommonItemManager();
        Player player = inventoryPlayer.getPlayer();

        // 检查是否是主菜单，如果是且没有Aiyatsbus插件，则隐藏附魔分类按钮
        if (inventoryPlayer.getInventoryName().equals("main_inventory")) {
            // 检查Aiyatsbus插件是否存在
            boolean aiyatsbusExists = Bukkit.getPluginManager().getPlugin("Aiyatsbus") != null;
            if (!aiyatsbusExists) {
                // 创建一个过滤后的物品列表，跳过附魔分类按钮
                List<CommonInventoryItem> filteredItems = new ArrayList<>();
                for (CommonInventoryItem item : items) {
                    if (item.getType() == null || !item.getType().equals("category: enchantments")) {
                        filteredItems.add(item);
                    }
                }
                // 使用过滤后的列表
                items = filteredItems;
            }
        }

        // 钓鱼分类现在总是可用，无需初始化检查

        // 检查尝试打开附魔分类但Aiyatsbus不存在的情况
        if (inventoryPlayer.getInventoryName().equals("category_enchantments")) {
            boolean aiyatsbusExists = Bukkit.getPluginManager().getPlugin("Aiyatsbus") != null;
            if (!aiyatsbusExists) {
                // 如果Aiyatsbus插件不存在，打开主菜单并通知玩家
                Player playerObj = inventoryPlayer.getPlayer();
                String msg = plugin.getMessagesConfig().getString("needAiyatsbus");
                playerObj.sendMessage(MessagesManager.getColoredMessage(plugin.prefix + msg));
                inventoryPlayer.setInventoryName("main_inventory");
                openInventory(inventoryPlayer);
                return;
            }
        }
        
        // 检查是否需要使用分页
        if (usePaginationMode(inventoryPlayer.getInventoryName()) && 
            inventoryPlayer.getInventoryName().startsWith("category_")) {
            openCategoryWithPagination(inventoryPlayer, inv, inventory);
            return;
        }

        //Add items for all inventories
        for(CommonInventoryItem itemInventory : items){
            for(int slot : itemInventory.getSlots()){
                String type = itemInventory.getType();
                if(type != null){
                    ItemStack item = null;
                    if(type.startsWith("discovery: ")){
                        item = setDiscovery(type.replace("discovery: ",""),inventoryPlayer);
                    }else if(type.startsWith("category: ")){
                        item = setCategory(type.replace("category: ",""),player);
                    }
                    if(item != null){
                        item = setItemActions(itemInventory,item);
                        inv.setItem(slot,item);
                    }
                    continue;
                }

                ItemStack item = commonItemManager.createItemFromCommonItem(itemInventory.getItem(),player);

                String openInventory = itemInventory.getOpenInventory();
                if(openInventory != null) {
                    item = ItemUtils.setTagStringItem(plugin,item, "codex_open_inventory", openInventory);
                }
                item = setItemActions(itemInventory,item);

                inv.setItem(slot,item);
            }
        }

        inventoryPlayer.getPlayer().openInventory(inv);
        players.add(inventoryPlayer);
        
        if (plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG-OPEN] InventoryPlayer已添加到管理器 - 玩家: " + player.getName() + 
                ", 界面名: " + inventoryPlayer.getInventoryName() + ", 当前管理器中玩家数: " + players.size());
        }
    }


    
    /**
     * 检查是否使用分页模式打开物品栏
     * @param inventoryName 物品栏名称
     * @return 是否使用分页模式
     */
    private boolean usePaginationMode(String inventoryName) {
        if (inventoryName == null) return false;
        
        // 如果是分类物品栏
        if (inventoryName.startsWith("category_")) {
            String categoryName = inventoryName.replace("category_", "").split(";")[0];
            
            // 对于附魔分类，检查Aiyatsbus插件是否存在
            if (categoryName.equals("enchantments")) {
                boolean aiyatsbusExists = Bukkit.getPluginManager().getPlugin("Aiyatsbus") != null;
                if (!aiyatsbusExists) {
                    return false; // 如果Aiyatsbus插件不存在，不使用分页
                }
            }
            
            return plugin.getConfigsManager().getCategoryPaginationConfig().getPagination(categoryName).isEnabled();
        }
        
        return false;
    }
    

    
    /**
     * 使用分页方式打开分类物品栏
     * @param inventoryPlayer 玩家物品栏信息
     * @param inv 创建的物品栏
     * @param inventory 物品栏配置
     */
    private void openCategoryWithPagination(InventoryPlayer inventoryPlayer, Inventory inv, CommonInventory inventory) {
        Player player = inventoryPlayer.getPlayer();
        List<CommonInventoryItem> items = inventory.getItems();
        CommonItemManager commonItemManager = plugin.getCommonItemManager();
        
        // 获取分类名称
        String categoryName = inventoryPlayer.getInventoryName().replace("category_", "").split(";")[0];
        Category category = plugin.getCategoryManager().getCategory(categoryName);
        
        if (category == null) {
            // 如果分类不存在，使用默认方法打开
            openInventoryDefault(inventoryPlayer, inv, items, commonItemManager);
            return;
        }
        
        // 获取分页配置
        CategoryPaginationConfig.CategoryPagination pagination = 
            plugin.getConfigsManager().getCategoryPaginationConfig().getPagination(categoryName);
        
        // 获取当前页码和每页物品数
        int currentPage = inventoryPlayer.getCurrentPage();
        int itemsPerPage = pagination.getItemsPerPage();
        
        // 获取所有发现项
        List<Discovery> discoveries = category.getDiscoveries();
        int totalPages = (int) Math.ceil((double) discoveries.size() / itemsPerPage);
        
        // 确保页码有效
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
            inventoryPlayer.setCurrentPage(currentPage);
        }
        
        // 计算当前页的起始和结束索引
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, discoveries.size());
        
        // 创建用于显示的发现项列表
        List<Discovery> pageDiscoveries = discoveries.subList(startIndex, endIndex);
        
        // 首先添加非发现项物品（如边框、导航按钮等）
        for (CommonInventoryItem itemInventory : items) {
            // 跳过发现项，它们将单独处理
            if (itemInventory.getType() != null && itemInventory.getType().startsWith("discovery: ")) {
                continue;
            }
            
            for (int slot : itemInventory.getSlots()) {
                ItemStack item;
                
                if (itemInventory.getItem() != null) {
                    item = commonItemManager.createItemFromCommonItem(itemInventory.getItem(), player);
                    
                    String openInventory = itemInventory.getOpenInventory();
                    if (openInventory != null) {
                        item = ItemUtils.setTagStringItem(plugin, item, "codex_open_inventory", openInventory);
                    }
                    
                    item = setItemActions(itemInventory, item);
                    inv.setItem(slot, item);
                }
            }
        }
        
        // 计算物品槽位
        Map<Integer, Integer> slotMap = pagination.calculateItemSlots(inventory.getSlots());
        
        // 添加当前页的发现项
        for (int i = 0; i < pageDiscoveries.size(); i++) {
            Discovery discovery = pageDiscoveries.get(i);
            if (discovery == null) continue;
            
            // 获取槽位
            Integer slot = slotMap.get(i);
            if (slot == null) continue;
            
            // 创建临时的InventoryPlayer来设置发现项
            InventoryPlayer tempPlayer = new InventoryPlayer(player, inventoryPlayer.getInventoryName());
            
            // 设置发现项
            ItemStack item = setDiscovery(discovery.getId(), tempPlayer);
            if (item != null) {
                inv.setItem(slot, item);
            }
        }
        
        // 添加分页按钮
        // 上一页按钮
        ItemStack prevButton = commonItemManager.createItemFromCommonItem(pagination.getPreviousButtonItem(), player);
        if (currentPage == 0) {
            // 如果是第一页，禁用上一页按钮
            ItemMeta meta = prevButton.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                lore.add(MessagesManager.getColoredMessage("&8已经是第一页"));
                MessagesManager.setItemLore(meta, lore);
                prevButton.setItemMeta(meta);
            }
        } else {
            prevButton = ItemUtils.setTagStringItem(plugin, prevButton, "codex_item_actions", "page_previous");
        }
        inv.setItem(pagination.getPreviousButtonSlot(), prevButton);
        
        // 下一页按钮
        ItemStack nextButton = commonItemManager.createItemFromCommonItem(pagination.getNextButtonItem(), player);
        if (currentPage >= totalPages - 1) {
            // 如果是最后一页，禁用下一页按钮
            ItemMeta meta = nextButton.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.getLore() != null ? meta.getLore() : new ArrayList<>();
                lore.add(MessagesManager.getColoredMessage("&8已经是最后一页"));
                MessagesManager.setItemLore(meta, lore);
                nextButton.setItemMeta(meta);
            }
        } else {
            nextButton = ItemUtils.setTagStringItem(plugin, nextButton, "codex_item_actions", "page_next");
        }
        inv.setItem(pagination.getNextButtonSlot(), nextButton);
        
        // 页面信息按钮
        ItemStack infoButton = commonItemManager.createItemFromCommonItem(pagination.getPageInfoItem(), player);
        ItemMeta infoMeta = infoButton.getItemMeta();
        if (infoMeta != null) {
            String displayName = infoMeta.getDisplayName()
                    .replace("%current_page%", String.valueOf(currentPage + 1))
                    .replace("%total_pages%", String.valueOf(totalPages));
            MessagesManager.setItemDisplayName(infoMeta, displayName);
            
            if (infoMeta.getLore() != null) {
                List<String> newLore = new ArrayList<>();
                for (String line : infoMeta.getLore()) {
                    newLore.add(line.replace("%total_items%", String.valueOf(discoveries.size())));
                }
                MessagesManager.setItemLore(infoMeta, newLore);
            }
            infoButton.setItemMeta(infoMeta);
        }
        inv.setItem(pagination.getPageInfoSlot(), infoButton);
        
        // 强制再次处理所有边框物品，确保它们被正确添加到物品栏中
        // 这将处理玻璃板等装饰物品
        for (CommonInventoryItem itemInventory : items) {
            if (itemInventory.getType() != null) {
                // 跳过特殊类型的物品
                continue;
            }
            
            // 处理普通物品，如边框、装饰等
            for (int slot : itemInventory.getSlots()) {
                if (itemInventory.getItem() != null) {
                    ItemStack item = commonItemManager.createItemFromCommonItem(itemInventory.getItem(), player);
                    
                    String openInventory = itemInventory.getOpenInventory();
                    if (openInventory != null) {
                        item = ItemUtils.setTagStringItem(plugin, item, "codex_open_inventory", openInventory);
                    }
                    
                    item = setItemActions(itemInventory, item);
                    inv.setItem(slot, item);
                }
            }
        }
        
        // 打开物品栏
        player.openInventory(inv);
        players.add(inventoryPlayer);
        
        if (plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG-OPEN] InventoryPlayer已添加到管理器 - 玩家: " + player.getName() + 
                ", 界面名: " + inventoryPlayer.getInventoryName() + ", 当前管理器中玩家数: " + players.size());
        }
    }
    
    /**
     * 使用默认方式打开库存
     */
    private void openInventoryDefault(InventoryPlayer inventoryPlayer, Inventory inv, List<CommonInventoryItem> items, CommonItemManager commonItemManager) {
        Player player = inventoryPlayer.getPlayer();
        
        for (CommonInventoryItem itemInventory : items) {
            for (int slot : itemInventory.getSlots()) {
                String type = itemInventory.getType();
                if (type != null) {
                    ItemStack item = null;
                    if (type.startsWith("discovery: ")) {
                        item = setDiscovery(type.replace("discovery: ", ""), inventoryPlayer);
                    } else if (type.startsWith("category: ")) {
                        item = setCategory(type.replace("category: ", ""), player);
                    }
                    if (item != null) {
                        item = setItemActions(itemInventory, item);
                        inv.setItem(slot, item);
                    }
                    continue;
                }

                ItemStack item = commonItemManager.createItemFromCommonItem(itemInventory.getItem(), player);

                String openInventory = itemInventory.getOpenInventory();
                if (openInventory != null) {
                    item = ItemUtils.setTagStringItem(plugin, item, "codex_open_inventory", openInventory);
                }
                item = setItemActions(itemInventory, item);

                inv.setItem(slot, item);
            }
        }
        
        player.openInventory(inv);
        players.add(inventoryPlayer);
    }

    private ItemStack setItemActions(CommonInventoryItem commonItem, ItemStack item) {
        List<String> clickActions = commonItem.getClickActions();
        if(clickActions != null && !clickActions.isEmpty()) {
            String actionsList = "";
            for(int i=0;i<clickActions.size();i++) {
                if(i==clickActions.size()-1) {
                    actionsList=actionsList+clickActions.get(i);
                }else {
                    actionsList=actionsList+clickActions.get(i)+"|";
                }
            }
            item = ItemUtils.setTagStringItem(plugin, item, "codex_item_actions", actionsList);
        }
        return item;
    }

    private void clickOnDiscoveryItem(InventoryPlayer inventoryPlayer,String discoveryName,ClickType clickType){
        String categoryName = inventoryPlayer.getInventoryName().replace("category_","").split(";")[0];
        Category category = plugin.getCategoryManager().getCategory(categoryName);
        if(category == null){
            return;
        }

        Discovery discovery = category.getDiscovery(discoveryName);
        if(discovery == null){
            return;
        }

        Player player = inventoryPlayer.getPlayer();
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        boolean hasDiscovered = playerDataManager.hasDiscovery(player,category.getName(),discoveryName);

        if(!hasDiscovered){
            return;
        }

        List<String> actions = discovery.getClickActions();
        if(actions != null){
            int cooldown = discovery.getClickActionsCooldown();
            if(cooldown != 0){
                long millisActionsExecuted = playerDataManager.getMillisActionsExecuted(player,category.getName(),discoveryName);
                long millisAvailable = millisActionsExecuted+(cooldown*1000L);
                long currentMillis = System.currentTimeMillis();

                if(millisActionsExecuted != 0 && millisAvailable > currentMillis){
                    FileConfiguration messagesConfig = plugin.getMessagesConfig();
                    MessagesManager msgManager = plugin.getMessagesManager();
                    String timeString = TimeUtils.getTime((millisAvailable-currentMillis)/1000,msgManager);
                    msgManager.sendMessage(player,messagesConfig.getString("clickActionsCooldown")
                            .replace("%time%",timeString),true);
                    return;
                }

                playerDataManager.setMillisActionsExecuted(player,category.getName(),discoveryName);
            }

            for(String action : actions){
                ActionUtils.executeAction(player,action,plugin,new ArrayList<>());
            }
        }
    }

    private void clickOnCategoryItem(InventoryPlayer inventoryPlayer, String categoryName, ClickType clickType){
        Category category = plugin.getCategoryManager().getCategory(categoryName);
        if(category == null){
            plugin.getLogger().warning("无法找到分类: " + categoryName + " - 玩家: " + inventoryPlayer.getPlayer().getName());
            // 向玩家发送错误消息
            inventoryPlayer.getPlayer().sendMessage(MessagesManager.getColoredMessage(
                plugin.prefix + "&c无法找到分类 &e" + categoryName + "&c，请联系管理员。"));
            return;
        }

        // 检查分类GUI是否存在
        String inventoryName = "category_" + categoryName;
        if (getInventory(inventoryName) == null) {
            plugin.getLogger().warning("分类存在，但缺少对应的GUI配置: " + inventoryName);
            // 向玩家发送错误消息
            inventoryPlayer.getPlayer().sendMessage(MessagesManager.getColoredMessage(
                plugin.prefix + "&c分类 &e" + categoryName + " &c的GUI配置缺失，请联系管理员。"));
            return;
        }

        Player player = inventoryPlayer.getPlayer();

        inventoryPlayer.setInventoryName(inventoryName);
        inventoryPlayer.setCurrentPage(0); // 重置页码
        openInventory(inventoryPlayer);
    }

    private void clickOnOpenInventoryItem(InventoryPlayer inventoryPlayer, String openInventory){
        // 检查物品栏配置是否存在
        if (getInventory(openInventory) == null) {
            plugin.getLogger().warning("无法找到物品栏配置: " + openInventory + " - 玩家: " + inventoryPlayer.getPlayer().getName());
            // 向玩家发送错误消息
            inventoryPlayer.getPlayer().sendMessage(MessagesManager.getColoredMessage(
                plugin.prefix + "&c无法打开物品栏 &e" + openInventory + "&c，请联系管理员。"));
            return;
        }
        
        inventoryPlayer.setInventoryName(openInventory);
        inventoryPlayer.setCurrentPage(0); // 重置页码
        openInventory(inventoryPlayer);
    }

    private void clickActionsItem(InventoryPlayer inventoryPlayer,String itemCommands){
        String[] sep = itemCommands.split("\\|");
        for(String action : sep){
            // 处理翻页动作
            if (action.equals("page_previous")) {
                handlePreviousPageAction(inventoryPlayer);
                return;
            } else if (action.equals("page_next")) {
                handleNextPageAction(inventoryPlayer);
                return;
            }
            
            ActionUtils.executeAction(inventoryPlayer.getPlayer(),action,plugin,new ArrayList<>());
        }
    }
    
    /**
     * 处理上一页动作
     * @param inventoryPlayer 玩家库存信息
     */
    private void handlePreviousPageAction(InventoryPlayer inventoryPlayer) {
        if (inventoryPlayer.getCurrentPage() > 0) {
            inventoryPlayer.previousPage();
            openInventory(inventoryPlayer);
        }
    }
    
    /**
     * 处理下一页动作
     * @param inventoryPlayer 玩家库存信息
     */
    private void handleNextPageAction(InventoryPlayer inventoryPlayer) {
        // 所有分类都从inventory.yml中获取配置
        String inventoryName = inventoryPlayer.getInventoryName();
        
        if (inventoryName.startsWith("category_")) {
            String categoryName = inventoryName.replace("category_", "").split(";")[0];
            Category category = plugin.getCategoryManager().getCategory(categoryName);
            
            if (category != null) {
                int itemsPerPage = plugin.getConfigsManager().getCategoryPaginationConfig().getPagination(categoryName).getItemsPerPage();
                int totalPages = (int) Math.ceil((double) category.getDiscoveries().size() / itemsPerPage);
                
                if (inventoryPlayer.getCurrentPage() < totalPages - 1) {
                    inventoryPlayer.nextPage();
                    openInventory(inventoryPlayer);
                }
            }
        }
    }

    public void clickInventory(InventoryPlayer inventoryPlayer, ItemStack item, ClickType clickType){
        if (plugin.getDebugManager().isDebugEnabled()) {
            plugin.getLogger().info("[DEBUG-CLICK] clickInventory被调用 - 玩家: " + inventoryPlayer.getPlayer().getName() + 
                ", 物品: " + (item != null ? item.getType().toString() : "null") + ", 点击类型: " + clickType);
        }
        
        if(item != null && item.hasItemMeta()) {
            if (plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG-CLICK] 物品有ItemMeta，检查各种标签");
            }
            
            // 修复：参照原始版本，按正确顺序处理点击事件
            String itemActions = ItemUtils.getTagStringItem(plugin, item, "codex_item_actions");
            if(itemActions != null) {
                if (plugin.getDebugManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG-CLICK] 🎯 找到item_actions标签: " + itemActions);
                }
                clickActionsItem(inventoryPlayer,itemActions);
                return;
            }

            String categoryName = ItemUtils.getTagStringItem(plugin, item, "codex_category");
            if(categoryName != null) {
                if (plugin.getDebugManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG-CLICK] 🎯 找到category标签: " + categoryName);
                }
                clickOnCategoryItem(inventoryPlayer,categoryName,clickType);
                return;
            }

            String discoveryName = ItemUtils.getTagStringItem(plugin, item, "codex_discovery");
            if(discoveryName != null) {
                if (plugin.getDebugManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG-CLICK] 🎯 找到discovery标签: " + discoveryName);
                }
                clickOnDiscoveryItem(inventoryPlayer,discoveryName,clickType);
                return;
            }

            String openInventory = ItemUtils.getTagStringItem(plugin, item, "codex_open_inventory");
            if(openInventory != null) {
                if (plugin.getDebugManager().isDebugEnabled()) {
                    plugin.getLogger().info("[DEBUG-CLICK] 🎯 找到open_inventory标签: " + openInventory);
                }
                clickOnOpenInventoryItem(inventoryPlayer,openInventory);
                return;
            }
            
            if (plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG-CLICK] ⚠️ 物品没有找到任何有效的Codex标签");
            }
        } else {
            if (plugin.getDebugManager().isDebugEnabled()) {
                plugin.getLogger().info("[DEBUG-CLICK] ⚠️ 物品为null或没有ItemMeta，无法处理");
            }
        }
    }

    public ItemStack setCategory(String categoryName,Player player){
        Category category = plugin.getCategoryManager().getCategory(categoryName);
        if(category == null){
            return null;
        }

        CommonItem commonItem;
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        CommonItemManager commonItemManager = plugin.getCommonItemManager();
        ArrayList<CommonVariable> variables = new ArrayList<>();

        int max = category.getDiscoveries().size();
        int totalDiscoveries = playerDataManager.getTotalDiscoveries(player,categoryName);
        String unlockedVariable = OtherUtils.getCurrentUnlockedVariable(totalDiscoveries,max,plugin.getMessagesConfig());
        commonItem = category.getCategoryItem();
        variables.add(new CommonVariable("%progress_bar%", OtherUtils.getProgressBar(totalDiscoveries,max,plugin.getConfigsManager().getMainConfigManager())));
        variables.add(new CommonVariable("%percentage%", OtherUtils.getPercentage(totalDiscoveries,max)+"%"));
        variables.add(new CommonVariable("%unlocked%", MessagesManager.getColoredMessage(unlockedVariable)));

        ItemStack item = commonItemManager.createItemFromCommonItem(commonItem,player);
        commonItemManager.replaceVariables(item,variables,player);

        item = ItemUtils.setTagStringItem(plugin,item,"codex_category",categoryName);
        return item;
    }

    public ItemStack setDiscovery(String discoveryName,InventoryPlayer inventoryPlayer){
        // Category could be:
        // category_<n>
        // category_<n>;<something>
        String categoryName = inventoryPlayer.getInventoryName().replace("category_","").split(";")[0];
        Category category = plugin.getCategoryManager().getCategory(categoryName);
        if(category == null){
            return null;
        }

        Discovery discovery = category.getDiscovery(discoveryName);
        if(discovery == null){
            return null;
        }

        Player player = inventoryPlayer.getPlayer();

        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        CommonItemManager commonItemManager = plugin.getCommonItemManager();

        ArrayList<CommonVariable> variables = new ArrayList<>();
        PlayerDataDiscovery playerDataDiscovery = playerDataManager.getDiscovery(player,category.getName(),discoveryName);

        ItemStack item;
        if(playerDataDiscovery != null){
            if(discovery.getCustomLevelUnlockedItem() != null){
                item = commonItemManager.createItemFromCommonItem(discovery.getCustomLevelUnlockedItem(),player);
            }else{
                item = commonItemManager.createItemFromCommonItem(category.getDefaultLevelUnlockedItem(),player);
            }
            // 对于钓鱼分类，优先使用Discovery中已保存的CustomFishing名称
            if (categoryName.equals("fishing")) {
                // 如果Discovery已经有CustomFishing的名称，就使用它；否则动态获取
                String displayName = discovery.getName();
                if (displayName != null && !displayName.equals(discoveryName)) {
                    // Discovery中已有CustomFishing的显示名称
                    variables.add(new CommonVariable("%name%", MessagesManager.getCustomFishingColoredMessage(displayName)));
                } else {
                    // 动态获取CustomFishing的显示名称作为回退
                    String customFishingDisplayName = plugin.getFishingManager().getCustomFishingDisplayName(discoveryName, player);
                    variables.add(new CommonVariable("%name%", MessagesManager.getCustomFishingColoredMessage(customFishingDisplayName)));
                }
            } else {
                variables.add(new CommonVariable("%name%", MessagesManager.getColoredMessage(discovery.getName())));
            }
            variables.add(new CommonVariable("%date%",MessagesManager.getColoredMessage(playerDataDiscovery.getDiscoverDate())));
            
            // 如果是附魔发现，处理附魔相关变量
            if (discovery.isEnchantmentDiscovery() && discovery.getEnchantmentId() != null) {
                // 获取物品的lore
                ItemMeta meta = item.getItemMeta();
                List<String> lore = meta.getLore();
                
                if (lore != null && !lore.isEmpty()) {
                    // 处理lore中的变量
                    List<String> newLore = plugin.getEnchantmentManager().processItemLore(
                        lore, discovery.getEnchantmentId());
                    
                    // 设置新的lore
                    MessagesManager.setItemLore(meta, newLore);
                    item.setItemMeta(meta);
                }
            }
        }else{
            if(discovery.getCustomLevelBlockedItem() != null){
                item = commonItemManager.createItemFromCommonItem(discovery.getCustomLevelBlockedItem(),player);
            }else{
                item = commonItemManager.createItemFromCommonItem(category.getDefaultLevelBlockedItem(),player);
            }
        }

        // 钓鱼分类特殊处理：完全使用CustomFishing的原始数据，不使用模板
        if (!categoryName.equals("fishing")) {
            // 非钓鱼分类：使用正常的模板替换流程
            // Replace %description% variable
            ItemMeta meta = item.getItemMeta();
            List<String> newLore = new ArrayList<>();
            List<String> lore = meta.getLore();
            for (String s : lore){
                if(s.contains("%description%")){
                    List<String> description = discovery.getDescription();
                    for(String line : description){
                        newLore.add(MessagesManager.getColoredMessage(line));
                    }
                }else{
                    newLore.add(MessagesManager.getColoredMessage(s));
                }
            }
            MessagesManager.setItemLore(meta, newLore);
            item.setItemMeta(meta);

            commonItemManager.replaceVariables(item,variables,player);
        }
        
        // 处理钓鱼分类的数据设置
        if (categoryName.equals("fishing")) {
            // 对于钓鱼分类，完全使用CustomFishing的lore
            ItemMeta fishMeta = item.getItemMeta();
            if (fishMeta != null) {
                // 先替换displayName中的%name%占位符
                if (fishMeta.hasDisplayName()) {
                    String displayName = fishMeta.getDisplayName();
                    for (CommonVariable variable : variables) {
                        displayName = displayName.replace(variable.getVariable(), variable.getValue());
                    }
                    fishMeta.setDisplayName(displayName);
                }
                
                if (playerDataDiscovery != null) {
                    // 获取CustomFishing的真实ID
                    String customFishingId = discoveryName; // 默认使用discoveryName
                    if (discovery != null && discovery.getDiscoveredOn() != null && 
                        discovery.getDiscoveredOn().getCustomFishingId() != null) {
                        customFishingId = discovery.getDiscoveredOn().getCustomFishingId();
                    }
                    
                    // 已发现的鱼类：使用CustomFishing的完整lore
                    List<String> customFishingLore = plugin.getFishingManager().getCustomFishingLore(customFishingId, player);
                    
                    if (!customFishingLore.isEmpty()) {
                        // 处理lore：过滤掉CustomFishing的动态占位符行，添加统计信息
                        List<String> processedLore = new ArrayList<>();
                        
                        // 调试：输出discoveryName和customFishingId
                        boolean isDebug = plugin.getDebugManager().isDebugEnabled();
                        if (isDebug) {
                            plugin.getLogger().info("[INVENTORY-DEBUG] 准备获取统计数据 - discoveryName: " + discoveryName + ", customFishingId: " + customFishingId);
                        }
                        
                        double maxSize = plugin.getFishingManager().getFishMaxSize(player, customFishingId);
                        int fishAmount = plugin.getFishingManager().getFishAmount(player, customFishingId);
                        
                        if (isDebug) {
                            plugin.getLogger().info("[INVENTORY-DEBUG] 获取到的统计数据 - 最大尺寸: " + maxSize + ", 数量: " + fishAmount);
                        }
                        
                        for (String line : customFishingLore) {
                            // 过滤掉包含CustomFishing动态占位符的行
                            if (line.contains("{size_formatted}") || 
                                line.contains("{size}") || 
                                line.contains("{weight}")) {
                                // 跳过这一行，不添加到processedLore中
                                continue;
                            }
                            processedLore.add(line);
                        }
                        
                        // 获取鱼类详细信息（组别、环境、稀有度）
                        Map<String, String> fishDetailInfo = plugin.getFishingManager().getFishDetailInfo(customFishingId, player.getLocation(), true);
                        
                        // 添加环境、稀有度、分组信息
                        processedLore.add("");
                        
                        // 稀有度
                        String rarity = fishDetailInfo.getOrDefault("rarity", "普通");
                        processedLore.add("<color:#7289da>稀有度: <color:#ffeea2>" + rarity);
                        
                        // 钓鱼环境
                        String environment = fishDetailInfo.getOrDefault("environment", "水中");
                        processedLore.add("<color:#7289da>环境: <color:#ffffff>" + environment);
                        
                        // 分组（带概率）
                        String biomesInfo = fishDetailInfo.get("biomes");
                        if (biomesInfo != null && !biomesInfo.isEmpty()) {
                            processedLore.add("<color:#7289da>可钓分组:");
                            // biomesInfo 可能包含多行（用换行符分隔）
                            String[] biomeLines = biomesInfo.split("\n");
                            for (String biomeLine : biomeLines) {
                                processedLore.add("<color:#a8e063>  " + biomeLine);
                            }
                        }
                        
                        // 添加统计信息
                        processedLore.add("");
                        processedLore.add("<color:#7289da>发现于: <color:#ffffff>" + playerDataDiscovery.getDiscoverDate());
                        processedLore.add("<color:#7289da>捕获数量: <color:#ffffff>" + fishAmount + "条");
                        processedLore.add("<color:#7289da>最大尺寸: <color:#ffffff>" + String.format("%.1f", maxSize) + "厘米");
                        
                        // 使用新的ItemMeta设置方法，自动处理颜色代码
                        MessagesManager.setItemLore(fishMeta, processedLore);
                    } else {
                        // 如果无法获取CustomFishing lore，使用原有逻辑作为回退
                        List<String> fishLore = fishMeta.getLore();
                        List<String> processedLore = plugin.getFishingManager().processItemLore(fishLore, discoveryName, player, true);
                        MessagesManager.setItemLore(fishMeta, processedLore);
                    }
                } else {
                    // 未发现的鱼类：处理discovery_blocked的lore占位符
                    // 获取CustomFishing的真实ID
                    String customFishingId = discoveryName; // 默认使用discoveryName
                    if (discovery != null && discovery.getDiscoveredOn() != null && 
                        discovery.getDiscoveredOn().getCustomFishingId() != null) {
                        customFishingId = discovery.getDiscoveredOn().getCustomFishingId();
                    }
                    
                    // 获取配置的discovery_blocked lore
                    List<String> blockedLore = fishMeta.getLore();
                    
                    if (blockedLore != null && !blockedLore.isEmpty()) {
                        // 处理lore中的占位符（%biomes%, %rarity%, %environment%等）
                        List<String> processedLore = plugin.getFishingManager().processItemLore(blockedLore, customFishingId, player, false);
                        MessagesManager.setItemLore(fishMeta, processedLore);
                    } else {
                        // 如果没有配置lore，使用默认提示
                        List<String> defaultLore = new ArrayList<>();
                        defaultLore.add("<color:#8c8c8c>继续钓鱼以发现这种鱼!");
                        MessagesManager.setItemLore(fishMeta, defaultLore);
                    }
                }
                item.setItemMeta(fishMeta);
            }
        }

        item = ItemUtils.setTagStringItem(plugin,item,"codex_discovery",discoveryName);
        return item;
    }
}
