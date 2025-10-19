# Codex 图鉴插件

> 原项目地址: https://www.spigotmc.org/resources/codex-rpg-discoveries-1-8-1-17.90371/

## 1. 插件简介

Codex 是一款 RPG 图鉴插件，支持通过击杀生物、进入区域等多种方式解锁图鉴条目，并为玩家提供文字、标题、声音等多样化奖励。

## 2. 功能特性

### 2.1 基础触发器

- `ITEM_OBTAIN` 首次获取指定物品时触发
- `COMMAND_RUN` 首次执行指定指令时触发
- `MOB_KILL` 击杀指定生物时触发
- `WORLDGUARD_REGION` 进入指定区域时触发

### 2.2 物品判定系统

- `item_type` 物品材质（Material）
- `custom_model_data` 自定义模型值（Integer）
- `components` `PersistentDataContainer` 中的自定义组件键，支持多个键以分号分隔
- `craft_engine_id` 兼容 Craft-Engine 自定义物品的唯一 ID

### 2.3 第三方插件兼容

- **Craft-Engine 兼容**
  - 读取 NBT 键 `craftengine:id`，与 Craft-Engine 物品ID对应
  - 实现跨插件物品识别系统

- **ItemsAdder 兼容**
  - 自动检测服务器是否安装 [ItemsAdder](https://spigotmc.org/resources/81208)
  - 通过 `CustomStack.byItemStack` 获取物品 ID，比对 `itemsadder_item_id` 字段
  - 插件启动时控制台输出挂钩状态

- **MMOItems 兼容**
  - 自动检测服务器是否安装 [MMOItems](https://spigotmc.org/resources/mmoitems.39267/)
  - 支持 YAML 字段 `mmoitems_type` 与 `mmoitems_id`，可单独或组合使用
  - 识别顺序：先读取物品 `PersistentDataContainer`，若不存在则回退至 NBT 键

- **Aiyatsbus 兼容**
  - 自动检测服务器是否安装 [Aiyatsbus](https://www.spigotmc.org/resources/aiyatsbus.110939/)
  - 支持原版附魔和 Aiyatsbus 附魔的识别与触发

### 2.4 附魔图鉴系统

- 自动识别服务器上所有可用的附魔（原版 + Aiyatsbus）
- 为每个附魔创建对应的发现项
- 生成美观的附魔图鉴GUI，包括边框和返回按钮
- 完全可配置的界面和功能
- 支持触发类型 `ENCHANTMENT_DISCOVER`

### 2.5 钓鱼图鉴系统

- 自动与 CustomFishing 插件集成，记录玩家钓鱼成果
- 根据生物群系和鱼类种类自动生成图鉴条目
- 记录每种鱼的最大尺寸和总捕获数量
- 自定义奖励和解锁通知系统
- 精美的图鉴界面，显示详细的鱼类信息

### 2.6 世界历史系统

- 专为 RPG 世界观构建设计的历史记录功能
- 支持通过各种触发方式解锁历史记录片段
- 玩家可以收集世界背景故事，了解服务器世界观
- 自定义奖励系统，鼓励玩家探索和收集历史信息
- 支持富文本格式，创建丰富多彩的历史描述

### 2.7 数据存储系统

- 多种数据存储方式：YAML文件、H2数据库、MySQL数据库
- 高性能的H2嵌入式数据库，无需额外配置即可获得优秀性能
- 可配置的MySQL连接池，支持跨服数据同步
- 智能内存管理，减少内存占用
- 自动保存系统，防止数据丢失

## 3. 配置示例

### 3.1 基础发现配置

```yml
discoveries:
  first_diamond:
    name: "&b钻石在手"
    description:
      - "&7玩家第一次获得钻石"
    discovered_on:
      type: ITEM_OBTAIN
      value:
        item_type: DIAMOND            # 可选，支持多个值 DIAMOND;EMERALD
        custom_model_data: 101        # 可选
        components: "codex:special"   # 可选，多键 codex:key1;codex:key2

  open_codex:
    name: "&d第一次打开图鉴"
    description: ["&7第一次使用 /codex"]
    discovered_on:
      type: COMMAND_RUN
      value:
        command: codex                # 不包含前导斜杠，可写多个 codex;menu

  ce_mythic_blade:
    name: "&6神话之刃"
    description: ["&7获得 Craft-Engine 自定义神话之刃"]
    discovered_on:
      type: ITEM_OBTAIN
      value:
        craft_engine_id: myplugin:mythic_blade

  ia_magic_wand:
    name: "&d魔法杖"
    description: ["&7使用 ItemsAdder 魔法杖"]
    discovered_on:
      type: ITEM_OBTAIN
      value:
        itemsadder_item_id: myitems:magic_wand

  enchant_sharpness:
    name: "&e锋利"
    description: ["&7发现原版锋利附魔"]
    discovered_on:
      type: ENCHANTMENT_DISCOVER
      value:
        enchantment_id: minecraft:sharpness

  enchant_telekinesis:
    name: "&d心灵手巧"
    description: ["&7发现 Aiyatsbus 心灵手巧附魔"]
    discovered_on:
      type: ENCHANTMENT_DISCOVER
      value:
        enchantment_id: aiyatsbus:telekinesis

  legendary_sword:
    name: "&6传奇之刃"
    description: ["&7拾取 MMOItems 传奇之刃"]
    discovered_on:
      type: ITEM_OBTAIN
      value:
        mmoitems_type: SWORD
        mmoitems_id: LEGENDARY_SWORD
```

### 3.2 附魔图鉴配置

```yaml
# 基本设置
settings:
  # 是否启用附魔图鉴功能
  enabled: true
  # 是否自动生成附魔图鉴
  auto_generate: true
  # 附魔分类名称
  category_name: "enchantments"
  # 附魔分类在主菜单中的位置
  main_menu_slot: 24

# 附魔过滤设置
filter:
  # 是否包含原版附魔
  include_vanilla: true
  # 是否包含Aiyatsbus附魔
  include_aiyatsbus: true
  # 排除的附魔列表
  excluded:
    - "minecraft:mending"
```

### 3.3 钓鱼图鉴配置

```yaml
config:
  inventory_items:
    category:
      id: FISHING_ROD
      name: "&7Category: #3498db&l钓鱼图鉴"
      lore:
        - "#eeeeee记录你在各个生物群系中"
        - "#eeeeee钓到的鱼类信息。"
    discovery_unlocked:
      id: "%icon_id%"
      name: "%name%"
      lore:
        - "&7种类: &f%type%"
        - "&7群系: &f%biome%"
        - "&7最大尺寸: &f%max_size% cm"
        - "&7总计钓到: &f%amount%条"
  rewards:
    per_discovery:
      - "centered_message: #eeeeee&l钓鱼图鉴更新"
      - "centered_message: &7发现新鱼种: %name%"
      - "title: 20;60;20;#eeeeee&l钓鱼图鉴更新;&7发现新鱼种: %name%"
      - "playsound: ENTITY_FISHING_BOBBER_SPLASH;10;1.0"
      - "console_command: xp give %player% 25"
```

### 3.4 世界历史配置

```yaml
config:
  inventory_items:
    category:
      id: BOOK
      name: "&7Category: #feb96b&lWorld History"
      lore:
        - "#eeeeeeIn your adventures you'll find some"
        - "#eeeeeeInteresting knowledge."
    discovery_unlocked:
      id: PAPER
      name: "%name%"
      lore:
        - "%description%"
  rewards:
    per_discovery:
      - "centered_message: #eeeeee&lCODEX UPDATED"
      - "centered_message: &7World History: %name%"
      - "title: 20;60;20;#eeeeee&lCODEX UPDATED;&7World History: %name%"
      - "playsound: BLOCK_GILDED_BLACKSTONE_STEP;10;0.1"
      - "console_command: xp give %player% 50"

discoveries:
  irium_attack_on_kryngel:
    name: "#feb96b&lIrium Attack on Kryngel"
    description:
      - "#eeeeee400 years ago a mysterious group of people called"
      - "#eeeeee'The Irium' attacked the city of Kryngel by entering"
      - "#eeeeeethe sewers unnoticed. They destroyed everything from"
      - "#eeeeeeinside, creating chaos across the streets..."
```

### 3.5 数据存储配置

```yaml
# 存储类型: file, h2, mysql
# 推荐使用h2，性能更好且无需额外配置
# - file: 使用YAML文件存储，简单但不支持跨服
# - h2: 使用嵌入式H2数据库，高性能且无需外部依赖
# - mysql: 使用MySQL数据库，支持跨服数据共享
storage_type: h2

# H2数据库配置
h2_database:
  pool:
    connectionTimeout: 5000
    maximumPoolSize: 10
    keepaliveTime: 30000
    idleTimeout: 600000
    maxLifetime: 1800000

# MySQL数据库配置(仅在storage_type为mysql时生效)
mysql_database:
  host: localhost
  port: 3306
  database: codex
  username: root
  password: password
  # 连接池配置
  pool:
    connectionTimeout: 5000
    maximumPoolSize: 10

# 内存管理
memory_management:
  # 内存清理间隔（秒），默认1800秒(30分钟)
  cleanup_interval: 1800
  # 是否在服务器低负载时自动清理内存中不在线玩家的数据
  clean_offline_players: true

# 玩家数据
# 数据保存间隔（秒），默认900秒(15分钟)
player_data_save: 900
auto_save_enabled: true
show_save_notifications: false
```

## 4. 配置字段说明

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `type` | 枚举 | 触发类型，取值：`ITEM_OBTAIN`、`COMMAND_RUN`、`MOB_KILL`、`WORLDGUARD_REGION` 等 |
| `item_type` | String | 物品材质，多个值用 `;` 分隔 |
| `custom_model_data` | Integer | 自定义模型数值，需完整匹配 |
| `components` | String | `PersistentDataContainer` 中的组件键列表，多个用 `;` 分隔，全部满足触发 |
| `craft_engine_id` | String | Craft-Engine 自定义物品 ID |
| `itemsadder_item_id` | String | ItemsAdder 自定义物品 ID |
| `mmoitems_type` | String | MMOItems 物品类型 |
| `mmoitems_id` | String | MMOItems 物品 ID |
| `enchantment_id` | String | 附魔 ID，格式为 `namespace:key` |
| `command` | String | 指令名（不含 `/`），多个用 `;` 分隔 |

如字段为空或不存在即忽略该判定。

## 5. 更新与安装

### 5.1 更新步骤

1. 将新的 jar 替换至 `plugins/` 目录
2. 重载或重启服务器
3. 在 `plugins/Codex/categories/` 下编辑对应 `*.yml`，参考配置示例填写新的触发条件
4. 使用 `/codex reload` 热重载配置即可生效

### 5.2 插件挂钩状态检查

插件启动时控制台会输出各个挂钩的状态：

```
[Codex] CraftEngine Hook: 成功
[Codex] ItemsAdder Hook: 成功
[Codex] MMOItems Hook: 成功
[Codex] Aiyatsbus Hook: 成功
```

## 6. 常见问题

1. **未触发发现？** 
   - 确认 YAML 缩进正确，字段拼写大小写无误
   - 检查物品是否满足所有设置的判定条件

2. **Craft-Engine 物品无法识别？** 
   - 使用 `/ce item info`（示例命令，实际以 Craft-Engine 为准）确认物品的 `ID` 与 `craft_engine_id` 一致

3. **附魔图鉴无法显示？**
   - 检查 `enchantments.yml` 中 `settings.enabled` 是否设为 true
   - 确认 Aiyatsbus 挂钩状态是否正常

4. **数据库连接错误？**
   - 检查数据库配置是否正确
   - MySQL模式下确认数据库用户权限是否足够

## 7. 贡献与支持

欢迎提交 PR、Issue 或在 Spigot 帖子中反馈问题。如有问题可通过以下方式联系：

- Spigot 论坛：在原帖下回复
- GitHub Issue：提交问题报告
