# 개발 노트

## 수정 필요

### Comment - 익명 처리 구분
- **파일**: `communityBoard/domain/model/Comment.java`
- 현재 `isAnonymous = true`이면 닉네임을 "익명"으로 표시하지만, 같은 게시글 내 여러 익명 댓글을 구분할 수단이 없음
- 익명1, 익명2처럼 게시글 단위로 익명 번호를 부여하는 방식 검토 필요
