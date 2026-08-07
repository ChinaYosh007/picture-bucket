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

## Edit own profile

`POST /user/edit`

Requires an authenticated session. `userAccount`, `email`, `userName`, `userAvatar`, and `userProfile` may be edited. When the email changes, `emailCode` must be a valid verification code sent to the new email address. Roles, membership fields, and other system-owned fields cannot be changed through this endpoint.

Request body:

```json
{
  "userAccount": "demo_user",
  "email": "demo@example.com",
  "emailCode": "123456",
  "userName": "Demo",
  "userAvatar": "https://example.com/avatar.png",
  "userProfile": "profile text"
}
```

`emailCode` is required only when changing the email. A changed account can be used for later logins immediately.

Response: `BaseResponse<boolean>`.

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

## Update own password

`POST /user/password/update`

Requires an authenticated session. The current password must be supplied and the new password must be 8 to 64 characters; the new password cannot equal the current password. On success, the current session ID is rotated.

Request body:

```json
{
  "currentPassword": "current-password",
  "newPassword": "new-password-at-least-8-chars",
  "confirmPassword": "new-password-at-least-8-chars"
}
```

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
