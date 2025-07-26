#!/bin/sh

set -e

DB_HOST="agent-db"
DB_PORT="5432"
DB_USER="user"
SPRING_PROFILE="${SPRING_PROFILES_ACTIVE:-default}"

echo "⏳ Attente de PostgreSQL sur $DB_HOST:$DB_PORT..."

until pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER"; do
  sleep 1
done

echo "✅ PostgreSQL est prêt"
echo "🚀 Lancement de l'application avec le profil Spring : $SPRING_PROFILE"

if [ -f "app.jar" ]; then
  exec java -jar app.jar --spring.profiles.active=$SPRING_PROFILE
else
  # On est sûrement en mode dev : fallback vers spring-boot:run
  echo "⚠️ app.jar introuvable, démarrage avec ./mvnw spring-boot:run"
  exec ./mvnw spring-boot:run -Dspring-boot.run.fork=false -Dspring-boot.run.profiles=$SPRING_PROFILE -Dspring.devtools.restart.enabled=true -Dspring.devtools.livereload.enabled=true
fi
