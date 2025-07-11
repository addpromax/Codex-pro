# Codex 图鉴插件

> 原项目地址: https://www.spigotmc.org/resources/codex-rpg-discoveries-1-8-1-17.90371/

## 1. 插件简介
Codex 是一款 RPG 图鉴插件，支持通过击杀生物、进入区域等多种方式解锁图鉴条目，并为玩家提供文字、标题、声音等多样化奖励。

## 2. 本次更新内容
1. **新增触发器**
   - `ITEM_OBTAIN` 首次获取指定物品时触发。
   - `COMMAND_RUN` 首次执行指定指令时触发。
2. **物品判定维度扩展**
   - `item_type` 物品材质（Material）。
   - `custom_model_data` 自定义模型值（Integer）。
   - `components` `PersistentDataContainer` 中的自定义组件键；支持多个键以分号分隔，全部存在即匹配。
   - `craft_engine_id` 兼容 Craft-Engine 自定义物品的唯一 ID。
3. **Craft-Engine 兼容**
   - 读取 NBT 键 `craftengine:id`，可与 Craft-Engine 定义的物品ID对应，实现跨插件识别。

## 3. YAML 配置示例
```yml
config:
  # 省略与旧版一致的显示、奖励配置...

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
```

## 4. 配置字段说明
| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| `type` | 枚举 | 触发类型，取值：`ITEM_OBTAIN`、`COMMAND_RUN`、`MOB_KILL`、`WORLDGUARD_REGION` 等 |
| `item_type` | String | 物品材质，多个值用 `;` 分隔 |
| `custom_model_data` | Integer | 自定义模型数值，需完整匹配 |
| `components` | String | `PersistentDataContainer` 中的组件键列表，多个用 `;` 分隔，全部满足触发 |
| `craft_engine_id` | String | Craft-Engine 自定义物品 ID |
| `command` | String | 指令名（不含 `/`），多个用 `;` 分隔 |

如字段为空或不存在即忽略该判定。

## 5. 更新步骤
1. 将新的 jar 替换至 `plugins/` 目录。
2. 重载或重启服务器。
3. 在 `plugins/Codex/categories/` 下编辑对应 `*.yml`，参考上方示例填写新的触发条件。
4. `/codex reload` 热重载配置即可生效。

## 6. 常见问题
1. **未触发发现？** 请确认 YAML 缩进正确，字段拼写大小写无误；检查物品是否满足所有设置的判定条件。
2. **Craft-Engine 物品无法识别？** 请使用 `/ce item info`（示例命令，实际以 Craft-Engine 为准）确认物品的 `ID` 与 `craft_engine_id` 一致。

## 7. 贡献与支持
欢迎提交 PR、Issue 或在 Spigot 帖子中反馈问题。
