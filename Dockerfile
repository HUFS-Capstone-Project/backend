# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle ./
RUN chmod +x ./gradlew

COPY src/main ./src/main

RUN --mount=type=cache,id=gradle-wrapper,target=/root/.gradle/wrapper,sharing=locked \
    --mount=type=cache,id=gradle-caches,target=/root/.gradle/caches,sharing=locked \
    ./gradlew --no-daemon bootJar -x test && \
    set -eux; \
    jar_path="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)"; \
    test -n "$jar_path"; \
    cp "$jar_path" /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy AS runner
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=builder --chown=spring:spring /workspace/app.jar /app/app.jar

USER spring:spring

EXPOSE 8080
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
