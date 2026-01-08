# 后端项目结构文档

本文档列出了 `backend` 目录下的主要 Java 文件及其作用说明。项目基于 Spring Boot 框架。

## 1. 核心入口
- **`MusicPlayerApplication.java`**: Spring Boot 应用启动类。

## 2. 配置 (Config)
- **`SecurityConfig.java`**: Spring Security 配置，处理认证、授权、CORS 等。
- **`RedisConfig.java`**: Redis 连接与模板配置。
- **`RedisPubSubConfig.java`**: Redis 发布/订阅配置（用于在线人数同步等）。
- **`WebSocketConfig.java`**: WebSocket 配置，注册端点。
- **`OssConfig.java`**: 对象存储（OSS）配置。
- **`StartupCleaner.java`**: 应用启动时的清理逻辑。

## 3. 控制器 (Controller)
- **`AuthController.java`**: 处理用户登录、注册、状态检查等认证请求。
- **`SongController.java`**: 处理歌曲相关的 API（列表、播放、删除/恢复、切换文件夹）。
- **`MemeController.java`**: 处理迷因漂流瓶相关的 API（随机获取、同步、删除）。
- **`CommentController.java`**: 处理评论相关的 API（获取、发表、点赞）。
- **`UserController.java`**: 用户管理相关 API。
- **`VideoController.java`**: 视频相关 API。
- **`HealthController.java`**: 健康检查接口。
- **`TestController.java`**: 测试接口。

## 4. 服务 (Service)
- **`UserService.java`**: 用户业务逻辑（注册、登录验证）。
- **`SongService.java`**: 歌曲业务逻辑（文件扫描、数据库同步、软删除）。
- **`MemeService.java`**: 迷因业务逻辑（随机获取、OSS 前缀扫描同步）。
- **`CommentService.java`**: 评论业务逻辑。
- **`VoteService.java`**: 投票（点赞/点踩）业务逻辑。
- **`VideoService.java`**: 视频业务逻辑。
- **`CustomUserDetailsService.java`**: Spring Security 用户详情加载服务。
- **`OssUtil.java`**: OSS 工具类。

- **`User.java`**: 用户实体类。
- **`Song.java`**: 歌曲实体类（包含播放量、删除状态等）。
- **`Meme.java`**: 迷因实体类。
- **`Comment.java`**: 评论实体类。放量、删除状态等）。
- **`Comment.java`**: 评论实体类。
- **`CommentLike.java`**: 评论点赞关联实体。
- **`SongVote.java`**: 歌曲投票关联实体。
- **`Video.java`**: 视频实体类。

## 6. 数据访问 (Repository)
- **`UserRepository.java`**: 用户表 DAO。
- **`SongRepository.java`**: 歌曲表 DAO。
- **`MemeRepository.java`**: 迷因表 DAO。
- **`CommentRepository.java`**: 评论表 DAO。
- **`CommentLikeRepository.java`**: 评论点赞表 DAO。
- **`SongVoteRepository.java`**: 歌曲投票表 DAO。

## 7. 数据传输对象 (DTO)
- **`AuthResponse.java`**: 认证响应 DTO。
- **`LoginRequest.java`**: 登录请求 DTO。
- **`RegisterRequest.java`**: 注册请求 DTO。
- **`CommentDTO.java`**: 评论信息 DTO。
- **`FolderSongCount.java`**: 文件夹歌曲统计 DTO。

## 8. 异常处理 (Exception)
- **`GlobalExceptionHandler.java`**: 全局异常处理器。
- **`ElementExistedException.java`**: 元素已存在异常。
- **`ElementNotExistException.java`**: 元素不存在异常。

## 9. 定时任务 (Task)
- **`SongSyncTask.java`**: 歌曲文件同步定时任务。
- **`VoteCountSyncTask.java`**: 投票计数同步定时任务。

## 10. 消息处理 (Handler/Listener)
- **`OnlineCountHandler.java`**: WebSocket 处理器，管理在线人数。
- **`OnlineCountListener.java`**: Redis 消息监听器，用于多实例间同步在线人数。

## 11. 枚举 (Enums)
- **`UserRole.java`**: 用户角色枚举（ADMIN, USER, STATION_MASTER）。
- **`VoteType.java`**: 投票类型枚举（LIKE, DISLIKE）。
