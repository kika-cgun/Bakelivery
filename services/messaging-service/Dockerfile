# Build stage — runs inside Docker, no pre-built JAR needed
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle/ gradle/
COPY libs/bakelivery-common/ libs/bakelivery-common/
COPY services/messaging-service/ services/messaging-service/
RUN ./gradlew :services:messaging-service:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/messaging-service/build/libs/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java","-jar","/app/app.jar"]
