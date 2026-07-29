#!/usr/bin/env bash
# dev DB → prod 신규 구축용 덤프.
#
#   schema.sql : 전체 스키마 (데이터 없음). prod 는 ddl-auto=validate 라 이게 엔티티와 맞아야 기동된다.
#   seed.sql   : 유저에 종속되지 않은 마스터 데이터만.
#
# 유저 데이터와 부하테스트 잔여물은 제외한다.
#   - medications / medication_dosages : LoadtestDataRunner 가 만든 __loadtest_medication__ 행 제외
#   - journal_tags : scope='SYSTEM' 만. scope='USER' 는 owner_user_id 로 users 를 참조하므로
#                    유저가 없는 prod 에 넣으면 FK 제약으로 import 가 실패한다.
#
# 마스터 테이블 4개는 docs/db_schema.md 와 docs/sql/*.sql 전체를 대조해 정한 목록이다.
# 다만 db_schema.md 에는 코드에 없는 Hospital 이 남아있는 등 드리프트가 있으므로,
# 덤프 전에 dev 실물로 한 번 더 확인한다. user_id 계열 컬럼이 없는 테이블을 뽑아
# 아래 4개 외에 데이터가 들어있는 테이블이 있는지 눈으로 본다:
#
#   SELECT t.TABLE_NAME, t.TABLE_ROWS
#     FROM information_schema.TABLES t
#    WHERE t.TABLE_SCHEMA = DATABASE()
#      AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS c
#                       WHERE c.TABLE_SCHEMA = t.TABLE_SCHEMA
#                         AND c.TABLE_NAME = t.TABLE_NAME
#                         AND c.COLUMN_NAME IN ('user_id', 'owner_user_id'))
#    ORDER BY t.TABLE_ROWS DESC;
#
#   (InnoDB 의 TABLE_ROWS 는 근사치다. 정확한 수가 필요하면 해당 테이블만 COUNT(*) 한다.)
#
# 사용:
#   DEV_DB_HOST=... DEV_DB_USER=... DEV_DB_PASS=... ./scripts/dump-prod-seed.sh [출력디렉터리]
#
# 주입:
#   mysql -h $PROD_HOST -u $PROD_USER -p $DB < prod-seed/schema.sql
#   mysql -h $PROD_HOST -u $PROD_USER -p $DB < prod-seed/seed.sql
set -euo pipefail

: "${DEV_DB_HOST:?DEV_DB_HOST 가 필요하다}"
: "${DEV_DB_USER:?DEV_DB_USER 가 필요하다}"
: "${DEV_DB_PASS:?DEV_DB_PASS 가 필요하다}"

DB="${DEV_DB_NAME:-attune}"
OUT="${1:-./prod-seed}"
LOADTEST_MED="__loadtest_medication__"

# 명령줄에 비밀번호를 두면 프로세스 목록에 노출되므로 환경변수로 넘긴다.
export MYSQL_PWD="$DEV_DB_PASS"

# --no-tablespaces: RDS 에서 PROCESS 권한 없이 덤프하기 위해 필요
# --set-gtid-purged=OFF: GTID 활성 인스턴스의 덤프를 다른 서버에 넣을 때 필요
# --skip-extended-insert: 행당 INSERT 1줄. 마스터 데이터는 소량이라 손해가 없고,
#                         눈으로 검토하거나 행 수를 세기 쉬워진다.
dump() {
  mysqldump --single-transaction --no-tablespaces --set-gtid-purged=OFF --skip-extended-insert \
    -h "$DEV_DB_HOST" -u "$DEV_DB_USER" "$@"
}

mkdir -p "$OUT"

dump --no-data "$DB" > "$OUT/schema.sql"

{
  dump --no-create-info --where="name <> '$LOADTEST_MED'" "$DB" medications
  dump --no-create-info \
    --where="medication_id NOT IN (SELECT id FROM medications WHERE name = '$LOADTEST_MED')" \
    "$DB" medication_dosages
  dump --no-create-info "$DB" terms
  dump --no-create-info --where="scope = 'SYSTEM'" "$DB" journal_tags
} > "$OUT/seed.sql"

# 필터가 실제로 걸렸는지 확인한다. 여기서 걸리면 prod 에 부하테스트 약이 들어간다.
if grep -q "$LOADTEST_MED" "$OUT/seed.sql"; then
  echo "실패: seed.sql 에 부하테스트 데이터가 남아있다." >&2
  exit 1
fi

# 마스터 테이블이 비어 있으면 앱은 뜨지만 복약 등록이 불가능해진다. 눈으로 확인할 수 있게 출력한다.
echo "생성 완료: $OUT/schema.sql, $OUT/seed.sql"
for table in medications medication_dosages terms journal_tags; do
  printf '  %-20s %s rows\n' "$table" "$(grep -c "INSERT INTO \`$table\`" "$OUT/seed.sql" || true)"
done
