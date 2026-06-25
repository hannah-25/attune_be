#!/usr/bin/env bash
# 공통 헬퍼. 각 스크립트에서 source 한다.
set -euo pipefail

# 리포지터리 루트로 이동 (스크립트 위치 기준)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$REPO_ROOT"

# gradlew 선택 (Windows Git Bash 포함)
if [ -x "./gradlew" ]; then
  GRADLE="./gradlew"
else
  GRADLE="gradle"
fi

log()  { printf '\033[1;34m[agent]\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m[ok]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[warn]\033[0m %s\n' "$*"; }
# 실패 시 원인 + 다음 행동을 함께 출력한다 (에이전트가 다음 단계를 추론할 수 있게).
fail() { printf '\033[1;31m[fail]\033[0m %s\n' "$*" >&2; [ "${2:-}" ] && printf '  → 다음: %s\n' "$2" >&2; exit 1; }
