# Easy课表 · 大学课程表与待办

> 课表 / 课程表 / 大学课程表 / 高校课表 / 教务系统一键导入 / 待办清单 / 桌面小组件 / Android 课表 App / Timetable &amp; Class Schedule

<p align="center">
  <img src="docs/brand/icon-512.png" width="168" alt="Easy课表图标：白猫与黄色笔记本">
</p>

<p align="center">
  <b>给中国大学生用的课表 + 待办应用</b><br>
  388 所高校教务系统一键导入 · 备忘录与待办 · 桌面小组件 · 天气提醒 · iOS 风格界面
</p>

<p align="center">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Compose-BOM_2026.09-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="minSdk" src="https://img.shields.io/badge/minSdk-26-3DDC84?logo=android&logoColor=white">
  <img alt="targetSdk" src="https://img.shields.io/badge/targetSdk-37-3DDC84?logo=android&logoColor=white">
  <img alt="License" src="https://img.shields.io/badge/License-GPL--3.0-blue">
</p>

---

## 这是什么

Easy课表是一个**完全离线**的 Android 课表应用：直接从学校的教务系统把课表抓进来，配上今日视图、桌面小组件、备忘录与待办提醒。所有数据存在手机本地（Room / SQLite），不注册、不上传、不联网也能用。

它面向的是最普通的大学场景：

- 每学期开头手动录入几十门课，一节课填五个字段——这件事本身就该被自动化；
- 教务系统各校千差万别（正方 / 青果 / 强智 / 金智 / URP / 各类定制），没有哪个 App 能一网打尽；
- 课表和"今天要交的作业、要开的会"天然是一件事，分成两个 App 反而更麻烦。

所以这个 App 做了三件事：**把课表导进来**、**把今天讲清楚**、**把要做的事记下来并叫醒你**。

### 四个不太一样的点

1. **课程表直接上桌面**：整周课表按节次画进小组件，跨节合并、当天高亮、周次自动过滤——不解锁就能知道下一节在哪。
2. **待办组件能在桌面上勾**：点圆圈就完成，带打勾与彩带反馈，和应用共用同一份数据库，几百毫秒内同步。
3. **天气贴在课上**：每张课程卡显示该时段的天气图标与温度；有雨或明显降温，上课前 3 小时提醒一次。
4. **备忘图文混排 + AI 整理**：图片跟着文字走；一句话自动拆成标题 / 正文 / 时间，没配 Key 就退回本地规则，永远有产出。

## 下载 / 安装

- **最新安装包**：[Releases · v1.0.1](https://github.com/chapwpzip/EasyKeBiao/releases/tag/v1.0.1)
  （`EasyKeBiao-v1.0.1.apk`，27.6 MB，arm64-v8a / armeabi-v7a / x86 / x86_64 四 ABI 整包，`versionCode 31`）
- **演示录屏**：[docs/demo.mp4](docs/demo.mp4)（约 28 秒：课表 → 今日 → 待办/备忘 → 我的 → 外观）
- 支持 **Android 8.0（API 26）及以上**，直接安装，无需注册账号
- 覆盖安装即可升级，数据库会按内置迁移逐步升级，**课表不会丢**

> 当前发布的是 **debug 签名** 测试包（包名 `com.easykebiao.app.debug`），可与正式版共存；
> 正式签名版本需自备 keystore 自行构建（见下方「构建」）。
>
> 注意：1.0.1 起应用标识为 `com.easykebiao.app.debug`，与 0.13.x 及更早版本（`com.imsx3d.classy.debug`）**不共用数据**，
> 升级前请先用旧版「导出 → 备份」，或按 [docs/releases/](docs/releases/) 里的说明迁移数据库。

## 界面

> 以下截图均在 Android 12 模拟器（1080×1920）实机截取，演示数据为虚构课表与待办。

| 课表 | 今日 | 待办 |
| --- | --- | --- |
| <img src="docs/screenshots/01-课表.png" width="250" alt="课表"> | <img src="docs/screenshots/02-今日.png" width="250" alt="今日"> | <img src="docs/screenshots/07-待办列表.png" width="250" alt="待办列表"> |

| 教务直连（388 所） | 备忘 | 深色模式 |
| --- | --- | --- |
| <img src="docs/screenshots/05-教务取校-388所.png" width="250" alt="388 所高校名录"> | <img src="docs/screenshots/09-备忘列表.png" width="250" alt="备忘列表"> | <img src="docs/screenshots/15-深色课表.png" width="250" alt="深色课表"> |

| 外观与主题 | 主题色彩 / 深浅色 | 我的 |
| --- | --- | --- |
| <img src="docs/screenshots/11-外观与主题.png" width="250" alt="外观与主题"> | <img src="docs/screenshots/12-主题色彩与深浅色.png" width="250" alt="主题色与深浅色"> | <img src="docs/screenshots/10-我的.png" width="250" alt="我的"> |

| 桌面小组件页 | 添加小组件 | 桌面上的待办组件 |
| --- | --- | --- |
| <img src="docs/screenshots/13-桌面小组件.png" width="250" alt="小组件页"> | <img src="docs/screenshots/14-添加小组件.png" width="250" alt="添加小组件"> | <img src="docs/screenshots/17-桌面待办组件.png" width="420" alt="桌面待办组件"> |

1.0.1 新增能力实拍：

| 今日（含天气徽标） | 桌面上的本周课表组件 |
| --- | --- |
| <img src="docs/screenshots/18-今日与天气.png" width="420" alt="今日页天气徽标"> | <img src="docs/screenshots/19-桌面本周课表组件.png" width="420" alt="桌面本周课表组件"> |

| 日程式待办表单：分类 / 截止 / 时间 | 地点 / 备注 / 优先级 |
| --- | --- |
| <img src="docs/screenshots/21-日程表单-分类与时间.png" width="420" alt="待办表单上半"> | <img src="docs/screenshots/22-日程表单-地点与优先级.png" width="420" alt="待办表单下半"> |

| 待办附图（列表缩略图） | 备忘图文混排 |
| --- | --- |
| <img src="docs/screenshots/25-待办附图.png" width="420" alt="待办附图"> | <img src="docs/screenshots/20-备忘图文混排.png" width="420" alt="备忘图文混排"> |

| AI 整理 | 天气设置 |
| --- | --- |
| <img src="docs/screenshots/23-AI整理.png" width="420" alt="AI 整理"> | <img src="docs/screenshots/24-天气设置.png" width="420" alt="天气设置"> |

更多截图在 [`docs/screenshots/`](docs/screenshots)；应用图标见 [`docs/brand/`](docs/brand)。

## 功能

### 一、课表

- **多课表**：一个学期可以有多张课表（不同年级/不同方案），随时切换，互不干扰。
- **周次与节次**：自定义学期起止、总周数、当前周；节次时间表可自建（`节次模板`），支持上午/下午/晚上不同节次与自定义时间。
- **课程**：名称、教师、地点、星期、起止节、周次（含单双周）、颜色、备注、别名（同一门课在不同课表里显示不同名字）。
- **网格与列表**：主界面是周网格，另提供课程列表、学期总览、课程详情页。
- **节假日与调休**：标记某天不上课，周次推进自动跳过。
- **冲突提示**：同一时段叠加时给出可视提示，便于调整。

### 二、导入（核心能力）

教务导入是本项目投入最多的地方：**内置 388 所高校的教务入口**，覆盖 35 种协议实现。

| 导入方式 | 说明 |
| --- | --- |
| 教务导课 | 选学校 → 内置 WebView 打开该校教务 → 登录后自动抓取课表并解析 |
| 分享码 | 把课表编码成一段文本，别人粘贴即可导入（跨设备迁移也用它） |
| 一键导课 | 从文件、图片里识别课表 |
| AI 网页识别 | 打开任意网页课表，交给识别流程抽取课程 |
| 演示 | 一键生成示例课表，用于体验或截图 |
| 手动新建 | 传统方式，支持从网格点空格直接新建 |

教务适配覆盖面（节选，`app/src/main/assets/schools.json`）：

- 正方教务（`zf_new` / `zf`，125 + 13 所）、青果（`qz` 等，67 所）、超星（43 所）、金智（`wisedu`，27 所）
- 强智、URP / URP New、EAMS 5、IEAS 2.1、各类学校定制协议（`hnust`、`neu`、`ustc`、`scu`、`pku`、`zju`…）
- 研究生系统（`xju_post` 等）单独标记状态

列表支持按拼音首字母/全拼检索与分组，未适配学校可提交"申请适配"。

### 三、今日

当天课程时间线 + 当前节次高亮 + 下一节课倒计时，加上当天的待办。锁屏之外，这是最常打开的一屏。

每张课程卡左侧还带一枚**天气徽标**：该节课开始时段的天气图标 + 温度（数据来自 Open-Meteo，按城市缓存 3 小时）。课程时间取不到对应小时就整块不占位，不会显示 `--`。

### 四、备忘录与待办

底栏「备忘录」里是两段式结构：**待办 / 备忘**，切换即时。

**待办**（对标轻量任务清单，而不是项目管理）

- 分组：已逾期 / 今天 / 之后 / 无日期，空分组自动隐藏；已完成折叠成一行。
- 单条待办：标题、备注、截止日、**提醒时刻**、重要标记、置顶、**步骤清单**（子任务，可逐项勾选）。
- **日程式填写页**（自上而下）：一句自然语言 + 三枚示例 chips → 点「识别填写」自动拆成标题 / 备注 / 时间；标题 + 分类（待办 / 活动 / 考试 / 作业 / 其他，列表左侧圆环按分类着色）；全天开关 + 截止（今天 / 明天 / 不设）+ 时间（即提醒时刻）；地点（独立字段，列表显示 📍）；备注；优先级（无 / 低 / 中 / 高，中以上显示旗标）。
- **一条待办可带一张图**：选图后压到长边 1600、JPEG 质量 85，存应用私有目录；列表行标题左侧 44dp 缩略图，桌面组件里 34dp 圆角位图；图片只存文件名，随待办一起删除，不留孤儿文件。
- 完成交互：点圆圈打勾，行文字加删除线，**屏幕上撒一次礼花**（自绘粒子，无需三方库）。
- 提醒：`AlarmManager` 精确闹钟（无权限时退化为 ±10 分钟窗口），通知里直接「完成 / 稍后 10 分钟」，重启后自动重建闹钟。
- 全部即时写库（Room），无网络依赖。

**备忘**：标题 + 正文 + 置顶，列表按置顶/最近修改排序。

- **图文混排**：正文里用 `[[img:文件名]]` 标记图片位置，列表卡片先出缩略图条、再用去掉标记的纯文本做摘要；标记不会漏到界面或导出文件里。
- **AI 整理**：一段口语（例如「明天下午三点在图书馆小组讨论」）拆成标题 + 正文 + 日期 + 时间；只预填、不直接写库，确认后才保存。接法是 OpenAI 兼容接口（自带 baseUrl / model / Key，Key 只存本机）；没配 Key、请求失败、返回不是 JSON，一律退回本地规则，永远有产出。
- 分类、优先级、地点与待办同一套字段，列表里同样显示。

### 五、桌面小组件

- 课表组件：**今日**（小 / 标准 / 宽）、**两日**、**本周视图**、**本周列表**、**课程行**，多档尺寸自动适配。
- **待办组件**：只显示未完成项，**可以直接在桌面上勾选**；勾选时行内打勾、划掉，随后整块卡片撒一次彩带；标题点击回到 App 备忘录栏。
- 组件数据本地直读，App 内任何改动即时推送刷新。

### 六、天气与提醒

- 预报走 [Open-Meteo](https://open-meteo.com/)：免费、不需要 Key、国内可直连，含逐小时温度 / 降水概率 / WMO 天气码。
- 城市在设置里从内置表手选（默认桂林），**不申请定位权限**——不引运行时权限、离线可用、审核友好。
- 快照缓存 3 小时：进设置页可「立即刷新」，过期才请求，请求失败保留上一次数据。
- 提醒用 `WorkManager` 3 小时周期检查（不是精确闹钟）：只看接下来 180 分钟内要开始的课，降水概率 ≥ 阈值（默认 50%）或当天最低温比前一天低 ≥ 阈值（默认 6℃）才提醒，同一门课同一天只提醒一次。
- 开关关掉即彻底不响；阈值可在设置里用滑杆调（降水 10–100%、降温 1–20℃）。

### 七、外观与动效

- 主题：默认（系统蓝）/ 春 / 海 / 桃 / 岩板 / 跟随系统取色；深浅色各自独立调色。
- 排版：iOS 风格字号阶梯、圆角、留白，我的页使用 iOS 式分组卡（inset-grouped）。
- 动效：
  - 页面推入 / 返回用水平位移 + 视差 + 淡入（不是 Material 的缩放）；
  - 底栏 tab 切换交叉淡入；
  - 列表条目与按钮按压缩放反馈；
  - 悬浮胶囊 Dock 与按钮使用**真实高斯模糊玻璃**（Android 13+ 且硬件加速时；否则退化为同色半透明材质）；
  - 组件勾选使用三帧序列（勾→彩带→常态），RemoteViews 不支持连续动画，故用跳帧实现。
- 图标：白猫 + 黄色笔记本，自适应图标（前景绘制在中央 72/108 安全区内），脚本见 `tools/`。

### 八、数据与导出

- 导出 **.ics 日历**、**HTML**、**纯文本（`#easykebiao-v1` 标记格式，可再导入）**、分享码。
- Room 数据库迁移全部手写（3→15），**不使用破坏性迁移**，升级不丢课表。
- 全离线：无账号、无埋点、无网络权限滥用。

## 技术栈

| 层 | 选型 |
| --- | --- |
| 语言 / UI | Kotlin，Jetpack Compose（BOM 2026.09），Material 3 + 自研 Glasense 设计体系（本地模块 `glasense-ui`） |
| 导航 | `navigation-compose` + 类型安全路由（`@Serializable` NavKey），转场自定义 |
| 数据 | Room 2.7（KSP、手写迁移、导出 schema）、DataStore、WorkManager |
| 异步 | Kotlin Coroutines / Flow |
| 网络与解析 | 内置 WebView 抓取 + Jsoup 解析，按学校协议适配；天气走 Open-Meteo（可选），AI 整理走 OpenAI 兼容接口（可选） |
| 图片与图形 | Coil、Canvas 自绘（礼花、组件位图）、`kyant0/backdrop` 真模糊玻璃 |
| 组件 | AppWidget（RemoteViews 位图与行列表两套渲染） |
| 测试 | JUnit4 + 契约测试（275 suites / 2404 tests），含数据库迁移、路由完整性、组件 XML 契约、组件几何一致性、学校数据一致性 |

最低 Android 8.0（API 26），目标 API 37。

## 构建

需要 JDK 21 与 Android SDK（compileSdk 37）。

```bash
# 1. 配置 SDK 路径
echo "sdk.dir=/path/to/android-sdk" > local.properties

# 2. 编译调试包
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-universal-debug.apk

# 3. 全量单测
./gradlew :app:testDebugUnitTest
```

> 仓库已开启 ABI 拆分（arm64-v8a / armeabi-v7a / x86_64），同时产出 universal 整包。
> release 变体需要自备签名（`keystore.properties`），否则 `verifyReleaseSigning` 会失败。
> 改版本号必须同步 `app/src/main/assets/release-notes.json`，`verifyReleaseNotes` 会校验两者一致。

## 目录结构

```
app/src/main/java/com/easykebiao/app/
├── data/            # Room 实体/DAO/迁移、解析器、仓库
│   ├── jw/          # 教务适配：协议识别、抓取、解析、学校模型
│   └── parser/      # 课表文本/HTML/ICS 解析
├── ui/
│   ├── nav/         # 类型安全路由、NavHost、转场
│   ├── screen/      # 课表、今日、我的、设置、导入、备忘录/待办
│   ├── component/   # Dock、卡片、动效、礼花、玻璃
│   └── theme/       # 颜色、字体、形状、主题预设
├── widget/          # 桌面小组件（课表 / 待办 / 提醒通知）
└── util/            # 偏好、格式化、导出
glasense-ui/         # 自研设计体系（颜色/排版/形状/组件）
docs/                # 设计文档、截图、品牌资源
tools/               # 构建期辅助脚本（图标生成等）
```

## 主要实现

按模块列出本项目自己实现或重做的部分：

1. **课表**：13 节周网格、多课表切换、周切换与回到本周、今日高亮与时间线、点课程编辑、配色三模式（色条 / 填充 / 统一底）。
2. **教务导入**：388 所高校入口库（拼音分组 + 首字母搜索）、教务直连、粘贴课表文本、`.ics` 文件导入、作息表编辑与课表重命名。
3. **今日**：日期大标题、当日课程时间轴、无课空状态；课程左侧显示该时段的天气图标与温度。
4. **待办**：四段分组（逾期 / 今天 / 之后 / 无日期）、子步骤清单、截止与提醒时刻、分类、优先级、地点、附图、完成礼花、闹钟提醒（含通知里的完成 / 稍后）。
5. **备忘**：inset 分组卡片列表、标题与正文编辑、置顶、插图（正文内嵌 `[[img:文件名]]`，删除备忘时一并清理），以及「AI 整理」一句话拆标题与正文（可填自己的 API Key，不填走本地规则）。
6. **桌面小组件**：今日·小 / 今日 / 两日 / 本周 / 待办五款；待办组件可在桌面直接勾选，带打勾与彩带三帧反馈，并显示每条待办的缩略图；尺寸不足时按阶梯降级。
7. **天气提醒**：Open-Meteo、内置城市手选（默认桂林，不申请定位权限）、3 小时缓存、上课前 3 小时内下雨或明显降温才提醒、同一门课同一天只提醒一次。
8. **界面与动效**：iOS 风格配色与字号、inset-grouped 列表、玻璃 Dock 与玻璃按钮、推入 / 返回位移转场、按压反馈、深色模式。
9. **设置**：课表显示、外观与主题（配色三模式 / 底栏样式 / 课表背景 / 主题色彩 / 深浅色）、小组件、提醒、天气、AI、启动直达页面、语言（中文 / English）、恢复默认设置。
10. **数据**：Room + 迁移链一路升到 v15（不清数据）、`.ics` 导出与分享、下载目录里的导出文件。

## 开源许可与第三方组件

本项目以 **GNU General Public License v3.0** 发布（见 [LICENSE](LICENSE)），在界面设计体系、教务解析算法与若干基础库上使用了下列开源项目，按其各自许可证保留署名：

| 项目 / 资源 | 许可证 | 用途 |
| --- | --- | --- |
| [IMSX3D/Classy](https://github.com/IMSX3D/Classy) | GPL-3.0 | 课表数据层与教务适配框架的基础实现（本仓库在其之上修改与扩展） |
| [NevoDev/Cresto](https://github.com/NevoDev/Cresto) | Apache-2.0 | 玻璃外观、倾斜按压、着色器涟漪、带模糊的进出场、礼花按钮等组件参考与移植 |
| Glasense UI（本地模块 `glasense-ui`） | 见模块内声明 | 设计体系：颜色、排版、形状、基础组件 |
| [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（Maven `io.github.kyant0:backdrop`） | Apache-2.0 | 高斯模糊玻璃 / 折射（Dock 与按钮） |
| [Kyant0/Shapes](https://github.com/Kyant0/Shapes)（Maven `io.github.kyant0:shapes`） | Apache-2.0 | 胶囊等形状 |
| [xioajiumi/Chinese_Universities](https://github.com/xioajiumi/Chinese_Universities) | 见仓库声明 | 高校名录（582 所）作为教务入口探测的输入 |
| [pypinyin](https://github.com/mozillazg/python-pinyin) | MIT | 生成学校名的拼音排序键（构建期工具，不随 App 分发） |
| [AndroidX / Jetpack Compose / Room / WorkManager](https://developer.android.com/jetpack) | Apache-2.0 | 应用框架 |
| [jsoup](https://jsoup.org/) | MIT | HTML 解析 |
| [Coil](https://coil-kt.github.io/coil/) | Apache-2.0 | 图片加载 |
| [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) | Apache-2.0 | 路由与 JSON |
| [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) | Apache-2.0 | 迁移契约测试（测试期） |

界面文案、图标、脚本与文档由作者在 AI 编程助手协作下完成；教务入口的探测脚本、判型规则与人工复核结果为作者产出。

### 二次开发提示

本仓库整体以 GPL-3.0 发布：分发修改版时请保留 [LICENSE](LICENSE) 与上表中的署名。
