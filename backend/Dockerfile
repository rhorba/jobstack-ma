FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw package -q -DskipTests

FROM eclipse-temurin:25-jre-alpine
RUN apk upgrade --no-cache \
    && apk add --no-cache curl \
    && addgroup -S app && adduser -S -G app app \
    && mkdir -p /data/cvs && chown app:app /data/cvs
WORKDIR /app
COPY --from=builder --chown=app:app /app/target/*.jar app.jar
USER app
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s CMD curl -sf http://localhost:8080/actuator/health || exit 1
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
