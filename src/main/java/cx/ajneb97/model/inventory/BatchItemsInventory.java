package cx.ajneb97.model.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用于管理批量添加物品的GUI界面
 */
public class BatchItemsInventory {
    
    private Player player;
    private Inventory inventory;
    private String title;
    private boolean isOpen;
    private List<ItemStack> addedItems;
    private Map<Integer, ItemStack> slotItemMap;
    private String categoryName; // 指定的分类名称
    
    /**
     * 创建一个批量物品添加界面
     * @param player 玩家
     * @param inventory 物品栏
     * @param title 标题
     * @param categoryName 分类名称
     */
    public BatchItemsInventory(Player player, Inventory inventory, String title, String categoryName) {
        this.player = player;
        this.inventory = inventory;
        this.title = title;
        this.isOpen = true;
        this.addedItems = new ArrayList<>();
        this.slotItemMap = new HashMap<>();
        this.categoryName = categoryName;
    }
    
    /**
     * 获取玩家
     * @return 玩家
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * 获取物品栏
     * @return 物品栏
     */
    public Inventory getInventory() {
        return inventory;
    }
    
    /**
     * 获取标题
     * @return 标题
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * 检查界面是否打开
     * @return 是否打开
     */
    public boolean isOpen() {
        return isOpen;
    }
    
    /**
     * 设置界面开关状态
     * @param open 是否打开
     */
    public void setOpen(boolean open) {
        isOpen = open;
    }
    
    /**
     * 添加物品到已添加物品列表
     * @param item 物品
     */
    public void addItem(ItemStack item) {
        addedItems.add(item.clone());
    }
    
    /**
     * 获取所有添加的物品
     * @return 物品列表
     */
    public List<ItemStack> getAddedItems() {
        return addedItems;
    }
    
    /**
     * 清除所有已添加的物品
     */
    public void clearItems() {
        addedItems.clear();
    }
    
    /**
     * 设置指定槽位的物品
     * @param slot 槽位
     * @param item 物品
     */
    public void setSlotItem(int slot, ItemStack item) {
        slotItemMap.put(slot, item);
    }
    
    /**
     * 获取指定槽位的物品
     * @param slot 槽位
     * @return 物品
     */
    public ItemStack getSlotItem(int slot) {
        return slotItemMap.get(slot);
    }
    
    /**
     * 移除指定槽位的物品
     * @param slot 槽位
     */
    public void removeSlotItem(int slot) {
        slotItemMap.remove(slot);
    }
    
    /**
     * 获取所有槽位物品
     * @return 槽位物品映射
     */
    public Map<Integer, ItemStack> getSlotItemMap() {
        return slotItemMap;
    }
    
    /**
     * 获取分类名称
     * @return 分类名称
     */
    public String getCategoryName() {
        return categoryName;
    }
    
    /**
     * 设置分类名称
     * @param categoryName 分类名称
     */
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
} 