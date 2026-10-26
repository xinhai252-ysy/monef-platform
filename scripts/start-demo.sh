#!/usr/bin/env sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
JAR="$PROJECT_DIR/release/monef-api-1.0.0.jar"
if [ ! -f "$JAR" ]; then
  (cd "$PROJECT_DIR/backend" && mvn -q package)
  JAR="$PROJECT_DIR/backend/target/monef-api-1.0.0.jar"
fi
printf '%s\n' 'Monef: http://127.0.0.1:8080' 'Demo login: demo@monef.local / MonefDemo!2026'
cd "$PROJECT_DIR/backend"
exec java -jar "$JAR" --spring.profiles.active=demo
