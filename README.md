# 卷影 · FolioFrame

本地优先的书影音与故事记录库，覆盖动漫、剧集、电影和书籍。数据保存在设备本地，支持 ZIP 备份与 WebDAV 同步，不依赖自建账号或服务器。

当前版本：**0.5.0**  
APK 版本号：`versionName 0.5.0` / `versionCode 9`  
Android 包名：`io.folioframe.juanying`

## 主要功能

### 记录与进度

- 支持动漫、剧集、电影、书籍四类记录
- 支持「在看、想看、看过、弃坑」四种状态
- 动漫和剧集按集记录进度，支持卡片上的 `+1`
- 电影采用看过即完成的进度逻辑
- 书籍进度单位为章，适合纸书和电子书
- 评分采用 1–5 星制
- 支持标题内联编辑、短评、收藏和收藏置顶
- 卡片固定高度，短评超出一行自动省略
- 列表重排使用上下移动动画，不使用闪烁动画

### 动漫与剧集的分季

- 一个作品可以包含多个季
- 每季独立保存进度、状态、评分、短评、简介和封面
- 支持切换季、新增季、删除季、搜索补全当前季资料
- 明确带有「第二季」「Season 2」等信息的条目会解析季数
- 同一作品的不同季可合并到同一条记录
- 补全资料时采用确认弹窗，由用户决定是否覆盖当前季资料

### 元数据搜索

- **Bangumi**：动漫、剧集、电影、书籍，按类型限定搜索结果
- **TMDB**：电影和剧集，需要在设置中填写 API Key
- **微信读书**：书籍，需要在设置中填写微信读书 API Key
- 搜索结果显示来源标签
- 搜不到时可以手动创建记录
- 手动添加和详情页都支持从设备选择本地封面

### 封面与阅读体验

- 列表页点击条目进入详情，详情页点击封面查看大图
- 支持替换、移除和本地上传封面
- 在线封面在导出与同步时自动打入备份包，无需手动操作
- Android 原生桥接可以绕过部分浏览器跨域限制缓存图片
- 纸黄墨绿浅色主题、暗色主题
- 支持跟随系统深浅色
- UI 使用更纱黑体，短评和简介使用衬线字体，数字和进度使用等宽字体
- 已针对三星 Fold 的外屏、内屏横竖屏做响应式适配

### 搜索、列表与统计

- 全屏半透明搜索浮层，回车搜索，右侧 `×` 清除
- 首页显示进行中的记录
- 分类页按最近进度时间排序
- 全部记录页可按状态和类型筛选
- 支持选择模式、全选、批量删除
- 统计页支持按年份切换
- 年度统计按「看完事件」计算，包含年度完成数量、分类数量、评分分布、月度追更活跃度和当前库存状态
- 统计页提供全部记录入口

## 元数据来源与凭据

| 来源 | 覆盖范围 | 凭据 |
|---|---|---|
| Bangumi | 动漫、剧集、电影、书籍 | 无需 |
| TMDB | 电影、剧集 | 自备 API Key |
| 微信读书 | 书籍 | 自备 API Key |

两个 Key 都只保存在本机 localStorage，不会写入备份包中的记录数据，也不会随记录同步到云端。

- TMDB Key 在 <https://www.themoviedb.org/settings/api> 申请。
- 微信读书 Key 在 <https://weread.qq.com/r/weread-skills> 申请，格式为 `wrk-` 开头。

微信读书接口在国内可直连，但只允许其自身域名作为来源，浏览器跨域规则会拦截 WebView 请求。Android 版通过原生桥接发起请求绕开该限制；浏览器版受此限制，书籍搜索请使用 Android 版。

## 数据与备份

### 本地存储

记录使用浏览器 / WebView 的 IndexedDB 保存，数据库名为 `folioframe`，记录存储在 `records` object store 中。主题偏好与 API Key 使用 localStorage 保存。

应用不会把记录上传到 FolioFrame 自有服务器。搜索时会访问对应的元数据来源。

### ZIP 备份内容

- `records.json`：记录、季数据、个人评分、短评、状态、进度等
- `covers/`：本地封面和已缓存的在线封面
- TMDB Key、主题和跟随系统设置

浏览器版使用文件下载。Android 版通过原生 `BackupFilesPlugin` 写入系统公共 Download 目录，导入时调用系统文件选择器。

### WebDAV 同步

设置页填写服务器地址、用户名和密码后可手动推送与拉取：

- 推送：打包记录与封面，整包上传到 `<地址>/folioframe-backup.zip`，覆盖上一次的同名文件
- 拉取：下载云端备份，覆盖同 ID 记录，未在云端的本地记录保留

WebDAV 地址与密码只保存在本机 localStorage，不写入备份包，也不随记录同步。由于 WebDAV 服务器普遍不返回 CORS 头，Android 版通过原生通道执行 PUT 与 GET。

## 平台形态

### 浏览器 / PWA

`frontend/` 是浏览器版本：

- `frontend/index.html`：应用主体，内联 CSS 和 JavaScript
- `frontend/manifest.webmanifest`：PWA 安装清单
- `frontend/sw.js`：浏览器离线缓存
- `frontend/icon.svg`：应用图标
- `frontend/assets/`：字体、图标和第三方资源

浏览器版可以通过本地 HTTP 服务访问，也可以安装为 PWA。直接打开 HTML 时，建议使用单文件版本。

### 单文件 HTML

```powershell
python build-single-file.py
```

生成 `FolioFrame-standalone.html`，内嵌应用图标、界面图标、字体和 JSZip，可直接复制到手机或其他设备使用，不依赖旁边的 assets 文件夹。

### Android APK

项目使用 Capacitor 7 封装 Android 版。构建脚本位于项目根目录：

```powershell
.\build-apk.ps1 -Channel public
```

脚本会执行前端同步、修正 Gradle 的 Java 版本、构建 release APK，并输出最终文件路径与 SHA256。

构建环境有三个注意点：

1. 前端同步必须从项目根目录执行，不能在 `android` 子目录执行。
2. 前端同步可能重新生成 `android/app/capacitor.build.gradle` 并写入 `JavaVersion.VERSION_21`。当前构建使用 Java 17，脚本会自动改回 `JavaVersion.VERSION_17`。
3. 首次下载 Gradle 8.11.1 时可能超时。`gradle-wrapper.properties` 已将 `networkTimeout` 提高到 120 秒；若仍受网络限制，可仅为当前 Gradle 进程设置代理，不要把代理地址写入仓库。

APK 发布前应检查：

- APK 内 `assets/public/index.html` 是否包含本次前端改动
- `versionCode`、`versionName` 和包名是否正确
- 签名是否可以覆盖安装并保留数据
- 真机覆盖安装后应用是否加载最新资源
- 导出 ZIP 是否能在系统文件管理器的 Download 中找到
- 导入 ZIP 后记录和封面是否完整恢复

## Android 行为说明

- Android 端不注册浏览器 Service Worker
- 启动时会执行一次旧 Service Worker 和资源缓存迁移，只清理旧资源缓存，不触碰 IndexedDB 记录
- Android 返回键在详情页返回对应列表，首页连续按两次才退出应用
- 原生 `BackupFilesPlugin` 提供公共 Download 导出、系统文件选择器导入、原生图片缓存、WebDAV 请求与 JSON 请求能力
- 设置 `android:allowBackup="false"`，系统不会通过常规 Auto Backup 在重装时隐式恢复应用私有数据
- 应用内 ZIP 导出与导入是正式的数据迁移方式，不受 `allowBackup` 设置影响

## 待办与已知边界

### 1. 年度统计详情列表

当前年度统计显示数量、分类、评分分布和追更活跃度，尚未列出「这一年具体看完了哪些作品」。

### 2. 分季简介的展示策略

导入某一季时，简介通常属于该季来源资料。作品合并后，如何同时展示系列简介与当前季简介，仍需进一步讨论。

### 3. 大体积备份的传输方式

WebDAV 推送目前把整个 ZIP 转为 base64 字符串穿过原生桥，封面较多时内存占用会明显上升。后续可改为由原生侧直接从文件流式上传。

## 相关文档

- [设计系统](frontend/DESIGN-SYSTEM.md)
- [交付总览](frontend/overview.md)

## 目录概览

```text
FolioFrame/
├─ frontend/
│  ├─ index.html
│  ├─ DESIGN-SYSTEM.md
│  ├─ overview.md
│  ├─ manifest.webmanifest
│  ├─ sw.js
│  └─ assets/
├─ android/
│  ├─ app/
│  └─ ...
├─ build-apk.ps1
├─ build-single-file.py
├─ capacitor.config.ts
├─ package.json
└─ README.md
```

## 许可

本项目为个人使用而开发，代码与设计文档未附带开源许可条款。
