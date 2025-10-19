package cx.ajneb97.model.structure;

import cx.ajneb97.model.item.CommonItem;

import java.util.ArrayList;
import java.util.List;

public class Discovery {

    private String id;
    private String name;
    private List<String> description;
    private DiscoveredOn discoveredOn;
    private String categoryName;
    private CommonItem customLevelBlockedItem;
    private CommonItem customLevelUnlockedItem;
    private List<String> customRewards;
    private int clickActionsCooldown;
    private List<String> clickActions;
    private String enchantmentId; // 附魔ID，格式为 namespace:key

    public Discovery(){
        this.description = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public DiscoveredOn getDiscoveredOn() {
        return discoveredOn;
    }

    public void setDiscoveredOn(DiscoveredOn discoveredOn) {
        this.discoveredOn = discoveredOn;
        
        // 如果是附魔发现，自动设置enchantmentId
        if (discoveredOn != null && 
            discoveredOn.getType() == DiscoveredOn.DiscoveredOnType.ENCHANTMENT_DISCOVER) {
            this.enchantmentId = discoveredOn.getEnchantmentId();
        }
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public CommonItem getCustomLevelBlockedItem() {
        return customLevelBlockedItem;
    }

    public void setCustomLevelBlockedItem(CommonItem customLevelBlockedItem) {
        this.customLevelBlockedItem = customLevelBlockedItem;
    }

    public CommonItem getCustomLevelUnlockedItem() {
        return customLevelUnlockedItem;
    }

    public void setCustomLevelUnlockedItem(CommonItem customLevelUnlockedItem) {
        this.customLevelUnlockedItem = customLevelUnlockedItem;
    }

    public List<String> getCustomRewards() {
        return customRewards;
    }

    public void setCustomRewards(List<String> customRewards) {
        this.customRewards = customRewards;
    }

    public int getClickActionsCooldown() {
        return clickActionsCooldown;
    }

    public void setClickActionsCooldown(int clickActionsCooldown) {
        this.clickActionsCooldown = clickActionsCooldown;
    }

    public List<String> getClickActions() {
        return clickActions;
    }

    public void setClickActions(List<String> clickActions) {
        this.clickActions = clickActions;
    }

    /**
     * 获取附魔ID
     * @return 附魔ID
     */
    public String getEnchantmentId() {
        return enchantmentId;
    }

    /**
     * 设置附魔ID
     * @param enchantmentId 附魔ID
     */
    public void setEnchantmentId(String enchantmentId) {
        this.enchantmentId = enchantmentId;
    }
    
    /**
     * 检查是否是附魔发现
     * @return 是否是附魔发现
     */
    public boolean isEnchantmentDiscovery() {
        return discoveredOn != null && 
               discoveredOn.getType() == DiscoveredOn.DiscoveredOnType.ENCHANTMENT_DISCOVER &&
               enchantmentId != null;
    }
}
