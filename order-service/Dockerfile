# =============================================================================
# STAGE 1 - BUILD
# =============================================================================
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# =============================================================================
# STAGE 2 - RUNTIME
# =============================================================================
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

COPY --from=build /build/target/order-service.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD wget --quiet --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
