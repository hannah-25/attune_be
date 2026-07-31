# 개발 노트

## 수정 필요

### SecurityUtils.getCurrentUserUuid() null 반환
- **파일**: `common/util/SecurityUtils.java`
- 인증 정보가 없으면 `null` 반환 → 이를 그대로 `repository.findById()`에 넘기면 NPE 발생 가능
- 인증 실패 시 `UnauthorizedException`을 던지도록 변경 검토 필요

### Comment - 익명 처리 구분
- **파일**: `communityBoard/domain/model/Comment.java`
- 현재 `isAnonymous = true`이면 닉네임을 "익명"으로 표시하지만, 같은 게시글 내 여러 익명 댓글을 구분할 수단이 없음
- 익명1, 익명2처럼 게시글 단위로 익명 번호를 부여하는 방식 검토 필요
