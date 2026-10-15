# 登录与 CSRF 防护

## 当前行为

- 继续使用 Redis Session 和 Cookie 认证，同一账号登录时清理其他会话。
- 自定义登录入口显式调用 Spring Security 会话策略：已有 Session ID 在登录时轮换，旧 CSRF Token 失效。
- 登出使用 Spring Security 的 LogoutHandler 清理 CSRF Token、Session、SecurityContextHolder 和 SecurityContextRepository。
- Session Cookie 保持已有 HttpOnly、Secure、有效期配置，显式设置 SameSite=Lax。
- 所有非安全方法均校验 CSRF，包括登录、注册、评论、投票、管理操作和匿名播放计数。
- 未登录访问受保护接口返回 JSON 401；权限不足返回 JSON 403。CSRF 失败返回 JSON 403，`code` 为 `CSRF_INVALID`。

## 网页接入

`frontend/src/services/http.js` 统一处理写请求：从 `/api/auth/csrf` 获取 Token，保存在当前页面内存中，
通过返回的 `headerName` 携带 `token`。后端 Token 保存在 Session 中，端点返回带 XOR 掩码的 Token，保留 Spring Security 的 BREACH 防护。

登录或登出成功后，前端丢弃旧 Token，下次写请求重新获取。跨标签页登录、登出或 Session 过期可能使缓存 Token 失效；
只在后端明确返回 `CSRF_INVALID` 时刷新 Token 并重试一次，不重试普通权限错误、业务错误或网络错误。
原生 Fetch 保留用于只读 GET 和 OSS 文件请求，站内写请求使用统一 Axios 实例。

## 脚本接入

仓库现有 `performance/locust/locustfile.py` 和 `network_check.py` 仅使用 GET，不需要 Token。
需要登录或执行写操作的脚本按以下顺序接入：

1. 创建会保留 Cookie 的客户端，例如 `requests.Session()` 或 Locust 的 `self.client`。
2. GET `/api/auth/csrf`，保留响应 Cookie；读取响应中的 `headerName` 和 `token`。
3. 使用同一客户端提交 POST `/api/auth/login`，将 `token` 放入 `headerName` 指定的请求头。
4. 登录成功后再次 GET `/api/auth/csrf`，获取新 Token。后续 POST/PUT/PATCH/DELETE 请求均使用同一 Cookie 会话和新 Token。
5. POST `/api/auth/logout` 同样携带 Token；登出成功后丢弃旧 Token。之后的写请求先重新获取 Token。

Token 失效时，可以在收到 `403` 且 `code=CSRF_INVALID` 后重新获取并重试一次。
不要将普通 403 或网络超时作为自动重试写操作的依据，不要记录或提交 Session Cookie、Token、账号密码。

## 运行配置

生产继续由 Nginx 提供 HTTPS。纯 HTTP 本地调试时，按已有方式设置 `SERVER_SESSION_COOKIE_SECURE=false`；
该设置不适用于生产。当前没有新增环境变量或数据库字段。

设计依据：[Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)、
[自定义认证与会话管理](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)、
[自定义登出端点](https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html)。
