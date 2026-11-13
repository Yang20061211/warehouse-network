@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

echo 正在编译...
if exist "build" rmdir /s /q "build"
mkdir "build\classes"

powershell -NoProfile -Command "$sources = Get-ChildItem -LiteralPath 'src' -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName; & javac --release 17 -encoding UTF-8 -d 'build/classes' $sources; exit $LASTEXITCODE"
if errorlevel 1 goto compile_error

jar --create --file "build\warehouse-network.jar" --main-class logistics.Main -C "build\classes" .
if errorlevel 1 goto compile_error

echo.
echo 编译完成：build\warehouse-network.jar
echo 双击「运行系统.bat」即可启动。
pause
exit /b 0

:compile_error
echo.
echo 编译失败：请确认已安装 JDK 17+。
pause
exit /b 1
