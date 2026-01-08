# 迷因漂流瓶 (Meme Bottle) 功能实现与优化 - 面试备忘

## 1. 功能概述
在音乐播放器中加入的彩蛋功能，用户可以随机打捞（获取）一张梗图，支持下载、管理（同步/删除）。底层基于 **阿里云 OSS** 存储海量图片，**Spring Boot + Vue 3** 实现业务逻辑。

## 2. 核心难点与优化方案 (面试重点)

### A. 随机获取性能优化 (Database)
*   **问题**：最初考虑使用 `ORDER BY RAND()` 随机排序。
    *   *缺陷*：在数据量大（如 10,000+ 条）时，MySQL 需要全表扫描生成临时表进行排序，性能极差 (O(NlogN))。
*   **优化**：采用 **Count + Offset** 算法。
    *   先 `SELECT COUNT(*)` 获取总数。
    *   生成一个 `0` 到 `count-1` 的随机数 `index`。
    *   使用 `PageRequest.of(index, 1)` 直接定位到该行 (Limit 1 Offset N)。
    *   *成效*：查询从全表扫描变为索引定位，查询复杂度接近 O(1)。

### B. 海量图片同步优化 (Sync Logic)
*   **问题**：OSS 上有数万张图片，同步元数据到数据库时，若每张图片都查询一次 "是否存在" (`SELECT count` 或 `exists`)，将产生 **N+1 次数据库交互**。
    *   *表现*：同步 1 万张图片需要几分钟，频繁 IO。
*   **优化**：**内存比对 + 批量写入**。
    *   **Step 1**: 一次性查出数据库所有已存在的 Key (`SELECT key FROM meme`)，存入内存 `HashSet`。
    *   **Step 2**: 遍历 OSS 列表，在内存中比对 `Set.contains()` (O(1))。
    *   **Step 3**: 将新图片收集到 List，使用 JPA `saveAll()` 批量插入。
    *   *成效*：数据库交互次数从 **N 次** 降为 **2 次**，耗时从分钟级降至秒级。

### C. 资源安全与访问控制 (OSS Security)
*   **问题**：
    *   直接存 Public URL 不安全，且 OSS Bucket 设为私有时会导致 403 Forbidden。
    *   硬编码 URL 过期时间导致维护困难。
*   **优化**：
    *   **动态签名**：数据库只存对象的 `Key` (文件名)，不存完整 URL。每次请求时，后端使用 `OSSClient` 动态生成 **预签名 URL (Presigned URL)**。
    *   **统一配置**：将过期时间（如 24h）提取到 `application.properties`，由工具类 `OssUtil` 统一管理，方便运维调整。

### D. 前端体验优化 (User Experience)
*   **预加载 (Preload)**：
    *   *机制*：在用户查看当前图片时，利用 `Promise` 和隐形 `Image` 对象在后台静默加载下一张图片。
    *   *效果*：点击 "下一张" 时实现**零延迟秒开**。
*   **布局抖动解决**：
    *   使用固定高度容器 (`height: 65vh`) + `object-fit: contain`，防止不同尺寸图片导致下方的按钮位置上下跳动。
*   **优雅降级**：
    *   图片加载失败（404/403）时，自动显示占位状态 ("这里空空如也")，防止显示破碎图标。

## 3. 技术栈亮点
*   **Backend**: Spring Boot, Spring Data JPA, Aliyun OSS SDK, Java Concurrency (`synchronized` 防止并发同步).
*   **Frontend**: Vue 3 (Composition API), Pinia (状态管理), CSS Variables (实现无缝深色模式适配).
