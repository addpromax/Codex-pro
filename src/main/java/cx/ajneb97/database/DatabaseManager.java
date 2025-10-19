package cx.ajneb97.database;

import cx.ajneb97.model.data.PlayerData;

import java.sql.Connection;

/**
 * 数据库管理器接口，定义所有与存储相关的操作
 * 提供统一的数据访问层，支持不同的数据库实现
 */
public interface DatabaseManager {

    /**
     * 初始化数据库连接和相关表结构
     * @return 是否成功初始化
     */
    boolean initialize();

    /**
     * 加载所有数据到内存
     */
    void loadData();

    /**
     * 获取数据库连接，用于直接SQL操作
     * @return 数据库连接对象，如果不支持则返回null
     */
    Connection getConnection();

    /**
     * 根据UUID异步获取玩家数据
     * @param uuid 玩家UUID
     * @param callback 回调函数
     */
    void getPlayer(String uuid, PlayerCallback callback);

    /**
     * 创建新玩家数据
     * @param player 玩家数据对象
     */
    void createPlayer(PlayerData player);

    /**
     * 更新玩家名称
     * @param player 玩家数据对象
     */
    void updatePlayerName(PlayerData player);

    /**
     * 添加一个发现项
     * @param uuid 玩家UUID
     * @param categoryName 分类名称
     * @param discoveryName 发现项名称
     * @param discoveryDate 发现日期
     */
    void addDiscovery(String uuid, String categoryName, String discoveryName, String discoveryDate);

    /**
     * 更新发现项的行为执行时间
     * @param uuid 玩家UUID
     * @param categoryName 分类名称
     * @param discoveryName 发现项名称
     */
    void updateMillisActionsExecuted(String uuid, String categoryName, String discoveryName);

    /**
     * 更新已完成的分类
     * @param uuid 玩家UUID
     * @param categoryName 分类名称
     */
    void updateCompletedCategories(String uuid, String categoryName);

    /**
     * 重置玩家特定分类或发现项的数据
     * @param uuid 玩家UUID
     * @param categoryName 分类名称 (可以为null表示所有分类)
     * @param discoveryName 发现项名称 (可以为null表示分类中所有发现项)
     */
    void resetDataPlayer(String uuid, String categoryName, String discoveryName);

    /**
     * 同步更新玩家所有数据
     * @param playerData 玩家数据对象
     */
    void updatePlayer(PlayerData playerData);
    
    /**
     * 同步分类和发现项到数据库
     */
    void syncCategoriesAndDiscoveries();
    
    /**
     * 关闭数据库连接
     */
    void shutdown();
} 