FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 로그 디렉토리 생성 (application-prod.yml의 logging.file.name 경로)
RUN mkdir -p /var/log/attune-me

COPY build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
