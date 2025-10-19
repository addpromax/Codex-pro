# CustomFishing Biome 判断失败原因诊断

## 问题分析

从日志可以确认：
1. ✅ `other_location` 存在（鱼钩位置）
2. ✅ `surrounding=water`（在水中）
3. ✅ `open_water=true`（开放水域）
4. ❌ 所有淡水鱼概率 = 0

**推理：`!biome` 条件返回 false，意味着 SparrowHeart 返回的 biome 在海洋列表中**

## 需要确认的事项

### 1. 实际 Biome 是什么？

在钓鱼的位置执行：
```
/customfishing debug biome
```

**预期输出格式**：
- `minecraft:river` ← 淡水
- `minecraft:ocean` ← 海洋
- `minecraft:plains` ← 平原（有水也算淡水）

### 2. 检查区块的生物群系

使用 F3 调试屏幕：
1. 按 F3
2. 查找 "Biome:" 一行
3. 记录显示的值

**可能的情况**：
- 玩家站在平原，但鱼钩落在海洋边缘
- 该区域实际是 `minecraft:ocean` 或 `minecraft:lukewarm_ocean` 等

## 临时诊断补丁

如果您想知道 CustomFishing 实际获取到的 biome，可以：

### 方法 1：添加调试输出到 loot-conditions.yml

```yaml
river_fish:
  conditions:
    '!biome':
    - minecraft:ocean
    - minecraft:deep_ocean
    # ... 其他海洋
    papi:  # 添加一个 PAPI 条件来输出 biome
      type: papi
      value1: "%player_name%"  # 只是为了触发
      value2: "%player_name%"
  list:
  - 'group_for_each:river_fish&no_star:*1'
  # ...
```

### 方法 2：修改 CustomFishing 源码添加调试

在 `BukkitRequirementManager.java` 第 891-892 行添加：

```java
String currentBiome = SparrowHeart.getInstance().getBiomeResourceLocation(location);
plugin.getLogger().info("[BIOME-DEBUG] Current biome: " + currentBiome + " at " + location);
plugin.getLogger().info("[BIOME-DEBUG] Checking against: " + biomes);
if (biomes.contains(currentBiome))
    plugin.getLogger().info("[BIOME-DEBUG] Biome IS in list (ocean)");
else
    plugin.getLogger().info("[BIOME-DEBUG] Biome NOT in list (not ocean)");
```

### 方法 3：使用测试配置

创建一个测试配置，记录所有可能的情况：

```yaml
# 测试：移除 biome 条件，看是否能钓到鱼
river_fish_test:
  conditions:
    in-water: true
    environment:
    - normal
  list:
  - gold_fish:+100  # 给一个很高的权重
```

如果这样能钓到 `gold_fish`，就确认是 biome 判断问题。

## 最可能的原因

基于经验，最可能的原因是：

### **该区域实际上是海洋生物群系**

即使看起来像河流或湖泊，在 Minecraft 中该区块可能被标记为海洋。特别是：
- 靠近海洋边缘的河流
- 大型水体（超过一定大小会被视为海洋）
- 地图生成时的生物群系边界问题

### 解决方案

1. **移动到明确的淡水区域**
   - 小河流
   - 内陆湖泊
   - 远离海洋的水域

2. **检查并修正 biome**
   - 使用 WorldEdit 或其他插件修改区块的 biome
   - `/worldedit biome minecraft:river`

3. **修改配置以兼容当前 biome**
   - 如果该区域确实是海洋，但您想把它当作淡水，可以移除 biome 条件
   - 或者添加该 biome 到淡水列表

## 下一步

请先执行 `/customfishing debug biome` 并告诉我输出结果，我们就能确定具体问题了。

