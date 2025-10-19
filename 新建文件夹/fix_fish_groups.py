#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修复 default_with_exp_fixed.yml 中鱼类的分组定义
为无星级的鱼添加 no_star 分组
"""

import yaml
import re
from pathlib import Path

def load_yaml_preserve_order(file_path):
    """加载 YAML 文件，保持顺序"""
    with open(file_path, 'r', encoding='utf-8') as f:
        return yaml.safe_load(f)

def save_yaml_preserve_format(data, file_path):
    """保存 YAML 文件，尽量保持原格式"""
    with open(file_path, 'w', encoding='utf-8') as f:
        yaml.dump(data, f, allow_unicode=True, default_flow_style=False, sort_keys=False)

def fix_fish_groups(input_file, output_file):
    """修复鱼类分组"""
    print(f"正在读取文件: {input_file}")
    data = load_yaml_preserve_order(input_file)
    
    fixed_count = 0
    star_levels = ['silver_star', 'golden_star', 'iridium_star']
    
    for fish_id, fish_data in data.items():
        if not isinstance(fish_data, dict):
            continue
            
        # 获取当前的 group 配置
        if 'group' not in fish_data:
            continue
            
        group = fish_data['group']
        
        # 将单个值转换为列表
        if isinstance(group, str):
            group = [group]
        elif not isinstance(group, list):
            continue
        
        # 检查是否是星级鱼
        has_star_level = any(star in group for star in star_levels)
        has_no_star = 'no_star' in group
        
        # 如果没有任何星级分组且没有 no_star，则添加 no_star
        if not has_star_level and not has_no_star:
            # 检查鱼类 ID 是否包含星级后缀
            is_star_fish = any(fish_id.endswith(f'_{star}') for star in star_levels)
            
            if not is_star_fish:
                # 确保 group 是列表
                if not isinstance(group, list):
                    group = [group]
                
                # 添加 no_star 到开头
                group.insert(0, 'no_star')
                fish_data['group'] = group
                fixed_count += 1
                print(f"✓ 为 {fish_id} 添加 no_star 分组，当前分组: {group}")
    
    print(f"\n修复完成！共修复 {fixed_count} 条鱼类")
    print(f"正在保存到: {output_file}")
    save_yaml_preserve_format(data, output_file)
    print("✓ 保存成功！")

if __name__ == '__main__':
    input_file = Path('default_with_exp_fixed.yml')
    output_file = Path('default_with_exp_fixed_v2.yml')
    
    if not input_file.exists():
        print(f"错误：找不到文件 {input_file}")
        exit(1)
    
    fix_fish_groups(input_file, output_file)
    print("\n提示：请检查生成的文件，确认无误后替换原文件")

