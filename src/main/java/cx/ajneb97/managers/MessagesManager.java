package cx.ajneb97.managers;

import cx.ajneb97.libs.centeredmessage.DefaultFontInfo;
import cx.ajneb97.utils.OtherUtils;
import cx.ajneb97.utils.PaperVersionChecker;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Adventure API直接导入，打包进插件
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessagesManager {

	private String prefix;
	private String timeSeconds;
	private String timeMinutes;
	private String timeHours;
	private String timeDays;

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getTimeSeconds() {
		return timeSeconds;
	}

	public void setTimeSeconds(String timeSeconds) {
		this.timeSeconds = timeSeconds;
	}

	public String getTimeMinutes() {
		return timeMinutes;
	}

	public void setTimeMinutes(String timeMinutes) {
		this.timeMinutes = timeMinutes;
	}

	public String getTimeHours() {
		return timeHours;
	}

	public void setTimeHours(String timeHours) {
		this.timeHours = timeHours;
	}

	public String getTimeDays() {
		return timeDays;
	}

	public void setTimeDays(String timeDays) {
		this.timeDays = timeDays;
	}

	public void sendMessage(CommandSender sender, String message, boolean prefix){
		if(!message.isEmpty()){
			if(prefix){
				sender.sendMessage(getColoredMessage(this.prefix+message));
			}else{
				sender.sendMessage(getColoredMessage(message));
			}
		}
	}

	// Adventure API实例（直接使用，不再需要反射）
	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
	private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
	private static Boolean paperItemMetaAvailable = null;
	
	public static String getColoredMessage(String text) {
		if (text == null) {
			Bukkit.getLogger().warning("[Codex] 消息内容为 null，请检查 messages.yml 是否缺少对应 key。");
			return "";
		}
		
		// 直接使用Adventure API（已打包进插件）
		try {
			return getColoredMessageWithMiniMessage(text);
		} catch (Exception e) {
			// 如果MiniMessage解析失败，回退到传统方法
			Bukkit.getLogger().warning("[Codex] MiniMessage解析失败，使用传统颜色代码: " + e.getMessage());
			return getColoredMessageLegacy(text);
		}
	}
	
	/**
	 * 检查Paper的ItemMeta Component支持是否可用
	 */
	private static boolean isPaperItemMetaAvailable() {
		if (paperItemMetaAvailable == null) {
			paperItemMetaAvailable = PaperVersionChecker.hasItemMetaComponent() && PaperVersionChecker.hasAdventureAPI();
			
			if (paperItemMetaAvailable) {
				Bukkit.getLogger().info("[Codex] Paper ItemMeta Component支持检测成功");
			} else {
				if (!PaperVersionChecker.isPaper()) {
					Bukkit.getLogger().info("[Codex] Paper ItemMeta Component支持不可用 - 当前服务器不是Paper");
				} else if (!PaperVersionChecker.hasAdventureAPI()) {
					Bukkit.getLogger().info("[Codex] Paper ItemMeta Component支持不可用 - Adventure API不可用");
				} else {
					Bukkit.getLogger().info("[Codex] Paper ItemMeta Component支持不可用 - 当前使用传统颜色代码格式");
				}
			}
		}
		return paperItemMetaAvailable;
	}
	
	/**
	 * 使用MiniMessage处理颜色代码，支持现代格式如<gradient>、<rainbow>等
	 */
	public static String getColoredMessageWithMiniMessage(String text) {
		if (text == null) return "";
		
		try {
			// 如果文本已经包含传统颜色代码（§），直接返回，不用MiniMessage处理
			if (text.contains("§")) {
				return text;
			}
			
			// 检查是否包含MiniMessage标签或CustomFishing标签
			if (containsMiniMessageTags(text) || containsCustomFishingTags(text)) {
				// 预处理文本
				String preprocessed = preprocessForMiniMessage(text);
				
				// 直接使用MiniMessage解析
				Component component = MINI_MESSAGE.deserialize(preprocessed);
				
				// 转换回传统的§格式字符串
				String result = LEGACY_SERIALIZER.serialize(component);
				
				return result;
			} else {
				// 如果没有MiniMessage标签，使用传统方法处理
				return getColoredMessageLegacy(text);
			}
		} catch (Exception e) {
			// 解析失败时回退到传统方法
			return getColoredMessageLegacy(text);
		}
	}
	
	/**
	 * 预处理文本以支持MiniMessage和CustomFishing格式
	 */
	private static String preprocessForMiniMessage(String text) {
		if (text == null) return "";
		
		String processed = text;
		
		// 转换CustomFishing格式
		if (containsCustomFishingTags(processed)) {
			processed = convertCustomFishingToMiniMessage(processed);
		}
		
		// 处理传统颜色代码
		processed = preprocessLegacyCodes(processed);
		
		return processed;
	}
	
	/**
	 * 传统的颜色代码处理方法（向后兼容）
	 */
	public static String getColoredMessageLegacy(String text) {
		if (text == null) return "";
		
		// 处理十六进制颜色代码
		if(OtherUtils.isNew()) {
			Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
			Matcher match = pattern.matcher(text);
			
			while(match.find()) {
				String color = text.substring(match.start(),match.end());
				text = text.replace(color, ChatColor.of(color)+"");
				
				match = pattern.matcher(text);
			}
		}

		// 处理传统颜色代码
		text = ChatColor.translateAlternateColorCodes('&', text);
		return text;
	}
	
	/**
	 * 检查文本是否包含MiniMessage标签
	 */
	private static boolean containsMiniMessageTags(String text) {
		if (text == null) return false;
		
		// 常见的MiniMessage标签模式
		String[] miniMessageTags = {
			"<gradient", "<rainbow>", "<color:", "<#", "</gradient>", "</rainbow>", "</color>",
			"<bold>", "<italic>", "<underlined>", "<strikethrough>", "<obfuscated>",
			"<reset>", "<newline>", "<br>", "<hover:", "<click:", "<key:",
			"<transition:", "<font:", "<decoration:", "<lang:"
		};
		
		String lowerText = text.toLowerCase();
		for (String tag : miniMessageTags) {
			if (lowerText.contains(tag.toLowerCase())) {
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 预处理传统颜色代码，将&代码转换为§代码，但保留MiniMessage标签
	 */
	private static String preprocessLegacyCodes(String text) {
		if (text == null) return "";
		
		// 保护MiniMessage标签不被影响，先替换成占位符
		String processed = text;
		
		// 只处理&后面跟着颜色代码字符的情况，避免影响MiniMessage标签
		processed = processed.replaceAll("&([0-9a-fA-FkKlLmMnNoOrR])", "§$1");
		
		return processed;
	}
	
	/**
	 * 专门处理CustomFishing格式的颜色代码
	 * CustomFishing使用类似 <#FF0000>text 的格式
	 */
	public static String getCustomFishingColoredMessage(String text) {
		if (text == null) return "";
		
		try {
			// 如果文本已经包含传统颜色代码（§），直接返回
			if (text.contains("§")) {
				return text;
			}
			
			// 检查是否包含CustomFishing特有的颜色格式
			if (containsCustomFishingTags(text)) {
				// 直接使用Adventure API（已打包进插件）
				String converted = convertCustomFishingToMiniMessage(text);
				return getColoredMessageWithMiniMessage(converted);
			} else {
				// 使用标准处理
				return getColoredMessage(text);
			}
		} catch (Exception e) {
			// 解析失败时回退到传统方法
			return getColoredMessageLegacy(text);
		}
	}
	
	/**
	 * 将CustomFishing格式转换为传统颜色代码（用于没有Adventure的环境）
	 */
	private static String convertCustomFishingToLegacy(String text) {
		if (text == null) return "";
		
		String converted = text;
		
		// 简单的颜色映射
		// 将 <#FF0000>text</#FF0000> 转换为最接近的传统颜色
		Pattern pattern = Pattern.compile("<#([a-fA-F0-9]{6})>(.*?)</#([a-fA-F0-9]{6})>");
		Matcher matcher = pattern.matcher(converted);
		
		while (matcher.find()) {
			String hexColor = matcher.group(1);
			String content = matcher.group(2);
			
			// 将十六进制颜色转换为最接近的传统颜色代码
			String legacyColor = hexToLegacyColor(hexColor);
			converted = converted.replace(matcher.group(0), legacyColor + content);
		}
		
		// 处理剩余的传统颜色代码
		return getColoredMessageLegacy(converted);
	}
	
	/**
	 * 将十六进制颜色转换为最接近的传统颜色代码
	 */
	private static String hexToLegacyColor(String hexColor) {
		// 简单的颜色映射逻辑
		if (hexColor == null || hexColor.length() != 6) return "§f";
		
		try {
			int rgb = Integer.parseInt(hexColor, 16);
			int r = (rgb >> 16) & 0xFF;
			int g = (rgb >> 8) & 0xFF;
			int b = rgb & 0xFF;
			
			// 基于RGB值选择最接近的传统颜色
			if (r > 200 && g > 200 && b > 200) return "§f"; // 白色
			if (r < 50 && g < 50 && b < 50) return "§0"; // 黑色
			if (r > 150 && g < 100 && b < 100) return "§c"; // 红色
			if (r < 100 && g > 150 && b < 100) return "§a"; // 绿色
			if (r < 100 && g < 100 && b > 150) return "§9"; // 蓝色
			if (r > 150 && g > 150 && b < 100) return "§e"; // 黄色
			if (r > 150 && g < 100 && b > 150) return "§d"; // 洋红色
			if (r < 100 && g > 150 && b > 150) return "§b"; // 青色
			if (r > 100 && g > 100 && b > 100) return "§7"; // 灰色
			
			return "§f"; // 默认白色
		} catch (Exception e) {
			return "§f"; // 默认白色
		}
	}
	
	/**
	 * 检查是否包含CustomFishing特有的颜色标签
	 */
	private static boolean containsCustomFishingTags(String text) {
		if (text == null) return false;
		
		// CustomFishing特有格式：<#RRGGBB>text（可能有或没有结束标签）
		// 检查是否包含 <#六位十六进制> 格式
		return text.matches(".*<#[a-fA-F0-9]{6}>.*");
	}
	
	/**
	 * 将CustomFishing格式转换为标准MiniMessage格式
	 * CustomFishing使用简化格式：<#RRGGBB>text，颜色会持续到下一个颜色标签或行尾
	 */
	private static String convertCustomFishingToMiniMessage(String text) {
		if (text == null) return "";
		
		String converted = text;
		
		// 方法1：如果有完整的开始和结束标签，转换为 <color:#RRGGBB>text</color>
		Pattern pairedPattern = Pattern.compile("<#([a-fA-F0-9]{6})>(.*?)</#\\1>");
		Matcher pairedMatcher = pairedPattern.matcher(converted);
		converted = pairedMatcher.replaceAll("<color:#$1>$2</color>");
		
		// 方法2：处理没有结束标签的情况（CustomFishing的简化格式）
		// 将 <#RRGGBB> 转换为 <color:#RRGGBB>
		Pattern openPattern = Pattern.compile("<#([a-fA-F0-9]{6})>");
		Matcher openMatcher = openPattern.matcher(converted);
		converted = openMatcher.replaceAll("<color:#$1>");
		
		// 清理剩余的结束标签（如果有的话）
		Pattern closePattern = Pattern.compile("</#[a-fA-F0-9]{6}>");
		Matcher closeMatcher = closePattern.matcher(converted);
		converted = closeMatcher.replaceAll("</color>");
		
		return converted;
	}
	
	/**
	 * 安全地获取带颜色的消息，如果消息为null则返回默认值
	 * @param text 要处理的文本
	 * @param defaultText 如果text为null时返回的默认文本
	 * @return 带颜色的消息
	 */
	public static String getColoredMessageSafe(String text, String defaultText) {
		if (text == null) {
			Bukkit.getLogger().warning("[Codex] 消息内容为 null，使用默认值: " + defaultText);
			return getColoredMessage(defaultText);
		}
		return getColoredMessage(text);
	}
	
	/**
	 * 安全地获取带颜色的消息并应用替换，如果消息为null则返回替换后的默认值
	 * @param text 要处理的文本
	 * @param defaultText 如果text为null时返回的默认文本
	 * @param placeholder 要替换的占位符
	 * @param replacement 替换的值
	 * @return 带颜色的消息
	 */
	public static String getColoredMessageSafeReplaced(String text, String defaultText, String placeholder, String replacement) {
		String message = text;
		if (message == null) {
			Bukkit.getLogger().warning("[Codex] 消息内容为 null，使用默认值: " + defaultText);
			message = defaultText;
		}
		
		try {
			message = message.replace(placeholder, replacement);
		} catch (Exception e) {
			Bukkit.getLogger().warning("[Codex] 替换占位符 " + placeholder + " 时出错: " + e.getMessage());
		}
		
		return getColoredMessage(message);
	}

	public static String getCenteredMessage(String message){
		int CENTER_PX = 154;
		int messagePxSize = 0;
		boolean previousCode = false;
		boolean isBold = false;

		for(char c : message.toCharArray()){
			if(c == '§'){
				previousCode = true;
				continue;
			}else if(previousCode == true){
				previousCode = false;
				if(c == 'l' || c == 'L'){
					isBold = true;
					continue;
				}else isBold = false;
			}else{
				DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
				messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
				messagePxSize++;
			}
		}

		int halvedMessageSize = messagePxSize / 2;
		int toCompensate = CENTER_PX - halvedMessageSize;
		int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
		int compensated = 0;
		StringBuilder sb = new StringBuilder();
		while(compensated < toCompensate){
			sb.append(" ");
			compensated += spaceLength;
		}
		return (sb.toString() + message);
	}
	
	/**
	 * 设置ItemMeta的显示名称，支持MiniMessage格式
	 * @param itemMeta 要修改的ItemMeta
	 * @param displayName 显示名称文本
	 */
	public static void setItemDisplayName(org.bukkit.inventory.meta.ItemMeta itemMeta, String displayName) {
		if (itemMeta == null || displayName == null) return;
		
		// 直接使用传统API + MiniMessage文本处理
		itemMeta.setDisplayName(getColoredMessage(displayName));
	}
	
	/**
	 * 设置ItemMeta的Lore，支持MiniMessage格式
	 * @param itemMeta 要修改的ItemMeta
	 * @param lore Lore列表
	 */
	public static void setItemLore(org.bukkit.inventory.meta.ItemMeta itemMeta, java.util.List<String> lore) {
		if (itemMeta == null || lore == null) return;
		
		// 直接使用传统API + MiniMessage文本处理
		java.util.List<String> coloredLore = new java.util.ArrayList<>();
		for (String line : lore) {
			coloredLore.add(getCustomFishingColoredMessage(line));
		}
		itemMeta.setLore(coloredLore);
	}
	
	/**
	 * 使用Component API设置ItemMeta显示名称
	 */
	private static void setItemDisplayNameComponent(org.bukkit.inventory.meta.ItemMeta itemMeta, String displayName) throws Exception {
		// 检查是否包含需要特殊处理的格式
		String processedName = displayName;
		if (containsMiniMessageTags(displayName) || containsCustomFishingTags(displayName)) {
			processedName = preprocessForMiniMessage(displayName);
		}
		
		// 直接使用MiniMessage解析
		Component component = MINI_MESSAGE.deserialize(processedName);
		
		// 尝试使用不同的方法设置Component displayName
		try {
			// 方法1: 使用HasDisplayName接口
			Class<?> hasDisplayNameClass = Class.forName("io.papermc.paper.inventory.meta.HasDisplayName");
			hasDisplayNameClass.getMethod("displayName", Component.class)
				.invoke(itemMeta, component);
		} catch (ClassNotFoundException e) {
			// 方法2: 直接使用ItemMeta的displayName方法
			itemMeta.getClass().getMethod("displayName", Component.class)
				.invoke(itemMeta, component);
		}
	}
	
	/**
	 * 使用Component API设置ItemMeta Lore
	 */
	private static void setItemLoreComponents(org.bukkit.inventory.meta.ItemMeta itemMeta, java.util.List<String> lore) throws Exception {
		java.util.List<Component> components = new java.util.ArrayList<>();
		
		for (int i = 0; i < lore.size(); i++) {
			String line = lore.get(i);
			
			try {
				// 处理每行lore
				String processedLine = line;
				
				if (containsMiniMessageTags(line) || containsCustomFishingTags(line)) {
					processedLine = preprocessForMiniMessage(line);
				}
				
				// 直接使用MiniMessage解析
				Component component = MINI_MESSAGE.deserialize(processedLine);
				components.add(component);
			} catch (Exception e) {
				Bukkit.getLogger().warning("[Codex] 处理lore行 [" + i + "] 时出错: " + line + ", 错误: " + e.getMessage());
				// 出错时，添加一个空component
				components.add(Component.empty());
			}
		}
		
		// 尝试使用不同的方法设置Component lore
		try {
			// 方法1: 使用HasLore接口
			Class<?> hasLoreClass = Class.forName("io.papermc.paper.inventory.meta.HasLore");
			hasLoreClass.getMethod("lore", java.util.List.class)
				.invoke(itemMeta, components);
		} catch (ClassNotFoundException e) {
			// 方法2: 直接使用ItemMeta的lore方法
			itemMeta.getClass().getMethod("lore", java.util.List.class)
				.invoke(itemMeta, components);
		}
	}
}
