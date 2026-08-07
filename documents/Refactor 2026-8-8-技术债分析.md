# Ice 项目技术债审查与整改方案

> 审查日期：2026-08-07  
> 审查范围：项目根目录下除 `ServerDeployment` 外的代码、配置、构建及文档  
> 目标：在不改变现有业务行为和用户可见功能的前提下，降低安全、可靠性、性能、可维护性和交付风险

## 1. 结论摘要

当前项目已经具备可运行的业务闭环：Vue 3 前端通过 Nginx 提供静态资源，Spring Boot 后端负责认证、歌曲、评论、投票和迷因功能，Redis 承担会话、缓存、投票集合及在线人数，MySQL 保存业务数据，OSS 保存媒体文件。

主要问题不是单点代码质量，而是“多个真实来源并存、边界依赖隐式约定”：

1. **安全边界不统一**：Spring Security、Session 中的 `user` 属性和控制器手工角色判断并存；迷因管理接口没有控制器级鉴权，WebSocket 允许任意来源连接，CSRF 被全局关闭。
2. **数据一致性有高风险路径**：投票同时写 MySQL 和 Redis 但不是原子操作；Redis 冷启动时定时任务可能把数据库票数覆盖为 0；缓存保存 JPA Entity 和签名 URL，失效、序列化和演进成本较高。
3. **规模化能力不足**：歌曲、评论、同步任务均有全量读取；歌单计数是逐歌单查询；缓存失效使用 Redis `KEYS`；随机迷因采用随机 offset，数据量增长后会退化。
4. **部署与配置不可审计**：证书私钥处于 Git 跟踪范围；生产构建关闭前端压缩并生成 source map；CI 的构建/推送部分被注释，工作流引用的根目录 `deploy.sh` 在当前项目中不存在。
5. **可验证性不足**：后端测试源目录为空，前端没有 lint/type/test 脚本；构建阶段明确 `-DskipTests`；健康检查只返回静态应用状态，不能反映数据库、Redis、OSS 状态。

整改应优先处理 P0 安全与数据完整性风险，再处理性能和结构重构。所有改动应采用兼容迁移：先增加观测和测试，再切换实现，最后清理旧路径；不要一次性重写前后端。

## 2. 当前系统基线

### 2.1 运行链路

```text
浏览器
  ├─ HTTPS / Nginx 静态资源与 SPA fallback
  ├─ /api/*  ───────────────> Spring Boot:8082
  └─ /ws/online ────────────> Spring WebSocket
                                  ├─ MySQL：用户、歌曲、评论、投票、迷因元数据
                                  ├─ Redis：Session、歌曲缓存、投票 Set、在线人数、站点开关
                                  └─ Aliyun OSS：歌曲与迷因文件
```

### 2.2 已识别的领域边界

- 认证：Session Cookie + Spring Security，但业务控制器仍从 `HttpSession` 读取 `"user"`。
- 歌曲：OSS 定时同步到 MySQL；歌曲列表使用 Redis 缓存并生成 OSS 签名 URL。
- 投票：MySQL 保存用户投票关系，Redis Set 负责实时计数，定时任务回写歌曲计数。
- 评论：MySQL 保存评论和评论点赞；服务层手工组装两层评论树。
- 在线人数：WebSocket 连接状态写 Redis，并通过 Redis Pub/Sub 广播。
- 前端：多个 API service、Pinia、EventBus、播放器 composables 和浏览器 LocalStorage 共同维护状态。

## 3. 技术债清单与证据

### 3.1 P0：应立即治理的安全与数据风险

| 编号 | 问题 | 证据 | 风险 | 整改方向 |
|---|---|---|---|---|
| P0-01 | 敏感凭据文件进入 Git 跟踪范围 | `git ls-files` 显示 `certs/beemusic.fun.key`；`.gitignore` 只忽略 `.env`，未忽略 `certs/` | 私钥泄露后 HTTPS 身份可被冒用；历史提交仍可能保留 | 立即轮换证书和私钥；从 Git 历史清理；仅挂载外部 Secret；增加 secret scanning 和提交钩子 |
| P0-02 | 迷因管理接口缺少权限校验 | `MemeController` 的 `POST /api/memes/sync`、`DELETE /api/memes/{id}` 没有角色判断 | 任意已认证用户可能同步或删除内容；若鉴权配置变化，边界更脆弱 | 统一使用 Spring Security 方法/请求授权，明确仅站长或管理员可操作，并添加拒绝用例 |
| P0-03 | WebSocket 允许任意 Origin | `WebSocketConfig.java:21-22` 使用 `.setAllowedOriginPatterns("*")` | 可被任意站点建立连接，造成在线人数污染、资源消耗和跨站滥用 | 改为配置化允许来源；连接数、消息大小、消息频率限流；异常断开时保证计数收敛 |
| P0-04 | Session Cookie 的 Secure 默认关闭且 CSRF 全局关闭 | `application.properties:6` 默认 `false`；`SecurityConfig.java:32` 禁用 CSRF | HTTPS 部署若未显式注入变量，Cookie 可能经 HTTP 传输；Cookie 认证接口缺少 CSRF 防护 | 按环境启用 Secure、SameSite；使用 CSRF Token 或改用明确的无状态认证方案；先通过集成测试确认兼容性 |
| P0-05 | 认证身份来源重复 | `AuthController` 把 User 放入 Session；多个 Controller 又自行读取 `"user"`，同时 SecurityConfig 建立 SecurityContext | Session 属性和 SecurityContext 可能不同步，角色变更、失效和并发登录行为难以推理 | 以 Spring Security `Authentication` 为唯一身份来源；逐接口迁移，保留兼容读取仅用于过渡并加监控 |
| P0-06 | Redis 冷启动可能覆盖持久化票数 | `VoteCountSyncTask.syncOnStartup()` 调用全量同步；同步逻辑对 Redis 缺失 Set 按 0 处理并写回 MySQL | Redis 清空或新环境启动时，MySQL 中已有票数可能被错误清零 | 明确 MySQL/Redis 的事实来源；增加 Redis 重建流程和版本标记；只有确认集合已完成重建后才允许回写 |
| P0-07 | Redis 与 MySQL 双写无一致性协议 | `VoteService.vote/cancel` 先写数据库关系再写 Redis，异常时没有补偿；事务只覆盖数据库 | 部分成功会造成用户投票关系、实时计数、回写计数不一致 | 先记录投票事件或 outbox，再异步重建 Redis；为重试、幂等和对账定义协议 |
| P0-08 | Redis JSON 使用宽泛默认类型 | `RedisConfig.java:42` 使用 `LaissezFaireSubTypeValidator` 和 `DefaultTyping.NON_FINAL` | 反序列化边界过宽，缓存数据被污染时扩大安全面；实体结构变化也容易导致缓存不可读 | 使用显式 DTO、固定序列化类型和版本化 cache key；禁止宽泛多态反序列化 |

### 3.2 P1：可靠性、性能与可扩展性

| 编号 | 问题 | 证据 | 影响 | 整改方向 |
|---|---|---|---|---|
| P1-01 | 歌曲列表无分页且每首歌查询两次 Redis | `SongService.getSongs()` 全量返回；`hydrateSongVoteCounts()` 按歌曲逐个 `SCARD` | 歌曲数增加后响应体、Redis RTT 和前端排序成本线性增长 | 先保持接口兼容增加分页/游标接口；投票计数批量读取；设定最大页大小 |
| P1-02 | 缓存失效使用 `KEYS` | `SongService.java:118` 调用 `redisTemplate.keys("songs:folder:*")` | Redis 大 Keyspace 下可能阻塞主线程，影响所有请求 | 使用固定 key 清单、版本化命名空间或 `SCAN`；删除操作与缓存更新建立明确顺序 |
| P1-03 | 定时任务全量扫描并在启动期执行重操作 | `VoteCountSyncTask` `findAll()`；`SongSyncTask` 启动新线程 sleep 5 秒后全量 OSS 同步 | 启动时间、数据库连接、OSS 配额和多实例重复执行不可控 | 使用分页/增量同步、分布式锁、任务状态表和可观测指标；启动任务改为受控生命周期任务 |
| P1-04 | 多实例下定时任务和 OSS 同步没有协调机制 | `@Scheduled` 直接执行，`syncAllSongs()` 无锁 | 多副本会重复扫 OSS、重复写库、互相清缓存 | 使用 ShedLock/数据库租约/单独 worker；先定义任务幂等键 |
| P1-05 | 歌曲同步不会处理 OSS 删除或内容变更 | `syncSongsFromOss()` 只找数据库不存在的 key | OSS 已删除文件仍可能在数据库和缓存中出现；同 key 替换无法识别 | 保存 ETag/LastModified/同步版本；定义删除、恢复和软删除策略 |
| P1-06 | 歌单计数存在 N 次查询 | `getFolderSongCounts()` 对 `AVAILABLE_FOLDERS` 每项执行一次 count | 固定歌单数量尚可，歌单扩展后数据库压力增加 | 用单条 `GROUP BY` 查询或缓存计数，并在同步/删除后定向失效 |
| P1-07 | 随机迷因的 count + offset 不是稳定 O(1) | `MemeService.getRandomMeme()` 先 count 再 `PageRequest.of(index, 1)` | offset 增大后数据库仍需扫描/跳过记录；count 与读取之间也可能变化 | 使用随机主键范围、预生成可用 ID、抽样表或数据库原生方案；以压测结果选型 |
| P1-08 | 评论读取全量并在内存组树 | `CommentService.getComments()` 一次读出歌曲全部评论 | 热门歌曲评论增长后内存、响应时间和排序成本不可控 | 分页根评论、分页回复；数据库排序和索引配套；限制内容长度和层级 |
| P1-09 | 评论点赞和投票的并发幂等依赖应用逻辑 | 评论点赞先 exists 再 save；投票先查询再 save | 并发请求可能触发唯一约束异常或计数竞争 | 数据库唯一约束 + upsert/幂等命令；计数由事实表重算或可靠事件更新 |
| P1-10 | 在线人数依赖长期 Redis 计数且异常路径吞错 | `OnlineCountHandler` 连接断开时增减计数；`broadcast()` 捕获异常后忽略 | 网络中断、进程崩溃会留下脏计数；故障无法定位 | 使用连接租约/心跳和 TTL，按实例维护集合；记录广播失败指标和结构化日志 |
| P1-11 | 健康检查不是依赖健康检查 | `HealthController` 固定返回 `UP`；`TestController` 另提供测试接口 | 容器可能在 Redis/MySQL 不可用时仍被认为健康；探针无法区分存活与就绪 | 引入 Actuator 的 liveness/readiness；依赖检查不泄露连接信息 |
| P1-12 | 异常处理和输入校验不完整 | DTO 无校验注解；评论使用 `Map<String,Object>`；多个 `RuntimeException`；`AuthController.status()` 捕获 `Exception` 后仍返回已认证 | 错误码不稳定、脏数据可入库、真实故障被伪装成成功 | DTO + Bean Validation；统一错误码；区分 4xx/5xx；禁止成功形状的降级响应 |

### 3.3 P1：交付、配置和运行安全

| 编号 | 问题 | 证据 | 影响 | 整改方向 |
|---|---|---|---|---|
| P1-13 | 生产构建关闭压缩并生成 source map | `frontend/vite.config.js:10-11` | 资源体积变大、源码结构暴露、首屏与带宽成本增加 | 生产默认 minify；source map 仅在受控构建产物中保留并限制访问 |
| P1-14 | CI 构建和推送阶段被整体注释 | `.github/workflows/deploy.yml:8-44` | 提交不能证明镜像可构建，远程部署可能拉到旧的 `latest` | 恢复可复现 build/push，使用 commit SHA/版本 tag，部署前执行 smoke test |
| P1-15 | CI 引用当前根目录不存在的 `deploy.sh` | `.github/workflows/deploy.yml:60-61`；当前根目录只有 `deploy.bat`，`deploy.sh` 位于被排除的备份目录 | 自动部署链路在服务器上可能直接失败 | 将正式部署入口纳入受控版本，并在 CI 中校验文件和命令；备份脚本不作为运行依赖 |
| P1-16 | Compose 依赖只保证启动顺序 | 根 `docker-compose.yml` 使用 `depends_on`，没有 healthcheck | Spring 可能在 Redis 尚未就绪时启动；前端可能在后端未就绪时工作 | 为 Redis、Spring 增加健康检查和条件依赖；设置合理重试 |
| P1-17 | 容器使用固定名称和 `latest`/本地 build 混合策略 | Compose 设置 `container_name`；镜像/部署脚本使用 `latest` | 不利于多副本、回滚和环境可复现；不同机器构建结果可能不同 | 使用项目/环境命名空间、不可变 tag、镜像 digest 和回滚记录 |
| P1-18 | Redis、数据库和 OSS 的连接配置缺少显式安全项 | `application.properties` 只配置 Redis host/port；Compose 直接启动 `redis:6.2` | Redis 无认证/TLS/资源限制，网络或误配置时扩大数据暴露面 | Secret 注入、最小网络暴露、认证/TLS、内存淘汰策略、备份与恢复演练 |
| P1-19 | Nginx TLS 和安全响应头治理不足 | `default.conf` 有 TLS 配置，但没有 HSTS、CSP、Frame/Content-Type 等响应头 | 降低浏览器侧防护能力；配置变更缺少自动验证 | 用受支持的 TLS 配置模板；分阶段加入安全头并验证播放器、OSS 和 WebSocket 兼容性 |

### 3.4 P2：可维护性与工程效率

| 编号 | 问题 | 证据 | 整改方向 |
|---|---|---|---|
| P2-01 | 后端无测试源，构建跳过测试 | 当前 `backend/src/test` 无 Java 测试；Dockerfile 使用 `mvn clean package -DskipTests` | 先覆盖认证、投票、权限、缓存失效、同步幂等和关键 Controller，再将测试纳入 CI |
| P2-02 | 前端没有 lint、format、type/test 脚本 | `frontend/package.json` 只有 dev/build/preview | 统一 ESLint/Prettier 规则；为 service、store、播放器状态和关键页面补组件/集成测试 |
| P2-03 | 业务实体直接作为 API 和缓存模型 | `Song`、`Meme` 等 Entity 直接由 Controller 返回，Song 还含 `@Transient url` | 数据库字段变化会影响 API；缓存序列化与持久化模型耦合 | 引入响应 DTO、缓存 DTO 和 Mapper；保留旧 JSON 字段的兼容期 |
| P2-04 | 配置和枚举在前后端重复，角色使用数字 | `SongService.AVAILABLE_FOLDERS` 与前端 `FOLDER_INFO` 重复；Pinia 角色用 1/2/3 | 增删歌单或角色容易只改一端；可读性和演进性差 | 后端提供版本化只读元数据接口，前端保留默认值作为降级；角色改为枚举/权限字符串 |
| P2-05 | 状态管理同时使用 Pinia、EventBus、组件本地状态和 LocalStorage | `store/index.js`、`utils/eventBus.js`、播放器 composables | 状态更新来源多，出现竞态时难以追踪 | 按领域划分 store；EventBus 仅保留跨域通知；定义状态所有权和持久化边界 |
| P2-06 | 手工错误处理造成静默失败和调试噪声 | 前端多个 `catch` 只清空列表/console；后端有 `System.err` 和 ignored exception | 用户无法区分空数据、网络故障和权限故障；生产排障信息不足 | 统一 API 错误模型、Toast/重试策略和结构化日志；保留可观测但不泄露敏感信息 |
| P2-07 | 依赖和 Java 基线偏激进且缺少治理 | Maven 使用 Java 22；后端没有依赖审计/更新策略；前端依赖版本使用范围符号 | 运行时升级和供应链漏洞修复不可控 | 选择受支持的 LTS 基线，锁定关键依赖，建立 Dependabot/审计和升级窗口 |
| P2-08 | 文档存在过时或过度乐观描述 | `BACKEND_STRUCTURE.md` 有重复条目；投票文档称“完美解决”；优化报告中的同步周期与当前代码不完全一致 | 新维护者会依据错误假设改动系统 | 将文档改为架构事实、约束、已知风险和可复现实验；每次结构变更同步更新 |

## 4. 整改原则与兼容策略

### 4.1 不改变实际行为

- 保持现有 URL、HTTP 方法、主要 JSON 字段、Session Cookie 名称和公开歌单 key 不变。
- 先新增 DTO、权限规则测试、分页接口和观测指标，再在兼容窗口内切换调用方。
- 缓存 key 采用新版本命名空间；新 key 并行读取一段时间，避免发布时全量失效。
- 数据库变更只增不删：先加索引/新列/事件表，再回填和校验，最后才考虑删除旧列。
- 投票、评论、歌曲同步先做双写对账或 shadow read，不在第一阶段直接改变用户看到的计数。
- 对生产配置使用环境变量默认值，但逐步把不安全默认值改为启动失败而不是静默降级。

### 4.2 事实来源定义

| 数据 | 短期兼容来源 | 目标事实来源 |
|---|---|---|
| 用户身份 | SecurityContext + Session 兼容属性 | Spring Security Authentication |
| 用户投票关系 | MySQL `song_votes` 旧与 Redis Set 对账 | MySQL 事实表，Redis 为可重建读模型 |
| 投票总数 | Redis 实时读、MySQL 回写 | 由事实表/事件可靠重建的计数读模型 |
| 歌曲元数据 | MySQL + OSS 增量同步 | 同步状态明确的 MySQL 投影，OSS 为文件事实来源 |
| 歌曲签名 URL | Redis 缓存成品 | 短 TTL 的专用缓存 DTO，不进入持久化 Entity |
| 在线人数 | Redis 计数兼容 | 带租约/心跳的连接集合或按实例聚合 |
| 站点开关 | Redis key | 配置表或版本化配置中心，Redis 仅作缓存（若确有持久化需求） |

## 5. 分阶段实施路线

### 阶段 0：止血与基线（1 个迭代）

目标是先消除不可接受风险，不改变业务流程。

1. 轮换已暴露的证书/私钥和所有可能进入日志或仓库的凭据，清理 Git 历史，并建立 secret scanning。
2. 给迷因同步/删除加权限测试和统一授权；限制 WebSocket Origin、消息大小和连接频率。
3. 检查生产环境 `SESSION_COOKIE_SECURE=true`、SameSite、Redis 认证和网络暴露；为不满足条件的生产配置增加启动告警。
4. 暂停“Redis 空集合直接回写 MySQL”为 0 的路径，先增加 Redis 重建/对账命令；保留现有接口返回格式。
5. 建立最小监控：HTTP 5xx/延迟、认证失败、Redis/DB 连接、同步成功/失败、投票对账差异和 WebSocket 连接数。
6. 修正文档中与代码不一致的部署入口说明；明确当前正式部署脚本的唯一来源。

**完成标准**：凭据不再被版本控制跟踪；未授权管理接口均返回 401/403；生产 WebSocket 只接受受信 Origin；Redis 清空演练不会造成数据库票数静默清零。

### 阶段 1：建立可验证的应用边界（1～2 个迭代）

1. 为认证、角色、歌曲删除/恢复、迷因管理、站点配置、评论和投票建立 Controller 集成测试。
2. 将请求 `Map` 改为带 `@Valid` 的 DTO，统一错误响应（例如 `code/message/requestId`），不返回数据库异常详情。
3. 统一使用 `Authentication` 获取当前用户；将角色判断替换为权限名称，保留旧角色字段读取兼容。
4. 拆分 `HealthController` 与测试接口；引入 liveness/readiness，删除或隔离会泄露 JDBC URL、Session ID 和上下文的测试输出。
5. 用固定类型的缓存 DTO 替换 Entity 缓存，禁用宽泛 Jackson 默认类型；为 cache key 增加版本号。

**完成标准**：核心授权矩阵有自动化测试；异常状态码稳定；缓存可在应用升级后安全失效；健康探针能区分存活和依赖就绪。

### 阶段 2：修复数据一致性与任务调度（2～3 个迭代）

1. 设计投票 outbox/事件表：数据库事务只提交事实，可靠消费者负责更新 Redis；消费者必须幂等、可重试、可对账。
2. 提供“从 MySQL 重建 Redis 投票集合”和“Redis/MySQL 计数对账”两个运维命令，记录批次、耗时、差异和结果。
3. 将投票同步、歌曲同步改为分页/增量，加入分布式锁、超时、重试退避和任务状态；禁止每个副本同时全量执行。
4. 歌曲同步保存对象版本信息，处理新增、删除、恢复和 OSS 内容变更；缓存失效改为版本切换或 `SCAN`。
5. 为评论点赞、投票关系增加数据库唯一约束和数据库层幂等写法；验证歌曲、评论、回复目标存在且属于同一业务范围。
6. 在线人数改为心跳/租约模型，过期连接自动回收；广播失败不能被静默吞掉。

**完成标准**：Redis 重建后计数与事实表一致；重复事件不产生重复关系或计数；多实例只产生一个同步批次；异常断开后在线人数在租约窗口内收敛。

### 阶段 3：容量与接口演进（2～4 个迭代）

1. 歌曲、评论和迷因列表增加分页/游标接口，并设置上限；旧接口先保持兼容，再推动前端切换。
2. 将歌曲投票计数改成批量读取或预聚合，歌单统计改为 `GROUP BY`/缓存。
3. 为 `songs(object_key, is_deleted)`、评论查询、点赞关系和投票关系补充并验证索引。
4. 为迷因随机读取建立基准测试，在随机主键、抽样表或专用索引方案中选择可证明的实现。
5. 统一 API service、Pinia domain store 和错误处理；逐步收缩 EventBus 与组件内部重复状态。
6. 生产构建恢复 minify，source map 仅上传到受控错误监控系统；优化静态资源缓存策略并验证 SPA、API、WebSocket 和 OSS 播放。

**完成标准**：在目标数据规模和并发下，接口延迟、响应体、Redis 命令数和数据库扫描量有可重复基准；前端构建产物不包含公开 source map，且功能回归通过。

### 阶段 4：交付工程化与长期治理（持续）

1. CI 分为依赖安装、后端测试、前端 lint/build/test、镜像构建、镜像扫描、部署 smoke test。
2. 镜像使用 commit SHA 或语义版本，不再以 `latest` 作为唯一发布标识；保留上一版本并支持回滚。
3. Compose 增加 healthcheck、资源限制、日志轮转、Secret 挂载和备份/恢复说明；固定基础镜像 digest。
4. 建立 OpenAPI/接口契约、数据库迁移工具、依赖升级窗口和变更记录。
5. 每月复核安全扫描、依赖漏洞、慢查询、错误预算、同步失败率、缓存命中率和容量趋势。

## 6. 建议的验证矩阵

| 领域 | 必须验证的场景 | 关键指标 |
|---|---|---|
| 认证 | 注册、登录、登出、过期 Session、单点登录限制、禁用用户 | 401/403 正确率、Session 一致性 |
| 授权 | 普通用户/管理员/站长访问每个管理接口 | 越权用例全部拒绝 |
| 投票 | 重复点击、切换、取消、并发、Redis 清空、消费重试 | 事实表与读模型差异为 0 |
| 歌曲 | OSS 新增/删除/替换、缓存命中、签名 URL 过期、分页 | P95 延迟、Redis 命令数、同步耗时 |
| 评论 | 超长/空内容、非法歌曲、非法父评论、并发点赞 | 4xx 分布、唯一约束冲突率 |
| WebSocket | 重连、异常断开、多实例、恶意 Origin、大消息/高频消息 | 连接数收敛、广播失败率 |
| 部署 | 冷启动、依赖未就绪、滚动更新、回滚、证书更新 | readiness、发布成功率、回滚时间 |
| 前端 | 首屏、播放、SPA 刷新、API 失败、权限失效、静态缓存 | 产物体积、LCP、JS 错误率 |

## 7. 不建议立即做的事情

- 不在没有数据对账和回滚方案前直接把 Redis 投票逻辑改成另一种存储。
- 不在没有接口兼容层前重命名所有 API 或修改 JSON 字段。
- 不把所有实体改成复杂领域模型作为第一步；先处理安全边界、事实来源和测试。
- 不用增加更多缓存掩盖全量查询、无分页和缺少索引的问题。
- 不把 `ServerDeployment` 备份脚本当作当前系统的实现依据；本审查已按要求将其排除。

## 8. 优先级总表

| 优先级 | 范围 | 目标 |
|---|---|---|
| P0 | 凭据、管理接口、Origin、Cookie/CSRF、投票数据、Redis 序列化 | 消除可被利用或造成不可逆数据损失的风险 |
| P1 | 全量查询、同步任务、健康检查、CI/CD、生产构建、基础设施 | 让系统可观测、可恢复、可扩展、可复现 |
| P2 | DTO、状态管理、依赖、文档、代码规范 | 降低后续变更成本，避免债务重新积累 |

最终建议以“阶段 0 完成”为继续扩容和结构重构的前置条件，以“阶段 1 的测试和事实来源定义”为所有后续改动的安全网。这样可以在保持当前网站行为的同时，逐步把隐式约定转化为可验证的契约。
