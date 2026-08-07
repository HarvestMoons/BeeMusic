# 前端项目结构文档

本文档列出了当前 `frontend/src` 下的主要文件及其职责。项目使用 Vue 3、Vite、Pinia 和 Vue Router。

## 1. 核心入口与配置

- **`src/main.js`**：初始化 Vue 应用、Pinia 和 Router，并挂载根组件。
- **`src/App.vue`**：根组件和全局布局容器。
- **`src/style.css`**：全局基础样式。

## 2. 视图（Views）

- **`src/views/HomePage.vue`**：首页，承载播放器和主站内容。
- **`src/views/About.vue`**：关于页面及彩蛋入口。
- **`src/views/MemePage.vue`**：迷因漂流瓶页面。

当前 `src/views` 中没有 `PrivacyPage.vue`；隐私政策相关 Markdown 资源也不应在结构文档中列为现存视图，新增后再补充。

## 3. 组件（Components）

### 3.1 通用组件（`src/components/common`）

- **`ConfirmModal.vue`**：删除等危险操作的确认弹窗。
- **`CountdownRing.vue`**：倒计时环形展示。
- **`EntryCover.vue`**：入口封面展示。
- **`Footnote.vue`**：书籍样式脚注及悬停详情。
- **`OnlineStatus.vue`**：通过 WebSocket 显示在线人数。
- **`SearchBar.vue`**：搜索输入组件。
- **`Toast.vue`**：全局轻量提示。
- **`ToggleSwitch.vue`**：通用开关控件。

### 3.2 特效组件（`src/components/effects`）

- **`BackgroundParticles.vue`**：背景粒子特效。
- **`BackgroundRipple.vue`**：点击波纹和随机视频背景效果。
- **`MySpinner.vue`**：加载动画。

### 3.3 认证组件（`src/components/feature/auth`）

- **`LoginModal.vue`**：登录弹窗。
- **`RegisterModal.vue`**：注册弹窗。

### 3.4 播放器组件（`src/components/feature/player`）

- **`Player.vue`**：核心播放器，协调当前歌曲、播放控制和歌单状态。
- **`PlayerSidebar.vue`**：播放器侧边栏，承载评论区和频谱开关。
- **`Playlist.vue`**：歌单展示、排序和歌曲选择。
- **`FolderSelector.vue`**：音乐文件夹/歌单切换。
- **`PlaybackRateControl.vue`**：播放倍速控制。
- **`VoteControls.vue`**：歌曲点赞、点踩和投票状态。
- **`CommentDrawer.vue`**：评论抽屉。

### 3.5 其他功能组件（`src/components/feature`）

- **`spectrum/SpectrumVisualizer.vue`**：音频频谱可视化。
- **`stats/SongFolderDonut.vue`**：歌曲文件夹分布图表。
- **`stats/HiddenUnlockTrigger.vue`**：隐藏歌单解锁触发器。

### 3.6 布局组件（`src/components/layout`）

- **`Sidebar.vue`**：全局侧边导航和设置入口。
- **`SiteFooter.vue`**：站点页脚。

## 4. 组合式函数（Composables）

- **`src/composables/useKeyboardShortcuts.js`**：全局键盘快捷键。
- **`src/composables/player/usePlayerPlaylistLoader.js`**：加载/切换歌曲文件夹，恢复文件夹播放倍速并选择起始歌曲。
- **`src/composables/player/usePlayerQueue.js`**：播放队列、上一首/下一首、随机和循环播放。
- **`src/composables/player/useStationMaster.js`**：站长歌曲删除/恢复操作。
- **`src/composables/player/usePlayerStorage.js`**：LocalStorage 播放器状态、排序偏好和播放倍速读写。

## 5. 状态管理（Store）

**`src/store/index.js`** 导出三个 Pinia Store：

- `useAuthStore`：登录状态、用户名、角色、隐藏歌单解锁状态和初始化请求。
- `useSiteConfigStore`：评论区启用状态及其加载/更新。
- `useThemeStore`：深色模式和背景粒子开关，并持久化到 LocalStorage。

## 6. 服务与工具（Services & Utils）

### 服务

- **`src/services/auth.js`**：注册、登录、登出和用户状态请求。
- **`src/services/meme.js`**：迷因随机获取和相关操作请求。
- **`src/services/siteConfig.js`**：评论区站点配置的读取和更新请求。

### 工具

- **`src/utils/audioGraph.js`**：Web Audio 音频节点/频谱图连接。
- **`src/utils/eventBus.js`**：组件间事件总线。
- **`src/utils/playerPlaylist.js`**：歌单排序和排序偏好处理。
- **`src/utils/playerShare.js`**：歌曲分享 URL 的编码、生成和解析。

## 7. 常量与路由

- **`src/constants/index.js`**：公开 API、认证 API 和 WebSocket 地址等常量。
- **`src/constants/folderDescriptions.js`**：音乐文件夹说明和展示文案。
- **`src/router/index.js`**：Vue Router 路由配置。

## 8. 资源（Assets）

- **`src/assets/icons`**：通用、播放器、主题和管理操作图标。
- **`src/assets/cover`**：入口封面和歌单封面。
- **`src/assets/css/global.css`**：全局 CSS 变量和通用样式。
- **`src/assets/fonts`**：项目字体资源。
