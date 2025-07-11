package cx.ajneb97.managers;

import cx.ajneb97.Codex;
import cx.ajneb97.model.data.PlayerDataCategory;
import cx.ajneb97.model.data.PlayerDataDiscovery;
import cx.ajneb97.model.internal.CommonVariable;
import cx.ajneb97.model.structure.Category;
import cx.ajneb97.model.structure.DiscoveredOn;
import cx.ajneb97.model.structure.Discovery;
import cx.ajneb97.utils.ActionUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import java.util.concurrent.ConcurrentHashMap;

import java.util.*;

public class DiscoveryManager {
    private Codex plugin;
    private final Map<DiscoveredOn.DiscoveredOnType,List<Discovery>> discoveryCache = new ConcurrentHashMap<>();
    public static final String CRAFT_ENGINE_ID_KEY = "craftengine:id";
    private final boolean itemsAdderPresent = Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;

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

    public boolean onDiscover(Player player,String categoryName,String discoveryName){
        Category category = plugin.getCategoryManager().getCategory(categoryName);
        Discovery discovery = category.getDiscovery(discoveryName);

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

        return true;
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
}
