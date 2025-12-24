#!/bin/bash

# Nexus发布脚本
# 用于将项目发布到自建的Nexus仓库

echo "开始发布到Nexus仓库..."

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

# 执行发布操作
echo "正在构建并发布项目..."
./gradlew clean build publish

# 检查发布结果
if [ $? -eq 0 ]; then
    echo "发布成功！"
    echo "请登录Nexus仓库查看已发布的组件"
else
    echo "发布失败，请检查错误信息"
    exit 1
fi