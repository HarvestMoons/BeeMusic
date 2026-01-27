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

### 测试结果 (中间优化 - 仅缓存元数据)
*(引入 Redis 缓存元数据，但仍实时生成签名 URL)*
| Metric | Value |
| :--- | :--- |
| RPS | ~3.46 (Peak 4.4) |
| Min Response Time | 1318 ms |
| Average Response Time | 6691 ms |
| Max Response Time | 28295 ms |
| Median Response Time | 5100 ms |

### 现象分析 (中间阶段)
虽然引入了 Redis，响应时间从 17s 降到了 6s 左右，这节省了 `ossClient.listObjects` 的网络开销。
**但是 6s 依然太慢！**
原因分析：代码中每次缓存命中后，**依然遍历整个歌曲列表** 并逐个调用 `ossClient.generatePresignedUrl` 生成签名链接。
如果歌单中有数百首歌曲，这个 CPU 密集型的签名操作（或者潜在的 SDK 内部处理）依然耗时巨大（推测每首签名耗时约 5-10ms，几百首累积达到数秒）。

### 进一步优化方案
**策略**：将“已生成的签名 URL”也一并缓存。
- OSS 签名链接有效期默认为 24 小时。
- 我们配置 Redis 缓存有效期为 6 小时（远小于签名有效期）。
- 这样，缓存命中时，**直接返回缓存的 JSON，无需任何计算**。
- 预计响应时间将降至 < 50ms。

### 测试结果 (最终优化 - 缓存全量数据)
*(待填入)*
| Metric | Value |
| :--- | :--- |
| RPS | - |
| Min Response Time | - |
| Average Response Time | - |
| Max Response Time | - |
| Median Response Time | - |

