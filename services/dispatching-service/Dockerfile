# Build stage — runs inside Docker, no pre-built JAR needed
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
COPY libs/bakelivery-common/ libs/bakelivery-common/
COPY services/dispatching-service/ services/dispatching-service/
RUN ./gradlew :services:dispatching-service:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/dispatching-service/build/libs/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","/app/app.jar"]
