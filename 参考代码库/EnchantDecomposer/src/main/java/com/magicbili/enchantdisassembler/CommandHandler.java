package com.magicbili.enchantdisassembler;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final EnchantDisassembler plugin;

    public CommandHandler(EnchantDisassembler plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            if(sender instanceof Player){
                plugin.getGuiHandler().openDisassembleGUI((Player) sender);
            } else {
                sender.sendMessage(plugin.getConfigManager().getMessage("invalid-command"));
            }
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            // 重载配置
            if (sender.hasPermission("enchantdisassembler.reload")) {
                plugin.getConfigManager().reloadConfigs();
                sender.sendMessage(plugin.getConfigManager().getMessage("config-reloaded"));
            } else {
                sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            }
            return true;
        }

        if(args.length==1){
            Player target = Bukkit.getPlayerExact(args[0]);
            if(target!=null){
                plugin.getGuiHandler().openDisassembleGUI(target);
                sender.sendMessage("已为 " + target.getName()+ " 打开分解GUI");
            } else {
                sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found"));
            }
            return true;
        }

        sender.sendMessage(plugin.getConfigManager().getMessage("invalid-command"));
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
        }

        return completions;
    }
}
