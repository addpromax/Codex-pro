#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量修复缺少环境分组的鱼类配置
根据鱼类名称模式自动判断应该属于哪个环境分组
"""

import re
import yaml
from pathlib import Path

# 鱼类名称到环境分组的映射规则
FISH_GROUP_MAPPING = {
    # 淡水鱼
    'azure_minnow': 'river_fish',           # 小鱼，淡水
    'celestial_char': 'river_fish',          # 红点鲑，淡水
    'mystic_minnow': 'river_fish',           # 小鱼，淡水
    'enigma_minnow': 'river_fish',           # 小鱼，淡水
    'luminous_guppy': 'river_fish',          # 孔雀鱼，淡水
    
    # 海洋鱼
    'coral_glowfish': 'ocean_fish',          # 珊瑚相关，海洋
    'coral_guppy': 'ocean_fish',             # 珊瑚孔雀鱼，海洋
    'gilded_goby': 'ocean_fish',             # 虾虎鱼，海洋
    'enigma_angelfish': 'ocean_fish',        # 神仙鱼，海洋
    'celestial_clownfish': 'ocean_fish',     # 小丑鱼，海洋
    'mystic_clownfish': 'ocean_fish',        # 小丑鱼，海洋
    'luminous_lionfish': 'ocean_fish',       # 狮子鱼，海洋
    'luminous_lobster': 'ocean_fish',        # 龙虾，海洋
    'frostscale_fugu': 'ocean_fish',         # 河豚，海洋
}

def get_base_fish_name(fish_id):
    """从带星级的鱼类ID中提取基础鱼类名称"""
    # 移除星级后缀
    for suffix in ['_iridium_star', '_golden_star', '_silver_star']:
        if fish_id.endswith(suffix):
            return fish_id[:-len(suffix)]
    return fish_id

def get_star_level(fish_id):
    """获取鱼类的星级"""
    if fish_id.endswith('_iridium_star'):
        return 'iridium_star'
    elif fish_id.endswith('_golden_star'):
        return 'golden_star'
    elif fish_id.endswith('_silver_star'):
        return 'silver_star'
    return 'no_star'

def fix_fish_config(yaml_path):
    """修复YAML配置文件中缺少环境分组的鱼类"""
    print(f"📖 读取配置文件: {yaml_path}")
    
    # 读取YAML文件
    with open(yaml_path, 'r', encoding='utf-8') as f:
        config = yaml.safe_load(f)
    
    if not config:
        print("❌ 配置文件为空")
        return
    
    fixed_count = 0
    skipped_count = 0
    
    # 遍历所有鱼类
    for fish_id, fish_config in config.items():
        if not isinstance(fish_config, dict):
            continue
            
        # 获取当前的分组配置
        groups = fish_config.get('group', [])
        if not isinstance(groups, list):
            continue
        
        # 获取基础鱼类名称
        base_name = get_base_fish_name(fish_id)
        
        # 检查是否在映射表中
        if base_name not in FISH_GROUP_MAPPING:
            continue
        
        # 获取应该添加的环境分组
        target_group = FISH_GROUP_MAPPING[base_name]
        
        # 检查是否已经有环境分组
        has_env_group = any(g in ['ocean_fish', 'river_fish', 'lava_fish'] for g in groups)
        
        if not has_env_group:
            # 添加环境分组
            star_level = get_star_level(fish_id)
            
            # 重建分组列表：星级分组 + 环境分组 + 其他分组
            new_groups = []
            
            # 1. 先添加星级分组
            if star_level in groups:
                new_groups.append(star_level)
            
            # 2. 添加环境分组
            new_groups.append(target_group)
            
            # 3. 添加其他分组（排除星级和环境分组）
            for g in groups:
                if g not in new_groups and g not in ['ocean_fish', 'river_fish', 'lava_fish', 
                                                       'no_star', 'silver_star', 'golden_star', 'iridium_star']:
                    new_groups.append(g)
            
            # 更新配置
            fish_config['group'] = new_groups
            fixed_count += 1
            print(f"✅ 修复: {fish_id:40s} -> 添加分组 {target_group}")
        else:
            skipped_count += 1
    
    if fixed_count > 0:
        # 备份原文件
        backup_path = yaml_path.with_suffix('.yml.backup')
        print(f"\n📦 备份原文件到: {backup_path}")
        with open(backup_path, 'w', encoding='utf-8') as f:
            yaml.dump(config, f, allow_unicode=True, default_flow_style=False, sort_keys=False)
        
        # 写入修复后的配置
        print(f"💾 保存修复后的配置到: {yaml_path}")
        with open(yaml_path, 'w', encoding='utf-8') as f:
            yaml.dump(config, f, allow_unicode=True, default_flow_style=False, sort_keys=False)
        
        print(f"\n✨ 修复完成!")
        print(f"   - 修复了 {fixed_count} 个鱼类配置")
        print(f"   - 跳过了 {skipped_count} 个已有环境分组的配置")
    else:
        print(f"\n✨ 没有需要修复的配置")
        print(f"   - 跳过了 {skipped_count} 个已有环境分组的配置")

def main():
    """主函数"""
    yaml_path = Path(__file__).parent / 'default_with_exp_fixed_v2.yml'
    
    if not yaml_path.exists():
        print(f"❌ 找不到配置文件: {yaml_path}")
        return
    
    fix_fish_config(yaml_path)
    
    print("\n" + "="*60)
    print("🎉 所有操作完成!")
    print("="*60)
    print("\n📝 接下来的步骤:")
    print("1. 检查修复后的配置文件")
    print("2. 重启服务器或重载CustomFishing")
    print("3. 使用 /codex fishing 查看钓鱼图鉴")

if __name__ == '__main__':
    main()

