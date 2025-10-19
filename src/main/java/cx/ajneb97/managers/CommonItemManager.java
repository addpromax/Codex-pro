package cx.ajneb97.managers;

import cx.ajneb97.Codex;
import cx.ajneb97.model.internal.CommonVariable;
import cx.ajneb97.model.item.*;
import cx.ajneb97.utils.ItemUtils;
import cx.ajneb97.utils.OtherUtils;
import cx.ajneb97.utils.ServerVersion;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
// import org.bukkit.inventory.meta.components.CustomModelDataComponent; // Paper 1.21.4+ 功能，暂时注释

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CommonItemManager {

    private Codex plugin;
    public CommonItemManager(Codex plugin){
        this.plugin = plugin;
    }

    public CommonItem createCommonItemFromItemStack(ItemStack item){
        CommonItem commonItem = new CommonItem(item.getType().name());
        commonItem.setAmount(item.getAmount());

        if(item.getDurability() != 0) {
            commonItem.setDurability(item.getDurability());
        }

        ServerVersion serverVersion = Codex.serverVersion;
        if(item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if(meta.hasDisplayName()) {
                commonItem.setName(meta.getDisplayName().replace("§", "&"));
            }
            if(meta.hasLore()) {
                List<String> lore = new ArrayList<String>();
                for(String l : meta.getLore()) {
                    lore.add(l.replace("§", "&"));
                }
                commonItem.setLore(lore);
            }
            
            // 附魔信息处理 - 检查Aiyatsbus API可用性
            if(cx.ajneb97.api.AiyatsbusAPI.isAvailable()) {
                try {
                    Map<cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment, Integer> fixedEnchants = cc.polarastrum.aiyatsbus.core.AiyatsbusUtilsKt.getFixedEnchants(item);
                    java.util.List<String> enchantList = new java.util.ArrayList<>();
                    for (Map.Entry<cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment, Integer> entry : fixedEnchants.entrySet()) {
                        cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment enchant = entry.getKey();
                        int level = entry.getValue();
                        String enchantId = enchant.getId();
                        enchantList.add(enchantId+";"+level);
                    }
                    commonItem.setEnchants(enchantList);
                } catch (Exception e) {
                    plugin.getDebugManager().debug("提取附魔信息时发生错误: " + e.getMessage());
                    // 跳过附魔信息，继续处理其他属性
                }
            } else {
                plugin.getDebugManager().debug("Aiyatsbus API不可用，跳过附魔信息提取");
                // 不设置附魔信息，但继续处理其他属性
            }
            // 其余meta处理保持不变
            Set<ItemFlag> flags = meta.getItemFlags();
            if(flags != null && !flags.isEmpty()) {
                List<String> flagsList = new ArrayList<String>();
                for(ItemFlag flag : flags) {
                    flagsList.add(flag.name());
                }
                commonItem.setFlags(flagsList);
            }

            if(OtherUtils.isNew() && meta.hasCustomModelData()) {
                if(!serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_21_R3)){
                    commonItem.setCustomModelData(meta.getCustomModelData());
                }
            }

            // CustomModelDataComponent 功能仅在 Paper 1.21.4+ 版本可用
            if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_21_R4) && plugin.getDependencyManager().isPaper()){
                loadCustomModelDataFromReflection(item, commonItem);
            }

            if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_20_R4)){
                if(meta.isHideTooltip()){
                    commonItem.setHideTooltip(true);
                }
            }

            if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_21_R2)){
                if(meta.hasTooltipStyle()){
                    NamespacedKey key = meta.getTooltipStyle();
                    commonItem.setTooltipStyle(key.getNamespace()+":"+key.getKey());
                }
            }

            if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_21_R3)){
                if(meta.hasItemModel()){
                    NamespacedKey key = meta.getItemModel();
                    commonItem.setModel(key.getNamespace()+":"+key.getKey());
                }
            }

            if(meta instanceof LeatherArmorMeta) {
                LeatherArmorMeta meta2 = (LeatherArmorMeta) meta;
                commonItem.setColor(meta2.getColor().asRGB());
            }

            if(meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta meta2 = (EnchantmentStorageMeta) meta;
                Map<Enchantment, Integer> enchants = meta2.getStoredEnchants();
                List<String> enchantsList = new ArrayList<String>();
                for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                    enchantsList.add(entry.getKey().getName()+";"+entry.getValue().intValue());
                }
                if(!enchantsList.isEmpty()) {
                    commonItem.setBookEnchants(enchantsList);
                }
            }
        }

        List<String> nbtList = ItemUtils.getNBT(plugin,item);
        if(!nbtList.isEmpty()) {
            commonItem.setNbt(nbtList);
        }

        commonItem.setAttributes(ItemUtils.getAttributes(plugin,item));
        commonItem.setSkullData(ItemUtils.getSkullData(item));
        commonItem.setPotionData(ItemUtils.getPotionData(item));
        commonItem.setFireworkData(ItemUtils.getFireworkData(item));
        commonItem.setBannerData(ItemUtils.getBannerData(item));
        commonItem.setBookData(ItemUtils.getBookData(item));
        commonItem.setTrimData(ItemUtils.getArmorTrimData(item));

        return commonItem;
    }
    public ItemStack createItemFromCommonItem(CommonItem commonItem, Player player){
        ItemStack item = ItemUtils.createItemFromID(commonItem.getId());
        item.setAmount(commonItem.getAmount());

        short durability = commonItem.getDurability();
        if(durability != 0) {
            item.setDurability(durability);
        }

        //MAIN META
        ItemMeta meta = item.getItemMeta();
        String name = commonItem.getName();
        if(name != null){
            name = OtherUtils.replaceGlobalVariables(name,player,plugin);
            MessagesManager.setItemDisplayName(meta, name);
        }

        List<String> lore = commonItem.getLore();
        if(lore != null) {
            List<String> loreCopy = new ArrayList<String>(lore);
            for(int i=0;i<loreCopy.size();i++) {
                String line = OtherUtils.replaceGlobalVariables(loreCopy.get(i),player,plugin);
                loreCopy.set(i, line);
            }
            MessagesManager.setItemLore(meta, loreCopy);
        }

        int customModelData = commonItem.getCustomModelData();
        if(customModelData != 0) {
            meta.setCustomModelData(customModelData);
        }

        ServerVersion serverVersion = Codex.serverVersion;
        // CustomModelDataComponent 功能仅在 Paper 1.21.4+ 版本可用
        if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_21_R4) && plugin.getDependencyManager().isPaper()){
            CommonItemCustomModelComponentData kitItemCustomModelComponentData = commonItem.getCustomModelComponentData();
            if(kitItemCustomModelComponentData != null){
                setCustomModelDataFromReflection(meta, kitItemCustomModelComponentData);
            }
        }

        // 还原附魔信息（全部用Aiyatsbus API）
        // 附魔处理 - 检查Aiyatsbus API可用性
        List<String> enchants = commonItem.getEnchants();
        if(enchants != null && cx.ajneb97.api.AiyatsbusAPI.isAvailable()) {
            try {
                for(int i=0;i<enchants.size();i++) {
                    String[] sep = enchants.get(i).split(";");
                    String enchantId = sep[0];
                    int enchantLevel = Integer.valueOf(sep[1]);
                    cc.polarastrum.aiyatsbus.core.AiyatsbusEnchantment enchant = cc.polarastrum.aiyatsbus.core.AiyatsbusUtilsKt.aiyatsbusEt(enchantId);
                    if(enchant != null) {
                        cc.polarastrum.aiyatsbus.core.AiyatsbusUtilsKt.addEt(item, enchant, enchantLevel);
                    }
                }
            } catch (Exception e) {
                plugin.getDebugManager().debug("添加附魔时发生错误: " + e.getMessage());
                // 跳过附魔添加，但继续处理物品的其他属性
            }
        } else if(enchants != null && !enchants.isEmpty()) {
            plugin.getDebugManager().debug("Aiyatsbus API不可用，跳过附魔添加 (物品有 " + enchants.size() + " 个附魔)");
        }

        if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_20_R4)){
            if(commonItem.isHideTooltip()){
                meta.setHideTooltip(true);
            }
        }

        if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_21_R2)){
            String tooltipStyle = commonItem.getTooltipStyle();
            if(tooltipStyle != null){
                String[] sep = tooltipStyle.split(":");
                meta.setTooltipStyle(new NamespacedKey(sep[0],sep[1]));
            }
        }

        if(serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_21_R3)){
            String model = commonItem.getModel();
            if(model != null){
                String[] sep = model.split(":");
                meta.setItemModel(new NamespacedKey(sep[0],sep[1]));
            }
        }

        item.setItemMeta(meta);

        //OTHER META
        int color = commonItem.getColor();
        if(color != 0) {
            LeatherArmorMeta meta2 = (LeatherArmorMeta) item.getItemMeta();
            meta2.setColor(Color.fromRGB(color));
            item.setItemMeta(meta2);
        }

        List<String> bookEnchants = commonItem.getBookEnchants();
        if(bookEnchants != null && !bookEnchants.isEmpty()) {
            EnchantmentStorageMeta meta2 = (EnchantmentStorageMeta) item.getItemMeta();
            for(int i=0;i<bookEnchants.size();i++) {
                String[] sep = bookEnchants.get(i).split(";");
                String enchantName = sep[0];
                int level = Integer.valueOf(sep[1]);
                meta2.addStoredEnchant(Enchantment.getByName(enchantName), level, true);
            }
            item.setItemMeta(meta2);
        }

        //ADVANCED DATA
        CommonItemSkullData skullData = commonItem.getSkullData();
        ItemUtils.setSkullData(item, skullData, null);

        CommonItemPotionData potionData = commonItem.getPotionData();
        ItemUtils.setPotionData(item, potionData);

        CommonItemFireworkData fireworkData = commonItem.getFireworkData();
        ItemUtils.setFireworkData(item, fireworkData);

        CommonItemBannerData bannerData = commonItem.getBannerData();
        ItemUtils.setBannerData(item, bannerData);

        CommonItemBookData bookData = commonItem.getBookData();
        ItemUtils.setBookData(item, bookData);

        CommonItemTrimData trimData = commonItem.getTrimData();
        ItemUtils.setArmorTrimData(item, trimData);

        List<String> attributes = commonItem.getAttributes();
        item = ItemUtils.setAttributes(plugin,item, attributes);


        //Item Flags
        meta = item.getItemMeta();
        List<String> flags = commonItem.getFlags();
        if(flags != null) {
            for(String flag : flags) {
                if (flag.equals("HIDE_ATTRIBUTES") && plugin.getDependencyManager().isPaper() &&
                        serverVersion.serverVersionGreaterEqualThan(serverVersion, ServerVersion.v1_21_R1)) {
                    //Fix PAPER HIDE_ATTRIBUTES
                    ItemUtils.addDummyAttribute(meta,plugin);
                }
                meta.addItemFlags(ItemFlag.valueOf(flag));
            }
        }
        item.setItemMeta(meta);

        if(!serverVersion.serverVersionGreaterEqualThan(serverVersion,ServerVersion.v1_20_R4)){
            List<String> nbtList = commonItem.getNbt();
            item = ItemUtils.setNBT(plugin,item, nbtList);
        }

        return item;
    }

    public void saveCommonItemOnConfig(CommonItem item,FileConfiguration config,String path){
        config.set(path+".id", item.getId());
        config.set(path+".name", item.getName());
        config.set(path+".amount", item.getAmount());
        if(item.getDurability() != 0) {
            config.set(path+".durability", item.getDurability());
        }
        if(item.getLore() != null && !item.getLore().isEmpty()) {
            config.set(path+".lore", item.getLore());
        }
        if(item.getEnchants() != null && !item.getEnchants().isEmpty()) {
            config.set(path+".enchants", item.getEnchants());
        }
        if(item.getFlags() != null && !item.getFlags().isEmpty()) {
            config.set(path+".item_flags", item.getFlags());
        }

        if(item.getCustomModelData() != 0) {
            config.set(path+".custom_model_data", item.getCustomModelData());
        }

        CommonItemCustomModelComponentData customModelComponentData = item.getCustomModelComponentData();
        if(customModelComponentData != null){
            if(!customModelComponentData.getFlags().isEmpty()) config.set(path+".custom_model_component_data.flags",customModelComponentData.getFlags());
            if(!customModelComponentData.getFloats().isEmpty()) config.set(path+".custom_model_component_data.floats",customModelComponentData.getFloats());
            if(!customModelComponentData.getColors().isEmpty()) config.set(path+".custom_model_component_data.colors",customModelComponentData.getColors());
            if(!customModelComponentData.getStrings().isEmpty()) config.set(path+".custom_model_component_data.strings",customModelComponentData.getStrings());
        }

        if(item.isHideTooltip()){
            config.set(path+".hide_tooltip", true);
        }
        if(item.getTooltipStyle() != null){
            config.set(path+".tooltip_style",item.getTooltipStyle());
        }
        if(item.getModel() != null){
            config.set(path+".model",item.getModel());
        }

        if(item.getColor() != 0) {
            config.set(path+".color", item.getColor());
        }
        if(item.getNbt() != null && !item.getNbt().isEmpty()) {
            config.set(path+".nbt", item.getNbt());
        }
        if(item.getAttributes() != null && !item.getAttributes().isEmpty()) {
            config.set(path+".attributes", item.getAttributes());
        }
        if(item.getBookEnchants() != null && !item.getBookEnchants().isEmpty()) {
            config.set(path+".book_enchants", item.getBookEnchants());
        }

        CommonItemSkullData skullData = item.getSkullData();
        if(skullData != null) {
            config.set(path+".skull_data.texture", skullData.getTexture());
            config.set(path+".skull_data.id", skullData.getId());
            config.set(path+".skull_data.owner", skullData.getOwner());
        }

        CommonItemPotionData potionData = item.getPotionData();
        if(potionData != null) {
            if(potionData.getPotionEffects() != null && !potionData.getPotionEffects().isEmpty()) {
                config.set(path+".potion_data.effects", potionData.getPotionEffects());
            }
            config.set(path+".potion_data.extended", potionData.isExtended());
            config.set(path+".potion_data.upgraded", potionData.isUpgraded());
            config.set(path+".potion_data.type", potionData.getPotionType());
            if(potionData.getPotionColor() != 0) {
                config.set(path+".potion_data.color", potionData.getPotionColor());
            }
        }

        CommonItemFireworkData fireworkData = item.getFireworkData();
        if(fireworkData != null) {
            if(fireworkData.getFireworkRocketEffects() != null && !fireworkData.getFireworkRocketEffects().isEmpty()) {
                config.set(path+".firework_data.rocket_effects", fireworkData.getFireworkRocketEffects());
            }
            config.set(path+".firework_data.star_effect", fireworkData.getFireworkStarEffect());
            if(fireworkData.getFireworkPower() != 0) {
                config.set(path+".firework_data.power", fireworkData.getFireworkPower());
            }
        }

        CommonItemBannerData bannerData = item.getBannerData();
        if(bannerData != null) {
            if(bannerData.getPatterns() != null && !bannerData.getPatterns().isEmpty()) {
                config.set(path+".banner_data.patterns", bannerData.getPatterns());
            }
            config.set(path+".banner_data.base_color", bannerData.getBaseColor());
        }

        CommonItemBookData bookData = item.getBookData();
        if(bookData != null) {
            config.set(path+".book_data.author", bookData.getAuthor());
            config.set(path+".book_data.title", bookData.getTitle());
            config.set(path+".book_data.pages", bookData.getPages());
            config.set(path+".book_data.generation", bookData.getGeneration());
        }

        CommonItemTrimData trimData = item.getTrimData();
        if(trimData != null){
            config.set(path+".trim_data.pattern", trimData.getPattern());
            config.set(path+".trim_data.material", trimData.getMaterial());
        }
    }

    public CommonItem getCommonItemFromConfig(FileConfiguration config, String path){
        String id = config.getString(path+".id");
        String name = config.contains(path+".name") ? config.getString(path+".name") : null;
        List<String> lore = config.contains(path+".lore") ? config.getStringList(path+".lore") : null;
        int amount = config.contains(path+".amount") ? config.getInt(path+".amount") : 1;
        short durability = config.contains(path+".durability") ? (short) config.getInt(path+".durability") : 0;
        int customModelData = config.contains(path+".custom_model_data") ? config.getInt(path+".custom_model_data") : 0;
        int color = config.contains(path+".color") ? config.getInt(path+".color") : 0;
        List<String> enchants = config.contains(path+".enchants") ? config.getStringList(path+".enchants") : null;
        List<String> flags = config.contains(path+".item_flags") ? config.getStringList(path+".item_flags") : null;
        List<String> bookEnchants = config.contains(path+".book_enchants") ? config.getStringList(path+".book_enchants") : null;
        List<String> nbtList = config.contains(path+".nbt") ? config.getStringList(path+".nbt") : null;
        List<String> attributes = config.contains(path+".attributes") ? config.getStringList(path+".attributes") : null;

        CommonItemCustomModelComponentData customModelComponentData = null;
        if(config.contains(path+".custom_model_component_data")) {
            List<String> cFlags = new ArrayList<>();
            List<String> cFloats = new ArrayList<>();
            List<String> cColors = new ArrayList<>();
            List<String> cStrings = new ArrayList<>();

            if(config.contains(path+".custom_model_component_data.flags")) {
                cFlags = config.getStringList(path+".custom_model_component_data.flags");
            }
            if(config.contains(path+".custom_model_component_data.floats")) {
                cFloats = config.getStringList(path+".custom_model_component_data.floats");
            }
            if(config.contains(path+".custom_model_component_data.colors")) {
                cColors = config.getStringList(path+".custom_model_component_data.colors");
            }
            if(config.contains(path+".custom_model_component_data.strings")) {
                cStrings = config.getStringList(path+".custom_model_component_data.strings");
            }

            customModelComponentData = new CommonItemCustomModelComponentData(cFlags,cColors,cFloats,cStrings);
        }

        boolean hideTooltip = config.getBoolean(path+".hide_tooltip");
        String tooltipStyle = config.contains(path+".tooltip_style") ? config.getString(path+".tooltip_style") : null;
        String model = config.contains(path+".model") ? config.getString(path+".model") : null;

        CommonItemSkullData skullData = null;
        if(config.contains(path+".skull_data")) {
            String skullTexture = null;
            String skullId = null;
            String skullOwner = null;
            if(config.contains(path+".skull_data.texture")) {
                skullTexture = config.getString(path+".skull_data.texture");
            }
            if(config.contains(path+".skull_data.id")) {
                skullId = config.getString(path+".skull_data.id");
            }
            if(config.contains(path+".skull_data.owner")) {
                skullOwner = config.getString(path+".skull_data.owner");
            }
            skullData = new CommonItemSkullData(skullOwner,skullTexture,skullId);
        }
        CommonItemPotionData potionData = null;
        if(config.contains(path+".potion_data")) {
            List<String> potionEffects = null;
            boolean extended = false;
            boolean upgraded = false;
            String potionType = null;
            int potionColor = 0;
            if(config.contains(path+".potion_data.effects")) {
                potionEffects = config.getStringList(path+".potion_data.effects");
            }
            if(config.contains(path+".potion_data.extended")) {
                extended = config.getBoolean(path+".potion_data.extended");
            }
            if(config.contains(path+".potion_data.upgraded")) {
                upgraded = config.getBoolean(path+".potion_data.upgraded");
            }
            if(config.contains(path+".potion_data.type")) {
                potionType = config.getString(path+".potion_data.type");
            }
            if(config.contains(path+".potion_data.color")) {
                potionColor = config.getInt(path+".potion_data.color");
            }

            potionData = new CommonItemPotionData(upgraded,extended,potionType,potionColor,potionEffects);
        }
        CommonItemFireworkData fireworkData = null;
        if(config.contains(path+".firework_data")) {
            List<String> rocketEffects = null;
            String starEffect = null;
            int power = 0;
            if(config.contains(path+".firework_data.rocket_effects")) {
                rocketEffects = config.getStringList(path+".firework_data.rocket_effects");
            }
            if(config.contains(path+".firework_data.star_effect")) {
                starEffect = config.getString(path+".firework_data.star_effect");
            }
            if(config.contains(path+".firework_data.power")) {
                power = config.getInt(path+".firework_data.power");
            }

            fireworkData = new CommonItemFireworkData(rocketEffects,starEffect,power);
        }
        CommonItemBannerData bannerData = null;
        if(config.contains(path+".banner_data")) {
            List<String> patterns = null;
            String baseColor = null;
            if(config.contains(path+".banner_data.patterns")) {
                patterns = config.getStringList(path+".banner_data.patterns");
            }
            if(config.contains(path+".banner_data.base_color")) {
                baseColor = config.getString(path+".banner_data.base_color");
            }

            bannerData = new CommonItemBannerData(patterns,baseColor);
        }
        CommonItemBookData bookData = null;
        if(config.contains(path+".book_data")) {
            List<String> pages = config.getStringList(path+".book_data.pages");
            String author = null;
            String generation = null;
            String title = null;
            if(config.contains(path+".book_data.author")) {
                author = config.getString(path+".book_data.author");
            }
            if(config.contains(path+".book_data.generation")) {
                generation = config.getString(path+".book_data.generation");
            }
            if(config.contains(path+".book_data.title")) {
                title = config.getString(path+".book_data.title");
            }

            bookData = new CommonItemBookData(pages,author,generation,title);
        }
        CommonItemTrimData trimData = null;
        if(config.contains(path+".trim_data")){
            String material = config.getString(path+".trim_data.material");
            String pattern = config.getString(path+".trim_data.pattern");
            trimData = new CommonItemTrimData(pattern,material);
        }

        CommonItem commonItem = new CommonItem(id);
        commonItem.setName(name);
        commonItem.setLore(lore);
        commonItem.setAmount(amount);
        commonItem.setDurability(durability);
        commonItem.setCustomModelData(customModelData);
        commonItem.setColor(color);
        commonItem.setEnchants(enchants);
        commonItem.setFlags(flags);
        commonItem.setBookEnchants(bookEnchants);
        commonItem.setNbt(nbtList);
        commonItem.setAttributes(attributes);
        commonItem.setSkullData(skullData);
        commonItem.setPotionData(potionData);
        commonItem.setFireworkData(fireworkData);
        commonItem.setBannerData(bannerData);
        commonItem.setBookData(bookData);
        commonItem.setTrimData(trimData);
        commonItem.setCustomModelComponentData(customModelComponentData);
        commonItem.setHideTooltip(hideTooltip);
        commonItem.setTooltipStyle(tooltipStyle);
        commonItem.setModel(model);

        return commonItem;
    }

    public void replaceVariables(ItemStack item, ArrayList<CommonVariable> variables, Player player){
        if(item.hasItemMeta()){
            ItemMeta meta = item.getItemMeta();
            if(meta.hasDisplayName()){
                String newName = meta.getDisplayName();
                for(CommonVariable variable : variables){
                    newName = newName.replace(variable.getVariable(),variable.getValue());
                }
                newName = OtherUtils.replaceGlobalVariables(newName,player,plugin);
                MessagesManager.setItemDisplayName(meta, newName);
            }
            if(meta.hasLore()){
                List<String> lore = meta.getLore();
                for(int i=0;i<lore.size();i++){
                    for(CommonVariable variable : variables){
                        String line = lore.get(i).replace(variable.getVariable(),variable.getValue());
                        line = OtherUtils.replaceGlobalVariables(line,player,plugin);
                        lore.set(i,line);
                    }
                }
                MessagesManager.setItemLore(meta, lore);
            }
            item.setItemMeta(meta);
        }
    }

    /**
     * 使用反射从ItemMeta加载CustomModelDataComponent数据
     * 这个方法处理Paper 1.21.4+ 的CustomModelDataComponent功能
     */
    private void loadCustomModelDataFromReflection(ItemStack item, CommonItem commonItem) {
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta == null || !meta.hasCustomModelData()) return;

            // 使用反射获取CustomModelDataComponent类和方法
            Class<?> componentClass = Class.forName("org.bukkit.inventory.meta.components.CustomModelDataComponent");
            Object customModelDataComponent = meta.getClass().getMethod("getCustomModelDataComponent").invoke(meta);

            if (customModelDataComponent == null) return;

            // 获取各种数据
            List<String> flags = getComponentDataList(customModelDataComponent, componentClass, "getFlags", Object::toString);
            List<String> colors = getComponentDataList(customModelDataComponent, componentClass, "getColors", color -> {
                try {
                    return String.valueOf(color.getClass().getMethod("asRGB").invoke(color));
                } catch (Exception e) {
                    return "0";
                }
            });
            List<String> floats = getComponentDataList(customModelDataComponent, componentClass, "getFloats", Object::toString);
            List<String> strings = getComponentDataList(customModelDataComponent, componentClass, "getStrings", Object::toString);

            // 设置到CommonItem
            commonItem.setCustomModelComponentData(new CommonItemCustomModelComponentData(
                    flags, colors, floats, strings
            ));

        } catch (Exception e) {
            // 版本不支持或其他错误，静默处理
            plugin.getLogger().fine("Failed to load CustomModelDataComponent via reflection: " + e.getMessage());
        }
    }

    /**
     * 使用反射设置CustomModelDataComponent数据到ItemMeta
     */
    private void setCustomModelDataFromReflection(ItemMeta meta, CommonItemCustomModelComponentData data) {
        try {
            // 获取CustomModelDataComponent实例
            Object customModelDataComponent = meta.getClass().getMethod("getCustomModelDataComponent").invoke(meta);
            Class<?> componentClass = Class.forName("org.bukkit.inventory.meta.components.CustomModelDataComponent");

            if (customModelDataComponent == null) return;

            // 设置各种数据
            setComponentDataList(customModelDataComponent, componentClass, "setFlags", data.getFlags(), Boolean::parseBoolean);
            setComponentDataList(customModelDataComponent, componentClass, "setFloats", data.getFloats(), Float::parseFloat);
            setComponentDataList(customModelDataComponent, componentClass, "setColors", data.getColors(), rgb -> {
                try {
                    Class<?> colorClass = Class.forName("org.bukkit.Color");
                    return colorClass.getMethod("fromRGB", int.class).invoke(null, Integer.parseInt(rgb));
                } catch (Exception e) {
                    return null;
                }
            });
            setComponentDataList(customModelDataComponent, componentClass, "setStrings", data.getStrings(), s -> s);

            // 设置回ItemMeta
            meta.getClass().getMethod("setCustomModelDataComponent", componentClass).invoke(meta, customModelDataComponent);

        } catch (Exception e) {
            // 版本不支持或其他错误，静默处理
            plugin.getLogger().fine("Failed to set CustomModelDataComponent via reflection: " + e.getMessage());
        }
    }

    /**
     * 通用方法：使用反射获取组件数据列表
     */
    @SuppressWarnings("unchecked")
    private <T> List<String> getComponentDataList(Object component, Class<?> componentClass, String methodName, 
                                                  java.util.function.Function<T, String> converter) {
        try {
            Object result = componentClass.getMethod(methodName).invoke(component);
            if (result instanceof List) {
                List<T> list = (List<T>) result;
                return list.stream().map(converter).collect(Collectors.toList());
            }
        } catch (Exception e) {
            // 方法不存在或调用失败
        }
        return new ArrayList<>();
    }

    /**
     * 通用方法：使用反射设置组件数据列表
     */
    private <T> void setComponentDataList(Object component, Class<?> componentClass, String methodName, 
                                          List<String> stringList, java.util.function.Function<String, T> converter) {
        try {
            if (stringList == null || stringList.isEmpty()) return;
            
            List<T> convertedList = stringList.stream()
                    .map(converter)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());
            
            componentClass.getMethod(methodName, List.class).invoke(component, convertedList);
        } catch (Exception e) {
            // 方法不存在或调用失败，静默处理
        }
    }

}
