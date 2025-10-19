package cx.ajneb97.managers;

import cx.ajneb97.Codex;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loot分组配置管理器
 * 管理CustomFishing的loot-conditions分组显示名称
 */
public class LootGroupConfigManager {
    
    private final Codex plugin;
    private final Map<String, String> groupNameCache = new ConcurrentHashMap<>();
    
    public LootGroupConfigManager(Codex plugin) {
        this.plugin = plugin;
        loadGroupNames();
    }
    
    /**
     * 加载分组名称配置
     */
    public void loadGroupNames() {
        groupNameCache.clear();
        
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("fishing_codex.loot_group_names");
        if (section != null) {
            for (String groupId : section.getKeys(false)) {
                String displayName = section.getString(groupId);
                if (displayName != null && !displayName.isEmpty()) {
                    groupNameCache.put(groupId, displayName);
                }
            }
            
            plugin.getLogger().info("[FISHING] 已加载 " + groupNameCache.size() + " 个loot分组名称配置");
        } else {
            plugin.getLogger().warning("[FISHING] 未找到loot_group_names配置，将使用默认分组名称");
            // 添加默认值
            groupNameCache.put("ocean_fish", "海水组");
            groupNameCache.put("river_fish", "淡水组");
            groupNameCache.put("lava_fish", "岩浆组");
        }
    }
    
    /**
     * 获取分组的显示名称
     * @param groupId loot分组ID（如ocean_fish）
     * @return 显示名称，如果未配置则返回格式化的ID
     */
    public String getGroupDisplayName(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            return "未知分组";
        }
        
        // 先检查缓存
        String displayName = groupNameCache.get(groupId);
        if (displayName != null) {
            return displayName;
        }
        
        // 如果没有配置，格式化ID作为显示名称
        return formatGroupId(groupId);
    }
    
    /**
     * 格式化分组ID为可读名称
     * 例如：ocean_fish -> Ocean Fish
     */
    private String formatGroupId(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            return groupId;
        }
        
        // 替换下划线为空格，并首字母大写
        String[] parts = groupId.split("_");
        StringBuilder formatted = new StringBuilder();
        
        for (String part : parts) {
            if (formatted.length() > 0) {
                formatted.append(" ");
            }
            if (!part.isEmpty()) {
                formatted.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    formatted.append(part.substring(1).toLowerCase());
                }
            }
        }
        
        return formatted.toString();
    }
    
    /**
     * 检查分组ID是否已配置
     */
    public boolean hasGroupName(String groupId) {
        return groupNameCache.containsKey(groupId);
    }
    
    /**
     * 获取所有已配置的分组
     */
    public Map<String, String> getAllGroupNames() {
        return new HashMap<>(groupNameCache);
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        loadGroupNames();
    }
}

