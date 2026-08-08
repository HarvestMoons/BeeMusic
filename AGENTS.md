# Ice Music Player - AI 项目说明

本文档是给 AI 编程助手的仓库级上下文。在修改代码前应先阅读本文档，并以当前源码、配置和构建文件为最终事实来源；`documents/` 中的结构说明可能没有覆盖最近的变更。

## 1. 项目概览

Ice（线上站点为 BeeMusic）是一个音乐播放器网站，提供：

- 按 OSS 文件夹组织和播放音乐；
- 用户注册、登录、登出和基于 Session Cookie 的认证；
- 评论及评论点赞；
- 歌曲点赞/点踩、按投票数排序；
- 迷因图片漂流瓶；
- 随机视频背景；
- WebSocket 在线人数；
- 站长角色对歌曲进行软删除、恢复和同步；
- 站点配置、主题和隐藏歌单彩蛋。

这是一个前后端分离、当前以 Docker Compose 部署的单仓库项目，不要把它当作纯 Java 项目或纯静态 Vue 项目处理。

## 2. 技术栈和目录

| 目录 | 作用 | 主要技术 |
| --- | --- | --- |
| `backend/` | REST API、认证、业务和定时任务 | Java 22、Spring Boot 3.2.4、Spring MVC、Security、Session、JPA、WebSocket |
| `frontend/` | SPA 用户界面 | Vue 3、Vite 7、Pinia、Vue Router、Axios、原生 Fetch |
| `documents/` | 设计记录、结构说明和待办 | Markdown |
| `performance/` | Locust 压测及网络检查脚本 | Python |
| `certs/` | Nginx HTTPS 证书挂载目录 | `.pem`/`.key`，属于敏感部署材料 |
| `ServerDeployment/` | 服务器部署相关副本配置 | Docker Compose |
| `download_oss_music.py` | 从 OSS 下载音乐的辅助脚本 | Python |
| `docker-compose.yml` | 本地/生产容器编排参考 | Spring、Nginx、Redis |

后端源码包为 `com.example.musicplayer`，入口是
`backend/src/main/java/com/example/musicplayer/MusicPlayerApplication.java`。

## 3. 后端架构

后端按 Controller -> Service -> Repository/Model 分层：

- `controller/`：HTTP API。主要控制器为 `AuthController`、`SongController`、
  `CommentController`、`MemeController`、`VideoController`、`UserController`、
  `SiteConfigController` 和 `HealthController`。
- `service/`：业务逻辑。重点类为 `SongService`、`VoteService`、`CommentService`、
  `MemeService`、`UserService`、`VideoService` 和 `OssUtil`。
- `model/`：JPA 实体，包括 `User`、`Song`、`Comment`、`CommentLike`、
  `SongVote`、`Meme` 和 `Video`。
- `repository/`：Spring Data JPA 数据访问接口。
- `config/`：Security、Redis、OSS、WebSocket 和 Redis Pub/Sub 配置。
- `task/`：`SongSyncTask` 每 30 分钟同步歌曲，`VoteCountSyncTask` 每 60 分钟同步投票计数。
- `handler/`、`listener/`：在线人数 WebSocket 处理和跨实例消息同步。
- `exception/`：领域异常和 `GlobalExceptionHandler`。

### 重要数据流

1. 歌曲基础信息和持久化计数在 MySQL；歌曲音频/图片等对象在阿里云 OSS。
2. 歌曲列表优先从 Redis 读取，未命中时从 MySQL 查询并缓存。
3. Redis Set `likes:{songId}` 和 `dislikes:{songId}` 保存实时投票成员及计数；
   投票关系先在 MySQL `song_votes` 事务中提交，事务提交后尽力更新 Redis；Redis 是可重建的实时读模型，
   MySQL 是唯一事实来源，定时任务会从 MySQL 重建 Redis。
4. 列表返回前会用 Redis Set 的实时计数覆盖缓存对象中的投票数。
5. Redis 缺失或重启后，投票重建逻辑会从 `song_votes` 恢复 Set，再由定时任务将变化回写
   `songs` 的计数字段。多实例定时任务使用 Redis 租约避免重复执行。
6. OSS URL 是带有效期的签名 URL，不要把它当作永久公共 URL 保存或硬编码。

### 认证与权限

- Spring Session Data Redis 保存 Session，浏览器通过 Cookie 维持登录状态。
- 业务接口的当前用户和角色统一从 Spring Security `Authentication`（控制器中使用
  `@AuthenticationPrincipal`）获取，不要从 Session 自定义的 `"user"` 属性读取身份。
- 公共 API 使用 `/api/public/**`，认证 API 使用 `/api/auth/**`。
- 歌曲投票、评论等写操作通常需要登录；删除/恢复/同步和站点配置管理接口需要相应角色。
- 角色定义见 `backend/src/main/java/com/example/musicplayer/enums/UserRole.java`；
  不要只在前端隐藏按钮，后端必须继续做权限校验。
- `/ws/**` 和健康检查端点按 Security 配置放行。

## 4. API 和前端约定

后端 API 统一位于 `/api` 下，常用端点包括：

| 功能 | 端点 |
| --- | --- |
| 认证 | `/api/auth/register`、`/login`、`/logout`、`/status` |
| 歌曲 | `/api/public/songs/get`、`/folder-counts`、`/play/{id}`、`/votes/{id}` |
| 投票 | `/api/songs/like/{id}`、`/dislike/{id}`、`/vote/{id}` |
| 评论 | `/api/public/comments/{songId}`、`/api/comments/add`、评论点赞/删除 |
| 迷因 | `/api/public/memes/random`、`/api/memes/sync`、`/api/memes/{id}` |
| 视频 | `/api/public/videos/random` |
| 健康 | `/api/public/health`、`/actuator/health/**` |
| 在线人数 | WebSocket `/ws/online` |

前端入口是 `frontend/src/main.js`，路由在 `frontend/src/router/index.js`，认证、主题和
站点配置在 `frontend/src/store/index.js`。播放器相关代码集中在
`frontend/src/components/feature/player/` 和 `frontend/src/composables/player/`。

- API 基础地址和公开 API 常量见 `frontend/src/constants/index.js`。
- API 请求同时使用 Axios 和原生 Fetch；修改接口时先搜索两种调用方式。
- 播放器内部状态通过 Pinia、composable 和 `frontend/src/utils/eventBus.js` 协作。
- `VoteControls.vue` 发出 `song-vote-updated` 事件，播放器据此同步歌单内存状态；
  修改投票返回结构时必须同时检查这一条链路。
- WebSocket 客户端在 `frontend/src/components/common/OnlineStatus.vue`，代理路径由
  `frontend/nginx/default.conf` 配置。

## 5. 配置、运行和部署

### 本地开发

后端要求 JDK 22 和 Maven：

```powershell
cd backend
mvn spring-boot:run
```

前端要求 Node.js 22，安装锁定依赖并启动 Vite：

```powershell
cd frontend
npm ci
npm run dev
```

前端可用脚本只有 `dev`、`build`、`preview`；后端当前没有专门的测试源码目录。

### Docker Compose

根目录 `docker-compose.yml` 包含：

- `spring`：后端，默认端口 8082；
- `frontend`：Nginx，暴露 80/443，反代 `/api/` 和 `/ws/`；
- `redis`：Redis 6.2，使用 `redis_data` volume。

构建/启动前要准备环境变量，至少包括 MySQL 连接信息和 OSS 凭据。变量名称以
`backend/src/main/resources/application.properties` 和 `docker-compose.yml` 为准：
`SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD`、
`ALIYUN_OSS_ACCESS_KEY`、`ALIYUN_OSS_SECRET_KEY` 等。

不要提交 `.env`、OSS 密钥、数据库密码或 `certs/*.key`。不要为了调试把这些值写进源码、
日志、测试输出或文档；仓库中已有的敏感文件应视为部署材料，修改配置时只引用环境变量。

生产 Nginx 配置在 `frontend/nginx/default.conf`：HTTP 强制跳转 HTTPS，静态文件长期缓存，
`/api/` 转发到 `spring:8082`，`/ws/` 转发并保留 WebSocket Upgrade 头。修改后端端口、
路径或 WebSocket 端点时必须同步检查 Compose、Nginx、健康检查和前端 URL。

## 6. 修改代码时的规则

1. 先搜索现有实现和调用方，优先复用现有 service、composable、常量和异常类型。
2. 涉及数据库字段或实体时，同时检查 Repository、DTO、Controller、缓存键和已有数据库约束；
   `spring.jpa.hibernate.ddl-auto=none`，应用不会自动替你迁移生产数据库。
3. 涉及歌曲列表时，同时考虑 MySQL、Redis 缓存、OSS 签名 URL、软删除和文件夹白名单。
4. 涉及投票时保持点赞/点踩互斥、Redis Set 与 `song_votes` 一致，以及前端即时状态同步。
5. 涉及认证或写 API 时同时检查 CSRF/CORS、Session Cookie、Security matcher、
   `Authentication`/`@AuthenticationPrincipal` 和角色校验。
6. 不要把“前端按钮隐藏”当作权限控制，不要在异常处静默返回成功或吞掉异常。
7. 新增环境变量时同步更新 `application.properties`、Compose 和必要的部署文档。
8. 保持现有代码风格；Java 使用包分层和构造器注入，Vue 使用 Composition API 和现有目录分类。
9. 优先做局部、可回滚的修改，不要顺手重构无关模块或生成大型构建产物。

## 7. 验证清单

修改后按影响范围执行：

```powershell
# 后端编译和测试
cd backend
mvn test

# 前端生产构建
cd frontend
npm run build
```

若只修改文档，不需要执行构建。若修改 Compose、Nginx 或环境变量，还要检查容器健康检查、
API 反代、HTTPS 证书挂载和 WebSocket 连接路径。提交前确认没有把 `target/`、`node_modules/`、
`dist/`、缓存、密钥或证书私钥加入 Git。

## 8. 相关文档索引

- `documents/BACKEND_STRUCTURE.md`：后端类和目录说明；
- `documents/FRONTEND_STRUCTURE.md`：前端组件说明；
- `documents/VOTE_AND_RANKING_IMPLEMENTATION.md`：投票、排序和缓存一致性；
- `documents/OPTIMIZATION_LOG.md`：性能优化记录；
- `docker-compose.yml`、`backend/pom.xml`、`frontend/package.json`：运行时事实来源。
