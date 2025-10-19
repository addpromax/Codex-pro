package com.magicbili.enchantdisassembler;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIListener implements Listener {

    private final EnchantDisassembler plugin;
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();

    public GUIListener(EnchantDisassembler plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        // 检查是否是分解GUI
        if (!plugin.getGuiHandler().isDisassembleGUI(event.getView())) {
            return;
        }

        // 防止快速点击
        if (lastClick.containsKey(uuid)) {
            long lastClickTime = lastClick.get(uuid);
            if (System.currentTimeMillis() - lastClickTime < 100) {
                event.setCancelled(true);
                return;
            }
        }
        lastClick.put(uuid, System.currentTimeMillis());

        // 禁止双击收集相同物品
        if (event.getClick() == org.bukkit.event.inventory.ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        // 处理点击事件
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        // 获取GUI配置
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig();
        int guiSize = guiConfig.getInt("size", 54);

        // 处理玩家背包内的点击
        if (clickedInventory.equals(player.getInventory())) {
            // 禁止将物品通过 shift 或移动操作放入 GUI
            if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
                return;
            }
            // 其他背包内操作（拖动、常规放置等）允许
            return;
        }

        // 处理GUI内的点击
        int slot = event.getSlot();
        ItemStack clickedItem = event.getCurrentItem();

        // 获取输入槽位列表
        List<Integer> inputSlots = guiConfig.getIntegerList("input.slots");

        // 检查是否是输入槽位
        if (inputSlots.contains(slot)) {
            // 输入槽位允许放置物品
            if (event.getAction() == InventoryAction.PLACE_ALL ||
                    event.getAction() == InventoryAction.PLACE_ONE ||
                    event.getAction() == InventoryAction.PLACE_SOME) {
                event.setCancelled(false);

                // 延迟1tick处理物品添加，确保物品已放入槽位
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Inventory guiInv = event.getView().getTopInventory();
                    ItemStack stackInSlot = guiInv.getItem(slot);
                    if (stackInSlot != null && stackInSlot.getType() != org.bukkit.Material.AIR) {
                        // 添加到分解池
                        plugin.getDisassembleManager().addItem(player, stackInSlot);
                        // 清空槽位并刷新 GUI
                        guiInv.setItem(slot, null);
                        plugin.getGuiHandler().updateGUI(player);
                        // 确保光标为空，防止占位玻璃回到玩家手上
                        player.setItemOnCursor(null);
                        plugin.debug("玩家 " + player.getName() + " 放置物品到输入槽位 " + slot);
                    }
                });
                return;
            }

            // SWAP_WITH_CURSOR 会将占位符给玩家，直接拦截并改为处理添加逻辑
            if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
                event.setCancelled(true);

                ItemStack cursorItem = event.getCursor();
                if (cursorItem != null && cursorItem.getType() != org.bukkit.Material.AIR) {
                    // 添加到分解池
                    plugin.getDisassembleManager().addItem(player, cursorItem);
                    player.setItemOnCursor(null);
                    plugin.getGuiHandler().updateGUI(player);
                }
                return;
            }

            // 不允许 shift-点击将占位符拿走或放置
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                    event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
                return;
            }

            // 不允许从输入槽拿出物品（物品放入后立即被系统回收）
            if (event.getAction().name().startsWith("PICKUP")) {
                event.setCancelled(true);
                return;
            }

            // 其他操作取消
            event.setCancelled(true);
            return;
        }

        // 检查是否是销毁按钮
        ConfigurationSection destroyConfig = guiConfig.getConfigurationSection("buttons.destroy");
        if (destroyConfig != null && slot == destroyConfig.getInt("slot", 49)) {
            plugin.getDisassembleManager().destroyAllItems(player);
            plugin.getGuiHandler().updateGUI(player);
            event.setCancelled(true);
            plugin.debug("玩家 " + player.getName() + " 点击了销毁按钮");
            return;
        }

        // 检查是否是返还按钮
        ConfigurationSection returnConfig = guiConfig.getConfigurationSection("buttons.return");
        if (returnConfig != null && slot == returnConfig.getInt("slot", 45)) {
            plugin.getDisassembleManager().returnItems(player);
            event.setCancelled(true);
            return;
        }

        // 检查是否点击稀有度池子领取奖励
        ConfigurationSection poolsSec = guiConfig.getConfigurationSection("pools");
        if (poolsSec != null) {
            for (String rarityDisplay : poolsSec.getKeys(false)) {
                ConfigurationSection poolCfg = poolsSec.getConfigurationSection(rarityDisplay);
                if (poolCfg == null) continue;
                int poolSlot = poolCfg.getInt("slot", -1);
                if (slot == poolSlot) {
                event.setCancelled(true);
                    // 将显示名称转换为内部 key
                    String rarityKey = plugin.getConfigManager().translateRarity(rarityDisplay);
                    plugin.getDisassembleManager().claimReward(player, rarityKey);
                    plugin.getGuiHandler().updateGUI(player);
                return;
            }
            }
        }

        // 全局禁止 shift-点击在分解 GUI 中移动物品
        if (event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }

        // 其他槽位禁止交互
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();

        // 检查是否是分解GUI
        if (!plugin.getGuiHandler().isDisassembleGUI(event.getView())) {
            return;
        }

        // 获取GUI配置
        FileConfiguration guiConfig = plugin.getConfigManager().getGuiConfig();
        List<Integer> inputSlots = guiConfig.getIntegerList("input.slots");

        // 检查拖拽是否涉及输入槽位
        for (int slot : event.getRawSlots()) {
            if (slot < event.getView().getTopInventory().getSize()) {
                // 如果是输入槽位，允许拖拽
                if (inputSlots.contains(slot)) {
                    continue;
                }

                // 非输入槽位取消拖拽
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();

        // 检查是否是分解GUI
        if (plugin.getGuiHandler().isDisassembleGUI(event.getView())) {
            // 保存玩家数据
            plugin.getDisassembleManager().savePlayerData(player);

            // 从打开列表中移除
            plugin.getGuiHandler().removeOpenInventory(player);

            // 不再自动返还物品，避免重复计分逻辑
        }
    }
}
