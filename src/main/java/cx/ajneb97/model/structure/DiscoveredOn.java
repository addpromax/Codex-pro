package cx.ajneb97.model.structure;

public class DiscoveredOn {

    private DiscoveredOnType type;
    private String mobType;
    private String mobName;
    private String regionName;
    private String itemType;
    private String command;
    private Integer customModelData;
    private String components;
    private String craftEngineId;
    private String itemsAdderId;
    private String mmoItemsType;
    private String mmoItemsId;
    private String enchantmentId;
    private String customFishingId;

    public DiscoveredOn(DiscoveredOnType type) {
        this.type = type;
    }

    public DiscoveredOnType getType() {
        return type;
    }

    public void setType(DiscoveredOnType type) {
        this.type = type;
    }

    public String getMobType() {
        return mobType;
    }

    public void setMobType(String mobType) {
        this.mobType = mobType;
    }

    public String getMobName() {
        return mobName;
    }

    public void setMobName(String mobName) {
        this.mobName = mobName;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Integer getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(Integer customModelData) {
        this.customModelData = customModelData;
    }

    public String getComponents() {
        return components;
    }

    public void setComponents(String components) {
        this.components = components;
    }

    public String getCraftEngineId() {
        return craftEngineId;
    }

    public void setCraftEngineId(String craftEngineId) {
        this.craftEngineId = craftEngineId;
    }

    public String getItemsAdderId() {
        return itemsAdderId;
    }

    public void setItemsAdderId(String itemsAdderId) {
        this.itemsAdderId = itemsAdderId;
    }

    public String getMmoItemsType() {
        return mmoItemsType;
    }

    public void setMmoItemsType(String mmoItemsType) {
        this.mmoItemsType = mmoItemsType;
    }

    public String getMmoItemsId() {
        return mmoItemsId;
    }

    public void setMmoItemsId(String mmoItemsId) {
        this.mmoItemsId = mmoItemsId;
    }

    public String getEnchantmentId() {
        return enchantmentId;
    }

    public void setEnchantmentId(String enchantmentId) {
        this.enchantmentId = enchantmentId;
    }

    public String getCustomFishingId() {
        return customFishingId;
    }

    public void setCustomFishingId(String customFishingId) {
        this.customFishingId = customFishingId;
    }

    public enum DiscoveredOnType{
        MOB_KILL,
        MYTHIC_MOB_KILL,
        ELITE_MOB_KILL,
        WORLDGUARD_REGION,
        ITEM_OBTAIN,
        COMMAND_RUN,
        ENCHANTMENT_DISCOVER,
        FISHING  // 钓鱼触发类型（通过CustomFishingListener直接触发）
    }
}
