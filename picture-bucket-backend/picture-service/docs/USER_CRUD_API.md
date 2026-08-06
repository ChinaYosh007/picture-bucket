# User CRUD API

Base URL: `http://localhost:8080/api`  
Authentication: Session Cookie; requests from the browser must include credentials.

All endpoints return `BaseResponse<T>`:

```json
{
  "code": 0,
  "data": {},
  "message": "ok"
}
```

## Create user (admin)

`POST /user/add`

Request body:

```json
{
  "userAccount": "demo_user",
  "userPassword": "password-at-least-8-chars",
  "email": "demo@example.com",
  "userName": "Demo",
  "userAvatar": "https://example.com/avatar.png",
  "userProfile": "profile text",
  "userRole": "user"
}
```

`userRole` is optional and defaults to `user`. Allowed values: `user`, `admin`.
`userAccount` must contain 3 to 64 letters, numbers, underscores, or hyphens.

Response: `BaseResponse<string>` (Snowflake user ID serialized as a string).

## Read user (admin)

`GET /user/get?id={id}`

Returns `BaseResponse<User>`. The password field is always `null` in the response.

## Read public user profile

`GET /user/get/vo?id={id}`

Returns `BaseResponse<UserVO>`:

```json
{
  "id": "2085034571005493250",
  "userAccount": "demo_user",
  "userName": "Demo",
  "userAvatar": "https://example.com/avatar.png",
  "userProfile": "profile text",
  "userRole": "user",
  "createTime": "2026-08-07T01:00:00"
}
```

Email and password are not returned by this endpoint.

## Update user (admin)

`POST /user/update`

Request body:

```json
{
  "id": "2085034571005493250",
  "userAccount": "demo_user",
  "email": "demo@example.com",
  "userName": "Demo",
  "userAvatar": "https://example.com/avatar.png",
  "userProfile": "profile text",
  "userRole": "user"
}
```

Only supplied fields are updated. `email` must be valid; `userRole`, if supplied, must be `user` or `admin`. A changed `userAccount` takes effect immediately for later logins.

Response: `BaseResponse<boolean>`.

## Delete user (admin)

`POST /user/delete`

Request body:

```json
{
  "id": "2085034571005493250"
}
```

Response: `BaseResponse<boolean>`.

## Page users (admin)

`POST /user/list/page/vo`

Request body:

```json
{
  "current": 1,
  "pageSize": 10,
  "sortField": "createTime",
  "sortOrder": "descend",
  "id": "2085034571005493250",
  "userAccount": "demo",
  "userName": "Demo",
  "userProfile": "profile",
  "userRole": "user"
}
```

`pageSize` must be between 1 and 50. Allowed sort fields: `id`, `userAccount`, `userName`, `userRole`, `createTime`, `updateTime`.

Response: `BaseResponse<Page<UserVO>>`.
