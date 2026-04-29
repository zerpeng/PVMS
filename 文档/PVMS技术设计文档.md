# PVMS：本地媒体资产管理系统 - 技术设计文档

## 1. 系统架构图 (概要)
用户界面 (Vue/React) -> API 网关 (Gateway/Sentinel) -> [认证中心 | 媒体库中心 | 媒体加工中心] -> 存储 (MySQL/Redis/本地硬盘)

## 2. 微服务模块划分

| 模块名称 | 端口 | 技术关键点 | 职责描述 |
| :--- | :--- | :--- | :--- |
| `pvms-gateway` | 8080 | Gateway, Sentinel | 统一入口、鉴权、防刷限流 |
| `pvms-auth` | 8081 | Spring Security, JWT | RBAC权限管理、访客申请流、登录 |
| `pvms-media` | 8082 | Virtual Threads, MyBatis Plus | 硬盘扫描、元数据解析、标签整理 |
| `pvms-processor` | 8083 | FFmpeg, Jave2 | 缩略图生成、视频转码、GIF导出 |
| `pvms-common` | - | Lombok, Hutool | 公用DTO、Result封装、工具类 |

## 3. 关键技术方案

### 3.1 JDK 21 特性应用
- **虚拟线程**：在 `pvms-media` 的目录递归扫描和 `pvms-processor` 的多文件处理中使用，解决 IO 密集型操作的线程阻塞问题。
- **模式匹配**：在解析不同格式（Image/Video）的元数据时使用，提高代码可读性。

### 3.2 分布式处理
- **服务治理**：通过 Nacos 实现所有服务的自动注册与发现。
- **配置中心**：利用 Nacos 动态管理扫描过滤规则、FFmpeg 路径等配置。
- **分布式事务**：使用 Seata 处理“添加目录”与“初始化任务记录”的跨服务事务。

### 3.3 媒体处理流程
1. `pvms-media` 识别到新文件。
2. 发送异步请求（或通过消息队列）至 `pvms-processor`。
3. `pvms-processor` 调用本地 FFmpeg 生成 320px 宽度的 WebP 缩略图。
4. 缩略图路径回填至数据库，前端通过网关访问静态资源映射。

### 3.4 逻辑删除策略
- **实现方式**：采用 MyBatis Plus 自带的逻辑删除插件。
- **字段定义**：在核心业务表（`media_file`, `monitor_directory`, `album`）中增加 `del_flag` 字段（0-正常，1-删除）。
- **安全性**：系统执行“移除”操作时，仅更新 `del_flag` 状态，绝对禁止调用物理文件删除指令，避免误删摄影师原始素材。

## 4. 数据库设计要点
- **分库设计**：`pvms_auth` 与 `pvms_media` 物理隔离。
- **索引优化**：对 `file_path` 建立唯一索引，对 `shot_time` 建立排序索引。

## 5. 开发建议
1. 优先搭建 `pvms-common` 和 `pvms-gateway`。
2. 使用 Nacos 作为所有服务的配置源。
3. 在处理超大规模目录时，建议引入消息队列（如 RabbitMQ）平滑处理高峰压力。
