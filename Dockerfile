# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /workspace

ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dkotlin.daemon.enabled=false"

COPY gradlew gradlew
COPY gradle gradle
COPY gradle.properties.docker gradle.properties
COPY settings.docker.gradle.kts settings.gradle.kts
COPY build.gradle.docker.kts build.gradle.kts
COPY core core
COPY server server
COPY core/build.gradle.docker.kts core/build.gradle.kts

RUN rm -f gradle/gradle-daemon-jvm.properties \
    && chmod +x gradlew \
    && ./gradlew :server:shadowJar \
        --no-daemon \
        --no-configuration-cache \
        --no-parallel \
        --max-workers=1 \
        -x test

FROM eclipse-temurin:25-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home towards

COPY --from=build /workspace/server/build/libs/server-all.jar /app/app.jar

USER towards
EXPOSE 8081

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD curl -fsS http://127.0.0.1:8081/health | grep -q '"status":"UP"'

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
