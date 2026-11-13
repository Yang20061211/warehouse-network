@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

set "JAR=build\warehouse-network.jar"

if not exist "%JAR%" (
  echo 首次运行，正在编译（需要 JDK 17 或更高版本）...
  if not exist "build\classes" mkdir "build\classes"
  powershell -NoProfile -Command "$sources = Get-ChildItem -LiteralPath 'src' -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName; & javac --release 17 -encoding UTF-8 -d 'build/classes' $sources; exit $LASTEXITCODE"
  if errorlevel 1 goto compile_error
  jar --create --file "%JAR%" --main-class logistics.Main -C "build\classes" .
  if errorlevel 1 goto compile_error
  echo 编译完成。
)

java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar "%JAR%" %*
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" (
  echo.
  echo 程序异常退出。
  pause
)
exit /b %EXIT_CODE%

:compile_error
echo.
echo 编译失败：请确认已安装 JDK 17+（在终端执行 java -version 检查）。
pause
exit /b 1
