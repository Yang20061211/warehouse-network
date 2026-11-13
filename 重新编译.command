#!/bin/zsh
# 重新编译并打包
cd "$(dirname "$0")" || exit 1

echo "正在编译..."
rm -rf build
mkdir -p build/classes
find src -name '*.java' > build/sources.txt

if ! javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt; then
  echo ""
  echo "编译失败：请确认已安装 JDK 17+。"
  read "?按回车键退出..."
  exit 1
fi

jar --create --file build/warehouse-network.jar --main-class logistics.Main -C build/classes . || exit 1
echo ""
echo "编译完成：build/warehouse-network.jar"
echo "双击「运行系统.command」即可启动。"
read "?按回车键退出..."
