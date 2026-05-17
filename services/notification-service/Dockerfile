# Build stage — Gradle needs all service dirs present (settings.gradle references all of them)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
COPY libs/ libs/
COPY services/ services/
RUN ./gradlew :services:notification-service:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/notification-service/build/libs/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java","-jar","/app/app.jar"]
