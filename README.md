# 卷影 · FolioFrame

本地优先的书影音与故事记录库，覆盖动漫、剧集、电影和书籍。数据默认保存在设备本地，应用提供可迁移的 ZIP 备份，不依赖自建账号或服务器。

## 当前版本：**0.4.0**

`versionName 0.4.0` · `versionCode 5` · Android 包名 `io.folioframe.juanying`

最新 APK：[Releases · v0.4.0](https://github.com/OliverArcher/FolioFrame/releases/tag/v0.4.0)

## 0.4.0 更新

- Bangumi 搜索迁移到 Cloudflare Pages Functions 官方域名代理，Android 端不再直连受限的 `api.bgm.tv`
- Bangumi 返回的 `lain.bgm.tv` 封面地址自动改写到同一 Pages 代理，并使用 Cloudflare 边缘缓存
- 代理提供 CORS 与 OPTIONS 预检支持，适配 Capacitor Android WebView
- Android 清单将 `android:allowBackup` 设为 `false`，停用系统隐式云备份与重装恢复；应用内 ZIP 备份不受影响
- APK 版本提升至 `versionCode 5`，保持 FishKing 发布签名以支持覆盖安装
- 更纱黑体由全量 TTF（各约 33MB）改为 WOFF2 分发（regular 19.5MB / bold 20.1MB），保留完整字符集避免标题字形回退，APK 体积与启动时字体解析开销同步下降

## 主要功能

### 记录与进度

- 支持动漫、剧集、电影、书籍四类记录
- 支持「在看、想看、看过、弃坑」四种状态
- 动漫和剧集按集记录进度，支持卡片上的 `+1`
- 电影采用看过即完成的进度逻辑
- 书籍进度单位为章，适合纸书和电子书
- 评分采用 1–5 星制
- 支持标题内联编辑、短评、收藏和收藏置顶

### 动漫与剧集的分季

- 一个作品可以包含多个季，每季独立保存进度、状态、评分、短评、简介和封面
- 支持切换季、新增季、删除季、搜索补全当前季资料
- 同一作品的不同季可合并到同一条记录

### 元数据搜索

- **Bangumi**：动漫、剧集、电影、书籍，按类型限定搜索结果
- **TMDB**：电影和剧集，需要在设置中填写 API Key
- **Open Library** / **Google Books**：书籍
- 搜索结果显示来源标签，搜不到时可以手动创建记录

### 封面与阅读体验

- 列表页点击条目进入详情，详情页点击封面查看大图
- 支持替换、移除和本地上传封面
- 在线封面可以缓存到本机
- 纸黄墨绿浅色主题、暗色主题，支持跟随系统深浅色
- UI 使用更纱黑体，短评和简介使用衬线字体，数字和进度使用等宽字体
- 已针对三星 Fold 的外屏、内屏横竖屏做响应式适配

### 搜索、列表与统计

- 全屏半透明搜索浮层，回车搜索
- 首页显示进行中的记录，分类页按最近进度时间排序
- 全部记录页可按状态和类型筛选，支持批量删除
- 统计页支持按年份切换，按「看完事件」统计年度数量、分类数量、评分分布、月度追更活跃度

## 数据与备份

### 本地存储

记录使用 WebView 的 IndexedDB 保存，数据库名为 `folioframe`。主题偏好和 TMDB API Key 使用 localStorage 保存。

应用不会把记录上传到 FolioFrame 自有服务器；TMDB API Key 只保存在本机。

### ZIP 备份内容

- `records.json`：记录、季数据、个人评分、短评、状态、进度等
- `covers/`：本地封面和已缓存的在线封面
- TMDB API Key、主题和跟随系统设置

Android 版通过原生 `BackupFilesPlugin` 写入系统公共 Download 目录，导入时调用系统文件选择器。

## Android 行为

- Android 端不注册浏览器 Service Worker
- 启动时会执行一次旧 Service Worker 和资源缓存迁移，只清理旧资源缓存，不触碰 IndexedDB 记录
- Android 返回键在详情页返回对应列表，首页连续按两次才退出应用
- 0.4.0 起设置 `android:allowBackup="false"`，系统不会再通过常规 Auto Backup 在重装时隐式恢复应用私有数据；应用内 ZIP 导出与导入是正式的数据迁移方式，不受 `allowBackup` 设置影响

## 数据来源

- [Bangumi](https://bgm.tv/)
- [TMDB](https://www.themoviedb.org/)
- [Open Library](https://openlibrary.org/)
- [Google Books](https://books.google.com/)
