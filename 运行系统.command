#!/bin/zsh
# 双击运行城际仓网规划系统
cd "$(dirname "$0")" || exit 1

JAR="build/warehouse-network.jar"

if [ ! -f "$JAR" ]; then
  echo "首次运行，正在编译（需要 JDK 17 或更高版本）..."
  mkdir -p build/classes
  find src -name '*.java' > build/sources.txt
  if ! javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt; then
    echo ""
    echo "编译失败：请确认已安装 JDK 17+（终端输入 java -version 检查）。"
    read "?按回车键退出..."
    exit 1
  fi
  jar --create --file "$JAR" --main-class logistics.Main -C build/classes . || exit 1
  echo "编译完成。"
fi

java -jar "$JAR"
