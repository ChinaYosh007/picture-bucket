# Picture Bucket 前端接口文档

> 本文档记录前端项目当前使用的全部后端接口，重构前端界面时 **必须保持这些接口调用契约不变**。
>
> - 后端服务：`picture-service`
> - 开发环境 Base URL：`http://localhost:8080`
> - 统一前缀：`/api`
> - 认证方式：Session Cookie（`withCredentials: true`，浏览器自动携带）
> - 请求封装：`src/request.ts`（axios 实例，超时 10s；邮箱验证码接口单独 60s）
> - 接口定义目录：`src/api/`（由 `openapi.config.js` + `@umijs/openapi` 生成）

---

## 0. 通用约定

### 统一响应结构

所有 HTTP 接口返回统一包装结构（`BaseResponse<T>`）：

```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| code | number | 业务状态码，`0` 表示成功；`40100` 表示未登录（前端拦截器会自动跳转登录页） |
| data | T | 业务数据 |
| message | string | 提示信息 |

### 分页结构

```json
{
  "current": 1,
  "pages": 10,
  "records": [],
  "size": 10,
  "total": 100
}
```

### 通用请求体

`DeleteRequest`：

```json
{ "id": 1 }
```

---

## 1. 用户模块 userController

文件：`src/api/userController.ts`

### 1.1 用户注册

- **POST** `/api/user/register`
- Body：`UserRegisterRequest`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| email | string | 邮箱 |
| userPassword | string | 密码 |
| checkPassword | string | 确认密码 |
| emailCode | string | 邮箱验证码 |
| inviteCode | string? | 邀请码（可选） |

- 返回：`BaseResponse<number>`（新用户 id）

### 1.2 用户登录

- **POST** `/api/user/login`
- Body：`UserLoginRequest`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| account | string | 邮箱或用户账号 |
| userPassword | string | 密码 |
| emailCode | string | 邮箱验证码 |

- 返回：`BaseResponse<LoginUserVO>`

`LoginUserVO`：`{ id, userAccount, email, userName, userAvatar, userProfile, userRole, vipExpireTime, vipCode, vipNumber, shareCode, createTime }`

### 1.3 用户退出登录

- **POST** `/api/user/logout`
- 无参数
- 返回：`BaseResponse<boolean>`

### 1.4 获取当前登录用户

- **GET** `/api/user/get/login`
- 无参数
- 返回：`BaseResponse<LoginUserVO>`

### 1.5 发送邮箱验证码

- **POST** `/api/user/email-code`
- Query：`account`（邮箱或已注册用户的账号）
- 返回：`BaseResponse<boolean>`
- 备注：SMTP 发送较慢，前端单独设置 60s 超时

### 1.6 兑换会员

- **POST** `/api/user/exchange/vip`
- Body：`VipExchangeRequest` → `{ vipCode }`
- 返回：`BaseResponse<boolean>`

### 1.7（登录用户）编辑个人资料

- **POST** `/api/user/edit`
- Body：`UserEditRequest` → `{ userName, userAvatar, userProfile }`
- 仅允许修改当前登录用户的昵称、头像和简介；不能修改账号、邮箱、密码或角色
- 返回：`BaseResponse<boolean>`

### 1.8（管理员）添加用户

- **POST** `/api/user/add`
- Body：`UserAddRequest` → `{ userAccount, userPassword, email, userName, userProfile, userAvatar, userRole }`
- `userAccount`、`userPassword` 和 `email` 为必填项；密码长度为 8–64 位
- 返回：`BaseResponse<number>`

### 1.9（管理员）删除用户

- **POST** `/api/user/delete`
- Body：`DeleteRequest`
- 返回：`BaseResponse<boolean>`

### 1.10（管理员）更新用户

- **POST** `/api/user/update`
- Body：`UserUpdateRequest` → `{ id, userAccount, email, userName, userProfile, userAvatar, userRole }`
- 不接受密码字段；密码重置应使用独立流程
- 返回：`BaseResponse<boolean>`

### 1.11（管理员）根据 id 获取用户

- **GET** `/api/user/get`
- Query：`id`
- 返回：`BaseResponse<User>`（密码字段始终为空，仅管理员）

### 1.12 根据 id 获取用户（脱敏）

- **GET** `/api/user/get/vo`
- Query：`id`
- 返回：`BaseResponse<UserVO>`

`UserVO`：`{ id, userAccount, userName, userProfile, userAvatar, userRole, createTime }`

### 1.13（管理员）分页获取用户列表

- **POST** `/api/user/list/page/vo`
- Body：`UserQueryRequest` → `{ current, pageSize, sortField, sortOrder, id, userAccount, userName, userProfile, userRole }`
- 返回：`BaseResponse<Page<UserVO>>`

---

## 2. 图片模块 pictureController

文件：`src/api/pictureController.ts`

### 2.1 上传图片（文件）

- **POST** `/api/picture/upload`
- Content-Type：`multipart/form-data`
- Query：`{ id?, picName?, spaceId?, fileUrl? }`
- FormData：`file`（图片文件）
- 返回：`BaseResponse<PictureVO>`

### 2.2 通过 URL 上传图片

- **POST** `/api/picture/upload/url`
- Body：`PictureUploadRequest` → `{ fileUrl, id?, picName?, spaceId? }`
- 返回：`BaseResponse<PictureVO>`

### 2.3 批量抓取上传图片

- **POST** `/api/picture/upload/batch`
- Body：`PictureUploadByBatchRequest` → `{ searchText, count, namePrefix }`
- 返回：`BaseResponse<number>`（成功数量）

### 2.4 删除图片

- **POST** `/api/picture/delete`
- Body：`DeleteRequest`
- 返回：`BaseResponse<boolean>`

### 2.5 更新图片（管理员）

- **POST** `/api/picture/update`
- Body：`PictureUpdateRequest` → `{ id, name, introduction, category, tags[] }`
- 返回：`BaseResponse<boolean>`

### 2.6 编辑图片（用户）

- **POST** `/api/picture/edit`
- Body：`PictureEditRequest` → `{ id, name, introduction, category, tags[] }`
- 返回：`BaseResponse<boolean>`

### 2.7 批量编辑图片

- **POST** `/api/picture/edit/batch`
- Body：`PictureEditByBatchRequest` → `{ pictureIdList[], spaceId, category?, tags[]?, nameRule? }`
- 返回：`BaseResponse<boolean>`

### 2.8 根据 id 获取图片（管理员）

- **GET** `/api/picture/get`
- Query：`id`
- 返回：`BaseResponse<Picture>`

### 2.9 根据 id 获取图片（脱敏）

- **GET** `/api/picture/get/vo`
- Query：`id`
- 返回：`BaseResponse<PictureVO>`

`PictureVO`：`{ id, url, thumbnailUrl, name, introduction, category, tags[], picSize, picWidth, picHeight, picScale, picFormat, picColor, userId, spaceId, createTime, editTime, updateTime, user, permissionList[] }`

### 2.10 分页获取图片列表（管理员）

- **POST** `/api/picture/list/page`
- Body：`PictureQueryRequest`
- 返回：`BaseResponse<Page<Picture>>`

### 2.11 分页获取图片列表（脱敏）

- **POST** `/api/picture/list/page/vo`
- Body：`PictureQueryRequest`
- 返回：`BaseResponse<Page<PictureVO>>`

`PictureQueryRequest` 字段：`{ current, pageSize, sortField, sortOrder, id, name, introduction, category, tags[], picSize, picWidth, picHeight, picScale, picFormat, searchText, userId, spaceId, nullSpaceId, reviewStatus, reviewMessage, reviewerId, reviewTime, startEditTime, endEditTime }`

### 2.12 分页获取图片列表（带缓存）

- **POST** `/api/picture/list/page/vo/cache`
- Body：`PictureQueryRequest`
- 返回：`BaseResponse<Page<PictureVO>>`

### 2.13 获取图片标签/分类列表

- **GET** `/api/picture/tag_category`
- 无参数
- 返回：`BaseResponse<PictureTagCategory>` → `{ tagList[], categoryList[] }`

### 2.14 图片审核（管理员）

- **POST** `/api/picture/review`
- Body：`PictureReviewRequest` → `{ id, reviewStatus, reviewMessage }`
- 返回：`BaseResponse<boolean>`

### 2.15 以图搜图

- **POST** `/api/picture/search/picture`
- Body：`SearchPictureByPictureRequest` → `{ pictureId }`
- 返回：`BaseResponse<ImageSearchResult[]>` → `[{ thumbUrl, fromUrl }]`

### 2.16 按颜色搜索图片

- **POST** `/api/picture/search/color`
- Body：`SearchPictureByColorRequest` → `{ picColor, spaceId? }`
- 返回：`BaseResponse<PictureVO[]>`

### 2.17 创建 AI 扩图任务

- **POST** `/api/picture/out_painting/create_task`
- Body：`CreatePictureOutPaintingTaskRequest` → `{ pictureId, parameters }`
- `parameters`：`{ xScale?, yScale?, topOffset?, bottomOffset?, leftOffset?, rightOffset?, outputRatio?, angle?, bestQuality?, limitImageSize?, addWatermark? }`
- 返回：`BaseResponse<CreateOutPaintingTaskResponse>` → `{ code, message, requestId, output: { taskId, taskStatus } }`

### 2.18 查询 AI 扩图任务

- **GET** `/api/picture/out_painting/get_task`
- Query：`taskId`
- 返回：`BaseResponse<GetOutPaintingTaskResponse>` → `{ requestId, output: { taskId, taskStatus, code, message, outputImageUrl, submitTime, scheduledTime, endTime, taskMetrics: { total, succeeded, failed } } }`

---

## 3. 空间模块 spaceController

文件：`src/api/spaceController.ts`

### 3.1 创建空间

- **POST** `/api/space/add`
- Body：`SpaceAddRequest` → `{ spaceName, spaceLevel?, spaceType }`（spaceType：0 私有 / 1 团队）
- 返回：`BaseResponse<number>`

### 3.2 删除空间

- **POST** `/api/space/delete`
- Body：`DeleteRequest`
- 返回：`BaseResponse<boolean>`

### 3.3 编辑空间（用户）

- **POST** `/api/space/edit`
- Body：`SpaceEditRequest` → `{ id, spaceName }`
- 返回：`BaseResponse<boolean>`

### 3.4 更新空间（管理员）

- **POST** `/api/space/update`
- Body：`SpaceUpdateRequest` → `{ id, spaceName, spaceLevel, maxSize, maxCount }`
- 返回：`BaseResponse<boolean>`

### 3.5 根据 id 获取空间（管理员）

- **GET** `/api/space/get`
- Query：`id`
- 返回：`BaseResponse<Space>`

`Space`：`{ id, spaceName, spaceLevel, spaceType, maxSize, maxCount, totalSize, totalCount, userId, createTime, editTime, updateTime, isDelete }`

### 3.6 根据 id 获取空间（脱敏）

- **GET** `/api/space/get/vo`
- Query：`id`
- 返回：`BaseResponse<SpaceVO>`（额外含 `user`、`permissionList[]`）

### 3.7 分页获取空间列表（管理员）

- **POST** `/api/space/list/page`
- Body：`SpaceQueryRequest` → `{ current, pageSize, sortField, sortOrder, id, spaceName, spaceLevel, spaceType, userId }`
- 返回：`BaseResponse<Page<Space>>`

### 3.8 分页获取空间列表（脱敏）

- **POST** `/api/space/list/page/vo`
- Body：`SpaceQueryRequest`
- 返回：`BaseResponse<Page<SpaceVO>>`

### 3.9 获取空间级别列表

- **GET** `/api/space/list/level`
- 无参数
- 返回：`BaseResponse<SpaceLevel[]>` → `[{ value, text, maxCount, maxSize }]`

---

## 4. 空间分析模块 spaceAnalyzeController

文件：`src/api/spaceAnalyzeController.ts`

所有分析接口请求体都支持三种范围（三选一）：
- `spaceId`：分析指定空间
- `queryPublic: true`：分析公共图库
- `queryAll: true`：分析全空间（仅管理员）

### 4.1 空间使用分析

- **POST** `/api/space/analyze/usage`
- Body：`SpaceUsageAnalyzeRequest`
- 返回：`BaseResponse<SpaceUsageAnalyzeResponse>` → `{ usedSize, maxSize, sizeUsageRatio, usedCount, maxCount, countUsageRatio }`

### 4.2 图片分类分析

- **POST** `/api/space/analyze/category`
- Body：`SpaceCategoryAnalyzeRequest`
- 返回：`BaseResponse<SpaceCategoryAnalyzeResponse[]>` → `[{ category, count, totalSize }]`

### 4.3 图片标签分析

- **POST** `/api/space/analyze/tag`
- Body：`SpaceTagAnalyzeRequest`
- 返回：`BaseResponse<SpaceTagAnalyzeResponse[]>` → `[{ tag, count }]`

### 4.4 图片大小分段分析

- **POST** `/api/space/analyze/size`
- Body：`SpaceSizeAnalyzeRequest`
- 返回：`BaseResponse<SpaceSizeAnalyzeResponse[]>` → `[{ sizeRange, count }]`

### 4.5 用户上传行为分析

- **POST** `/api/space/analyze/user`
- Body：`SpaceUserAnalyzeRequest` → `{ spaceId?/queryPublic?/queryAll?, userId, timeDimension: 'day' | 'week' | 'month' }`
- 返回：`BaseResponse<SpaceUserAnalyzeResponse[]>` → `[{ period, count }]`

### 4.6 空间使用排行（管理员）

- **POST** `/api/space/analyze/rank`
- Body：`SpaceRankAnalyzeRequest` → `{ topN }`
- 返回：`BaseResponse<Space[]>`

---

## 5. 空间成员模块 spaceUserController（团队空间）

文件：`src/api/spaceUserController.ts`

### 5.1 添加空间成员

- **POST** `/api/spaceUser/add`
- Body：`SpaceUserAddRequest` → `{ spaceId, userId, spaceRole }`（角色：`viewer` / `editor` / `admin`）
- 返回：`BaseResponse<number>`

### 5.2 删除空间成员

- **POST** `/api/spaceUser/delete`
- Body：`DeleteRequest`
- 返回：`BaseResponse<boolean>`

### 5.3 编辑成员角色

- **POST** `/api/spaceUser/edit`
- Body：`SpaceUserEditRequest` → `{ id, spaceRole }`
- 返回：`BaseResponse<boolean>`

### 5.4 查询单个成员

- **POST** `/api/spaceUser/get`
- Body：`SpaceUserQueryRequest` → `{ id?, spaceId?, userId?, spaceRole? }`
- 返回：`BaseResponse<SpaceUser>` → `{ id, spaceId, userId, spaceRole, createTime, updateTime }`

### 5.5 查询空间成员列表

- **POST** `/api/spaceUser/list`
- Body：`SpaceUserQueryRequest`
- 返回：`BaseResponse<SpaceUserVO[]>`（含关联 `user`、`space` 对象）

### 5.6 查询我加入的团队空间

- **POST** `/api/spaceUser/list/my`
- 无参数
- 返回：`BaseResponse<SpaceUserVO[]>`

---

## 6. 文件测试模块 fileController（测试用）

文件：`src/api/fileController.ts`

### 6.1 测试上传

- **POST** `/api/file/test/upload`
- Content-Type：`multipart/form-data`，字段 `file`
- 返回：`BaseResponse<string>`

### 6.2 测试下载

- **GET** `/api/file/test/download/`
- Query：`filepath`
- 返回：文件流（any）

---

## 7. 健康检查 mainController

文件：`src/api/mainController.ts`

### 7.1 健康检查

- **GET** `/api/health`
- 返回：`BaseResponse<string>`

---

## 8. WebSocket：图片协同编辑

文件：`src/utils/pictureEditWebSocket.ts`（`PictureEditWebSocket` 类）

### 连接地址

```
ws://localhost:8080/api/ws/picture/edit?pictureId={pictureId}
```

- 携带 Cookie 认证（与 HTTP 接口同一 Session）
- `binaryType: 'blob'`，消息体为 JSON 字符串

### 消息模型

```json
{
  "type": "消息类型",
  "...": "按类型扩展的字段"
}
```

### 客户端事件

| 事件 | 触发时机 |
| --- | --- |
| open | 连接建立 |
| close | 连接关闭 |
| error | 连接错误 |
| 其他 `type` | 由服务端消息中的 `type` 字段触发（如编辑动作广播、用户进出等） |

### 客户端发送

通过 `sendMessage(object)` 发送 JSON 消息，例如编辑操作、进入/退出编辑等，具体类型与后端约定一致（图片协同编辑页面使用）。

---

## 9. 前端路由与接口对照

| 路由 | 页面 | 主要接口 |
| --- | --- | --- |
| `/` | 公共图库首页 | `picture/list/page/vo/cache`、`picture/tag_category`、`picture/search/color` |
| `/user/login` | 登录 | `user/login`、`user/email-code` |
| `/user/register` | 注册 | `user/register`、`user/email-code` |
| `/user_exchange_vip` | 兑换会员 | `user/exchange/vip` |
| `/my_space` | 我的空间 | `spaceUser/list/my`、`space/list/page/vo` |
| `/add_space` | 创建空间 | `space/add`、`space/list/level` |
| `/space/:id` | 空间详情 | `space/get/vo`、`picture/list/page/vo`、`space/analyze/usage` |
| `/space_analyze` | 空间分析 | `space/analyze/*` 全部 |
| `/add_picture` | 创建/编辑图片 | `picture/upload`、`picture/upload/url`、`picture/edit`、`picture/tag_category` |
| `/add_picture/batch` | 批量创建 | `picture/upload/batch` |
| `/picture/:id` | 图片详情 | `picture/get/vo`、`picture/delete`、`picture/search/picture`、`picture/out_painting/*`、`/api/ws/picture/edit` |
| `/search_picture` | 以图搜图 | `picture/search/picture` |
| `/admin/userManage` | 用户管理 | `user/list/page/vo`、`user/add`、`user/update`、`user/delete` |
| `/admin/pictureManage` | 图片管理 | `picture/list/page`、`picture/review`、`picture/update`、`picture/delete` |
| `/admin/spaceManage` | 空间管理 | `space/list/page`、`space/update`、`space/delete`、`space/analyze/rank` |
| `/spaceUserManage/:id` | 空间成员管理 | `spaceUser/list`、`spaceUser/add`、`spaceUser/edit`、`spaceUser/delete` |

---

_文档生成时间：2026-08-07 · 依据 `src/api/*.ts` 与 `src/api/typings.d.ts` 整理_
