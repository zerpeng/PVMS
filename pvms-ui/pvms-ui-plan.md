# PVMS 前端项目开发计划 (pvms-ui)

## 1. 目标与范围 (Objective)
基于确认的高端 UI 原型 (`02_professional_studio.html`)，搭建一个现代化、组件化、响应式的单页应用 (SPA)。该应用将对接 PVMS 后端网关 (`http://localhost:8080`)，实现媒体库的真实数据展示和交互。

## 2. 技术栈 (Tech Stack)
*   **核心框架**: Vue 3 (Composition API)
*   **构建工具**: Vite (极速冷启动和热更新)
*   **状态管理**: Pinia (管理暗黑模式状态、当前选中的媒体文件)
*   **网络请求**: Axios (封装 API 请求)
*   **图标库**: `lucide-vue-next` (与原型保持一致的专业矢量图标)
*   **样式方案**: 原生 CSS Variables (复用原型中的高定色彩系统和动画过渡)

## 3. 架构设计与组件拆分 (Architecture)

### 3.1 目录结构
```text
pvms-ui/
├── index.html
├── package.json
├── vite.config.js
└── src/
    ├── main.js
    ├── App.vue                 # 根组件，负责整体左右布局
    ├── api/
    │   ├── request.js          # Axios 实例封装 (统一拦截器)
    │   └── media.js            # 媒体相关 API 定义
    ├── assets/
    │   └── styles/
    │       └── theme.css       # 全局亮/暗色变量及通用样式
    ├── components/
    │   ├── layout/
    │   │   ├── Sidebar.vue     # 左侧导航栏
    │   │   └── Header.vue      # 顶部搜索与主题切换
    │   └── gallery/
    │       ├── Gallery.vue     # 瀑布流容器
    │       ├── AssetCard.vue   # 单个媒体卡片组件
    │       └── DetailPanel.vue # 右侧滑出的 EXIF 详情面板
    └── stores/
        └── app.js              # Pinia Store: 管理主题(theme)和选中项(selectedAsset)
```

### 3.2 核心业务逻辑
1.  **主题切换 (Theme Toggle)**：在 `app.js` Store 中维护 `theme` 状态，并持久化到 `localStorage`。`App.vue` 根据状态动态切换 `<html>` 标签的 `data-theme` 属性。
2.  **数据流转 (Data Flow)**：`Gallery.vue` 在挂载时通过 Axios 调用后端的 `/api/media/files` 接口获取分页数据。
3.  **详情联动 (Detail Interaction)**：点击 `AssetCard` 时，更新 Pinia Store 中的 `selectedAsset` 状态。`DetailPanel` 监听到状态变化后，自动滑出并展示对应的 EXIF 信息，同时按需调用后端的详情接口。

## 4. 实施步骤 (Implementation Steps)

### 阶段一：项目初始化
1.  在根目录下使用 `npm create vite@latest pvms-ui -- --template vue` 初始化项目。
2.  安装核心依赖：`npm install axios pinia lucide-vue-next`。
3.  配置 `vite.config.js`，设置开发服务器代理 (Proxy)，解决本地开发时的跨域问题（将 `/api` 转发至 `http://localhost:8080/api`）。

### 阶段二：样式迁移与基础布局
1.  提取原型中的 `:root` 和 `[data-theme="dark"]` 变量，建立 `src/assets/styles/theme.css`。
2.  实现 `App.vue`，构建 `Sidebar` + `Main (Header + Gallery)` 的 flex 响应式布局。
3.  集成 `lucide-vue-next`，替换原型中的 `<i data-lucide="..."></i>` 标签。

### 阶段三：组件开发与状态管理
1.  配置 Pinia Store (`stores/app.js`)，实现亮暗模式的全局切换。
2.  拆分并实现 `Sidebar.vue`、`Header.vue`。
3.  拆分并实现 `Gallery.vue` 和 `AssetCard.vue`，先使用静态 Mock 数据验证瀑布流布局和悬浮动画。
4.  实现 `DetailPanel.vue`，处理侧边栏的滑入/滑出交互。

### 阶段四：API 联调与动态渲染
1.  封装 Axios (`api/request.js`)，处理基础的错误响应。
2.  在 `api/media.js` 中定义后端的 `list()` 和 `detail()` 接口调用。
3.  替换 `Gallery.vue` 中的 Mock 数据，实现真实的媒体列表渲染。
4.  处理图片防盗链或资源路径映射（如将相对路径拼接为网关的静态资源地址）。

## 5. 验证与验收 (Verification)
*   **UI 还原度**: 确保组件化后的界面与 `02_professional_studio.html` 在亮/暗模式下的视觉效果完全一致。
*   **交互流畅度**: 确保卡片悬浮、详情面板滑出、主题切换等动画无卡顿。
*   **数据连通性**: 确保前端能正确显示后端 `pvms-media` 模块扫描解析出的真实数据。

---
*Drafted during Plan Mode. Awaiting user approval to transition to Execution Mode.*
