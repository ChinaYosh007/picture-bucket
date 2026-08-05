# Picture Bucket Frontend

Picture Bucket 前端（Vue 3 + Vite + Ant Design Vue + Pinia）。

## 联调信息

| 项 | 值 |
| --- | --- |
| 后端端口 | `8080` |
| 后端 context-path | `/api` |
| 前端开发端口 | `5173` |
| Swagger | http://localhost:8080/api/swagger-ui.html |

## 已对接用户接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/user/register` | 邮箱 + 密码 + 验证码（可选邀请码） |
| POST | `/api/user/login` | 邮箱或账号 + 密码 + 验证码 |
| POST | `/api/user/email-code` | 发送验证码（`account` 可为邮箱或账号） |
| GET | `/api/user/get/login` | 当前登录用户 |
| POST | `/api/user/logout` | 退出登录 |

## 开发

```sh
npm install
npm run dev
```

## 从 OpenAPI 重新生成客户端

```sh
npm run openapi
```

## 构建

```sh
npm run build
```
