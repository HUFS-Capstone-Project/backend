# syntax=docker/dockerfile:1
# Render 배포용 Dockerfile

FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

# 1) Gradle 래퍼·빌드 설정을 먼저 복사해 Docker 레이어 캐시 활용
COPY gradlew gradlew
COPY settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

# 2) 소스 복사 후 bootJar 빌드(배포 속도를 위해 테스트 생략)
COPY src ./src
COPY config ./config
RUN ./gradlew --no-daemon clean bootJar -x test

# 3) 실행 JAR 파일명을 고정하지 않고 boot JAR 하나로 정규화
RUN set -eux; \
    jar_path="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | head -n 1)"; \
    test -n "$jar_path"; \
    cp "$jar_path" build/app.jar

FROM eclipse-temurin:21-jre-jammy AS runner
WORKDIR /app

# 컨테이너 보안을 위해 root가 아닌 사용자로 실행
RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=builder /workspace/build/app.jar /app/app.jar
RUN chown spring:spring /app/app.jar

USER spring:spring

# 웹 서비스가 수신할 포트
EXPOSE 8080
ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod

# ENTRYPOINT로 기동 고정, JVM 옵션은 JAVA_TOOL_OPTIONS 등 환경 변수로 조정
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]