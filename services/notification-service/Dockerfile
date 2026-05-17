# Build stage — runs inside Docker, no pre-built JAR needed
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
COPY libs/bakelivery-common/ libs/bakelivery-common/
COPY services/notification-service/ services/notification-service/
RUN ./gradlew :services:notification-service:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/notification-service/build/libs/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java","-jar","/app/app.jar"]
