FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:resolve dependency:resolve-plugins

COPY src ./src
RUN mvn -B -DskipTests package \
    && mv target/docentia-*.jar /app/app.jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S docentia \
    && adduser -S docentia -G docentia

COPY --from=builder --chown=docentia:docentia /app/app.jar /app/app.jar

USER docentia

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
