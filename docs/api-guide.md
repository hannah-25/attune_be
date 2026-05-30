# API Guide

프론트엔드 연동용 API 엔드포인트 명세입니다. 인증이 필요한 모든 요청에는 `Authorization: Bearer <token>` 헤더를 포함해야 합니다.

---

## 일지 (`/api/v1/journals`)

> 모든 요청에 `Authorization: Bearer <token>` 헤더 필요.

---

### 일지 상세 조회

```
GET /api/v1/journals/{date}
```

해당 날짜의 활성 태그 전체 + 체크 기록을 한 번에 반환한다.

**Path Parameter**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| date | string (yyyy-MM-dd) | 조회 날짜 |

**Response** `200 OK`

```json
{
  "activeTags": {
    "conditions": [
      { "tagId": 1, "condition": "머리 맑음", "conditionType": "UP", "visible": false }
    ],
    "sideEffects": [
      { "tagId": 2, "sideEffect": "입마름", "visible": false }
    ],
    "troubles": [
      { "tagId": 3, "trouble": "회의 시간 놓침", "type": "TIME_MANAGEMENT", "visible": false }
    ],
    "goals": [
      { "goalId": 1, "content": "할 일 미리 정리하기" }
    ]
  },
  "checked": {
    "conditions": [
      { "tagId": 1, "condition": "머리 맑음", "conditionType": "UP", "checkedAt": "2026-05-09T10:00:00" }
    ],
    "sideEffects": [
      { "tagId": 2, "sideEffect": "입마름", "checkedAt": "2026-05-09T10:00:00" }
    ],
    "troubles": [
      { "tagId": 3, "trouble": "회의 시간 놓침", "type": "TIME_MANAGEMENT", "checkedAt": "2026-05-09T10:00:00" }
    ],
    "sleep": { "sleepHour": 7.5, "sleepQuality": "GOOD" },
    "meal": { "ateBreakfast": true, "ateLunch": true, "ateDinner": false },
    "goals": [
      { "goalId": 1, "content": "할 일 미리 정리하기", "score": 8 }
    ],
    "memo": "오늘은 컨디션이 좋았다."
  }
}
```

> `sleep`, `meal`은 기록이 없으면 `null`.  
> `memo`는 기록이 없으면 `null`.

---

### 기간별 일지 날짜 목록 조회

```
GET /api/v1/journals?startDate={date}&endDate={date}
```

기간 내 일지 데이터가 하나라도 존재하는 날짜 목록을 반환한다.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| startDate | string (yyyy-MM-dd) | ✅ | 시작일 |
| endDate | string (yyyy-MM-dd) | ✅ | 종료일 |

**Response** `200 OK`

```json
{ "dates": ["2026-05-01", "2026-05-03", "2026-05-09"] }
```

---

### 단일 일지 삭제

```
DELETE /api/v1/journals/{date}
```

해당 날짜의 모든 일지 기록(체크 로그, 수면/식사, 목표 점수, 메모)을 삭제한다.

**Response** `200 OK`

```json
{ "deletedDate": "2026-05-09", "success": true }
```

---

### 기간별 일지 삭제

```
DELETE /api/v1/journals?startDate={date}&endDate={date}
```

**Response** `200 OK`

```json
{
  "deletedRange": { "startDate": "2026-05-01", "endDate": "2026-05-09" },
  "count": 42
}
```

> `count`: 삭제된 레코드 총 수

---

## 일지 - 감정/증상 태그 (`/api/v1/journals`)

### 감정/증상 태그 목록 조회

```
GET /api/v1/journals/condition-tags
```

**Response** `200 OK`

```json
[
  { "tagId": 1, "condition": "머리 맑음", "conditionType": "UP", "visible": false }
]
```

**ConditionType 값**: `UP` / `DOWN` / `TIGHT` / `FOGGY` / `CALM` / `USER_INPUT`

---

### 감정/증상 태그 추가

```
POST /api/v1/journals/condition-tags
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| condition | string | ✅ | 태그 이름 |
| conditionType | ConditionType | ✅ | 유형 |
| journalDate | string (yyyy-MM-dd) | ✅ | 일지 날짜 |

**Response** `201 Created` — `ConditionTagResponse`

**409** 동일 이름의 활성 태그 이미 존재

---

### 감정/증상 태그 삭제

```
DELETE /api/v1/journals/condition-tags/{tagId}?journalDate={date}
```

`journalDate` 이후의 체크 로그가 함께 삭제된다.

**Response** `204 No Content`  
**404** 태그 없음

---

### 감정/증상 태그 visible 토글

```
PATCH /api/v1/journals/condition-tags/{tagId}/visible
```

**Response** `200 OK` — `ConditionTagResponse` (토글된 `visible` 값 포함)  
**404** 태그 없음

---

### 감정/증상 체크

```
POST /api/v1/journals/conditions
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| tagId | number | ✅ | 태그 ID |

**Response** `201 Created`

```json
{ "tagId": 1, "condition": "머리 맑음", "conditionType": "UP", "checkedAt": "2026-05-09T10:00:00" }
```

**404** 태그 없음

---

### 감정/증상 체크 취소

```
DELETE /api/v1/journals/conditions?tagId={id}&date={date}
```

해당 날짜의 체크 로그를 전부 삭제한다.

**Response** `204 No Content`  
**404** 태그 없음

---

## 일지 - 부작용 태그 (`/api/v1/journals`)

### 부작용 태그 목록 조회

```
GET /api/v1/journals/side-effect-tags
```

**Response** `200 OK`

```json
[
  { "tagId": 2, "sideEffect": "입마름", "visible": false }
]
```

---

### 부작용 태그 추가

```
POST /api/v1/journals/side-effect-tags
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| sideEffect | string | ✅ | 태그 이름 |
| journalDate | string (yyyy-MM-dd) | ✅ | 일지 날짜 |

**Response** `201 Created` — `SideEffectTagResponse`

**409** 동일 이름의 활성 태그 이미 존재

---

### 부작용 태그 삭제

```
DELETE /api/v1/journals/side-effect-tags/{tagId}?journalDate={date}
```

**Response** `204 No Content`  
**404** 태그 없음

---

### 부작용 태그 visible 토글

```
PATCH /api/v1/journals/side-effect-tags/{tagId}/visible
```

**Response** `200 OK` — `SideEffectTagResponse`  
**404** 태그 없음

---

### 부작용 체크

```
POST /api/v1/journals/side-effects
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| tagId | number | ✅ | 태그 ID |

**Response** `201 Created`

```json
{ "tagId": 2, "sideEffect": "입마름", "checkedAt": "2026-05-09T10:00:00" }
```

---

### 부작용 체크 취소

```
DELETE /api/v1/journals/side-effects?tagId={id}&date={date}
```

**Response** `204 No Content`  
**404** 태그 없음

---

## 일지 - 업무 실수/불편 태그 (`/api/v1/journals`)

### 트러블 태그 목록 조회

```
GET /api/v1/journals/trouble-tags
```

**Response** `200 OK`

```json
[
  { "tagId": 3, "trouble": "회의 시간 놓침", "type": "TIME_MANAGEMENT", "visible": false }
]
```

**TroubleType 값**: `INATTENTION` / `HYPERACTIVITY` / `IMPULSIVITY` / `TIME_MANAGEMENT` / `COGNITIVE_ERROR` / `USER_INPUT`

---

### 트러블 태그 추가

```
POST /api/v1/journals/trouble-tags
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| trouble | string | ✅ | 태그 이름 |
| type | TroubleType | ✅ | 유형 |
| journalDate | string (yyyy-MM-dd) | ✅ | 일지 날짜 |

**Response** `201 Created` — `TroubleTagResponse`

**409** 동일 이름의 활성 태그 이미 존재

---

### 트러블 태그 삭제

```
DELETE /api/v1/journals/trouble-tags/{tagId}?journalDate={date}
```

**Response** `204 No Content`  
**404** 태그 없음

---

### 트러블 태그 visible 토글

```
PATCH /api/v1/journals/trouble-tags/{tagId}/visible
```

**Response** `200 OK` — `TroubleTagResponse`  
**404** 태그 없음

---

### 트러블 체크

```
POST /api/v1/journals/troubles
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| tagId | number | ✅ | 태그 ID |

**Response** `201 Created`

```json
{ "tagId": 3, "trouble": "회의 시간 놓침", "type": "TIME_MANAGEMENT", "checkedAt": "2026-05-09T10:00:00" }
```

---

### 트러블 체크 취소

```
DELETE /api/v1/journals/troubles?tagId={id}&date={date}
```

**Response** `204 No Content`  
**404** 태그 없음

---

## 일지 - 목표 성취도 (`/api/v1/journals`)

### 목표 추가

```
POST /api/v1/journals/goals
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| content | string (50자 이내) | ✅ | 목표 내용 |
| journalDate | string (yyyy-MM-dd) | ✅ | 일지 날짜 |

**Response** `201 Created`

```json
{ "goalId": 1, "content": "할 일 미리 정리하기" }
```

---

### 목표 내용 수정

```
PATCH /api/v1/journals/goals/{goalId}
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| content | string (50자 이내) | ✅ | 수정할 내용 |

**Response** `200 OK` — `CreateGoalResponse`  
**404** 목표 없음  
**409** 동일 내용의 목표 이미 존재

---

### 목표 삭제

```
DELETE /api/v1/journals/goals/{goalId}?journalDate={date}
```

`journalDate` 이후의 점수 로그가 함께 삭제된다.

**Response** `204 No Content`  
**404** 목표 없음

---

### 목표 점수 등록/수정

```
POST /api/v1/journals/{date}/goals
```

해당 날짜의 점수를 등록하거나 이미 있으면 덮어쓴다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| goalId | number | ✅ | 목표 ID |
| score | number (0~10) | ✅ | 달성 점수 |

**Response** `200 OK`

```json
{ "goalId": 1, "score": 8, "journalDate": "2026-05-09" }
```

---

## 일지 - 수면/식사 (`/api/v1/journals/{date}/sleep-meal`)

### 수면/식사 등록/수정

```
POST /api/v1/journals/{date}/sleep-meal
```

해당 날짜 기록을 등록하거나 이미 있으면 덮어쓴다. 모든 필드 선택 사항이므로 필요한 것만 전송해도 된다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| sleepHour | number (0~24) | ❌ | 수면 시간 |
| sleepQuality | SleepQuality | ❌ | 수면 질 |
| ateBreakfast | boolean | ❌ | 아침식사 여부 |
| ateLunch | boolean | ❌ | 점심식사 여부 |
| ateDinner | boolean | ❌ | 저녁식사 여부 |

**SleepQuality 값**: `GOOD` / `NORMAL` / `BAD`

**Response** `201 Created`

```json
{
  "journalDate": "2026-05-09",
  "sleepHour": 7.5,
  "sleepQuality": "GOOD",
  "ateBreakfast": true,
  "ateLunch": true,
  "ateDinner": false
}
```

---

## 일지 - 메모 (`/api/v1/journals/{date}/memo`)

### 메모 조회

```
GET /api/v1/journals/{date}/memo
```

**Response** `200 OK`

```json
{ "journalDate": "2026-05-09", "memo": "오늘은 컨디션이 좋았다." }
```

**204** 해당 날짜 메모 없음

---

### 메모 등록/수정

```
POST /api/v1/journals/{date}/memo
```

이미 존재하면 덮어쓴다.

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| memo | string | ❌ | 메모 내용 |

**Response** `201 Created`

```json
{ "journalDate": "2026-05-09", "memo": "오늘은 컨디션이 좋았다." }
```

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
GET /v1/community/posts?page=0&size=20
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| q | string | ❌ | - | 제목·본문 키워드 검색 (대소문자 무시). 생략 시 전체 조회 |
| category | PostCategory | ❌ | - | 카테고리 필터. 생략 시 전체 카테고리 |
| page | number | ❌ | 0 | 페이지 번호 (0부터 시작) |
| size | number | ❌ | 20 | 페이지 크기 |

> `q`와 `category`는 AND 조건으로 결합됩니다.

**Response** `200 OK` — `Page<PostResponse>`

```json
{
  "content": [ /* PostResponse 배열 */ ],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

**PostResponse 필드**

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
