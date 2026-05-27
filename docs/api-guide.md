# API Guide

프론트엔드 연동용 API 엔드포인트 명세입니다. 인증이 필요한 모든 요청에는 `Authorization: Bearer <token>` 헤더를 포함해야 합니다.

---

## 커뮤니티 게시판 (`/v1/community`)

### 게시글 작성

```
POST /v1/community/posts
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | string | ✅ | 제목 |
| content | string | ✅ | 본문 |
| postCategory | PostCategory | ✅ | 카테고리 |
| isAnonymous | boolean | ✅ | 익명 여부 |

**PostCategory 값**: `DEFAULT` / `DISORDER_INFO` / `MEDICATION` / `DAILY_LIFE`

**Response** `201 Created`

---

### 게시글 목록 조회 / 검색

```
GET /v1/community/posts
GET /v1/community/posts?q=콘서타
GET /v1/community/posts?category=MEDICATION
GET /v1/community/posts?q=콘서타&category=MEDICATION
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| q | string | ❌ | 제목·본문 키워드 검색 (대소문자 무시). 생략 시 전체 조회 |
| category | PostCategory | ❌ | 카테고리 필터. 생략 시 전체 카테고리 |

> `q`와 `category`는 AND 조건으로 결합됩니다.

**Response** `200 OK` — `PostResponse[]`

| 필드 | 타입 | 설명 |
|------|------|------|
| id | number | 게시글 ID |
| title | string | 제목 |
| content | string | 본문 |
| postCategory | PostCategory | 카테고리 |
| isAnonymous | boolean | 익명 여부 |
| isOwner | boolean | 현재 로그인 사용자의 게시글 여부 |
| createdAt | string (ISO 8601) | 생성일시 |
| updatedAt | string (ISO 8601) | 수정일시 |

---

### 게시글 상세 조회

```
GET /v1/community/posts/{postId}
```

**Response** `200 OK` — `PostResponse` (위 필드 동일)  
**404** 게시글 없음

---

### 게시글 수정

```
PUT /v1/community/posts/{postId}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| title | string | ✅ | 수정할 제목 |
| content | string | ✅ | 수정할 본문 |
| postCategory | PostCategory | ✅ | 수정할 카테고리 |

**Response** `200 OK` — `PostResponse`  
**403** 작성자 본인이 아닌 경우  
**404** 게시글 없음

---

### 게시글 삭제

```
DELETE /v1/community/posts/{postId}
```

**Response** `204 No Content`  
**403** 작성자 본인이 아닌 경우  
**404** 게시글 없음

---

## 커뮤니티 댓글 (`/v1/community`)

### 댓글 작성

```
POST /v1/community/posts/{postId}/comments
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| content | string | ✅ | 댓글 내용 |
| isAnonymous | boolean | ✅ | 익명 여부 |

**Response** `201 Created` — `CreateCommentResponse`

---

### 댓글 목록 조회

```
GET /v1/community/posts/{postId}/comments
```

**Response** `200 OK` — `CommentResponse[]`

| 필드 | 타입 | 설명 |
|------|------|------|
| id | number | 댓글 ID |
| content | string | 댓글 내용 |
| isAnonymous | boolean | 익명 여부 |
| isOwner | boolean | 현재 로그인 사용자의 댓글 여부 |
| createdAt | string (ISO 8601) | 생성일시 |

---

### 댓글 수정

```
PUT /v1/community/posts/{postId}/comments/{commentId}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| content | string | ✅ | 수정할 댓글 내용 |

**Response** `200 OK` — `UpdateCommentResponse`  
**403** 작성자 본인이 아닌 경우

---

### 댓글 삭제

```
DELETE /v1/community/posts/{postId}/comments/{commentId}
```

**Response** `204 No Content`  
**403** 작성자 본인이 아닌 경우
