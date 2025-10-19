package cx.ajneb97.managers;

import cx.ajneb97.Codex;
import cx.ajneb97.config.CommonConfig;
import cx.ajneb97.model.structure.Category;
import cx.ajneb97.model.structure.Discovery;
import cx.ajneb97.model.structure.DiscoveredOn;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;

public class CategoryManager {

	private Codex plugin;
	private ArrayList<Category> categories;
	public CategoryManager(Codex plugin) {
		this.plugin = plugin;
		this.categories = new ArrayList<>();
	}

	public ArrayList<Category> getCategories() {
		return categories;
	}

	public void setCategories(ArrayList<Category> categories) {
		this.categories = categories;
		// 刷新 discovery 缓存索引
		plugin.getDiscoveryManager().rebuildCache();
	}

	public Category getCategory(String name){
		for(Category c : categories){
			if(c.getName().equals(name)){
				return c;
			}
		}
		return null;
	}

	public int getTotalDiscoveries(String categoryName){
		Category category = getCategory(categoryName);
		if(category == null){
			return 0;
		}
		return category.getDiscoveries().size();
	}

	public int getTotalDiscoveries(){
		int total = 0;
		for(Category category : categories){
			total = total+category.getDiscoveries().size();
		}
		return total;
	}


	/**
	 * 添加新的分类
	 * @param category 要添加的分类
	 */
	public void addCategory(Category category) {
		// 检查是否已存在同名分类
		if (getCategory(category.getName()) != null) {
			plugin.getLogger().warning("分类 " + category.getName() + " 已存在，跳过添加");
			return; // 如果已存在，则不添加
		}

		this.categories.add(category);
		
		// 保存分类到文件
		saveCategory(category);
		
		// 重建缓存以确保新分类的发现项可以被触发
		plugin.getDiscoveryManager().rebuildCache();
		
		// 如果使用H2数据库，同步分类信息到数据库中
	}
	
	/**
	 * 保存分类到文件
	 * @param category 要保存的分类
	 */
	public void saveCategory(Category category) {
		try {
			String categoryName = category.getName();
			String folderName = "categories";
			
			// 确保目录存在
			File folder = new File(plugin.getDataFolder(), folderName);
			if (!folder.exists()) {
				folder.mkdirs();
			}
			
			// 创建或获取配置文件
			CommonConfig commonConfig = new CommonConfig(categoryName + ".yml", plugin, folderName, true);
			commonConfig.registerConfig();
			
			FileConfiguration config = commonConfig.getConfig();
			
			// 保存分类基本信息
			config.set("config.inventory_items.category.id", category.getCategoryItem().getId());
			config.set("config.inventory_items.category.name", category.getCategoryItem().getName());
			config.set("config.inventory_items.category.lore", category.getCategoryItem().getLore());
			
			config.set("config.inventory_items.discovery_unlocked.id", category.getDefaultLevelUnlockedItem().getId());
			config.set("config.inventory_items.discovery_unlocked.name", category.getDefaultLevelUnlockedItem().getName());
			config.set("config.inventory_items.discovery_unlocked.lore", category.getDefaultLevelUnlockedItem().getLore());
			
			config.set("config.inventory_items.discovery_blocked.id", category.getDefaultLevelBlockedItem().getId());
			config.set("config.inventory_items.discovery_blocked.name", category.getDefaultLevelBlockedItem().getName());
			config.set("config.inventory_items.discovery_blocked.lore", category.getDefaultLevelBlockedItem().getLore());
			
			config.set("config.rewards.per_discovery", category.getDefaultRewardsPerDiscovery());
			config.set("config.rewards.all_discoveries", category.getDefaultRewardsAllDiscoveries());
			
			// 保存发现项
			for (Discovery discovery : category.getDiscoveries()) {
				String discoveryPath = "discoveries." + discovery.getId();
				
				config.set(discoveryPath + ".name", discovery.getName());
				config.set(discoveryPath + ".description", discovery.getDescription());
				
				// 保存发现条件
				DiscoveredOn discoveredOn = discovery.getDiscoveredOn();
				if (discoveredOn != null) {
					config.set(discoveryPath + ".discovered_on.type", discoveredOn.getType().name());
					
					String valuePath = discoveryPath + ".discovered_on.value";
					
					// 根据不同的发现类型保存不同的值
					switch (discoveredOn.getType()) {
						case MOB_KILL:
							config.set(valuePath + ".mob_name", discoveredOn.getMobName());
							config.set(valuePath + ".mob_type", discoveredOn.getMobType());
							break;
						case WORLDGUARD_REGION:
							config.set(valuePath + ".region_name", discoveredOn.getRegionName());
							break;
						case ITEM_OBTAIN:
							config.set(valuePath + ".item_type", discoveredOn.getItemType());
							if (discoveredOn.getCustomModelData() != null) {
								config.set(valuePath + ".custom_model_data", discoveredOn.getCustomModelData());
							}
							config.set(valuePath + ".components", discoveredOn.getComponents());
							config.set(valuePath + ".craft_engine_id", discoveredOn.getCraftEngineId());
							config.set(valuePath + ".custom_fishing_id", discoveredOn.getCustomFishingId());
							config.set(valuePath + ".itemsadder_item_id", discoveredOn.getItemsAdderId());
							config.set(valuePath + ".mmoitems_type", discoveredOn.getMmoItemsType());
							config.set(valuePath + ".mmoitems_id", discoveredOn.getMmoItemsId());
							break;
						case COMMAND_RUN:
							config.set(valuePath + ".command", discoveredOn.getCommand());
							break;
						case ENCHANTMENT_DISCOVER:
							config.set(valuePath + ".enchantment_id", discoveredOn.getEnchantmentId());
							break;
						case FISHING:
							config.set(valuePath + ".custom_fishing_id", discoveredOn.getCustomFishingId());
							break;
						case MYTHIC_MOB_KILL:
						case ELITE_MOB_KILL:
							config.set(valuePath + ".mob_type", discoveredOn.getMobType());
							break;
					}
				}
				
				// 保存自定义奖励
				if (discovery.getCustomRewards() != null) {
					config.set(discoveryPath + ".rewards", discovery.getCustomRewards());
				}
				
				// 保存点击动作
				if (discovery.getClickActions() != null) {
					config.set(discoveryPath + ".click_actions", discovery.getClickActions());
				}
				
				// 保存点击动作冷却时间
				if (discovery.getClickActionsCooldown() > 0) {
					config.set(discoveryPath + ".click_actions_cooldown", discovery.getClickActionsCooldown());
				}
				
				// 保存自定义物品
				if (discovery.getCustomLevelUnlockedItem() != null) {
					config.set(discoveryPath + ".inventory_items.discovery_unlocked.id", discovery.getCustomLevelUnlockedItem().getId());
					config.set(discoveryPath + ".inventory_items.discovery_unlocked.name", discovery.getCustomLevelUnlockedItem().getName());
					config.set(discoveryPath + ".inventory_items.discovery_unlocked.lore", discovery.getCustomLevelUnlockedItem().getLore());
				}
				
				if (discovery.getCustomLevelBlockedItem() != null) {
					config.set(discoveryPath + ".inventory_items.discovery_blocked.id", discovery.getCustomLevelBlockedItem().getId());
					config.set(discoveryPath + ".inventory_items.discovery_blocked.name", discovery.getCustomLevelBlockedItem().getName());
					config.set(discoveryPath + ".inventory_items.discovery_blocked.lore", discovery.getCustomLevelBlockedItem().getLore());
				}
			}
			
			// 保存配置
			commonConfig.saveConfig();
			
			plugin.getLogger().info("分类 " + categoryName + " 已保存到文件");
		} catch (Exception e) {
			plugin.getLogger().warning("保存分类 " + category.getName() + " 时出错: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
