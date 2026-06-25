# Token Refresh API Spec (Frontend)

## 1. Endpoint

- Method: `POST`
- URL: `/v1/auth/reissue`
- Request Body: none

## 2. Request Rules

### 2.1 Common headers

- `Authorization: Bearer <accessToken>` (required)
  - Expired access token is allowed for claim extraction.
  - Missing header returns `401 Unauthorized`.
- `X-Client-Type` (optional, default `web`)
  - Allowed: `web`, `ios`, `android`

### 2.2 Refresh token delivery

- `web`
  - Refresh token must come from cookie `refresh_token`.
  - Request must include `withCredentials: true`.
- `ios` / `android`
  - Refresh token must be sent in header `X-Refresh-Token: <refreshToken>`.

## 3. Cookie Behavior (web)

On successful reissue, server rotates refresh token and sends a new cookie.

- Cookie name: `refresh_token`
- `HttpOnly: true`
- `Secure: true`
- `Path: /v1/auth`
- `Max-Age: <jwt.refresh-token-expiration>`
  - Current main config: `604800` seconds (7 days)
- `SameSite`: not explicitly set in code (container/server default applies)

## 4. Success Response

Status: `200 OK`

### 4.1 web

```json
{
  "accessToken": "<newAccessToken>",
  "expiresIn": 3600
}
```

- `refreshToken` is not included in body for web clients.

### 4.2 ios/android

```json
{
  "accessToken": "<newAccessToken>",
  "expiresIn": 3600,
  "refreshToken": "<newRefreshToken>"
}
```

## 5. Error Response

### 5.1 Error body format (when handled by GlobalExceptionHandler)

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "<server message>",
  "timestamp": "2026-05-26T10:30:00"
}
```

### 5.2 Case-by-case status

- `401` + empty body
  - web: missing `refresh_token` cookie
  - ios/android: missing `X-Refresh-Token` header
- `401` + `ErrorResponse`
  - Missing `Authorization`: access token required
  - Invalid/malformed access token
  - Expired login session in cache (no stored refresh token)
  - Refresh token mismatch
  - Note: server error messages are currently returned in Korean.

## 6. Frontend Implementation Guide

1. If a protected API returns `401`, call `POST /v1/auth/reissue` once.
2. If reissue succeeds:
   - web: update only `accessToken` from response body (cookie is updated by browser).
   - ios/android: update both `accessToken` and `refreshToken`.
3. If reissue fails with `401`, clear auth state and redirect to login.
4. Prevent infinite loops: allow only one reissue retry per failed request.

## 7. Axios Examples

### 7.1 web

```ts
await axios.post(
  "/v1/auth/reissue",
  null,
  {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "X-Client-Type": "web",
    },
    withCredentials: true,
  }
);
```

### 7.2 ios/android

```ts
await axios.post(
  "/v1/auth/reissue",
  null,
  {
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "X-Client-Type": "ios", // or "android"
      "X-Refresh-Token": refreshToken,
    },
  }
);
```

## 8. Note on `expiresIn` unit

- DTO comment says `ms`, but actual response value currently comes directly from config and behaves as seconds.
- Current main config values:
  - access token: `3600` (1 hour)
  - refresh token: `604800` (7 days)
