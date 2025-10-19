package net.Indyuce.mmoitems.api.interaction;

import io.lumine.mythic.lib.MythicLib;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.comp.flags.CustomFlag;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.Indyuce.mmoitems.api.item.mmoitem.VolatileMMOItem;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.stat.data.CommandData;
import net.Indyuce.mmoitems.stat.data.CommandListData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class UseItem {
    protected final Player player;
    protected final PlayerData playerData;
    protected final VolatileMMOItem mmoitem;

    protected static final Random RANDOM = new Random();

    @Deprecated
    public UseItem(@NotNull Player player, @NotNull NBTItem nbtItem) {
        this(PlayerData.get(player), nbtItem);
    }

    public UseItem(@NotNull PlayerData playerData, @NotNull NBTItem nbtItem) {
        this.player = playerData.getPlayer();
        this.playerData = playerData;
        this.mmoitem = new VolatileMMOItem(nbtItem);
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }

    public VolatileMMOItem getMMOItem() {
        return mmoitem;
    }

    public NBTItem getNBTItem() {
        return mmoitem.getNBT();
    }

    public ItemStack getItem() {
        return mmoitem.getNBT().getItem();
    }

    public boolean checkItemRequirements() {
        return checkItemRequirements(true);
    }

    protected boolean flagCheck(boolean flagCheck, CustomFlag customFlag) {
        // Performance option, WG flags can sometimes be expensive in performance
        return !flagCheck || MythicLib.plugin.getFlags().isFlagAllowed(getPlayer(), customFlag);
    }

    /**
     * Apply item costs and requirements. This method should be overriden to
     * check for WorldGuard flags as well as the two-handed restriction.
     *
     * @return If the item can be used
     */
    public boolean checkItemRequirements(boolean message) {
        return playerData.getRPG().canUse(mmoitem.getNBT(), message);
    }

    /**
     * Execute commands after checking for WG flags;
     * this does NOT check for the command cooldown.
     */
    public void executeCommands() {
        if (mmoitem.hasData(ItemStats.COMMANDS) && flagCheck(MMOItems.plugin.getLanguage().commandFlagChecks, CustomFlag.MI_COMMANDS))
            ((CommandListData) mmoitem.getData(ItemStats.COMMANDS)).getCommands().forEach(this::scheduleCommandExecution);
    }

    /**
     * Instantly fires the command if it has no delay
     * or schedule its execution if it does have some
     *
     * @param command Command to execute
     */
    private void scheduleCommandExecution(CommandData command) {
        String parsed = MythicLib.plugin.getPlaceholderParser().parse(player, command.getCommand());

        if (!command.hasDelay()) dispatchCommand(parsed, command.isConsoleCommand(), command.hasOpPerms());
        else
            Bukkit.getScheduler().runTaskLater(MMOItems.plugin, () -> dispatchCommand(parsed, command.isConsoleCommand(), command.hasOpPerms()), (long) command.getDelay() * 20);
    }

    /**
     * Dispatches a command
     *
     * @param parsed  Command with placeholders already parsed
     * @param console If the console should dispatch the command, when
     *                set to false the player dispatches it.
     * @param op      Very dangerous option, dispatches the command
     *                as a server administrator
     */
    private void dispatchCommand(String parsed, boolean console, boolean op) {
        if (console) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            return;
        }

        if (op && !player.isOp()) {
            player.setOp(true);
            try {
                Bukkit.dispatchCommand(player, parsed);
            } catch (Exception ignored) {
                player.setOp(false);
            } finally {
                player.setOp(false);
            }
        } else Bukkit.dispatchCommand(player, parsed);
    }

    @Deprecated
    @Nullable
    public static UseItem getItem(Player player, NBTItem item, String type) {
        return Type.get(type).toUseItem(player, item);
    }

    @Deprecated
    public static UseItem getItem(@NotNull Player player, @NotNull NBTItem item, @NotNull Type type) {
        return type.toUseItem(PlayerData.get(player), item);
    }
}
