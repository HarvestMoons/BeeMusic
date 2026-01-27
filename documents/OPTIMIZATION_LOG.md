# 性能优化日志

## 1. 目标
优化后端接口性能，重点关注 `SongService.getSongs` 的响应速度，引入 Redis 缓存。

## 2. 基准测试 (Baseline)

### 测试环境
- **时间**: 2026-01-27
- **分支**: main
- **工具**: Locust
- **接口**: `GET /api/public/songs/get` (获取当前文件夹歌曲列表)

### 测试配置 (Locust)
- **Target RPS**: 50
- **Duration**: 30s
- **Host**: https://beemusic.fun (或本地 localhost:8080)

### 测试结果 (优化前)
*(在此处填入运行 Locust 后的结果，例如：RPS, P95 Latency 等)*

| Metric | Value |
| :--- | :--- |
| RPS | ~1.41 |
| Min Response Time | 4310 ms |
| Average Response Time | 17227 ms |
| Max Response Time | 30421 ms |
| Median Response Time | 17000 ms |
| 95% Response Time | 27000 ms |
| Failure Rate | 0% |

### 现象分析
当前实现中，`getSongs` 每次请求都会执行 `syncSongsFromOss -> ossClient.listObjects` 并循环生成签名 URL。这会导致：
1. 网络 I/O 延迟高。
2. 频繁调用阿里云 OSS API（可能涉及费用）。
3. 随着并发增加，响应时间线性或指数级增长。

## 3. 优化方案
1. **引入 Redis 缓存**：缓存计算好的歌曲列表数据。
2. **异步同步**：将 OSS 同步逻辑移除出主请求链路，改为定时任务或独立触发。
3. **缓存策略**：
   - Key: `songs:folder:{folderName}`
   - TTL: 30分钟
