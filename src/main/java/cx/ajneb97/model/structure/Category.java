package cx.ajneb97.model.structure;

import cx.ajneb97.model.item.CommonItem;

import java.util.ArrayList;

public class Category {

    private String name;
    private CommonItem categoryItem;
    private CommonItem defaultLevelBlockedItem;
    private CommonItem defaultLevelUnlockedItem;
    private ArrayList<Discovery> discoveries;
    private ArrayList<String> defaultRewardsPerDiscovery;
    private ArrayList<String> defaultRewardsAllDiscoveries;

    public Category(){
        this.discoveries = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CommonItem getCategoryItem() {
        return categoryItem;
    }

    public void setCategoryItem(CommonItem categoryItem) {
        this.categoryItem = categoryItem;
    }

    public CommonItem getDefaultLevelBlockedItem() {
        return defaultLevelBlockedItem;
    }

    public void setDefaultLevelBlockedItem(CommonItem defaultLevelBlockedItem) {
        this.defaultLevelBlockedItem = defaultLevelBlockedItem;
    }

    public CommonItem getDefaultLevelUnlockedItem() {
        return defaultLevelUnlockedItem;
    }

    public void setDefaultLevelUnlockedItem(CommonItem defaultLevelUnlockedItem) {
        this.defaultLevelUnlockedItem = defaultLevelUnlockedItem;
    }

    public ArrayList<Discovery> getDiscoveries() {
        return discoveries;
    }

    public void setDiscoveries(ArrayList<Discovery> discoveries) {
        this.discoveries = discoveries;
    }

    /**
     * 添加一个发现项到分类中
     * @param discovery 要添加的发现项
     */
    public void addDiscovery(Discovery discovery) {
        if (this.discoveries == null) {
            this.discoveries = new ArrayList<>();
    }

        // 检查是否已存在相同ID的发现项
        for (Discovery existingDiscovery : this.discoveries) {
            if (existingDiscovery.getId().equals(discovery.getId())) {
                return; // 如果已存在，则不添加
            }
        }
        
        this.discoveries.add(discovery);
    }

    public ArrayList<String> getDefaultRewardsPerDiscovery() {
        return defaultRewardsPerDiscovery;
    }

    public void setDefaultRewardsPerDiscovery(ArrayList<String> defaultRewardsPerDiscovery) {
        this.defaultRewardsPerDiscovery = defaultRewardsPerDiscovery;
    }

    public ArrayList<String> getDefaultRewardsAllDiscoveries() {
        return defaultRewardsAllDiscoveries;
    }

    public void setDefaultRewardsAllDiscoveries(ArrayList<String> defaultRewardsAllDiscoveries) {
        this.defaultRewardsAllDiscoveries = defaultRewardsAllDiscoveries;
    }

    public Discovery getDiscovery(String discoveryName){
        for(Discovery discovery : discoveries){
            if(discovery.getId().equals(discoveryName)){
                return discovery;
            }
        }
        return null;
    }
}
