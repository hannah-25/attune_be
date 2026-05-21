# 1. 빌드된 jar를 실행할 가벼운 자바 17 환경 베이스 이미지 선택
FROM openjdk:17-jdk-slim

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 빌드된 JAR 파일을 컨테이너 내부로 복사
COPY build/libs/*.jar app.jar

# 4. 컨테이너가 켜질 때 스프링 부트 실행 명령 지정
ENTRYPOINT ["java", "-jar", "app.jar"]