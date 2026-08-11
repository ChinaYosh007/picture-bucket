# Picture Bucket 前端项目长期记忆

## 项目概况
- Vue 3 + TypeScript + Vite + Ant Design Vue 4 + Pinia + vue-router 的图片云图库前端。
- 后端 picture-service：`http://localhost:8080`，统一前缀 `/api`，Session Cookie 认证（withCredentials）。
- 接口层由 `openapi.config.js` + `@umijs/openapi` 生成，位于 `src/api/`；封装在 `src/request.ts`。
- 完整接口清单见根目录 `API接口文档.md`（2026-08-07 整理）。

## 设计规范（2026-08-07 二版）
- 用户偏好：不要暗黑风，要白色系 + 花哨流式感（流光渐变、玻璃拟态、丰富动效）。
- 主题：白底 `#f6f7fb`，主色紫 `#6c5ce7`，辅助青 `#00c9a7` / 天蓝 `#38bdf8` / 粉 `#f572b6`，全局渐变 `--pb-grad: linear-gradient(120deg, #6c5ce7, #38bdf8 55%, #00c9a7)`。
- CSS 变量定义在 `App.vue` 的 `:root`（--pb-ink / --pb-muted / --pb-grad / --pb-shadow 等）；body 有两层固定 aurora 光斑动画。
- 字体：Plus Jakarta Sans + PingFang SC / 微软雅黑。
- antd 主题走 `App.vue` ConfigProvider token 定制（浅色算法）；ECharts 用默认浅色 + transparent 背景。
- 登录/注册页有互动小人组件 `src/components/LoginBuddy.vue`（peek/cover/idle 三态），改动表单时注意保留 focus/blur 状态联动。

## 注意事项
- 修改 UI 时不得改变 `src/api/*` 的接口契约（路径/方法/参数/响应结构）。
- 构建命令 `npm run build` 含 vue-tsc 类型检查，必须保持 0 错误。
- 沙箱内构建前需先挪开旧 `dist` 目录（safe-delete 清空 dist 会失败）。
