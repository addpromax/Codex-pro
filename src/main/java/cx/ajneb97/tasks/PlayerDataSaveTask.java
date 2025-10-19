package cx.ajneb97.tasks;

import cx.ajneb97.Codex;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerDataSaveTask {

	private Codex plugin;
	private boolean end;
	private volatile boolean saving = false;
	
	public PlayerDataSaveTask(Codex plugin) {
		this.plugin = plugin;
		this.end = false;
	}
	
	public void end() {
		end = true;
	}
	
	public boolean isSaving() {
		return saving;
	}
	
	public void start(int seconds) {
		long ticks = seconds* 20L;
		
		new BukkitRunnable() {
			@Override
			public void run() {
				if(end) {
					this.cancel();
				}else {
					execute();
				}
			}
			
		}.runTaskTimerAsynchronously(plugin, 0L, ticks);
	}
	
	public void execute() {
		// 如果任务已结束或正在保存，跳过本次执行
		if (end || saving) {
			return;
		}
		
		saving = true;
		try {
			long startTime = System.currentTimeMillis();
			boolean showNotifications = plugin.getConfigsManager().getMainConfigManager().isShowSaveNotifications();
			
			if (showNotifications) {
				plugin.getLogger().info("执行定时数据保存...");
			}
			
			// 使用数据库管理器保存数据
			String storageType = plugin.getConfigsManager().getMainConfigManager().getStorageType();
			boolean isFileStorage = "file".equalsIgnoreCase(storageType);
			int savedCount = 0;
			
			// 保存已修改的玩家数据 - 定时保存使用简洁版本
			savedCount = plugin.getPlayerDataManager().saveAllModifiedData(false);
			
			if (showNotifications && savedCount > 0) {
				if (isFileStorage) {
					plugin.getLogger().info("保存了 " + savedCount + " 个已修改的玩家数据到文件");
				} else {
					plugin.getLogger().info("保存了 " + savedCount + " 个已修改的玩家数据到数据库");
				}
			}
			
			long timeTaken = System.currentTimeMillis() - startTime;
			
			if (showNotifications) {
				plugin.getLogger().info("定时数据保存完成，耗时: " + timeTaken + "ms");
			}
		} catch (Exception e) {
			plugin.getLogger().severe("定时数据保存失败: " + e.getMessage());
			e.printStackTrace();
		} finally {
			saving = false;
		}
	}
}
