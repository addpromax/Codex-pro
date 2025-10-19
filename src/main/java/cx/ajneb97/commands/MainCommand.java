package cx.ajneb97.commands;

import cx.ajneb97.Codex;
import cx.ajneb97.managers.InventoryManager;
import cx.ajneb97.managers.MessagesManager;
import cx.ajneb97.model.inventory.CommonInventory;
import cx.ajneb97.model.inventory.InventoryPlayer;
import cx.ajneb97.model.structure.Category;
import cx.ajneb97.model.structure.Discovery;
import cx.ajneb97.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;


public class MainCommand implements CommandExecutor, TabCompleter {

	private Codex plugin;
	private DatabaseCommand databaseCommand;

	public MainCommand(Codex plugin){
		this.plugin = plugin;
		this.databaseCommand = new DatabaseCommand(plugin);
	}

	public boolean onCommand(CommandSender sender, Command c, String label, String[] args){
		MessagesManager msgManager = plugin.getMessagesManager();
		FileConfiguration messagesConfig = plugin.getMessagesConfig();

		if (!(sender instanceof Player)){
		   if(args.length >= 1){
			   String arg = args[0].toLowerCase();
			   switch(arg){
				   case "reload":
					   reload(sender,msgManager,messagesConfig);
					   break;
				   case "resetplayer":
					   resetPlayer(sender,args,msgManager,messagesConfig);
					   break;
				   case "unlock":
					   unlock(sender,args,msgManager,messagesConfig);
					   break;
				   case "open":
					   open(sender,args,msgManager,messagesConfig);
					   break;
				   case "help":
					   help(sender,args,msgManager,messagesConfig);
					   break;
				   case "debug":
					   debug(sender,args,msgManager,messagesConfig);
					   break;
				   case "debugenchants":
					   debugEnchants(sender,msgManager,messagesConfig);
					   break;
				   case "reloadenchants":
					   reloadEnchants(sender, msgManager, messagesConfig);
					   break;
				   case "autosave":
					   autoSave(sender, args, msgManager, messagesConfig);
					   break;
				   case "batch":
					   msgManager.sendMessage(sender, messagesConfig.getString("mustBeAPlayer"), true);
					   break;
				   case "db":
				   case "database":
					   if (!PlayerUtils.isCodexAdmin(sender)) {
						   msgManager.sendMessage(sender,messagesConfig.getString("noPermissions"),true);
						   return false;
					   }
					   return databaseCommand.handleCommand(sender, args);
				   default:
					   wrongCommand(sender,msgManager,messagesConfig);
					   break;

			   }
		   }
		   return false;
	   	}

		Player player = (Player) sender;
		if(args.length >= 1){
			String arg = args[0].toLowerCase();
			switch(arg){
				case "reload":
					reload(sender,msgManager,messagesConfig);
					break;
				case "resetplayer":
					resetPlayer(sender,args,msgManager,messagesConfig);
					break;
				case "unlock":
					unlock(sender,args,msgManager,messagesConfig);
					break;
				case "open":
					open(sender,args,msgManager,messagesConfig);
					break;
				case "help":
					help(sender,args,msgManager,messagesConfig);
					break;
				case "verify":
					verify(player,msgManager,messagesConfig);
					break;
				case "debug":
					debug(sender,args,msgManager,messagesConfig);
					break;
				case "debugenchants":
					debugEnchants(sender,msgManager,messagesConfig);
					break;
				case "reloadenchants":
					reloadEnchants(sender, msgManager, messagesConfig);
					break;
				case "autosave":
					autoSave(sender, args, msgManager, messagesConfig);
					break;
				case "batch":
					openBatch(player, args, msgManager, messagesConfig);
					break;
				case "db":
				case "database":
					if (!PlayerUtils.isCodexAdmin(sender)) {
						msgManager.sendMessage(sender,messagesConfig.getString("noPermissions"),true);
						return true;
					}
					return databaseCommand.handleCommand(sender, args);
				default:
					wrongCommand(sender,msgManager,messagesConfig);
					break;
			}
		}else{
			noArguments(player,msgManager,messagesConfig);
		}

	   	return true;
	}

	public void wrongCommand(CommandSender sender,MessagesManager msgManager,FileConfiguration messagesConfig){
		msgManager.sendMessage(sender,messagesConfig.getString("commandDoesNotExist"),true);
	}

	public void help(CommandSender sender,String[] args,MessagesManager msgManager,FileConfiguration messagesConfig){
		// /codex help
		// /codex help <page>
		if(!PlayerUtils.isCodexAdmin(sender)){
			msgManager.sendMessage(sender,messagesConfig.getString("noPermissions"),true);
			return;
		}

		int page = 1;
		if (args.length > 1) {
			try {
				page = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				msgManager.sendMessage(sender,messagesConfig.getString("commandHelpNoValidPage"),true);
				return;
			}
		}
		String key = "helpCommandPage"+page;
		if(messagesConfig.contains(key)){
			for(String line : messagesConfig.getStringList(key)){
				msgManager.sendMessage(sender, line, false);
			}
		}else{
			msgManager.sendMessage(sender,messagesConfig.getString("commandHelpNoValidPage"),true);
		}
	}

	public void reload(CommandSender sender,MessagesManager msgManager,FileConfiguration messagesConfig){
		if(!PlayerUtils.isCodexAdmin(sender)){
			msgManager.sendMessage(sender,messagesConfig.getString("noPermissions"),true);
			return;
		}

		boolean isDebug = plugin.getDebugManager().isDebugEnabled();
		
		sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage("&e开始重新加载配置文件..."));
		if (isDebug) {
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage("&7详细信息请查看控制台日志"));
		}
		
		if(!plugin.getConfigsManager().reload()){
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage("&c配置重新加载失败!"));
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage("&c请检查控制台错误信息"));
			if (isDebug) {
				sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage("&7常见问题:"));
				sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage("&7- 检查yml文件语法是否正确"));
				sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage("&7- 确保分类文件包含必要的config节点"));
				sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage("&7- 确保发现项的discovered_on配置正确"));
			}
			return;
		}
		
		// 显示当前加载的分类数量
		int categoryCount = plugin.getCategoryManager().getCategories().size();
		int totalDiscoveries = plugin.getCategoryManager().getTotalDiscoveries();
		
		sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage("&a配置重新加载成功!"));
		sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage("&7已加载分类: &e" + categoryCount + " &7个"));
		if (isDebug) {
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage("&7总发现项: &e" + totalDiscoveries + " &7个"));
		}
	}

	public void resetPlayer(CommandSender sender,String[] args,MessagesManager msgManager,FileConfiguration messagesConfig){
		// /codex resetplayer <player> (opt)<category> (opt)<discovery>
		if(!PlayerUtils.isCodexAdmin(sender)){
			msgManager.sendMessage(sender,messagesConfig.getString("noPermissions"),true);
			return;
		}

		if(args.length <= 1){
			msgManager.sendMessage(sender,messagesConfig.getString("commandResetPlayerError"),true);
			return;
		}

		String playerName = args[1];
		String category = null;
		String discovery = null;
		if(args.length > 2){
			category = args[2];
		}
		if(args.length > 3){
			discovery = args[3];
		}

		String result = plugin.getPlayerDataManager().resetDataPlayer(playerName,category,discovery,messagesConfig);
		if(result != null){
			msgManager.sendMessage(sender,result,true);
		}
	}

	public void noArguments(Player player,MessagesManager msgManager,FileConfiguration messagesConfig){
		if(plugin.getVerifyManager().isCriticalErrors()){
			msgManager.sendMessage(player,messagesConfig.getString("pluginCriticalErrors"),true);
			return;
		}

		plugin.getInventoryManager().openInventory(new InventoryPlayer(player,"main_inventory"));
	}

	public void verify(Player player,MessagesManager msgManager,FileConfiguration messagesConfig){
		// /codex verify
		if(!PlayerUtils.isCodexAdmin(player)){
			msgManager.sendMessage(player,messagesConfig.getString("noPermissions"),true);
			return;
		}
		plugin.getVerifyManager().sendVerification(player);
	}

	public void unlock(CommandSender sender,String[] args,MessagesManager msgManager,FileConfiguration messagesConfig) {
		// /codex unlock <player> <category> <discovery> (opt)<send message>
		if (!PlayerUtils.isCodexAdmin(sender)) {
			msgManager.sendMessage(sender, messagesConfig.getString("noPermissions"), true);
			return;
		}

		if (args.length <= 3) {
			msgManager.sendMessage(sender, messagesConfig.getString("commandUnlockErrorUse"), true);
			return;
		}

		Player player = Bukkit.getPlayer(args[1]);
		if(player == null){
			msgManager.sendMessage(sender, messagesConfig.getString("playerNotOnline"), true);
			return;
		}

		String categoryName = args[2];
		String discoveryName = args[3];
		Category category = plugin.getCategoryManager().getCategory(categoryName);
		if(category == null){
			msgManager.sendMessage(sender, messagesConfig.getString("categoryDoesNotExist"), true);
			return;
		}
		Discovery discovery = category.getDiscovery(discoveryName);
		if(discovery == null){
			msgManager.sendMessage(sender, messagesConfig.getString("discoveryDoesNotExist")
					.replace("%category%",categoryName), true);
			return;
		}

		boolean sendMessage = true;
		if(args.length >= 5){
			try{
				sendMessage = Boolean.parseBoolean(args[4]);
			}catch(Exception ignore){}
		}

		boolean canDiscover = plugin.getDiscoveryManager().onDiscover(player,categoryName,discoveryName);
		if(!canDiscover){
			if(sendMessage){
				msgManager.sendMessage(sender, messagesConfig.getString("playerAlreadyHasDiscovery")
						.replace("%category%",categoryName)
						.replace("%discovery%",discoveryName)
						.replace("%player%",player.getName()), true);
			}
		}else{
			if(sendMessage){
				msgManager.sendMessage(sender, messagesConfig.getString("playerUnlockDiscovery")
						.replace("%category%",categoryName)
						.replace("%discovery%",discoveryName)
						.replace("%player%",player.getName()), true);
			}
		}
	}

	public void open(CommandSender sender,String[] args,MessagesManager msgManager,FileConfiguration messagesConfig) {
		// /codex open <player> <inventory>
		if (!PlayerUtils.isCodexAdmin(sender)) {
			msgManager.sendMessage(sender, messagesConfig.getString("noPermissions"), true);
			return;
		}

		if (args.length <= 2) {
			msgManager.sendMessage(sender, messagesConfig.getString("commandUnlockErrorUse"), true);
			return;
		}

		Player player = Bukkit.getPlayer(args[1]);
		if(player == null){
			msgManager.sendMessage(sender, messagesConfig.getString("playerNotOnline"), true);
			return;
		}

		InventoryManager inventoryManager = plugin.getInventoryManager();
		CommonInventory commonInventory = inventoryManager.getInventory(args[2]);
		if(commonInventory == null){
			msgManager.sendMessage(sender, messagesConfig.getString("inventoryDoesNotExists").replace("%inventory%",args[2]), true);
			return;
		}

		inventoryManager.openInventory(new InventoryPlayer(player,commonInventory.getName()));
		msgManager.sendMessage(sender, messagesConfig.getString("playerOpenInventory").replace("%inventory%",args[2])
				.replace("%player%",player.getName()), true);
	}

	/**
	 * 打开批量添加物品界面
	 * @param player 玩家
	 * @param args 命令参数
	 * @param msgManager 消息管理器
	 * @param messagesConfig 消息配置
	 */
	public void openBatch(Player player, String[] args, MessagesManager msgManager, FileConfiguration messagesConfig) {
		if (!player.hasPermission("codex.admin.batch")) {
			msgManager.sendMessage(player, messagesConfig.getString("batchNoPermission"), true);
			return;
		}
		
		// 检查是否提供了分类名称参数
		if (args.length < 2) {
			msgManager.sendMessage(player, messagesConfig.getString("batchMissingCategory"), true);
			return;
		}
		
		String categoryName = args[1];
		
		// 打开批量添加物品界面
		plugin.getBatchItemsManager().openBatchItemsInventory(player, categoryName);
	}

	/**
	 * 开启或关闭调试模式
	 * @param sender 命令发送者
	 * @param args 命令参数
	 * @param msgManager 消息管理器
	 * @param messagesConfig 消息配置
	 */
	public void debug(CommandSender sender, String[] args, MessagesManager msgManager, FileConfiguration messagesConfig) {
		// /codex debug <on/off>
		if (!PlayerUtils.isCodexAdmin(sender)) {
			msgManager.sendMessage(sender, messagesConfig.getString("noPermissions"), true);
			return;
		}

		if (args.length < 2) {
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage(messagesConfig.getString("debugUsage")));
			return;
		}

		String option = args[1].toLowerCase();
		boolean debugEnabled;

		if (option.equals("on") || option.equals("true")) {
			debugEnabled = true;
		} else if (option.equals("off") || option.equals("false")) {
			debugEnabled = false;
		} else {
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage(messagesConfig.getString("debugInvalidOption")));
			return;
		}

		// 使用统一的DebugManager设置调试模式
		plugin.getDebugManager().setDebugEnabled(debugEnabled);
		
		// 发送消息
		if (debugEnabled) {
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage(messagesConfig.getString("debugEnabled")));
		} else {
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage(messagesConfig.getString("debugDisabled")));
		}
	}

	/**
	 * 打印所有附魔ID和中文名称的映射关系
	 * @param sender 命令发送者
	 * @param msgManager 消息管理器
	 * @param messagesConfig 消息配置
	 */
	public void debugEnchants(CommandSender sender, MessagesManager msgManager, FileConfiguration messagesConfig) {
		// /codex debugenchants
		if (!PlayerUtils.isCodexAdmin(sender)) {
			msgManager.sendMessage(sender, messagesConfig.getString("noPermissions"), true);
			return;
		}

		sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage(messagesConfig.getString("debugEnchantsStart")));
		
		// 打印所有附魔ID和中文名称的映射关系
		cx.ajneb97.api.AiyatsbusAPI.printAllEnchantmentNames();
		
		// 获取所有附魔ID
		List<String> allEnchantIds = cx.ajneb97.api.AiyatsbusAPI.getAllEnchantmentIds();
		
		// 打印每个附魔的详细信息
		plugin.getLogger().info("===== 开始打印所有附魔的详细信息 =====");
		plugin.getLogger().info("共找到 " + allEnchantIds.size() + " 个附魔");
		
		int successCount = 0;
		int failCount = 0;
		
		for (String id : allEnchantIds) {
			try {
				// 获取附魔名称
				String name = cx.ajneb97.api.AiyatsbusAPI.getEnchantmentName(id);
				
				// 获取适用物品
				List<String> targets = cx.ajneb97.api.AiyatsbusAPI.getTargets(id);
				
				// 获取冲突附魔
				List<String> conflicts = cx.ajneb97.api.AiyatsbusAPI.getConflicts(id);
				
				// 获取冲突附魔名称
				List<String> conflictNames = new ArrayList<>();
				for (String conflictId : conflicts) {
					String conflictName = cx.ajneb97.api.AiyatsbusAPI.getEnchantmentName(conflictId);
					conflictNames.add(conflictName != null ? conflictName : conflictId);
				}
				
				if (name != null && !name.isEmpty()) {
					plugin.getLogger().info("[成功] 附魔ID: " + id);
					plugin.getLogger().info("  - 中文名称: " + name);
					plugin.getLogger().info("  - 适用物品: " + targets);
					plugin.getLogger().info("  - 冲突附魔ID: " + conflicts);
					plugin.getLogger().info("  - 冲突附魔名称: " + conflictNames);
					successCount++;
				} else {
					plugin.getLogger().info("[失败] 附魔ID: " + id + " -> 无法获取中文名称");
					plugin.getLogger().info("  - 适用物品: " + targets);
					plugin.getLogger().info("  - 冲突附魔ID: " + conflicts);
					failCount++;
				}
			} catch (Exception e) {
				plugin.getLogger().info("[错误] 附魔ID: " + id + " -> 处理时出错: " + e.getMessage());
				failCount++;
			}
		}
		
		plugin.getLogger().info("===== 附魔详细信息统计 =====");
		plugin.getLogger().info("总数: " + allEnchantIds.size());
		plugin.getLogger().info("成功: " + successCount);
		plugin.getLogger().info("失败: " + failCount);
		plugin.getLogger().info("=============================");
		
		sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage(messagesConfig.getString("debugEnchantsFinish")));
	}

	public void reloadEnchants(CommandSender sender, MessagesManager msgManager, FileConfiguration messagesConfig) {
		// /codex reloadenchants
		if(!PlayerUtils.isCodexAdmin(sender)){
			msgManager.sendMessage(sender, messagesConfig.getString("noPermissions"), true);
			return;
		}

		try {
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage(messagesConfig.getString("reloadEnchantsStart")));
			
			// 清除缓存
			plugin.getEnchantmentInfoManager().clearCache();
			
			// 重新加载附魔信息
			plugin.getEnchantmentManager().getAllEnchantments();
			
			// 提示成功
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage(messagesConfig.getString("reloadEnchantsSuccess")));
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage(messagesConfig.getString("reloadEnchantsTip")));
		} catch (Exception e) {
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage("&c重新加载附魔信息时发生错误: " + e.getMessage()));
			e.printStackTrace();
		}
	}

	public void autoSave(CommandSender sender, String[] args, MessagesManager msgManager, FileConfiguration messagesConfig) {
		// /codex autosave <on/off>
		if (!PlayerUtils.isCodexAdmin(sender)) {
			msgManager.sendMessage(sender, messagesConfig.getString("noPermissions"), true);
			return;
		}

		if (args.length < 2) {
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage(messagesConfig.getString("autoSaveUsage")));
			return;
		}

		String option = args[1].toLowerCase();
		boolean autoSaveEnabled;

		if (option.equals("on") || option.equals("true")) {
			autoSaveEnabled = true;
		} else if (option.equals("off") || option.equals("false")) {
			autoSaveEnabled = false;
		} else {
			sender.sendMessage(Codex.prefix + MessagesManager.getColoredMessage(messagesConfig.getString("autoSaveInvalidOption")));
			return;
		}

		// 修改配置并保存
		plugin.getConfigsManager().getMainConfigManager().getConfigFile().getConfig().set("auto_save_enabled", autoSaveEnabled);
		plugin.getConfigsManager().getMainConfigManager().getConfigFile().saveConfig();
		
		// 重新加载配置
		plugin.getConfigsManager().getMainConfigManager().configure();
		
		// 重新启动或停止自动保存任务
		plugin.reloadPlayerDataSaveTask();

		if (autoSaveEnabled) {
			msgManager.sendMessage(sender, messagesConfig.getString("autoSaveEnabled"), true);
		} else {
			msgManager.sendMessage(sender, messagesConfig.getString("autoSaveDisabled"), true);
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
		List<String> completions = new ArrayList<>();
		List<String> commands = new ArrayList<>();
		
		switch(args.length){
			case 1:
				if(PlayerUtils.isCodexAdmin(sender)){
					commands.add("reload");commands.add("resetplayer");commands.add("unlock");
					commands.add("open");commands.add("help");commands.add("verify");commands.add("debug");
					commands.add("debugenchants");commands.add("reloadenchants");
					commands.add("database");commands.add("db"); // 添加数据库命令
					commands.add("autosave"); // 添加自动保存命令
					commands.add("batch"); // 添加批量物品命令
				}
				for(String c : commands) {
					if(args[0].isEmpty() || c.startsWith(args[0].toLowerCase())) {
						completions.add(c);
					}
				}
				return completions;
			case 2:
				// 数据库命令补全
				if ((args[0].equalsIgnoreCase("db") || args[0].equalsIgnoreCase("database")) && PlayerUtils.isCodexAdmin(sender)) {
					String arg = args[1].toLowerCase();
					if("info".startsWith(arg)) {
						completions.add("info");
					}
					if("migrate".startsWith(arg)) {
						completions.add("migrate");
					}
					return completions;
				}
				
				if(PlayerUtils.isCodexAdmin(sender)){
					commands.add("resetplayer");commands.add("unlock");commands.add("open");
					commands.add("help");commands.add("debug");commands.add("autosave");
					commands.add("batch"); // 添加批量物品命令
				}
				for(String c : commands) {
					if(args[0].equalsIgnoreCase(c)){
						if(c.equals("resetplayer")){
							completions = new ArrayList<>();
							addPlayers(completions,args[1]);
							addAllWord(completions,args[1]);
							return completions;
						}else if(c.equals("unlock") || c.equals("open")){
							completions = new ArrayList<>();
							addPlayers(completions,args[1]);
							return completions;
						}else if(c.equals("debug") || c.equals("autosave")){
							completions = new ArrayList<>();
							completions.add("on");
							completions.add("off");
							return completions;
						}else if(c.equals("help")){
							completions = new ArrayList<>();
							for(int i = 1;i<=3;i++){
								String pageNum = String.valueOf(i);
								if(pageNum.startsWith(args[1].toLowerCase())){
									completions.add(pageNum);
								}
							}
							return completions;
						}else if(c.equals("batch")){
							// 获取已存在的分类，以便用户避免使用这些名称
							ArrayList<Category> categories = plugin.getCategoryManager().getCategories();
							List<String> existingCategories = new ArrayList<>();
							for(Category category : categories) {
								existingCategories.add(category.getName());
							}
							
							// 提供一些建议的分类名称
							completions = new ArrayList<>();
							List<String> suggestions = Arrays.asList(
								"custom_items", "rare_items", "special_items", "event_items", 
								"weapons", "tools", "armor", "accessories", 
								"custom_enchants", "artifacts", "collections", "seasonal"
							);
							
							for(String suggestion : suggestions) {
								// 如果分类名不存在且匹配输入前缀，添加到建议中
								if(!existingCategories.contains(suggestion) && 
									(args[1].isEmpty() || suggestion.startsWith(args[1].toLowerCase()))) {
									completions.add(suggestion);
								}
							}
							
							return completions;
						}
					}
				}
				break;
			case 3:
				// 数据库命令补全
				if ((args[0].equalsIgnoreCase("db") || args[0].equalsIgnoreCase("database")) 
						&& args[1].equalsIgnoreCase("migrate") 
						&& PlayerUtils.isCodexAdmin(sender)) {
					String arg = args[2].toLowerCase();
					if ("file".startsWith(arg)) {
						completions.add("file");
					}
					if ("h2".startsWith(arg)) {
						completions.add("h2");
					}
					if ("mysql".startsWith(arg)) {
						completions.add("mysql");
					}
					return completions;
				}
				
				if(PlayerUtils.isCodexAdmin(sender)){
					commands.add("resetplayer");commands.add("unlock");commands.add("open");
				}
				for(String c : commands) {
					if(args[0].equalsIgnoreCase(c)){
						if(c.equals("resetplayer") || c.equals("unlock")){
							return getCategoryCompletions(args,2);
						}else if(c.equals("open")){
							return getInventoryCompletions(args,2);
						}
					}
				}
				break;
			case 4:
				if(PlayerUtils.isCodexAdmin(sender)){
					commands.add("resetplayer");commands.add("unlock");
				}
				for(String c : commands) {
					if(args[0].equalsIgnoreCase(c)){
						if(c.equals("resetplayer") || c.equals("unlock")){
							return getDiscoveryCompletions(args,3);
						}
					}
				}
				break;
			case 5:
				if(PlayerUtils.isCodexAdmin(sender)){
					commands.add("unlock");
				}
				for(String c : commands) {
					if(args[0].equalsIgnoreCase(c)){
						if(c.equals("unlock")){
							completions = new ArrayList<>();
							completions.add("true");
							completions.add("false");
							return completions;
						}
					}
				}
				break;
		}
		
		return completions;
	}

	private List<String> getInventoryCompletions(String[] args,int argInventoryPos){
		List<String> completions = new ArrayList<>();
		String argInventory = args[argInventoryPos];

		ArrayList<CommonInventory> inventories = plugin.getInventoryManager().getInventories();
		for(CommonInventory inventory : inventories) {
			if(argInventory.isEmpty() || inventory.getName().toLowerCase().startsWith(argInventory.toLowerCase())) {
				completions.add(inventory.getName());
			}
		}

		if(completions.isEmpty()){
			return null;
		}
		return completions;
	}

	private List<String> getCategoryCompletions(String[] args,int argCategoryPos){
		List<String> completions = new ArrayList<>();
		String argCategory = args[argCategoryPos];

		ArrayList<Category> categories = plugin.getCategoryManager().getCategories();
		for(Category category : categories) {
			if(argCategory.isEmpty() || category.getName().toLowerCase().startsWith(argCategory.toLowerCase())) {
				completions.add(category.getName());
			}
		}

		if(completions.isEmpty()){
			return null;
		}
		return completions;
	}

	private List<String> getDiscoveryCompletions(String[] args,int argDiscoveryPos){
		List<String> completions = new ArrayList<>();
		String argDiscovery = args[argDiscoveryPos];
		String argCategory = args[argDiscoveryPos-1];

		Category category = plugin.getCategoryManager().getCategory(argCategory);
		if(category == null){
			return null;
		}

		ArrayList<Discovery> discoveries = category.getDiscoveries();
		for(Discovery discovery : discoveries) {
			if(argDiscovery.isEmpty() || discovery.getId().toLowerCase().startsWith(argDiscovery.toLowerCase())) {
				completions.add(discovery.getId());
			}
		}

		if(completions.isEmpty()){
			return null;
		}
		return completions;
	}

	private void addAllWord(List<String> completions,String arg){
		if(arg.isEmpty() || "*".startsWith(arg.toLowerCase())) {
			completions.add("*");
		}
	}

	private void addPlayers(List<String> completions,String arg){
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(arg.isEmpty() || p.getName().toLowerCase().startsWith(arg.toLowerCase())){
				completions.add(p.getName());
			}
		}
	}
}
