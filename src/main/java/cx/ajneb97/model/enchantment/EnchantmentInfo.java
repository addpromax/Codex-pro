package cx.ajneb97.model.enchantment;

import java.util.ArrayList;
import java.util.List;

/**
 * 存储附魔的详细信息
 */
public class EnchantmentInfo {
    
    private String id;                  // 附魔ID，格式为 namespace:key
    private String namespace;           // 命名空间，如 minecraft 或 aiyatsbus
    private String key;                 // 键名，如 sharpness
    private String name;                // 显示名称
    private int maxLevel;               // 最大等级
    private String rarity;              // 稀有度
    private List<String> description;   // 描述
    private List<String> applicableItems; // 适用物品
    private List<String> conflicts;     // 冲突附魔
    
    public EnchantmentInfo(String id) {
        this.id = id;
        
        // 解析命名空间和键名
        if (id.contains(":")) {
            String[] parts = id.split(":", 2);
            this.namespace = parts[0];
            this.key = parts[1];
        } else {
            this.namespace = "minecraft";
            this.key = id;
        }
        
        this.description = new ArrayList<>();
        this.applicableItems = new ArrayList<>();
        this.conflicts = new ArrayList<>();
    }
    
    /**
     * 获取附魔ID
     * @return 附魔ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取命名空间
     * @return 命名空间
     */
    public String getNamespace() {
        return namespace;
    }
    
    /**
     * 获取键名
     * @return 键名
     */
    public String getKey() {
        return key;
    }
    
    /**
     * 获取显示名称
     * @return 显示名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 设置显示名称
     * @param name 显示名称
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * 获取最大等级
     * @return 最大等级
     */
    public int getMaxLevel() {
        return maxLevel;
    }
    
    /**
     * 设置最大等级
     * @param maxLevel 最大等级
     */
    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }
    
    /**
     * 获取稀有度
     * @return 稀有度
     */
    public String getRarity() {
        return rarity;
    }
    
    /**
     * 设置稀有度
     * @param rarity 稀有度
     */
    public void setRarity(String rarity) {
        this.rarity = rarity;
    }
    
    /**
     * 获取描述
     * @return 描述
     */
    public List<String> getDescription() {
        return description;
    }
    
    /**
     * 设置描述
     * @param description 描述
     */
    public void setDescription(List<String> description) {
        this.description = description;
    }
    
    /**
     * 添加描述行
     * @param line 描述行
     */
    public void addDescription(String line) {
        this.description.add(line);
    }
    
    /**
     * 获取适用物品
     * @return 适用物品
     */
    public List<String> getApplicableItems() {
        return applicableItems;
    }
    
    /**
     * 设置适用物品
     * @param applicableItems 适用物品
     */
    public void setApplicableItems(List<String> applicableItems) {
        this.applicableItems = applicableItems;
    }
    
    /**
     * 添加适用物品
     * @param item 物品
     */
    public void addApplicableItem(String item) {
        this.applicableItems.add(item);
    }
    
    /**
     * 获取冲突附魔
     * @return 冲突附魔
     */
    public List<String> getConflicts() {
        return conflicts;
    }
    
    /**
     * 设置冲突附魔
     * @param conflicts 冲突附魔
     */
    public void setConflicts(List<String> conflicts) {
        this.conflicts = conflicts;
    }
    
    /**
     * 添加冲突附魔
     * @param enchant 附魔
     */
    public void addConflict(String enchant) {
        this.conflicts.add(enchant);
    }
} 