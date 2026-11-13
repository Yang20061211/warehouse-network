# 去除 AI 味 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 Java Swing 课程设计改成克制、务实的桌面工具界面，并清理代码和文档中的模板化包装与无依据声明。

**Architecture:** 保留现有模型、算法、服务和页面职责，只调整 `Theme`/`Ui` 的视觉原语、登录与主框架布局、页面文案，以及已确认无调用的辅助代码。所有功能行为继续由现有 `NetworkService` 和页面类承担。

**Tech Stack:** Java 17、Swing、Java2D、`javac`/`jar`，无第三方运行时依赖。

**Spec:** `docs/superpowers/specs/2026-11-13-remove-ai-slop-design.md`

## Global Constraints

- 工作分支必须是 `codex/remove-ai-slop`，不合并、不推送 `main`。
- 初始数据不变量：30 城、435 条无向边、870 个邻接边结点、29 条 MST 边、MST 总权值 117.97。
- 不改变账号、角色权限、数据文件格式、启动方式、算法或最短路径口径。
- 不引入新依赖、测试框架、构建工具或 Web 技术。
- 临时 JDK 只用于本次验证，不修改系统默认 Java。
- 不删除承担实际功能的图表、地图、表格、角色隔离或演示入口。
- 每完成一个任务，重新编译并运行自检；纯视觉任务还必须重新生成并查看相关截图。

## Review Focus

- 管理员登录后仍能看到城市管理和算法对比，查询员登录后仍看不到这两个入口。
- 最小窗口 `1180×740` 下登录页、主窗口、表格、地图和底部状态栏不截断。
- 新增、编辑、删除和排序城市后，图、MST、路径选择器及持久化数据保持一致。
- `--shots` 截图后不得留下排序后的临时数据，`data/cities.txt` 内容必须恢复。
- 文档中的图片引用、账号、命令、性能数字和不变量必须与当前分支实际结果一致。

---

### Task 1: 建立可复现基线

**Files:**
- Modify: 无
- Verify: `build/warehouse-network.jar`（Git 忽略）
- Capture: `/tmp/warehouse-baseline/`

**Interfaces:**
- Consumes: 现有 `logistics.Main` 的 `--selftest` 和 `--shots <dir>` 接口。
- Produces: 后续任务使用的 `$JDK17_HOME/bin/java`、`$JDK17_HOME/bin/javac` 和基线截图。

- [ ] **Step 1: 确认分支和工作区**

Run:

```bash
git branch --show-current
git status --short --branch
```

Expected: 当前分支为 `codex/remove-ai-slop`，除设计/计划文档外没有产品代码改动。

- [ ] **Step 2: 下载 arm64 JDK 17 到临时目录**

Run:

```bash
rm -rf /tmp/warehouse-jdk17 /tmp/warehouse-jdk17.tar.gz
mkdir -p /tmp/warehouse-jdk17
curl -L --fail -o /tmp/warehouse-jdk17.tar.gz \
  'https://api.adoptium.net/v3/binary/latest/17/ga/mac/aarch64/jdk/hotspot/normal/eclipse'
tar -xzf /tmp/warehouse-jdk17.tar.gz -C /tmp/warehouse-jdk17
JDK17_HOME="$(find /tmp/warehouse-jdk17 -type d -path '*/Contents/Home' -print -quit)"
"$JDK17_HOME/bin/java" -version
```

Expected: 输出 OpenJDK 17.x，命令退出码为 0。

- [ ] **Step 3: 编译并打包**

Run:

```bash
rm -rf build
mkdir -p build/classes
find src -name '*.java' > /tmp/warehouse-sources.txt
"$JDK17_HOME/bin/javac" --release 17 -encoding UTF-8 \
  -d build/classes @/tmp/warehouse-sources.txt
"$JDK17_HOME/bin/jar" --create --file build/warehouse-network.jar \
  --main-class logistics.Main -C build/classes .
```

Expected: 编译和打包退出码均为 0。

- [ ] **Step 4: 记录自检和截图基线**

Run:

```bash
shasum data/cities.txt > /tmp/warehouse-cities-before.sha256
"$JDK17_HOME/bin/java" -jar build/warehouse-network.jar --selftest \
  > /tmp/warehouse-baseline-selftest.txt
mkdir -p /tmp/warehouse-baseline
"$JDK17_HOME/bin/java" -jar build/warehouse-network.jar \
  --shots /tmp/warehouse-baseline
shasum data/cities.txt > /tmp/warehouse-cities-after.sha256
diff /tmp/warehouse-cities-before.sha256 /tmp/warehouse-cities-after.sha256
```

Expected: `--selftest` 输出 30 城、435 条无向边、870 个边结点、29 条 MST 边和 117.97；截图文件存在；数据文件哈希一致。

- [ ] **Step 5: 提交设计与计划**

Run:

```bash
git add docs/superpowers
git commit -m "docs: plan AI-slop cleanup"
```

Expected: 提交只包含设计与计划文档。

---

### Task 2: 收敛共享视觉原语

**Files:**
- Modify: `src/logistics/ui/Theme.java`
- Modify: `src/logistics/ui/Ui.java`
- Verify: `src/logistics/ui/LoginFrame.java`、`src/logistics/ui/MainFrame.java` 及各页面编译通过

**Interfaces:**
- Consumes: 现有 `Ui.PillButton`、`Ui.NavButton`、`Ui.BadgeLabel`、`Ui.StatTile`、`Ui.RoundedPanel` 和 `Theme.*` 颜色常量。
- Produces: 中性暖灰背景、深色文字、低饱和青绿色主色、浅色侧栏和更小圆角；保留所有公开组件类名与构造器签名。

- [ ] **Step 1: 先记录当前模板色和渐变**

Run:

```bash
rg -n '0x3B, 0x82, 0xF6|0x25, 0x63, 0xEB|GradientPaint|RadialGradientPaint' src/logistics/ui
```

Expected: 找到当前蓝色主色与登录页渐变，作为待移除目标。

- [ ] **Step 2: 修改 `Theme` 的固定色值**

将主题固定为：

- `BG = #F3F4F1`
- `CARD = #FFFFFF`
- `BORDER = #D9DED8`
- `BORDER_STRONG = #B9C2BA`
- `TEXT = #202622`
- `TEXT_SOFT = #3B443E`
- `MUTED = #66736A`
- `FAINT = #879188`
- `PRIMARY = #376553`
- `PRIMARY_DARK = #234B3D`
- `PRIMARY_SOFT = #E8F0EB`
- `SUCCESS = #2E7D5A`
- `SUCCESS_SOFT = #E9F4EE`
- `WARNING = #A66300`
- `WARNING_SOFT = #FFF4E1`
- `DANGER = #B42318`
- `DANGER_SOFT = #FDECEA`
- `SIDEBAR = #E9EEE9`
- `SIDEBAR_ITEM = #DDE5DE`
- `SIDEBAR_TEXT = #4B5B51`
- `SIDEBAR_MUTED = #738178`
- `SIDEBAR_ACTIVE = #D2E0D6`

- [ ] **Step 3: 简化共享组件外观**

在 `Ui` 中把卡片圆角从 `16/12` 降为 `8/6`，按钮圆角从 `10` 降为 `6`；侧栏激活项使用浅绿底和 `Theme.PRIMARY_DARK` 文字/图标，不再使用深蓝底和白色文字；表格和输入框保持清晰边框，不增加阴影或渐变。

- [ ] **Step 4: 编译验证**

Run Task 1 的 `javac` 命令。

Expected: 编译通过，无缺失常量或签名错误。

- [ ] **Step 5: 提交共享样式**

```bash
git add src/logistics/ui/Theme.java src/logistics/ui/Ui.java
git commit -m "style: simplify shared Swing theme"
```

Expected: 提交只包含共享主题和组件样式。

---

### Task 3: 重做登录页

**Files:**
- Modify: `src/logistics/ui/LoginFrame.java`
- Verify: `/tmp/warehouse-clean/login.png`

**Interfaces:**
- Consumes: `Auth.login(String, String)`、`Auth.demoUser(Role)`、`MainFrame(NetworkService, User)`。
- Produces: `LoginFrame(NetworkService service)`、`clearError()` 和 `demoLogin(Role)` 的现有公开/内部行为不变。

- [ ] **Step 1: 记录待删除的官网式元素**

Run:

```bash
rg -n 'CITY NETWORK STUDIO|DATA STRUCTURES|欢迎登录|或使用演示账号|GradientPaint|RadialGradientPaint|BrandPanel' \
  src/logistics/ui/LoginFrame.java
```

Expected: 当前文件中这些元素均存在。

- [ ] **Step 2: 改为中心单列表单**

删除 `BrandPanel`、网络节点常量、渐变和光晕绘制。窗口改为约 `460×560`，背景使用 `Theme.BG`，表单使用白色、细边框和足够留白；标题使用“城际仓网规划系统”，副标题使用“数据结构课程设计”。

- [ ] **Step 3: 保留登录行为并压低文案**

保留账号、密码、错误提示、登录按钮、管理员演示和查询员演示。删除英文品牌、课程宣传语、口号标签和重复版权信息；账号密码说明只保留一行，字号不低于 10px。

- [ ] **Step 4: 编译并生成登录截图**

Run:

```bash
mkdir -p /tmp/warehouse-clean
"$JDK17_HOME/bin/java" -jar build/warehouse-network.jar \
  --shots /tmp/warehouse-clean
```

Expected: 编译通过并生成 `/tmp/warehouse-clean/login.png`；截图没有渐变、英文品牌或官网式左栏。

- [ ] **Step 5: 提交登录页**

```bash
git add src/logistics/ui/LoginFrame.java
git commit -m "style: simplify login screen"
```

---

### Task 4: 调整主框架与页面文案

**Files:**
- Modify: `src/logistics/ui/MainFrame.java`
- Modify: `src/logistics/ui/OverviewPanel.java`
- Modify: `src/logistics/ui/CityPanel.java`
- Modify: `src/logistics/ui/AdjacencyPanel.java`
- Modify: `src/logistics/ui/MstPanel.java`
- Modify: `src/logistics/ui/PathPanel.java`
- Modify: `src/logistics/ui/BenchmarkPanel.java`
- Modify: `src/logistics/ui/AboutPanel.java`
- Verify: `/tmp/warehouse-clean/` 下所有页面截图

**Interfaces:**
- Consumes: Task 2 的 `Theme`/`Ui` 样式，以及现有各 Panel 构造器。
- Produces: 不改变 `MainFrame.showCard(String)`、`setStatus(String)`、`refreshAll()` 和 `demo*` 截图接口。

- [ ] **Step 1: 记录当前营销/模板文案**

Run:

```bash
rg -n '递进工程实现|一键入口|毫秒级|欢迎|CITY NETWORK|远低于|不是空话|非常直观' \
  src/logistics/ui README.md docs/实验报告.md
```

Expected: 列出需要压缩或改为事实陈述的文案。

- [ ] **Step 2: 简化标题栏和侧栏**

在 `MainFrame` 中删除渐变 Logo 使用，标题只保留系统名和角色信息；侧栏改为 `Theme.SIDEBAR` 浅色背景并加右侧分隔线；底部数据概况改成紧凑文本区，不再套深色卡片；保留角色导航、退出登录和状态栏。

- [ ] **Step 3: 压低页面层级**

各页面继续使用现有表格、地图、图表和结果区。把页面副标题改为短说明，删除口号式“工程实现”“毫秒级”宣传和重复解释；保留算法口径、操作提示和错误信息。

- [ ] **Step 4: 编译、自检并生成全部截图**

Run Task 1 的编译命令，然后：

```bash
"$JDK17_HOME/bin/java" -jar build/warehouse-network.jar --selftest \
  > /tmp/warehouse-clean-selftest.txt
"$JDK17_HOME/bin/java" -jar build/warehouse-network.jar \
  --shots /tmp/warehouse-clean
```

Expected: 自检中的 30/435/870/29/117.97 不变；生成登录页、管理员各页面和查询员页面截图。

- [ ] **Step 5: 检查最小窗口和角色隔离**

Run:

```bash
rg -n 'setMinimumSize|canEdit\\(\\)' src/logistics/ui/MainFrame.java
```

Expected: 最小尺寸仍为 `1180×740`；城市管理和算法对比仍受 `canEdit()` 控制。

- [ ] **Step 6: 提交主框架和页面清理**

```bash
git add src/logistics/ui
git commit -m "style: remove template UI treatment"
```

---

### Task 5: 删除死代码和说明噪声

**Files:**
- Modify: `src/logistics/ui/Ui.java`
- Modify: `src/logistics/model/City.java`
- Modify: `src/logistics/model/Graph.java`
- Modify: `src/logistics/app/Role.java`
- Modify: 仅限确认有装饰分隔线或复述性注释的现有 Java 文件

**Interfaces:**
- Consumes: Task 2-4 完成后的实际调用关系。
- Produces: 对外行为不变；只删除确认无引用的辅助方法、装饰分隔线和无效注释。

- [ ] **Step 1: 复查候选方法是否仍无调用**

Run:

```bash
for name in 'Ui.vbox' 'Ui.hbox' 'Ui.title' 'Ui.muted' 'Ui.alpha' 'Ui.millisShort' \
  'Ui.softCard' 'Ui.info' 'Ui.LogoMark' 'withName' 'Graph.vertex' 'getDescription'; do
  printf '\n%s\n' "$name"
  rg -n -F "$name" src README.md docs || true
done
```

Expected: 只出现定义或注释说明，没有产品调用。

- [ ] **Step 2: 删除确认无调用的方法**

删除设计文档列出的未使用方法，以及登录页和主框架不再使用后的 `Ui.LogoMark`；若视觉调整后某方法已被使用，则保留并在提交说明中记录。

- [ ] **Step 3: 清理装饰性注释**

删除 `──── ... ────` 分隔线和只重复下一行代码的注释；保留解释稳定排序、循环指针、O(V²)/O(E log E)、MST 拓扑和 Dijkstra 口径的注释。

- [ ] **Step 4: 编译与自检**

Run Task 1 的编译命令，再运行：

```bash
"$JDK17_HOME/bin/java" -jar build/warehouse-network.jar --selftest \
  > /tmp/warehouse-code-clean-selftest.txt
```

Expected: 编译通过，固定不变量与 Task 1 一致。

- [ ] **Step 5: 提交代码清理**

```bash
git add src
git commit -m "refactor: remove unused UI helpers and noise"
```

---

### Task 6: 修正文档并更新截图

**Files:**
- Modify: `README.md`
- Modify: `docs/实验报告.md`
- Modify: `shots/*.png`
- Modify: `docs/images/ui-*.png`
- Delete: 不再被引用的旧 UI 截图

**Interfaces:**
- Consumes: Task 4 生成的最终截图、Task 5 的最终自检输出。
- Produces: README 和报告中的功能、账号、命令、图片、性能数据均与当前分支一致。

- [ ] **Step 1: 删除无依据测试声明**

Run:

```bash
rg -n '自动化测试|界面回归|服务层自动化' README.md docs/实验报告.md
```

Expected: 找到需要修正的现有声明。

- [ ] **Step 2: 重写 README 的包装性段落**

保留项目简介、运行方式、演示账号、功能表、目录、命令和设计要点；删除口号式引文、重复强调和未验证宣传，只描述当前可执行能力。

- [ ] **Step 3: 修正实验报告**

把测试章节改成真实执行过的 `--selftest`、截图模式和数据不变量检查；删除仓库中不存在的自动化测试声明；将最终性能数据、日期、JDK 版本和机器信息集中记录；删除前后不一致的重复数字。

- [ ] **Step 4: 替换截图**

用 `/tmp/warehouse-clean/` 中的最终截图替换 `shots/` 对应文件，并按下列关系更新报告图片：

- `login.png` → `docs/images/ui-login.png`
- `overview.png` → `docs/images/ui-overview.png`
- `cities.png` → `docs/images/ui-cities.png`
- `sorted.png` → `docs/images/ui-sorted.png`
- `adjacency.png` → `docs/images/ui-adjacency.png`
- `mst.png` → `docs/images/ui-mst.png`
- `path.png` → `docs/images/ui-path.png`
- `benchmark.png` → `docs/images/ui-benchmark.png`
- `viewer-overview.png` → `docs/images/ui-viewer.png`

保留 `architecture.png`、`dll.png`、`adjacency-diagram.png`，除非其内容也被发现与实际实现不一致。

- [ ] **Step 5: 检查图片引用和数据恢复**

Run:

```bash
rg -o '!\[[^]]*\]\(([^)]+)\)' README.md docs | sed -E 's/.*\]\((.*)\)/\1/' |
while IFS= read -r p; do
  case "$p" in
    docs/*) [ -e "$p" ] || echo "MISSING $p" ;;
    *) [ -e "docs/$p" ] || echo "MISSING $p" ;;
  esac
done
shasum data/cities.txt > /tmp/warehouse-cities-final.sha256
diff /tmp/warehouse-cities-before.sha256 /tmp/warehouse-cities-final.sha256
```

Expected: 没有 `MISSING` 输出；城市数据哈希与基线一致。

- [ ] **Step 6: 最终验证并提交**

Run:

```bash
git diff --check
git status --short
git diff --stat main...HEAD
```

Expected: 无空白错误；改动限定在 UI、确认无用的代码和文档；分支仍为 `codex/remove-ai-slop`。

```bash
git add README.md docs shots
git commit -m "docs: align project docs and screenshots"
```
