#!/bin/bash

# 批量发布所有starter模块到Nexus仓库

echo "开始发布所有starter模块到Nexus仓库..."

# 检查nexus.properties文件是否存在
if [ ! -f "nexus.properties" ]; then
    echo "错误: nexus.properties文件不存在，请先创建并配置Nexus仓库信息"
    exit 1
fi

# 检查Gradle wrapper是否存在
if [ ! -f "gradlew" ]; then
    echo "错误: gradlew文件不存在，请确保在项目根目录执行此脚本"
    exit 1
fi

# 获取所有starter模块
echo "获取所有starter模块..."
modules=$(./gradlew projects | grep 'itsuka-.*-spring-boot-starter' | sed 's/^Project /:/' | sed 's/$/:publish/' | tr '\n' ' ')

echo "将发布以下模块:"
echo $modules

# 执行发布操作
echo "正在发布所有starter模块..."
./gradlew $modules

# 检查发布结果
if [ $? -eq 0 ]; then
    echo "所有starter模块发布成功！"
    echo "请登录Nexus仓库查看已发布的组件"
else
    echo "发布失败，请检查错误信息"
    exit 1
fi