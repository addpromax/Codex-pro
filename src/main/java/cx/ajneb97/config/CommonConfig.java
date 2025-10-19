package cx.ajneb97.config;

import cx.ajneb97.Codex;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommonConfig {

    private String fileName;
    private FileConfiguration fileConfiguration = null;
    private File file = null;
    private String route;
    private Codex plugin;
    private String folderName;
    private boolean newFile;
    private boolean isFirstTime;

    public CommonConfig(String fileName, Codex plugin, String folderName, boolean newFile){
        this.fileName = fileName;
        this.plugin = plugin;
        this.newFile = newFile;
        this.folderName = folderName;
        this.isFirstTime = false;
    }

    public String getPath(){
        return this.fileName;
    }

    public void registerConfig(){
        if(folderName != null){
            file = new File(plugin.getDataFolder() +File.separator + folderName,fileName);
        }else{
            file = new File(plugin.getDataFolder(), fileName);
        }

        route = file.getPath();

        if(!file.exists()){
            isFirstTime = true;
            if(newFile) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                if(folderName != null){
                    plugin.saveResource(folderName+File.separator+fileName, false);
                }else{
                    plugin.saveResource(fileName, false);
                }

            }
        }

        fileConfiguration = new YamlConfiguration();
        try {
            fileConfiguration.load(file);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
    public void saveConfig() {
        try {
            fileConfiguration.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        if (fileConfiguration == null) {
            reloadConfig();
        }
        return fileConfiguration;
    }

    public boolean reloadConfig() {
        if (fileConfiguration == null) {
            if(folderName != null){
                file = new File(plugin.getDataFolder() +File.separator + folderName, fileName);
            }else{
                file = new File(plugin.getDataFolder(), fileName);
            }

        }
        fileConfiguration = YamlConfiguration.loadConfiguration(file);

        if(file != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(file);
            fileConfiguration.setDefaults(defConfig);
        }
        return true;
    }

    public String getRoute() {
        return route;
    }

    public boolean isFirstTime() {
        return isFirstTime;
    }

    public void setFirstTime(boolean firstTime) {
        isFirstTime = firstTime;
    }

    /**
     * 获取配置文件的原始字符串内容
     * @return 配置文件的原始内容
     * @throws IOException 如果读取失败
     */
    public String getStringContent() throws IOException {
        try {
            // 使用之前创建好的file对象，确保路径正确
            if (file == null || !file.exists()) {
                throw new IOException("配置文件不存在: " + fileName);
            }
            byte[] encoded = Files.readAllBytes(file.toPath());
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().severe("读取配置文件内容失败: " + fileName + " - " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * 保存字符串内容到配置文件
     * @param content 要保存的内容
     * @throws IOException 如果保存失败
     */
    public void saveStringContent(String content) throws IOException {
        try {
            // 使用之前创建好的file对象，确保路径正确
            if (file == null) {
                throw new IOException("配置文件未初始化: " + fileName);
            }
            
            // 确保父目录存在
            File parent = file.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new IOException("无法创建父目录: " + parent.getPath());
                }
            }
            
            // 验证内容是否为有效的YAML格式
            try {
                YamlConfiguration testConfig = new YamlConfiguration();
                testConfig.loadFromString(content);
                plugin.getLogger().info("YAML验证通过，内容有效");
            } catch (InvalidConfigurationException e) {
                plugin.getLogger().severe("内容不是有效的YAML格式: " + e.getMessage());
                plugin.getLogger().severe("问题可能出现在: " + getErrorContext(content, e.getMessage()));
                throw new IOException("无法保存内容，YAML格式无效: " + e.getMessage(), e);
            }
            
            // 使用临时文件写入，然后重命名，以防写入中断
            File tempFile = new File(file.getParentFile(), file.getName() + ".tmp");
            Files.write(tempFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
            
            // 备份原文件以防发生错误
            if (file.exists()) {
                File backupFile = new File(file.getParentFile(), file.getName() + ".bak");
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                Files.copy(file.toPath(), backupFile.toPath());
            }
            
            // 重命名临时文件为目标文件
            if (file.exists()) {
                file.delete(); // 确保先删除原文件，防止在某些系统上无法重命名
            }
            
            if (!tempFile.renameTo(file)) {
                throw new IOException("无法重命名临时文件到目标文件: " + file.getPath());
            }
            
            plugin.getLogger().info("成功保存配置文件: " + fileName);
            
            // 确保配置可以正确加载
            try {
                YamlConfiguration testReload = new YamlConfiguration();
                testReload.load(file);
            } catch (InvalidConfigurationException e) {
                // 如果加载失败，尝试恢复备份
                File backupFile = new File(file.getParentFile(), file.getName() + ".bak");
                if (backupFile.exists()) {
                    Files.copy(backupFile.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().warning("配置加载失败，已恢复备份: " + e.getMessage());
                } else {
                    plugin.getLogger().severe("配置加载失败，且无法恢复备份: " + e.getMessage());
                }
                throw new IOException("保存的配置文件无法正确加载: " + e.getMessage(), e);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("保存配置文件内容失败: " + fileName + " - " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * 提取YAML错误上下文，帮助调试
     * @param content YAML内容
     * @param errorMessage 错误信息
     * @return 包含错误上下文的字符串
     */
    private String getErrorContext(String content, String errorMessage) {
        // 尝试从错误信息中提取行号
        int lineNumber = -1;
        String linePattern = "line (\\d+)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(linePattern);
        java.util.regex.Matcher matcher = pattern.matcher(errorMessage);
        if (matcher.find()) {
            try {
                lineNumber = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }
        
        if (lineNumber <= 0) {
            return "无法确定错误位置";
        }
        
        // 分割内容为行
        String[] lines = content.split("\n");
        if (lineNumber > lines.length) {
            return "错误行号超出内容范围";
        }
        
        // 提取错误行及其上下文
        StringBuilder context = new StringBuilder();
        context.append("错误发生在第 ").append(lineNumber).append(" 行附近:\n");
        
        // 添加前3行和后3行作为上下文（如果存在）
        int start = Math.max(0, lineNumber - 4);
        int end = Math.min(lines.length - 1, lineNumber + 2);
        
        for (int i = start; i <= end; i++) {
            context.append(i + 1).append(": ");
            if (i + 1 == lineNumber) {
                context.append(">>> ");
            } else {
                context.append("    ");
            }
            context.append(lines[i]).append("\n");
        }
        
        return context.toString();
    }
}
