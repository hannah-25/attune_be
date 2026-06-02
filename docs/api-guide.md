# API Guide

## Medication

### GET /v1/medications

약 이름 또는 성분명으로 마스터 약물을 검색한다. `q` 생략 시 전체 목록을 반환한다.

**인증:** 필요 (JWT)

**Query Parameters**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| `q` | 선택 | 검색 키워드 (약 이름 또는 성분명, 대소문자 무시) |

**Response 200**

```json
[
  {
    "medicationId": 1,
    "name": "콘서타",
    "ingredient": "메틸페니데이트",
    "dosageOptions": [
      { "dosageId": 3, "amount": 18.00 },
      { "dosageId": 4, "amount": 27.00 },
      { "dosageId": 5, "amount": 36.00 }
    ]
  },
  {
    "medicationId": 2,
    "name": "스트라테라",
    "ingredient": "아토목세틴",
    "dosageOptions": [
      { "dosageId": 6, "amount": 10.00 },
      { "dosageId": 7, "amount": 18.00 },
      { "dosageId": 8, "amount": 25.00 },
      { "dosageId": 9, "amount": 40.00 }
    ]
  }
]
```

`dosageOptions`는 활성화된 용량(`isActive=true`)만 포함하며, amount 오름차순 정렬된다.  
검색 결과가 없으면 빈 배열 `[]`을 반환한다.
