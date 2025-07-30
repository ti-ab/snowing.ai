FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY . .

ARG BUILD_PROFILE=prod
ENV BUILD_PROFILE=$BUILD_PROFILE

RUN sed -i 's/\r$//' mvnw
RUN ./mvnw clean package -DskipTests -P${BUILD_PROFILE} && \
    cp target/*.jar app.jar

# ---------------------------------
FROM eclipse-temurin:21-jdk AS final
WORKDIR /app

RUN apt-get update && apt-get install -y postgresql-client && apt-get clean

# Copie le script AVANT le reste
COPY wait-for-postgres.sh /app/wait-for-postgres.sh
RUN sed -i 's/\r$//' /app/wait-for-postgres.sh
RUN chmod +x /app/wait-for-postgres.sh

COPY --from=builder /app/app.jar /app/app.jar

# ⚠️ évite de faire COPY . . ici sauf si nécessaire

EXPOSE 8082

CMD ["./wait-for-postgres.sh"]
