# ------------------------------
# STAGE 1 — Builder
# ------------------------------
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY . .

ARG BUILD_PROFILE=prod
ENV BUILD_PROFILE=$BUILD_PROFILE

RUN ./mvnw clean package -DskipTests -P${BUILD_PROFILE} && \
    cp target/*.jar app.jar

# ------------------------------
# STAGE 2 — Runtime
# ------------------------------
FROM eclipse-temurin:21-jdk AS final
WORKDIR /app

# Installe le client PostgreSQL
RUN apt-get update && apt-get install -y postgresql-client && apt-get clean

# Copie le script de démarrage intelligent
COPY wait-for-postgres.sh .
RUN chmod +x wait-for-postgres.sh

# Copie les sources et/ou jar
COPY --from=builder /app/app.jar ./app.jar
COPY . .

# Port exposé par Spring Boot
EXPOSE 8087

CMD ["./wait-for-postgres.sh"]
