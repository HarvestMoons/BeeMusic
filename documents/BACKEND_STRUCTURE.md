# 后端项目结构文档

本文档列出了当前 `backend/src/main/java/com/example/musicplayer` 下的主要 Java 文件及其职责。项目基于 Spring Boot，源码结构以当前文件为准。

## 1. 核心入口

- **`MusicPlayerApplication.java`**：Spring Boot 应用启动类，同时启用定时任务。
- **`package-info.java`**：包级说明。

## 2. 配置（Config）

- **`SecurityConfig.java`**：Spring Security 配置，处理 Session 认证、角色授权、CORS 和公开端点放行。
- **`RedisConfig.java`**：Redis 连接及普通/JSON `RedisTemplate` 配置。
- **`RedisPubSubConfig.java`**：Redis 发布/订阅配置，用于在线人数等跨实例消息。
- **`WebSocketConfig.java`**：注册 `/ws/online` WebSocket 端点及允许来源。
- **`OssConfig.java`**：阿里云 OSS 客户端配置。
- 在线人数通过 Redis 有序集合、过期时间和 WebSocket 心跳维护，不在应用启动时清空全局计数。

## 3. 控制器（Controller）

- **`AuthController.java`**：注册、登录、登出和登录状态。
- **`SongController.java`**：歌曲列表、文件夹统计、播放量、投票、软删除/恢复和同步。
- **`MemeController.java`**：迷因随机获取、OSS 同步和删除。
- **`CommentController.java`**：评论查询、发表、点赞和删除。
- **`UserController.java`**：用户相关操作，包括隐藏歌单解锁。
- **`VideoController.java`**：随机视频。
- **`SiteConfigController.java`**：读取和切换评论区开关；切换操作仅允许站长。
- **`HealthController.java`**：公开健康检查。
- **`TestController.java`**：数据库、连通性和认证测试接口。

## 4. 服务（Service）

- **`UserService.java`**：用户注册、登录验证和用户状态。
- **`CustomUserDetailsService.java`**：按用户名加载 Spring Security 用户。
- **`CustomUserDetails.java`**：把 `User` 适配为 `UserDetails`，提供角色权限和启用状态。
- **`SongService.java`**：歌曲查询、Redis 缓存、OSS 签名 URL、文件夹同步、播放量和软删除。
- **`MemeService.java`**：迷因随机查询、OSS 前缀扫描同步和删除。
- **`CommentService.java`**：评论及评论点赞业务。
- **`VoteService.java`**：歌曲点赞/点踩业务，维护 MySQL 投票关系和 Redis Set。
- **`VideoService.java`**：视频查询。
- **`OssUtil.java`**：OSS 对象访问和签名 URL 工具。
- **`RedisTaskLock.java`**：使用 Redis `SETNX` 和带令牌的 Lua 释放脚本实现定时任务租约。

## 5. 实体模型（Model）

- **`User.java`**：用户实体，包含角色和账户状态。
- **`Song.java`**：歌曲实体，包含文件夹、播放量、投票计数和软删除状态。
- **`Meme.java`**：迷因实体。
- **`Comment.java`**：评论实体。
- **`CommentLike.java`**：用户与评论的点赞关联实体。
- **`SongVote.java`**：用户与歌曲的点赞/点踩关联实体。
- **`Video.java`**：视频实体。

## 6. 数据访问（Repository）

- **`UserRepository.java`**：用户表 DAO。
- **`SongRepository.java`**：歌曲表 DAO。
- **`MemeRepository.java`**：迷因表 DAO。
- **`CommentRepository.java`**：评论表 DAO。
- **`CommentLikeRepository.java`**：评论点赞表 DAO。
- **`SongVoteRepository.java`**：歌曲投票表 DAO。

## 7. 数据传输对象（DTO）

- **`AuthResponse.java`**：认证和用户状态响应。
- **`LoginRequest.java`**：登录请求。
- **`RegisterRequest.java`**：注册请求。
- **`CommentDTO.java`**：评论及相关用户/点赞信息。
- **`FolderSongCount.java`**：文件夹歌曲统计。

## 8. 异常处理（Exception）

- **`GlobalExceptionHandler.java`**：统一转换业务异常和参数错误响应。
- **`ElementExistedException.java`**：元素已存在异常。
- **`ElementNotExistException.java`**：元素不存在异常。
- **`UnexpectedNullElementException.java`**：预期对象为空时使用的异常。

## 9. 定时任务（Task）

- **`SongSyncTask.java`**：每 30 分钟同步 OSS 歌曲文件和数据库记录。
- **`VoteCountSyncTask.java`**：每 60 分钟通过 Redis Pipeline 同步歌曲投票计数，并使用 `RedisTaskLock` 防止多实例重复执行。

## 10. 消息处理（Handler/Listener）

- **`OnlineCountHandler.java`**：WebSocket 连接、心跳、断开及 Redis 在线人数维护。
- **`OnlineCountListener.java`**：维护本实例 WebSocket 会话，并接收跨实例在线人数消息。

## 11. 枚举（Enums）

- **`UserRole.java`**：用户角色（`ADMIN`、`USER`、`STATION_MASTER`）。
- **`VoteType.java`**：投票类型（`LIKE`、`DISLIKE`）。
