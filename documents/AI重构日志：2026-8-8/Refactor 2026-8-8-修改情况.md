# 2026-08-08 高优先级整改变更记录

本次依据 `documents/Refactor 2026-8-7.md` 实施，重点处理 P0/P1 中影响可靠性、性能和可扩展性的项目。证书私钥已由维护者替换，本次不修改证书文件。

## 已修改的问题与解决方式

### 1. 认证角色和管理接口边界

- `CustomUserDetails` 不再把所有用户都映射为 `ROLE_USER`，而是依据数据库角色映射为 `ROLE_USER`、`ROLE_ADMIN` 或 `ROLE_STATION_MASTER`。
- Spring Security 显式保护 `/api/memes/**` 和 `/api/admin/**`，迷因同步、删除等管理接口不再仅依赖“是否已登录”。
- 原测试接口 `/api/public/test/**` 改为仅管理员可访问，避免公开数据库连接信息、Session ID 和安全上下文。
- WebSocket 不再允许任意 Origin，改为读取 `APP_WEBSOCKET_ALLOWED_ORIGINS`。

### 2. 投票数据的 Redis 冷启动保护

- 增加 `music:votes:redis-rebuilt` 重建标记。
- 启动或检测到标记缺失时，先从 MySQL `song_votes` 重建 `likes:{songId}` 和 `dislikes:{songId}` Set，再回写歌曲计数。
- Redis Set 缺失不再直接解释为“票数为 0”，避免 Redis 清空后把 MySQL 持久化计数错误清零。
- 投票同步和歌曲 OSS 同步都使用 Redis 租约锁，避免多实例重复执行。
- 租约释放使用 token 校验，避免一个任务释放另一个任务持有的锁。

### 3. 歌曲列表和缓存操作优化

- `SongService.hydrateSongVoteCounts()` 从逐歌曲 `SCARD` 改为 Redis Pipeline，减少网络往返。
- 歌曲缓存失效从 Redis `KEYS` 改为渐进式 `SCAN`，避免大 Keyspace 下阻塞 Redis。
- Redis 投票重建删除旧投票 key 时同样使用 `SCAN`。

### 4. 在线人数多实例和异常断线可靠性

- 删除启动时清空全局在线计数的 `StartupCleaner`，避免一个实例重启影响其他实例。
- 在线连接改用带过期时间的 Redis ZSET 记录，服务端广播时清理过期连接。
- 歌曲听众按歌曲使用独立 ZSET，并维护歌曲索引，异常断线后可自动收敛。
- 前端每 30 秒发送 WebSocket 心跳，连接持续期间不会因没有切歌而过期。

### 5. 运行健康与生产构建

- 引入 Spring Boot Actuator，只暴露 `health` 和 `info`，并启用 liveness/readiness 探针。
- 后端容器增加 `/actuator/health/readiness` 健康检查，并安装最小化 `curl` 依赖。
- Compose 增加 Redis、Spring 健康检查和条件依赖，避免仅按容器启动顺序启动业务。
- 前端生产构建恢复压缩并关闭公开 source map，降低产物体积和源码暴露风险。
- Session Cookie 的默认 `Secure` 改为开启；生产部署仍应显式设置环境变量。

## 与现有行为的兼容说明

- 业务 API 路径、歌曲 key、投票接口和主要响应字段未改变。
- 在线人数 Redis key 结构发生变化，旧的 `music:online:total` 和 `music:online:song:listeners` 不再读取；新实例会从当前 WebSocket 连接重新建立状态。
- 投票 Redis key 保持不变；仅增加重建标记和重建流程。
- 前端生产资源文件会变化，但播放器、路由和接口调用方式不变。

## 仍需人工完成的配置与发布动作

1. **确认环境变量**：生产环境设置精确的 `APP_CORS_ALLOWED_ORIGINS` 和 `APP_WEBSOCKET_ALLOWED_ORIGINS`，只保留实际域名；不要使用 `*`。
2. **确认 Cookie 配置**：HTTPS 生产环境保持 `SERVER_SESSION_COOKIE_SECURE=true`；本地使用 HTTP 开发时，如确有需要，单独在本地环境覆盖为 `false`。
3. **重新构建并启动容器**：由于后端增加 Actuator、`curl`、健康检查和任务逻辑，需要执行 `docker compose up -d --build`，不能只重启旧容器。
4. **观察首次启动日志**：首次启动或 Redis 数据丢失后，应看到投票关系重建日志；重建期间不要手动删除 MySQL 投票表。
5. **核对部署工作流**：当前 `../../.github/workflows/deploy.yml` 仍引用服务器上的 `deploy.sh`，而该脚本不在本项目正式目录中；请维护者确认服务器上的脚本路径、镜像 tag 策略和回滚方式后再恢复自动发布。
6. **证书**：证书私钥已由维护者替换，但仍建议确认新私钥未被 Git 跟踪，并在服务器完成证书挂载和 Nginx 配置检查。

## ServerDeployment/Ice 已适配内容

服务器部署目录已同步到当前镜像运行要求：

- Compose 使用 `IMAGE_TAG`，未设置时兼容使用 `latest`。
- Spring、前端和 Redis 增加 healthcheck，并按健康状态启动依赖服务。
- Spring 注入 `APP_CORS_ALLOWED_ORIGINS` 和 `APP_WEBSOCKET_ALLOWED_ORIGINS`。
- `deploy.sh` 使用 `docker compose pull`、健康等待和 HTTPS 健康探测，不再拉取后立即宣告成功。
- 服务器上的 `../../.env` 与证书目录已加入 Git 忽略规则，避免部署凭据和私钥被提交。

建议发布时通过服务器 `../../.env` 设置不可变的 `IMAGE_TAG`，例如 Git commit SHA；如暂时继续使用 `latest`，应确认镜像推送完成后再执行脚本。

## 尚未在本次实施的项目

- 投票 outbox/事件表和完整对账命令：本次先实现安全的 Redis 重建保护，避免直接引入数据迁移风险。
- 歌曲、评论和迷因分页接口：这些会改变请求/响应形态，需要先与前端协商兼容版本。
- OSS 删除/替换的增量同步：当前同步仍以新增 key 为主，后续应补充对象版本和同步状态。
- CI 镜像不可变 tag、扫描和自动回滚：依赖服务器部署脚本和发布策略确认。
- CSRF 全面收紧：当前系统使用 Cookie Session，生产环境应在确认前端请求流程后补 Token 或等价防护。

## 验证结果

- 后端 `mvn test`：通过；当前项目没有测试用例。
- 前端 `npm run build`：通过。
- `docker compose config --quiet`：通过。
- `git diff --check`：通过。
