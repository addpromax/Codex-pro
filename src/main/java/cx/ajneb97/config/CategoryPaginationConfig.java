package cx.ajneb97.config;

import cx.ajneb97.Codex;
import cx.ajneb97.model.item.CommonItem;
import cx.ajneb97.model.item.CommonItemSkullData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分类分页配置管理器
 * 用于管理所有分类的分页配置信息
 */
public class CategoryPaginationConfig {
    private Codex plugin;
    private Map<String, CategoryPagination> categoryPaginationMap;
    
    public CategoryPaginationConfig(Codex plugin) {
        this.plugin = plugin;
        this.categoryPaginationMap = new HashMap<>();
    }
    
    /**
     * 加载所有分类的分页配置
     */
    public void loadAllPaginations() {
        // 清空旧的配置
        categoryPaginationMap.clear();
        
        // 获取inventory.yml配置
        FileConfiguration config = plugin.getConfigsManager().getInventoryConfigManager().getConfig();
        ConfigurationSection inventoriesSection = config.getConfigurationSection("inventories");
        
        if (inventoriesSection == null) {
            plugin.getLogger().warning("未找到inventories配置节，无法加载分页配置");
            return;
        }
        
        // 遍历所有物品栏
        for (String inventoryName : inventoriesSection.getKeys(false)) {
            // 只处理分类物品栏
            if (inventoryName.startsWith("category_")) {
                String categoryName = inventoryName.replace("category_", "");
                ConfigurationSection inventoryConfig = inventoriesSection.getConfigurationSection(inventoryName);
                
                if (inventoryConfig != null) {
                    // 检查配置节中是否有config部分
                    ConfigurationSection configSection = inventoryConfig.getConfigurationSection("config");
                    
                    if (configSection != null) {
                        ConfigurationSection paginationSection = configSection.getConfigurationSection("pagination");
                        
                        if (paginationSection != null) {
                            // 加载分类的分页配置
                            CategoryPagination pagination = loadCategoryPaginationFromInventory(categoryName, paginationSection);
                            categoryPaginationMap.put(categoryName, pagination);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 从inventory.yml加载单个分类的分页配置
     * @param categoryName 分类名称
     * @param paginationConfig 分页配置节
     * @return 分页配置
     */
    private CategoryPagination loadCategoryPaginationFromInventory(String categoryName, ConfigurationSection paginationConfig) {
        CategoryPagination pagination = new CategoryPagination();
        
        // 是否启用分页
        pagination.setEnabled(paginationConfig.getBoolean("enabled", true));
        
        // 每页物品数量
        pagination.setItemsPerPage(paginationConfig.getInt("items_per_page", 28));
        
        // 物品范围
        if (paginationConfig.contains("item_ranges")) {
            ConfigurationSection rangesSection = paginationConfig.getConfigurationSection("item_ranges");
            if (rangesSection != null) {
                for (String rangeKey : rangesSection.getKeys(false)) {
                    String rangeValue = rangesSection.getString(rangeKey);
                    if (rangeValue != null) {
                        String[] parts = rangeValue.split("-");
                        if (parts.length == 2) {
                            try {
                                int start = Integer.parseInt(parts[0]);
                                int end = Integer.parseInt(parts[1]);
                                pagination.addItemRange(rangeKey, start, end);
                            } catch (NumberFormatException e) {
                                plugin.getLogger().warning("无效的物品范围: " + rangeValue + " 于分类 " + categoryName);
                            }
                        }
                    }
                }
            }
        }
        
        // 上一页按钮
        ConfigurationSection prevSection = paginationConfig.getConfigurationSection("previous_button");
        if (prevSection != null) {
            pagination.setPreviousButtonSlot(prevSection.getInt("slot", 48));
            
            ConfigurationSection prevItemSection = prevSection.getConfigurationSection("item");
            if (prevItemSection != null) {
                CommonItem prevItem = new CommonItem(prevItemSection.getString("id", "PLAYER_HEAD"));
                prevItem.setName(prevItemSection.getString("name", "&7上一页"));
                prevItem.setLore(prevItemSection.getStringList("lore"));
                
                ConfigurationSection skullSection = prevItemSection.getConfigurationSection("skull_data");
                if (skullSection != null) {
                    CommonItemSkullData skullData = new CommonItemSkullData(
                        null,
                        skullSection.getString("texture"),
                        null
                    );
                    prevItem.setSkullData(skullData);
                }
                
                pagination.setPreviousButtonItem(prevItem);
            } else {
                // 默认上一页按钮
                CommonItem prevItem = new CommonItem("PLAYER_HEAD");
                prevItem.setName("&7上一页");
                CommonItemSkullData skullData = new CommonItemSkullData(
                    null,
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzdhZWU5YTc1YmYwZGY3ODk3MTgzMDE1Y2NhMGIyYTdkNzU1YzYzMzg4ZmYwMTc1MmQ1ZjQ0MTlmYzY0NSJ9fX0=",
                    null
                );
                prevItem.setSkullData(skullData);
                pagination.setPreviousButtonItem(prevItem);
            }
        } else {
            // 默认上一页按钮
            CommonItem prevItem = new CommonItem("PLAYER_HEAD");
            prevItem.setName("&7上一页");
            CommonItemSkullData skullData = new CommonItemSkullData(
                null,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzdhZWU5YTc1YmYwZGY3ODk3MTgzMDE1Y2NhMGIyYTdkNzU1YzYzMzg4ZmYwMTc1MmQ1ZjQ0MTlmYzY0NSJ9fX0=",
                null
            );
            prevItem.setSkullData(skullData);
            pagination.setPreviousButtonItem(prevItem);
            pagination.setPreviousButtonSlot(45);
        }
        
        // 下一页按钮
        ConfigurationSection nextSection = paginationConfig.getConfigurationSection("next_button");
        if (nextSection != null) {
            pagination.setNextButtonSlot(nextSection.getInt("slot", 53));
            
            ConfigurationSection nextItemSection = nextSection.getConfigurationSection("item");
            if (nextItemSection != null) {
                CommonItem nextItem = new CommonItem(nextItemSection.getString("id", "PLAYER_HEAD"));
                nextItem.setName(nextItemSection.getString("name", "&7下一页"));
                nextItem.setLore(nextItemSection.getStringList("lore"));
                
                ConfigurationSection skullSection = nextItemSection.getConfigurationSection("skull_data");
                if (skullSection != null) {
                    CommonItemSkullData skullData = new CommonItemSkullData(
                        null,
                        skullSection.getString("texture"),
                        null
                    );
                    nextItem.setSkullData(skullData);
                }
                
                pagination.setNextButtonItem(nextItem);
            } else {
                // 默认下一页按钮
                CommonItem nextItem = new CommonItem("PLAYER_HEAD");
                nextItem.setName("&7下一页");
                CommonItemSkullData skullData = new CommonItemSkullData(
                    null,
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjgyYWQxYjljYjRkZDIxMjU5YzBkNzVhYTMxNWZmMzg5YzNjZWY3NTJiZTM5NDkzMzgxNjRiYWM4NGE5NmUifX19",
                    null
                );
                nextItem.setSkullData(skullData);
                pagination.setNextButtonItem(nextItem);
            }
        } else {
            // 默认下一页按钮
            CommonItem nextItem = new CommonItem("PLAYER_HEAD");
            nextItem.setName("&7下一页");
            CommonItemSkullData skullData = new CommonItemSkullData(
                null,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjgyYWQxYjljYjRkZDIxMjU5YzBkNzVhYTMxNWZmMzg5YzNjZWY3NTJiZTM5NDkzMzgxNjRiYWM4NGE5NmUifX19",
                null
            );
            nextItem.setSkullData(skullData);
            pagination.setNextButtonItem(nextItem);
            pagination.setNextButtonSlot(53);
        }
        
        // 页面信息按钮
        ConfigurationSection infoSection = paginationConfig.getConfigurationSection("page_info");
        if (infoSection != null) {
            pagination.setPageInfoSlot(infoSection.getInt("slot", 49));
            
            ConfigurationSection infoItemSection = infoSection.getConfigurationSection("item");
            if (infoItemSection != null) {
                CommonItem infoItem = new CommonItem(infoItemSection.getString("id", "PAPER"));
                infoItem.setName(infoItemSection.getString("name", "&7第 &e%current_page% &7页，共 &e%total_pages% &7页"));
                infoItem.setLore(infoItemSection.getStringList("lore"));
                pagination.setPageInfoItem(infoItem);
            } else {
                // 默认页面信息按钮
                CommonItem infoItem = new CommonItem("PAPER");
                infoItem.setName("&7第 &e%current_page% &7页，共 &e%total_pages% &7页");
                List<String> lore = new ArrayList<>();
                lore.add("&7总共 &e%total_items% &7个物品");
                infoItem.setLore(lore);
                pagination.setPageInfoItem(infoItem);
            }
        } else {
            // 默认页面信息按钮
            CommonItem infoItem = new CommonItem("PAPER");
            infoItem.setName("&7第 &e%current_page% &7页，共 &e%total_pages% &7页");
            List<String> lore = new ArrayList<>();
            lore.add("&7总共 &e%total_items% &7个物品");
            infoItem.setLore(lore);
            pagination.setPageInfoItem(infoItem);
            pagination.setPageInfoSlot(49);
        }
        
        return pagination;
    }
    

    
    /**
     * 获取分类的分页配置
     * @param categoryName 分类名称
     * @return 分页配置，如果不存在则返回默认配置
     */
    public CategoryPagination getPagination(String categoryName) {
        CategoryPagination pagination = categoryPaginationMap.get(categoryName);
        if (pagination == null) {
            // 如果没有配置，创建一个默认的
            pagination = new CategoryPagination();
            pagination.setEnabled(true);
            pagination.setItemsPerPage(28);
            
            // 设置默认按钮
            CommonItem prevItem = new CommonItem("PLAYER_HEAD");
            prevItem.setName("&7上一页");
            CommonItemSkullData prevSkullData = new CommonItemSkullData(
                null,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzdhZWU5YTc1YmYwZGY3ODk3MTgzMDE1Y2NhMGIyYTdkNzU1YzYzMzg4ZmYwMTc1MmQ1ZjQ0MTlmYzY0NSJ9fX0=",
                null
            );
            prevItem.setSkullData(prevSkullData);
            pagination.setPreviousButtonItem(prevItem);
            pagination.setPreviousButtonSlot(36);
            
            CommonItem nextItem = new CommonItem("PLAYER_HEAD");
            nextItem.setName("&7下一页");
            CommonItemSkullData nextSkullData = new CommonItemSkullData(
                null,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjgyYWQxYjljYjRkZDIxMjU5YzBkNzVhYTMxNWZmMzg5YzNjZWY3NTJiZTM5NDkzMzgxNjRiYWM4NGE5NmUifX19",
                null
            );
            nextItem.setSkullData(nextSkullData);
            pagination.setNextButtonItem(nextItem);
            pagination.setNextButtonSlot(44);
            
            CommonItem infoItem = new CommonItem("PAPER");
            infoItem.setName("&7第 &e%current_page% &7页，共 &e%total_pages% &7页");
            List<String> lore = new ArrayList<>();
            lore.add("&7总共 &e%total_items% &7个物品");
            infoItem.setLore(lore);
            pagination.setPageInfoItem(infoItem);
            pagination.setPageInfoSlot(40);
            
            categoryPaginationMap.put(categoryName, pagination);
        }
        return pagination;
    }
    
    /**
     * 分页配置类，存储单个分类的分页设置
     */
    public static class CategoryPagination {
        private boolean enabled;
        private int itemsPerPage;
        private Map<String, ItemRange> itemRanges;
        private int previousButtonSlot;
        private CommonItem previousButtonItem;
        private int nextButtonSlot;
        private CommonItem nextButtonItem;
        private int pageInfoSlot;
        private CommonItem pageInfoItem;
        
        public CategoryPagination() {
            this.enabled = true;
            this.itemsPerPage = 28;
            this.itemRanges = new HashMap<>();
            this.previousButtonSlot = 36;
            this.nextButtonSlot = 44;
            this.pageInfoSlot = 40;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public int getItemsPerPage() {
            return itemsPerPage;
        }
        
        public void setItemsPerPage(int itemsPerPage) {
            this.itemsPerPage = itemsPerPage;
        }
        
        public void addItemRange(String rangeName, int start, int end) {
            itemRanges.put(rangeName, new ItemRange(start, end));
        }
        
        public Map<String, ItemRange> getItemRanges() {
            return itemRanges;
        }
        
        public int getPreviousButtonSlot() {
            return previousButtonSlot;
        }
        
        public void setPreviousButtonSlot(int previousButtonSlot) {
            this.previousButtonSlot = previousButtonSlot;
        }
        
        public CommonItem getPreviousButtonItem() {
            return previousButtonItem;
        }
        
        public void setPreviousButtonItem(CommonItem previousButtonItem) {
            this.previousButtonItem = previousButtonItem;
        }
        
        public int getNextButtonSlot() {
            return nextButtonSlot;
        }
        
        public void setNextButtonSlot(int nextButtonSlot) {
            this.nextButtonSlot = nextButtonSlot;
        }
        
        public CommonItem getNextButtonItem() {
            return nextButtonItem;
        }
        
        public void setNextButtonItem(CommonItem nextButtonItem) {
            this.nextButtonItem = nextButtonItem;
        }
        
        public int getPageInfoSlot() {
            return pageInfoSlot;
        }
        
        public void setPageInfoSlot(int pageInfoSlot) {
            this.pageInfoSlot = pageInfoSlot;
        }
        
        public CommonItem getPageInfoItem() {
            return pageInfoItem;
        }
        
        public void setPageInfoItem(CommonItem pageInfoItem) {
            this.pageInfoItem = pageInfoItem;
        }
        
        /**
         * 计算分类物品的槽位分布
         * @param guiSize GUI大小
         * @return 槽位映射表
         */
        public Map<Integer, Integer> calculateItemSlots(int guiSize) {
            Map<Integer, Integer> slotMap = new HashMap<>();
            
            // 如果有自定义的物品范围，则使用自定义范围
            if (!itemRanges.isEmpty()) {
                int index = 0;
                for (Map.Entry<String, ItemRange> entry : itemRanges.entrySet()) {
                    ItemRange range = entry.getValue();
                    for (int slot = range.getStart(); slot <= range.getEnd(); slot++) {
                        // 确保槽位在GUI范围内
                        if (slot >= 0 && slot < guiSize) {
                            slotMap.put(index, slot);
                            index++;
                        }
                    }
                }
                return slotMap;
            }
            
            // 否则使用默认的槽位计算逻辑
            int slot = 10; // 从第二行第二列开始
            int index = 0;
            
            while (index < itemsPerPage) {
                // 跳过边框位置
                if (slot % 9 == 0) {
                    slot++; // 跳过左边缘
                }
                if (slot % 9 == 8) {
                    slot += 2; // 跳过右边缘
                }
                
                // 如果已经到了底部导航栏，则停止
                if (slot >= guiSize - 9) {
                    break;
                }
                
                slotMap.put(index, slot);
                index++;
                slot++;
            }
            
            return slotMap;
        }
    }
    
    /**
     * 物品范围类，存储物品的起始和结束槽位
     */
    public static class ItemRange {
        private int start;
        private int end;
        
        public ItemRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
        
        public int getStart() {
            return start;
        }
        
        public int getEnd() {
            return end;
        }
    }
} 