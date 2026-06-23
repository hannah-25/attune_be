-- ============================================================
-- 초기 데이터 seed (신규 DB 구축 시 사용)
-- 실행 전제: 테이블이 이미 존재하는 상태여야 함
--            (ddl-auto=create/update 로 앱 기동 후 실행, 또는 DDL 수동 적용 후 실행)
-- 멱등성: journal_tags는 INSERT IGNORE(unique constraint 보호),
--         terms는 WHERE NOT EXISTS 로 중복 실행 안전
-- 시스템 태그 변경 시: SystemJournalTagDefinitions.java 수정 후 이 파일도 함께 갱신
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 1. medications
-- ============================================================
INSERT INTO `medications`
  (`drug_class`, `source_url`, `effect`, `formulation`, `generic_name`, `graph_url`, `image_url`, `name`, `side_effect`, `description`, `typical_dosage_range`)
VALUES
('ADHD 자극제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=2020072300005','부주의, 충동성, 과잉행동 개선','OROS 서방정','methylphenidate',NULL,'https://common.health.kr/shared/images/sb_photo/big3/202007230000501.jpg','콘서타OROS서방정','식욕저하, 불면, 두통, 복통, 불안, 과민성, 틱, 혈압/심박 증가 가능','OROS 방출 시스템으로 1일 1회 오전 복용하며 학교나 업무 시간 대부분을 길게 덮는 데 강점이 있다. 정제를 자르거나 씹거나 부수면 안 되며, 캡슐형 제제처럼 개봉 복용하지 않는다.','18/27/36/54mg 제형. 소아 최대 54mg/day, 청소년·성인 최대 72mg/day 참고'),
('ADHD 자극제','https://health.kr/searchDrug/result_take.asp?drug_cd=2011122300003','부주의, 충동성, 과잉행동 개선','서방캡슐','methylphenidate',NULL,'https://common.health.kr/shared/images/sb_photo/big3/201112230000302.jpg','메디키넷리타드캡슐','식욕저하, 불면, 두통, 복통, 불안/긴장감, 혈압/심박 증가 가능','속방 성분과 서방 성분이 섞인 캡슐형 제제라 비교적 빠른 발현과 일정 시간 유지를 함께 노린다. 콘서타보다 지속시간이 짧은 편이라 오후 늦게까지 약효가 남는 것이 부담일 때 조절 여지가 있다.','5/10/20/30/40mg 제형. 최대 60mg/day 참고'),
('ADHD 자극제','https://health.kr/searchDrug/result_drug.asp?drug_cd=A11ABBBBB1650','부주의, 충동성, 과잉행동 개선','속방정','methylphenidate',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11ABBBBB165001.jpg','페니드정','식욕저하, 불면, 두통, 복통, 빈맥, 불안, 틱 악화 가능','속방형이라 효과 발현이 빠르고 지속시간은 짧다. 세밀한 용량 조절이나 긴 지속형 약의 빈틈을 보충하는 용도로 쓰기 쉽지만, 복용 횟수가 늘 수 있다.','6세 이상: 5mg 1일 2회(아침/점심 전) 시작, 주 5-10mg 증량. 성인: 20-30mg/day 2-3회 분할. 최대 60mg/day'),
('ADHD 자극제','https://health.kr/searchDrug/result_take.asp?drug_cd=A11AOOOOO7720','부주의, 충동성, 과잉행동 개선','서방캡슐','methylphenidate',NULL,'https://common.health.kr/shared/images/ext_images/pack_img/P_A11AOOOOO7720_02.jpg','메타데이트CD서방캡슐','식욕저하, 불면, 두통, 복통, 불안, 혈압/심박 증가 가능','캡슐 안에 속방/서방 비드가 함께 들어 있는 지속형 methylphenidate 제품군이다. 오전 발현과 이후 유지 효과를 함께 노리며, OROS 정제인 콘서타와 제형이 다르다.','20mg 1일 1회 아침 시작, 주 10-20mg 증량. 함량 10/20/30/40/50/60mg. 최대 60mg/day'),
('ADHD 자극제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=2015061600002','부주의, 충동성, 과잉행동 개선','조절방출캡슐','methylphenidate',NULL,'https://common.health.kr/shared/images/sb_photo/big3/201506160000201.jpg','비스펜틴조절방출캡슐','식욕저하, 불면, 두통, 복통, 불안, 혈압/심박 증가 가능','조절방출 캡슐로 긴 지속시간을 목표로 한다. 콘서타처럼 길게 가는 축이지만 캡슐형이라는 점이 다르며, 캡슐 개봉 복용 가능 자료가 있으나 내부 알갱이는 씹거나 부수면 안 된다.','10/30/50/60mg 제형. 최대 60mg/day 참고'),
('ADHD 비자극제','https://health.kr/searchDrug/result_take.asp?drug_cd=A11APPPPP0420','부주의, 충동성 개선','캡슐','atomoxetine',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11APPPPP042001.jpg','스트라테라캡슐','구역, 복통, 식욕저하, 졸림, 피로, 어지러움, 기분 변화, 자살사고 경고','대표적인 아토목세틴 오리지널 제품이다. 자극제가 아니라 효과가 즉각적이기보다 서서히 나타나며, 자극제의 불면·식욕저하·심박 자극이 부담될 때 대안으로 고려될 수 있다. 캡슐은 열지 않는다.','70kg 이하: 0.5mg/kg/day 시작, 1.2mg/kg/day 목표. 70kg 초과/성인: 40mg 시작, 80mg 목표. 최대 100mg/day'),
('ADHD 비자극제','https://health.kr/searchDrug/result_take.asp?drug_cd=2014031800001','부주의, 충동성 개선','캡슐','atomoxetine',NULL,'https://common.health.kr/shared/images/sb_photo/big3/201403180000102.jpg','아토목신캡슐','구역, 복통, 식욕저하, 졸림, 피로, 어지러움, 자살사고 경고','아토목세틴 제네릭이다. 효과 축은 스트라테라와 같고, 상품명/제조사/함량/외형 차이가 있어 검색 대응을 위해 별도 등록한다.','확인 함량: 10/18/25/40/60/80mg. 70kg 이하 0.5mg/kg/day 시작 후 1.2mg/kg/day 목표, 최대 1.4mg/kg/day 또는 100mg/day 중 낮은 용량. 70kg 초과/성인 40mg 시작 후 80mg 목표, 최대 100mg/day'),
('ADHD 비자극제','https://www.health.kr/searchDrug/result_take.asp?drug_cd=2014032700024','부주의, 충동성 개선','캡슐','atomoxetine',NULL,NULL,'아토세라캡슐','구역, 복통, 식욕저하, 졸림, 피로, 어지러움, 자살사고 경고','아토목세틴 제네릭이다. 스트라테라와 같은 비자극제 계열이며, 효과 발현이 느린 편이라 꾸준한 복용과 추적 평가가 중요하다.','확인 함량: 10/18/25/40mg. 70kg 이하 0.5mg/kg/day 시작 후 1.2mg/kg/day 목표, 최대 1.4mg/kg/day 또는 100mg/day 중 낮은 용량. 70kg 초과/성인 40mg 시작 후 80mg 목표, 최대 100mg/day'),
('ADHD 비자극제','https://health.kr/searchDrug/result_take.asp?drug_cd=2013013000021','부주의, 충동성 개선','캡슐','atomoxetine',NULL,NULL,'아토모테라캡슐','구역, 복통, 식욕저하, 졸림, 피로, 어지러움, 자살사고 경고','아토목세틴 제네릭이다. 상품명 검색 대응용으로 별도 등록하며, 자극제와 달리 남용 우려가 상대적으로 낮은 비자극제 축이다.','확인 함량: 18/25/40mg. 70kg 이하 0.5mg/kg/day 시작 후 1.2mg/kg/day 목표, 최대 1.4mg/kg/day 또는 100mg/day 중 낮은 용량. 70kg 초과/성인 40mg 시작 후 80mg 목표, 최대 100mg/day'),
('ADHD 비자극제','https://www.health.kr/searchDrug/result_take.asp?drug_cd=2016062700032','부주의, 충동성 개선','캡슐','atomoxetine',NULL,NULL,'아목세틴캡슐','구역, 복통, 식욕저하, 졸림, 피로, 어지러움, 자살사고 경고','아토목세틴 제네릭이다. 성분 기반 설명은 스트라테라와 동일하게 관리하되, 사용자가 약봉투 상품명으로 검색할 수 있도록 별도 등록한다.','확인 함량: 25/40mg. 70kg 이하 0.5mg/kg/day 시작 후 1.2mg/kg/day 목표, 최대 1.4mg/kg/day 또는 100mg/day 중 낮은 용량. 70kg 초과/성인 40mg 시작 후 80mg 목표, 최대 100mg/day'),
('ADHD 비자극제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=2014030300031','부주의, 충동성 개선','캡슐','atomoxetine',NULL,'https://common.health.kr/shared/images/sb_photo/big3/201403030003102.jpg','도모틴캡슐','구역, 복통, 식욕저하, 졸림, 피로, 어지러움, 자살사고 경고','아토목세틴 제네릭이다. 자극제처럼 빠르게 체감되는 약은 아니며, 효과와 부작용을 몇 주 단위로 관찰하는 쪽에 가깝다.','확인 함량: 10/40mg. 70kg 이하 0.5mg/kg/day 시작 후 1.2mg/kg/day 목표, 최대 1.4mg/kg/day 또는 100mg/day 중 낮은 용량. 70kg 초과/성인 40mg 시작 후 80mg 목표, 최대 100mg/day'),
('ADHD 비자극제','https://health.kr/searchDrug/result_drug.asp?drug_cd=2013032500008','부주의, 충동성 개선','캡슐','atomoxetine',NULL,'https://common.health.kr/shared/images/sb_photo/big3/201303250000801.jpg','환인아토목세틴캡슐','구역, 복통, 식욕저하, 졸림, 피로, 어지러움, 자살사고 경고','아토목세틴 제네릭이다. 상품명이 성분명형이라 검색 매칭에 중요하며, 스트라테라 계열과 같은 주의사항을 공유한다.','확인 함량: 10/18/25/40/60/80mg. 70kg 이하 0.5mg/kg/day 시작 후 1.2mg/kg/day 목표, 최대 1.4mg/kg/day 또는 100mg/day 중 낮은 용량. 70kg 초과/성인 40mg 시작 후 80mg 목표, 최대 100mg/day'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/lookup.cfm?setid=d6fb2750-cdab-4749-ba0d-7534840a5892&version=3','부주의, 충동성, 과잉행동 개선','속방정','methylphenidate',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=5mglabel.jpg&setid=d6fb2750-cdab-4749-ba0d-7534840a5892&type=img','Ritalin','식욕저하, 불면, 두통, 복통, 불안, 혈압/심박 증가','전통적인 속방형 methylphenidate 브랜드다. 빠른 발현과 짧은 지속시간이 특징이며, 여러 번 나누어 복용하거나 보충용으로 쓰는 맥락이 많다.','6세 이상: 5mg 1일 2회 시작, 주 5-10mg 증량. 성인: 20-30mg/day 2-3회 분할. 최대 60mg/day'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/search.cfm?query=Ritalin LA','부주의, 충동성, 과잉행동 개선','서방캡슐','methylphenidate',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=ritalin-la-04.jpg&setid=effd952d-ac94-47bb-b107-589a4934dcca&type=img','Ritalin LA','식욕저하, 불면, 두통, 복통, 불안, 혈압/심박 증가','Ritalin의 장시간 캡슐형 제품군이다. 속방형보다 복용 횟수를 줄이고 오전 이후 유지 효과를 노린다.','10-20mg 1일 1회 아침 시작, 주 10mg 증량. 함량 10/20/30/40mg. 최대 60mg/day'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/lookup.cfm?setid=987b4983-1edc-40f1-adc9-724747080969','부주의, 충동성, 과잉행동 개선','속방정','dexmethylphenidate',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=Dexmethylphenidate-02.jpg&setid=987b4983-1edc-40f1-adc9-724747080969&type=img','Focalin','식욕저하, 불면, 두통, 복통, 불안, 혈압/심박 증가','methylphenidate의 활성 이성질체인 dexmethylphenidate 제품이다. 속방형이라 빠른 발현과 짧은 지속시간 쪽에 가깝다.','2.5mg 1일 2회 시작, 주 2.5-5mg 증량. 최대 20mg/day(10mg 1일 2회)'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=00f1d83e-93cc-4bbe-9c3b-c488062271ab','부주의, 충동성, 과잉행동 개선','서방캡슐','dexmethylphenidate',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=5681.jpg&setid=00f1d83e-93cc-4bbe-9c3b-c488062271ab&type=img','Focalin XR','식욕저하, 불면, 두통, 복통, 불안, 혈압/심박 증가','dexmethylphenidate의 장시간형이다. Focalin보다 복용 편의성과 하루 커버리지를 늘리는 데 초점이 있다.','소아 5mg, 성인 10mg 1일 1회 아침 시작. 주 5mg(소아) 또는 10mg(성인) 증량. 최대 소아 30mg/day, 성인 40mg/day'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=2c312c31-3198-4775-91ab-294e0b4b9e7f','부주의, 충동성, 과잉행동 개선','경피 패치','methylphenidate',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=daytrana-11.jpg&setid=2c312c31-3198-4775-91ab-294e0b4b9e7f&type=img','Daytrana','패치 부위 피부반응, 식욕저하, 불면, 두통, 불안','먹는 약이 아니라 피부에 붙이는 methylphenidate 패치다. 착용 시간으로 약효 노출을 조절할 수 있는 점이 가장 큰 차별점이다.','10mg 패치로 시작, 필요 시 15/20/30mg로 주 단위 조절. 효과 필요 2시간 전 부착, 보통 9시간 착용. 최대 30mg/9시간'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/search.cfm?query=Quillivant XR','부주의, 충동성, 과잉행동 개선','서방 현탁액','methylphenidate',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=quillivant-xr-20.jpg&setid=c2dc2109-44a6-4797-b04e-18761dd9d45a&type=img','Quillivant XR','식욕저하, 불면, 복통, 두통, 불안, 혈압/심박 증가','액상 XR 제형이라 알약을 삼키기 어려운 환자에게 제형상 장점이 있다.','20mg 1일 1회 아침 시작, 주 10-20mg 증량. 최대 60mg/day'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=defc1205-8e90-4b1e-b862-05e4c35c7364','부주의, 충동성, 과잉행동 개선','씹어먹는 ER 정제','methylphenidate',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=quillichew-er-6.jpg&setid=defc1205-8e90-4b1e-b862-05e4c35c7364&type=img','QuilliChew ER','식욕저하, 불면, 체중감소, 두통, 불안','씹어먹는 서방정이라 삼키는 정제가 어려운 경우 차별점이 있다. 서방 제형이므로 임의 분쇄/변형은 피한다.','해외 자료 기준, 20mg 1일 1회 시작, 60mg/day 초과 비권장'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=33f70f58-c871-42c8-8adb-345caeafefcd','부주의, 충동성, 과잉행동 개선','구강붕해 XR 정제','methylphenidate',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=cotempla-xr-odt-4.jpg.jpg&setid=33f70f58-c871-42c8-8adb-345caeafefcd&type=img','Cotempla XR-ODT','식욕저하, 불면, 두통, 복통, 불안, 혈압/심박 증가','물 없이 입에서 녹여 복용하는 XR 제형이다. 알약 삼킴이 어려운 경우 장점이 있다.','해외 자료 기준, 17.3mg 1일 1회 시작, 51.8mg/day 초과 비권장'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=d95dede0-b1ff-4489-8f91-3bbe122852bf','부주의, 충동성, 과잉행동 개선','지연방출/서방캡슐','methylphenidate',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=jor00-0012-04.jpg&setid=d95dede0-b1ff-4489-8f91-3bbe122852bf&type=img','Jornay PM','불면, 식욕저하, 두통, 불안, 혈압/심박 증가','저녁에 복용해 다음 날 아침부터 효과가 나타나도록 설계된 독특한 methylphenidate 제형이다. 아침 기상 직후 증상이 큰 경우 차별점이 있다.','20mg 1일 1회 저녁 시작, 주 20mg 증량. 보통 18:30-21:30 복용. 최대 100mg/day'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/search.cfm?query=1648195&searchdb=rxcui','부주의, 충동성, 과잉행동 개선','서방캡슐','methylphenidate',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=aptensio-01.jpg&archiveid=964488','Aptensio XR','두통, 불면, 식욕저하, 복통, 불안, 혈압/심박 증가','methylphenidate ER 캡슐 제품군이다. 복용 횟수를 줄이고 하루 중 일정 시간 효과를 유지하는 데 초점이 있다.','해외 자료 기준, 10mg 1일 1회 시작, 60mg/day 초과 비권장'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/lookup.cfm?setid=00b5e716-5564-4bbd-acaf-df2bc45a5663&version=9','부주의, 충동성, 과잉행동 개선','캡슐','serdexmethylphenidate / dexmethylphenidate',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=azstarys-05.jpg&setid=00b5e716-5564-4bbd-acaf-df2bc45a5663&type=img','Azstarys','식욕저하, 불면, 구역, 복통, 불안, 혈압/심박 증가','전구약물 성분과 활성 dexmethylphenidate가 함께 들어간 복합제다. 빠른 시작과 지속 효과를 함께 노리는 구조다.','39.2/7.8mg 1일 1회 아침 시작. 1주 후 26.1/5.2mg로 감량 또는 52.3/10.4mg로 증량 가능. 최대 52.3/10.4mg/day'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/search.cfm?query=Adderall','부주의, 충동성, 과잉행동 개선','속방정','mixed amphetamine salts',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=adderall-image-1.jpg&setid=f22635fe-821d-4cde-aa12-419f8b53db81&type=img','Adderall','입마름, 식욕저하, 불면, 두통, 체중감소, 불안, 빈맥','암페타민 혼합염 속방형이다. 효과 발현은 빠른 축이나 남용/의존 위험 관리가 중요하다.','3-5세: 2.5mg/day 시작. 6세 이상: 5mg 1일 1-2회 시작, 주 5mg 증량. 드물게 40mg/day 초과 필요'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?lang=en&setid=aff45863-ffe1-4d4f-8acf-c7081512a6c0','부주의, 충동성, 과잉행동 개선','서방캡슐','mixed amphetamine salts',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=adderall-xr-02.jpg&setid=aff45863-ffe1-4d4f-8acf-c7081512a6c0&type=img','Adderall XR','입마름, 식욕저하, 불면, 두통, 체중감소, 불안, 빈맥, 남용/의존 주의','암페타민 혼합염 서방형이다. 단계적 방출 캡슐로 1일 1회 복용을 목표로 한다.','해외 자료 기준, 소아 10mg 시작, 성인 20mg/day 사용 예'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/lookup.cfm?setid=a310fc51-2743-4755-8398-fed5402283f6','부주의, 충동성, 과잉행동 개선','캡슐/츄어블','lisdexamfetamine',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=5916.jpg&setid=a310fc51-2743-4755-8398-fed5402283f6&type=img','Vyvanse','식욕저하, 불면, 체중감소, 입마름, 복통, 구역, 불안','전구약물이라 체내 전환 후 작용한다. 1일 1회 장시간 효과를 목표로 하며, 암페타민 계열이므로 남용/의존 주의가 필요하다.','해외 자료 기준, 30mg 시작, 최대 70mg/day'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/lookup.cfm?setid=f469fb38-0380-4621-9db3-a4f429126156&version=9','부주의, 충동성, 과잉행동 개선','정제','amphetamine sulfate',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=5mg-bottle-label.jpg&setid=f469fb38-0380-4621-9db3-a4f429126156&type=img','Evekeo','입마름, 식욕저하, 불면, 불안, 혈압/심박 증가','amphetamine sulfate 제제다. 속방형 자극제 축에 가깝고, 남용/의존과 심혈관계 주의가 중요하다.','해외 자료 기준, 소아 5mg 시작 후 주 단위 조절'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?lang=en&setid=cc717b9b-22ea-4c60-a1d4-ee38a40bce78','부주의, 충동성, 과잉행동 개선','정제/서방캡슐','dextroamphetamine',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=dexedrine-dextroamphetamine-sulfate-spansule-susta-2.jpg&setid=cc717b9b-22ea-4c60-a1d4-ee38a40bce78&type=img','Dexedrine','식욕저하, 불면, 입마름, 불안, 혈압/심박 증가','dextroamphetamine 계열의 오래된 브랜드다. 정제와 Spansule 형태가 있으며, 자극제 공통 관리가 중요하다.','3-5세: 2.5mg/day 시작. 6세 이상: 5mg 1일 1-2회 시작, 주 5mg 증량. 보통 최대 40mg/day 참고'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=d6394df5-f2c9-47eb-b57e-f3e9cfd94f84','부주의, 충동성, 과잉행동 개선','정제','dextroamphetamine',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=2-5mg-bottle-label.jpg&setid=d6394df5-f2c9-47eb-b57e-f3e9cfd94f84&type=img','Zenzedi','식욕저하, 불면, 입마름, 불안, 혈압/심박 증가','dextroamphetamine 속방정 제품이다. 빠른 발현과 짧은 지속시간 쪽에 가깝다.','3-5세: 2.5mg/day 시작. 6세 이상: 5mg 1일 1-2회 시작, 주 5mg 증량. 보통 최대 40mg/day 참고'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/fda/fdaDrugXsl.cfm?setid=ae304b29-0b40-40ec-ad0d-76b742d4a9b9','부주의, 충동성, 과잉행동 개선','서방 현탁액/정제','amphetamine',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=dyanavel-xr-oral-suspension-and-tablets-1.jpg&id=937369','Dyanavel XR','식욕저하, 불면, 복통, 불안, 혈압/심박 증가, 남용/의존 주의','amphetamine XR 제형이다. 액상 또는 정제 형태가 있어 복용 제형 선택지가 넓다.','2.5mg 또는 5mg 1일 1회 아침 시작, 4-7일마다 2.5-10mg 증량. 최대 20mg/day'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/lookup.cfm?setid=141a7970-3f06-44ea-9ab7-aeece2c085fc','부주의, 충동성, 과잉행동 개선','서방캡슐','mixed amphetamine salts',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=mydayis-03.jpg&setid=141a7970-3f06-44ea-9ab7-aeece2c085fc&type=img','Mydayis','입마름, 식욕저하, 불면, 체중감소, 불안, 빈맥','긴 지속시간을 목표로 한 mixed amphetamine salts 제형이다. 청소년/성인 쪽 장시간 커버리지를 고려할 때 차별점이 있다.','13세 이상: 12.5mg 1일 1회 아침 시작, 주 12.5mg 증량. 최대 50mg/day'),
('ADHD 자극제','https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=d0905e2c-a02f-ec4f-59a3-6f7ad0acbbf6','부주의, 충동성, 과잉행동 개선','정제','methamphetamine hydrochloride',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=Methamphentamine-02.jpg&setid=d0905e2c-a02f-ec4f-59a3-6f7ad0acbbf6&type=img','Desoxyn','식욕저하, 불면, 혈압/심박 증가, 불안, 남용/의존 위험','methamphetamine 성분의 ADHD 적응증 약이지만 남용/위험성 때문에 매우 신중하게 다뤄야 하는 약이다. 기본 노출 데이터에는 넣되, 국내 앱에서는 해외/고위험 표시가 필요하다.','6세 이상: 5mg 1일 1-2회 시작, 주 5mg 증량. 권장 범위 20-25mg/day'),
('ADHD 비자극제 - 알파2 작용제','https://dailymed.nlm.nih.gov/dailymed/lookup.cfm?setid=1bb3fac5-11e8-4d1c-a5e4-2520abcdeb47','충동성, 과잉행동, 감정조절 보조','서방정','guanfacine',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=64725-0515.jpg&setid=1bb3fac5-11e8-4d1c-a5e4-2520abcdeb47&type=img','Intuniv','졸림, 피로, 저혈압, 서맥, 어지러움, 구역, 갑작스러운 중단 주의','자극제가 아닌 알파2 작용제다. 충동성, 과잉행동, 감정조절, 틱/수면 문제가 동반될 때 고려되는 축이며 졸림과 혈압 저하를 더 주의한다.','1mg 1일 1회 시작, 주 1mg 이하 증량. 목표 0.05-0.12mg/kg/day, 총 1-7mg/day 범위'),
('ADHD 비자극제 - 알파2 작용제','https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=7676af27-4d8f-49c4-a50f-6b0892e73c8f','과잉행동, 충동성, 수면 문제 보조','서방정','clonidine',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=clonidine-03.jpg&setid=7676af27-4d8f-49c4-a50f-6b0892e73c8f&type=img','Kapvay','졸림, 진정, 저혈압, 서맥, 어지러움, 입마름, 감량 중단 필요','clonidine ER 제제다. 수면 문제, 틱, 과잉행동/충동성이 함께 있을 때 보조적으로 고려되는 축이며 진정과 저혈압에 주의한다.','0.1mg 취침 전 시작, 주 0.1mg/day 증량. 0.2mg/day 이상은 아침/취침 전 분할. 최대 0.4mg/day'),
('ADHD 비자극제 - 노르에피네프린 재흡수 억제제','https://dailymed.nlm.nih.gov/dailymed/lookup.cfm?setid=aedf408d-0f84-418d-9416-7c39ddb0d29a','부주의, 충동성, 과잉행동 개선','ER 캡슐','viloxazine',NULL,'https://dailymed.nlm.nih.gov/dailymed/image.cfm?name=qelbree-05.jpg&setid=aedf408d-0f84-418d-9416-7c39ddb0d29a&type=img','Qelbree','졸림, 피로, 구역, 식욕저하, 불면, 두통, 입마름, 변비, 자살사고 경고','FDA 승인 비자극제다. atomoxetine과 같은 비자극제 축으로 볼 수 있지만 별도 성분이며, 졸림과 불면이 모두 나타날 수 있다.','6-11세 100mg 시작 후 최대 400mg/day. 12-17세 200mg 시작 후 최대 400mg/day. 성인 200mg 시작 후 최대 600mg/day'),
('SSRI 항우울제/항불안제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=A11AOOOOO5474','범불안, 우울, 불안 증상 완화','정제','escitalopram',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11AOOOOO547401.jpg','렉사프로정','오심, 불면 또는 졸림, 피로, 발한, 성기능 저하','ADHD에 불안이나 우울이 동반될 때 장기 조절 후보로 고려될 수 있는 SSRI다. 즉시 진정시키는 약은 아니며, 수 주에 걸쳐 불안 민감도와 예기불안을 낮추는 방향으로 작용한다. 자극제와 병용 시 초반 불면이나 초조가 겹치는지 관찰한다.','보통 10mg 1일 1회 시작, 필요 시 최소 1주 후 20mg/day까지 증량. 최대 20mg/day'),
('SSRI 항우울제/항불안제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=A11A0310A0239','공황, 사회불안, PTSD, 우울 증상 완화','정제','sertraline',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11A0310A023902.jpg','졸로푸트정','오심, 설사, 떨림, 불면, 식욕저하, 성기능 저하','공황과 사회불안 쪽 근거가 강한 SSRI다. ADHD 자극제와 병용할 때는 초반 불면, 떨림, 초조감이 겹칠 수 있어 낮은 용량부터 조절하는 경우가 많다.','25-50mg 시작, 최대 200mg/day'),
('SSRI 항우울제/항불안제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=A11AOOOOO2721','공황, 사회불안, 범불안, 우울 증상 완화','서방정','paroxetine',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11AOOOOO272101.jpg','팍실CR정','졸림, 구역, 체중 증가, 성기능 저하, 중단 증상','공황과 불안 증상에 쓰이는 SSRI다. 비교적 진정감이 느껴질 수 있지만 체중 증가, 성기능 저하, 중단 증상이 문제가 될 수 있다. CYP2D6 억제 영향 때문에 atomoxetine과 병용 시 농도 상승 가능성을 고려한다.','공황장애: 12.5mg 1일 1회 시작, 주 12.5mg 증량. 최대 75mg/day 참고'),
('SSRI 항우울제/항불안제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=A11AOOOOO2927','공황, 우울, 강박 증상 완화','캡슐','fluoxetine',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11AOOOOO292702.jpg','푸로작캡슐','오심, 불면, 불안/초조, 두통, 식욕 변화, 성기능 저하','반감기가 긴 SSRI라 중단 증상이 상대적으로 덜한 편으로 다뤄진다. 반대로 초반 활성화, 불면, 초조가 부담될 수 있다. CYP2D6 억제 영향 때문에 atomoxetine과 병용 시 주의가 필요하다.','공황장애: 10mg/day 시작 후 1주 뒤 20mg/day. 최대 60mg/day. 우울증은 보통 20mg/day 시작'),
('SNRI 항우울제/항불안제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=A11ABBBBB1493','범불안, 사회불안, 공황, 우울 증상 완화','서방캡슐','venlafaxine',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11ABBBBB149301.jpg','이팩사엑스알서방캡슐','오심, 불면, 발한, 입마름, 혈압 상승, 성기능 저하','SSRI 반응이 부족하거나 우울과 불안이 함께 두드러질 때 고려되는 SNRI다. ADHD 자극제와 병용하면 혈압, 심박, 불면을 함께 관찰하는 것이 중요하다.','37.5-75mg 시작, 보통 75-225mg/day'),
('SNRI 항우울제/항불안제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=4q13ol39olfgl','범불안, 우울, 통증 동반 증상 완화','장용캡슐','duloxetine',NULL,'https://common.health.kr/shared/images/sb_photo/big3/4q13ol39olfgl01.jpg','심발타캡슐','오심, 입마름, 변비, 졸림, 불면, 어지러움, 식욕저하','불안과 우울에 통증, 근육통, 신경병성 통증이 함께 있을 때 후보가 될 수 있는 SNRI다. ADHD 자극제와 병용 시 혈압, 심박, 불면 악화를 같이 본다.','범불안/우울: 보통 30mg 1일 1회 시작 후 60mg/day. 필요 시 최대 120mg/day'),
('비벤조디아제핀 항불안제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=A11AOOOOO2919','범불안 증상 완화','정제','buspirone',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11AOOOOO291901.jpg','부스론정','어지러움, 구역, 두통, 졸림 또는 초조, 신경과민','벤조디아제핀처럼 바로 진정시키는 약이 아니라 꾸준히 복용하면서 불안을 낮추는 약이다. 의존성 부담이 상대적으로 적어 장기 불안 조절 후보가 될 수 있다.','7.5mg 1일 2회 또는 5mg 1일 3회 시작, 2-3일마다 5mg/day 증량. 보통 20-30mg/day, 최대 60mg/day'),
('벤조디아제핀계 항불안제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=A11ABBBBB0920','급성 불안, 공황 증상 완화','정제','alprazolam',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11ABBBBB092002.jpg','자낙스정','졸림, 어지러움, 기억력 저하, 운동실조, 의존/금단','작용이 빠른 편이라 급성 공황이나 강한 불안에 단기 보조로 쓰일 수 있다. 장기 유지치료 중심 약은 아니며 의존, 내성, 금단 위험 때문에 신중히 다뤄야 한다.','불안: 0.25-0.5mg 1일 3회 시작, 최대 4mg/day. 공황장애는 0.5mg 1일 3회 시작 후 신중히 증량'),
('벤조디아제핀계 항불안제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=A11ABBBBB1153','공황 증상 완화','정제','clonazepam',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11ABBBBB115304.jpg','리보트릴정','졸림, 어지러움, 운동실조, 기억력 저하, 우울, 의존/금단','공황장애에 쓰이는 벤조디아제핀 후보 약물이다. alprazolam보다 작용 시간이 긴 축으로 볼 수 있으나, 졸림과 인지 둔화, 의존 위험은 반드시 관리해야 한다.','공황장애: 0.25mg 1일 2회 시작, 3일 후 1mg/day까지 증량 가능. 최대 4mg/day'),
('벤조디아제핀계 항불안제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=A11ABBBBB0915','급성 불안, 긴장 완화','정제','lorazepam',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11ABBBBB091502.jpg','아티반정','진정, 졸림, 어지러움, 무력감, 기억력 저하, 의존/금단','불안 증상을 단기간 빠르게 낮추는 보조 후보 약물이다. ADHD 약물의 각성감과 반대로 진정과 주의력 저하가 생길 수 있어 운전, 학습, 업무 영향을 같이 봐야 한다.','불안: 보통 2-3mg/day를 2-3회 분할, 범위 1-10mg/day. 불면 동반 시 취침 전 2-4mg 사용 가능'),
('항히스타민계 항불안/진정제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=A11ABBBBB2522','불안, 긴장, 초조 완화','시럽','hydroxyzine',NULL,'https://common.health.kr/shared/images/ext_images/pack_img/P_8806541000910.jpg','유시락스시럽','졸림, 입마름, 어지러움, 집중력 저하, 항콜린성 증상','항히스타민 진정 효과를 이용해 불안과 긴장을 낮추는 후보 약물이다. 의존성은 벤조디아제핀보다 부담이 적지만 졸림과 집중력 저하가 ADHD 증상 평가를 흐릴 수 있다.','불안/긴장: 성인 50-100mg 1일 4회. 6세 미만 50mg/day 분할, 6세 이상 50-100mg/day 분할'),
('상황성 불안 보조제','https://health.kr/searchDrug/result_drug.asp?drug_cd=A11ABBBBB0903','심박 증가, 떨림 등 신체 불안 증상 완화','정제','propranolol',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11ABBBBB090302.jpg','인데놀정','서맥, 저혈압, 피로, 어지러움, 천식/기관지수축 주의','발표, 시험, 면접 같은 상황에서 심장 두근거림과 떨림 같은 신체 불안 증상을 낮추는 보조 후보 약물이다. 핵심 불안 사고나 공황장애 장기치료 약은 아니다.','불안: 40mg 1일 1회, 필요 시 40mg 1일 3회까지. 급성 상황성 불안은 40mg/day 단기 사용 참고'),
('수면/우울 보조 항우울제','https://health.kr/searchDrug/result_drug.asp?drug_cd=A11ABBBBB1118','우울, 불면 동반 불안 완화 보조','정제','mirtazapine',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11ABBBBB111802.jpg','레메론정','졸림, 식욕 증가, 체중 증가, 입마름, 어지러움','불안 자체보다 우울, 식욕저하, 불면이 같이 있을 때 후보가 될 수 있다. 졸림과 식욕 증가가 특징이라 ADHD 자극제의 불면과 식욕저하가 부담될 때 보조적으로 검토될 수 있다.','보통 15-45mg/day'),
('수면/우울 보조 항우울제','https://www.health.kr/searchDrug/result_drug.asp?drug_cd=A11A0300A0250','우울, 불면 증상 완화 보조','정제','trazodone',NULL,'https://common.health.kr/shared/images/sb_photo/big3/A11A0300A025002.jpg','트리티코정','졸림, 어지러움, 입마름, 기립성 저혈압, 드물게 지속발기증','우울과 불면이 동반될 때 수면 보조 목적으로 검토되는 항우울제다. ADHD 자극제 복용 후 불면이 문제가 되는 경우 후보가 될 수 있으나, 다음 날 졸림과 기립성 저혈압을 확인해야 한다.','우울증 라벨 기준 150mg/day 분할 시작, 3-4일마다 50mg 증량. 외래 최대 400mg/day, 입원 최대 600mg/day');

-- ============================================================
-- 2. medication_dosages
-- ============================================================
INSERT INTO `medication_dosages` (`medication_id`, `amount`, `is_active`) VALUES
(1,18.00,TRUE),(1,27.00,TRUE),(1,36.00,TRUE),(1,54.00,TRUE),
(2,5.00,TRUE),(2,10.00,TRUE),(2,20.00,TRUE),(2,30.00,TRUE),(2,40.00,TRUE),
(3,5.00,TRUE),
(4,10.00,TRUE),(4,20.00,TRUE),(4,30.00,TRUE),(4,40.00,TRUE),(4,50.00,TRUE),(4,60.00,TRUE),
(5,10.00,TRUE),(5,30.00,TRUE),(5,50.00,TRUE),(5,60.00,TRUE),
(6,40.00,TRUE),(6,80.00,TRUE),
(7,10.00,TRUE),(7,18.00,TRUE),(7,25.00,TRUE),(7,40.00,TRUE),(7,60.00,TRUE),(7,80.00,TRUE),
(8,10.00,TRUE),(8,18.00,TRUE),(8,25.00,TRUE),(8,40.00,TRUE),
(9,18.00,TRUE),(9,25.00,TRUE),(9,40.00,TRUE),
(10,25.00,TRUE),(10,40.00,TRUE),
(11,10.00,TRUE),(11,40.00,TRUE),
(12,10.00,TRUE),(12,18.00,TRUE),(12,25.00,TRUE),(12,40.00,TRUE),(12,60.00,TRUE),(12,80.00,TRUE),
(13,5.00,TRUE),
(14,10.00,TRUE),(14,20.00,TRUE),(14,30.00,TRUE),(14,40.00,TRUE),
(15,2.50,TRUE),(15,10.00,TRUE),
(16,5.00,TRUE),(16,10.00,TRUE),
(17,10.00,TRUE),(17,15.00,TRUE),(17,20.00,TRUE),(17,30.00,TRUE),
(18,20.00,TRUE),
(19,20.00,TRUE),
(20,17.30,TRUE),
(21,20.00,TRUE),
(22,10.00,TRUE),
(23,39.20,TRUE),(23,26.10,TRUE),(23,52.30,TRUE),
(24,2.50,TRUE),(24,5.00,TRUE),
(25,10.00,TRUE),(25,20.00,TRUE),
(26,30.00,TRUE),
(27,5.00,TRUE),
(28,2.50,TRUE),(28,5.00,TRUE),
(29,2.50,TRUE),(29,5.00,TRUE),
(30,2.50,TRUE),(30,5.00,TRUE),
(31,12.50,TRUE),
(32,5.00,TRUE),
(33,1.00,TRUE),
(34,0.10,TRUE),(34,0.20,TRUE),
(35,100.00,TRUE),(35,200.00,TRUE),
(36,10.00,TRUE),
(37,25.00,TRUE),(37,50.00,TRUE),
(38,12.50,TRUE),
(39,10.00,TRUE),(39,20.00,TRUE),
(40,37.50,TRUE),(40,75.00,TRUE),
(41,30.00,TRUE),(41,60.00,TRUE),
(42,5.00,TRUE),(42,7.50,TRUE),
(43,0.25,TRUE),(43,0.50,TRUE),
(44,0.25,TRUE),(44,1.00,TRUE),
(45,1.00,TRUE),(45,2.00,TRUE),(45,3.00,TRUE),(45,4.00,TRUE),
(46,50.00,TRUE),(46,100.00,TRUE),
(47,40.00,TRUE),
(48,15.00,TRUE),
(49,50.00,TRUE),(49,150.00,TRUE);

-- ============================================================
-- 3. journal_tags (SYSTEM 기본 태그 46개)
--    condition: 14개 / side_effect: 13개 / trouble: 19개
--    INSERT IGNORE: 중복 실행 시 unique constraint(owner_key, category, name, tag_type) 위반 행 스킵
-- ============================================================
INSERT IGNORE INTO `journal_tags`
  (`category`, `name`, `tag_type`, `scope`, `owner_user_id`, `owner_key`,
   `is_active`, `default_visible`, `created_at`, `updated_at`)
VALUES
-- CONDITION (14)
('CONDITION','평온',       'CALM',  'SYSTEM',NULL,0x00000000000000000000000000000000,1,1,NOW(6),NOW(6)),
('CONDITION','차분함',     'CALM',  'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('CONDITION','에너지 많음','UP',    'SYSTEM',NULL,0x00000000000000000000000000000000,1,1,NOW(6),NOW(6)),
('CONDITION','들뜸',       'UP',    'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('CONDITION','말이 많아짐','UP',    'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('CONDITION','의욕 없음',  'DOWN',  'SYSTEM',NULL,0x00000000000000000000000000000000,1,1,NOW(6),NOW(6)),
('CONDITION','피곤함',     'DOWN',  'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('CONDITION','몸이 무거움','DOWN',  'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('CONDITION','불안함',     'TIGHT', 'SYSTEM',NULL,0x00000000000000000000000000000000,1,1,NOW(6),NOW(6)),
('CONDITION','초조함',     'TIGHT', 'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('CONDITION','예민함',     'TIGHT', 'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('CONDITION','머리 멍함',  'FOGGY', 'SYSTEM',NULL,0x00000000000000000000000000000000,1,1,NOW(6),NOW(6)),
('CONDITION','생각이 끊김','FOGGY', 'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('CONDITION','집중이 흐려짐','FOGGY','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
-- SIDE_EFFECT (13)
('SIDE_EFFECT','식욕 저하',  'NONE','SYSTEM',NULL,0x00000000000000000000000000000000,1,1,NOW(6),NOW(6)),
('SIDE_EFFECT','입마름',     'NONE','SYSTEM',NULL,0x00000000000000000000000000000000,1,1,NOW(6),NOW(6)),
('SIDE_EFFECT','두통',       'NONE','SYSTEM',NULL,0x00000000000000000000000000000000,1,1,NOW(6),NOW(6)),
('SIDE_EFFECT','메스꺼움',   'NONE','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('SIDE_EFFECT','두근거림',   'NONE','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('SIDE_EFFECT','손 떨림',    'NONE','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('SIDE_EFFECT','불면',       'NONE','SYSTEM',NULL,0x00000000000000000000000000000000,1,1,NOW(6),NOW(6)),
('SIDE_EFFECT','졸림',       'NONE','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('SIDE_EFFECT','피로감',     'NONE','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('SIDE_EFFECT','어지러움',   'NONE','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('SIDE_EFFECT','불안 증가',  'NONE','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('SIDE_EFFECT','예민함',     'NONE','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('SIDE_EFFECT','기분 저하',  'NONE','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
-- TROUBLE (19)
('TROUBLE','딴생각',            'INATTENTION',    'SYSTEM',NULL,0x00000000000000000000000000000000,1,1,NOW(6),NOW(6)),
('TROUBLE','깜빡함',            'INATTENTION',    'SYSTEM',NULL,0x00000000000000000000000000000000,1,1,NOW(6),NOW(6)),
('TROUBLE','설명을 놓침',       'INATTENTION',    'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','중요한 내용을 빠뜨림','INATTENTION',  'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','집중이 끊김',       'INATTENTION',    'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','미룸',              'TIME_MANAGEMENT','SYSTEM',NULL,0x00000000000000000000000000000000,1,1,NOW(6),NOW(6)),
('TROUBLE','시작이 어려움',     'TIME_MANAGEMENT','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','지각함',            'TIME_MANAGEMENT','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','예상보다 오래 걸림','TIME_MANAGEMENT','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','우선순위 못 정함',  'TIME_MANAGEMENT','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','급하게 처리',       'IMPULSIVITY',    'SYSTEM',NULL,0x00000000000000000000000000000000,1,1,NOW(6),NOW(6)),
('TROUBLE','확인 전 제출함',    'IMPULSIVITY',    'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','감정적으로 반응함', 'IMPULSIVITY',    'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','가만히 있기 어려움','HYPERACTIVITY',  'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','말이 많아짐',       'HYPERACTIVITY',  'SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','숫자/단위 실수',    'COGNITIVE_ERROR','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','날짜/일정 착각',    'COGNITIVE_ERROR','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','항목 혼동',         'COGNITIVE_ERROR','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6)),
('TROUBLE','순서를 헷갈림',     'COGNITIVE_ERROR','SYSTEM',NULL,0x00000000000000000000000000000000,1,0,NOW(6),NOW(6));

-- ============================================================
-- 4. terms
-- 개발/초기 배포용 약관 데이터입니다.
-- 운영 반영 전 법무 검토를 거친 최종 문구로 교체해야 합니다.
-- 동일한 type + version 데이터가 있으면 중복 삽입하지 않습니다.
-- ============================================================
INSERT INTO `terms` (`version`, `type`, `content`, `effective_at`, `created_at`)
SELECT
    1,
    'TERMS_OF_SERVICE',
    CONCAT(
        'Attune 서비스 이용약관', CHAR(10), CHAR(10),
        '1. 목적', CHAR(10),
        '본 약관은 Attune이 제공하는 복약 기록, 상태 기록, 일정 및 리포트 등 서비스의 이용 조건을 정합니다.', CHAR(10), CHAR(10),
        '2. 서비스의 성격', CHAR(10),
        'Attune이 제공하는 정보와 분석 결과는 건강 관리를 돕기 위한 참고 자료이며, 의료인의 진단·처방 또는 치료를 대신하지 않습니다.', CHAR(10), CHAR(10),
        '3. 회원의 의무', CHAR(10),
        '회원은 정확한 정보를 입력하고 계정 정보를 안전하게 관리해야 하며, 서비스를 위법하거나 부정한 목적으로 이용해서는 안 됩니다.', CHAR(10), CHAR(10),
        '4. 서비스의 변경 및 중단', CHAR(10),
        '운영상 또는 기술상 필요한 경우 서비스의 전부 또는 일부가 변경되거나 일시 중단될 수 있습니다.', CHAR(10), CHAR(10),
        '5. 회원 탈퇴', CHAR(10),
        '회원은 언제든지 서비스에서 탈퇴할 수 있으며, 관련 법령 및 개인정보 처리방침에 따라 데이터가 처리됩니다.'
    ),
    '2026-06-23 00:00:00',
    NOW(6)
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `terms`
    WHERE `type` = 'TERMS_OF_SERVICE'
      AND `version` = 1
);

INSERT INTO `terms` (`version`, `type`, `content`, `effective_at`, `created_at`)
SELECT
    1,
    'PRIVACY_POLICY',
    CONCAT(
        'Attune 개인정보 처리방침', CHAR(10), CHAR(10),
        '1. 수집 항목', CHAR(10),
        'Attune은 회원가입 및 서비스 제공을 위해 이메일, 닉네임, 로그인 제공자 정보와 회원이 입력한 복약·상태·일정·메모 정보를 처리할 수 있습니다.', CHAR(10), CHAR(10),
        '2. 이용 목적', CHAR(10),
        '회원 식별, 계정 관리, 복약 및 기록 서비스 제공, 맞춤형 리포트 생성, 알림 발송, 문의 대응 및 서비스 개선을 위해 개인정보를 이용합니다.', CHAR(10), CHAR(10),
        '3. 보유 및 이용 기간', CHAR(10),
        '개인정보는 회원 탈퇴 시 지체 없이 파기합니다. 다만 관련 법령에 따라 보존할 의무가 있는 정보는 해당 기간 동안 별도로 보관합니다.', CHAR(10), CHAR(10),
        '4. 제3자 제공 및 처리 위탁', CHAR(10),
        '서비스 제공에 필요한 경우 외부 인프라, 이메일·알림 또는 AI 처리 사업자에게 업무를 위탁할 수 있으며, 세부 내용은 운영 정책에 따라 고지합니다.', CHAR(10), CHAR(10),
        '5. 이용자의 권리', CHAR(10),
        '이용자는 자신의 개인정보에 대한 열람, 정정, 삭제, 처리정지 및 동의 철회를 요청할 수 있습니다.'
    ),
    '2026-06-23 00:00:00',
    NOW(6)
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `terms`
    WHERE `type` = 'PRIVACY_POLICY'
      AND `version` = 1
);

INSERT INTO `terms` (`version`, `type`, `content`, `effective_at`, `created_at`)
SELECT
    1,
    'MARKETING_CONSENT',
    CONCAT(
        'Attune 마케팅 정보 수신 동의', CHAR(10), CHAR(10),
        'Attune의 신규 기능, 이벤트, 혜택 및 서비스 관련 광고성 정보를 이메일 또는 푸시 알림으로 수신하는 데 동의합니다.', CHAR(10),
        '본 동의는 선택 사항이며, 동의하지 않아도 기본 서비스를 이용할 수 있습니다.', CHAR(10),
        '동의 후에도 설정 화면을 통해 언제든지 수신 동의를 철회할 수 있습니다.'
    ),
    '2026-06-23 00:00:00',
    NOW(6)
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `terms`
    WHERE `type` = 'MARKETING_CONSENT'
      AND `version` = 1
);

INSERT INTO `terms` (`version`, `type`, `content`, `effective_at`, `created_at`)
SELECT
    1,
    'AI_ANALYSIS_CONSENT',
    CONCAT(
        'Attune AI 분석 기능 이용 동의', CHAR(10), CHAR(10),
        '1. 처리 목적', CHAR(10),
        '회원이 기록한 복약 내역, 상태, 증상 및 생활 기록을 분석하여 약물 치료 경과 리포트와 참고용 인사이트를 제공합니다.', CHAR(10), CHAR(10),
        '2. 외부 AI 처리', CHAR(10),
        'AI 분석을 위해 필요한 기록 데이터가 Google Gemini 등 외부 AI 서비스로 전송되어 처리될 수 있습니다.', CHAR(10), CHAR(10),
        '3. 유의 사항', CHAR(10),
        'AI 분석 결과는 참고 정보이며 의료인의 진단, 처방 또는 치료를 대체하지 않습니다. 건강 상태나 약물 복용에 관한 결정은 반드시 의료인과 상의해야 합니다.', CHAR(10), CHAR(10),
        '4. 동의 철회', CHAR(10),
        '동의는 선택 사항이며 언제든지 철회할 수 있습니다. 철회 후에는 새로운 AI 분석이 수행되지 않으며 기본 통계 기능은 계속 이용할 수 있습니다.'
    ),
    '2026-06-23 00:00:00',
    NOW(6)
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1
    FROM `terms`
    WHERE `type` = 'AI_ANALYSIS_CONSENT'
      AND `version` = 1
);
