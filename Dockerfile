FROM eclipse-temurin:17-jdk-alpine

ENV TZ=Asia/Seoul

WORKDIR /app

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]