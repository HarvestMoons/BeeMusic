# 前端项目结构文档

本文档列出了 `frontend` 目录下的主要文件及其作用说明。

## 1. 核心入口与配置
- **`src/main.js`**: 项目入口文件，负责初始化 Vue 应用、挂载插件（Pinia, Router）等。
- **`src/App.vue`**: 根组件，包含全局布局结构。
- **`src/style.css`**: 全局基础样式文件。

## 2. 视图 (Views)
- **`src/views/HomePage.vue`**: 首页，包含播放器核心功能。
- **`src/views/About.vue`**: 关于页面，展示项目信息和彩蛋。
- **`src/views/PrivacyPage.vue`**: 隐私政策页面，渲染 Markdown 内容。

## 3. 组件 (Components)

### 3.1 通用组件 (`src/components/common`)
- **`ConfirmModal.vue`**: 通用确认模态框（如删除确认）。
- **`HelpTooltip.vue`**: 帮助提示组件，提供悬浮提示信息。
- **`OnlineStatus.vue`**: 显示当前在线人数。
- **`SearchBar.vue`**: 搜索栏组件。
- **`Toast.vue`**: 全局轻量级提示消息组件。

### 3.2 特效组件 (`src/components/effects`)
- **`BackgroundParticles.vue`**: 背景粒子特效。
- **`BackgroundRipple.vue`**: 点击波纹特效。
- **`MySpinner.vue`**: 加载中的旋转动画组件。

### 3.3 功能组件 - 认证 (`src/components/feature/auth`)
- **`LoginModal.vue`**: 用户登录模态框。
- **`RegisterModal.vue`**: 用户注册模态框。

### 3.4 功能组件 - 播放器 (`src/components/feature/player`)
- **`Player.vue`**: 核心播放器组件，集成播放控制、歌曲信息等。
- **`PlayerSidebar.vue`**: 播放器侧边栏，包含评论区和频谱开关。
- **`Playlist.vue`**: 播放列表组件，展示歌曲列表。
- **`FolderSelector.vue`**: 歌单（文件夹）选择器。
- **`PlaybackRateControl.vue`**: 播放倍速控制组件。
- **`VoteControls.vue`**: 歌曲点赞/点踩控制组件。
- **`CommentDrawer.vue`**: 评论区抽屉组件。

### 3.5 功能组件 - 其他 (`src/components/feature`)
- **`spectrum/SpectrumVisualizer.vue`**: 音频频谱可视化组件。
- **`stats/SongFolderDonut.vue`**: 歌曲分布统计图表（甜甜圈图）。
- **`stats/HiddenUnlockTrigger.vue`**: 隐藏歌单解锁触发器。

### 3.6 布局组件 (`src/components/layout`)
- **`Sidebar.vue`**: 全局侧边导航栏。

## 4. 组合式函数 (Composables)
- **`src/composables/useKeyboardShortcuts.js`**: 全局键盘快捷键逻辑封装。
- **`src/composables/player/useStationMaster.js`**: 站长管理功能（删除/恢复歌曲）逻辑封装。
- **`src/composables/player/usePlayerStorage.js`**: 播放器本地存储（LocalStorage）读写逻辑封装。

## 5. 状态管理 (Store)
- **`src/store/index.js`**: Pinia 状态管理入口，主要管理用户认证状态 (`authStore`)。

## 6. 服务与工具 (Services & Utils)
- **`src/services/auth.js`**: 认证相关的 API 请求封装。
- **`src/utils/eventBus.js`**: 简单的事件总线，用于组件间通信。

## 7. 常量与配置 (Constants)
- **`src/constants/index.js`**: 全局常量定义（如 API 基础路径）。
- **`src/constants/folderDescriptions.js`**: 歌单描述文本配置。

## 8. 路由 (Router)
- **`src/router/index.js`**: Vue Router 路由配置。

## 9. 资源 (Assets)
- **`src/assets/icons`**: SVG 图标资源。
- **`src/assets/cover`**: 歌单封面图片。
- **`src/assets/css/global.css`**: 全局 CSS 变量和通用样式。
- **`src/assets/markdown`**: Markdown 文档资源（如隐私政策）。
