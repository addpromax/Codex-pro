# EnchantDisassembler

> 一个面向 Paper / Spigot 服务器的 Aiyatsbus 附魔分解与奖励插件

## ✨ 功能亮点

* **附魔分解**：支持所有带附魔物品（含附魔书），自动识别 Aiyatsbus 稀有度。
* **点数池系统**：各稀有度独立累计点数，支持池子上限与溢出池。
* **奖励领取**：池子满后点击领取；奖励由后台命令执行，天然兼容 PlaceholderAPI。
* **高度可自定义 GUI**：`gui.yml` 全面定义布局、材质、模型数据；背景板可一键开启/关闭。
* **跨服 MySQL 同步**：使用复合键 `(uuid, rarity)` 保证高并发下的数据安全，彻底杜绝刷点。
* **MiniMessage 兼容**：所有标题 / 提示文本均支持 Adventure MiniMessage 与经典 `&` 颜色码。

## 🖼️ 截图
> 由于各服 GUI 可自定义，此处省略示例。如需默认示例请查看 `src/main/resources/gui.yml`。

## 📦 安装

1. 确保服务器已安装 **[Aiyatsbus](https://github.com/PolarAstrum/Aiyatsbus)** 并启用。
2. 从 Releases 下载 `EnchantDisassembler-x.x.x.jar` 上传至 `plugins/` 目录。
3. 重启 / 重新加载服务器生成默认配置文件。
4. 若需跨服同步，编辑 `config.yml` ➜ `database.type` 改为 `mysql`，填写连接信息并保证表前缀不与已有表冲突。

## ⚙️ 主要配置

### `config.yml`（核心）

```yml
# 调试模式
debug: false

# 数据库配置（sqlite / mysql）
# ... 详见文件内注释

# 池子大小上限
pool-sizes:
  common: 20
  uncommon: 15
  rare: 10
  epic: 5
  legendary: 3
  splendid: 2
  artifact: 1

# 奖励命令
reward-commands:
  rare:
    - "give %player% minecraft:diamond 3"
```

### `gui.yml`（界面）
* `title`、`size` — 背包标题与行数
* `background.enabled` — 是否填充背景板
* `input.slots` — 玩家放入待分解物品的槽位
* `buttons.return` — 返还按钮
* `pools` — 各稀有度池子显示的槽位、材质、Lore 等

### `messages.yml`（多语言）
所有逻辑提示文本，支持 MiniMessage 与 `&` 颜色码，可根据需要翻译或本地化。

## 🔧 指令 & 权限

| 指令 | 作用 | 权限 |
|------|------|-------|
| `/ed` | 打开分解界面（控制台可指定 `/ed <玩家>`） | enchantdisassembler.use |
| `/ed reload` | 重新加载所有配置 | enchantdisassembler.reload |

## 🛠️ 开发 & 构建

项目使用 **Maven**。在源码根目录执行：

```bash
mvn clean package -DskipTests
```

生成的 jar 位于 `target/`。

## 🤝 贡献

欢迎提交 Issue / PR：
1. Fork → 修改 → Pull Request
2. 确保代码通过 `mvn test` 与基本运行测试

## 📜 许可证

本项目基于 MIT License 开源，详见 `LICENSE` 文件。 