# PVMS 系统接口文档 (v1.2)

## 1. 认证模块 (pvms-auth)
| 方法 | 路径 | 说明 | 权限 |
| :--- | :--- | :--- | :--- |
| POST | `/api/auth/login` | 用户登录 | 公开 |
| POST | `/api/auth/visitor/apply` | 访客申请访问 | 公开 |
| GET | `/api/auth/me` | 获取当前用户信息 | 已登录 |
| PUT | `/api/auth/admin/audit` | 审批访客申请 | 管理员 |

## 2. 媒体库模块 (pvms-media)
| 方法 | 路径 | 说明 | 权限 |
| :--- | :--- | :--- | :--- |
| GET | `/api/media/directories` | 获取监控目录列表 | 管理员/用户 |
| POST | `/api/media/directories` | 添加监控目录 | 管理员 |
| DELETE | `/api/media/directories/{id}` | 移除目录 (逻辑删除) | 管理员 |
| POST | `/api/media/scan` | 触发目录扫描任务 | 管理员 |
| GET | `/api/media/files` | 分页获取媒体列表 | 已登录 |
| GET | `/api/media/files/{id}` | 获取媒体详细元数据 | 已登录 |
| DELETE | `/api/media/files/{id}` | 移除媒体 (逻辑删除) | 管理员/用户 |
| GET | `/api/media/timeline` | 获取时间线聚合统计 | 已登录 |
| GET | `/api/media/search` | 全文检索媒体 | 已登录 |
| POST | `/api/media/albums` | 创建相册 (普通/智能) | 管理员/用户 |

## 3. 加工模块 (pvms-processor)
| 方法 | 路径 | 说明 | 权限 |
| :--- | :--- | :--- | :--- |
| POST | `/api/processor/transcode` | 视频转码任务 | 管理员/用户 |
| POST | `/api/processor/gif` | 视频转GIF任务 | 管理员/用户 |
| POST | `/api/processor/slideshow` | 照片合成视频任务 | 管理员/用户 |
| GET | `/api/processor/tasks/{id}` | 查询异步任务进度 | 已登录 |

## 4. 统一响应规范
状态码定义：
- `200`: 成功
- `401`: 未登录或Token失效
- `403`: 角色权限不足
- `500`: 系统执行异常

响应示例：
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": { ... }
}
```
