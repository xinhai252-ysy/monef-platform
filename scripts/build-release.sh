#!/usr/bin/env sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
(cd "$PROJECT_DIR/frontend" && pnpm install --frozen-lockfile && pnpm test && VITE_PREVIEW_MODE=false pnpm build)
mkdir -p "$PROJECT_DIR/backend/src/main/resources/static"
cp -R "$PROJECT_DIR/frontend/dist/." "$PROJECT_DIR/backend/src/main/resources/static/"
(cd "$PROJECT_DIR/backend" && mvn verify)
mkdir -p "$PROJECT_DIR/release"
cp "$PROJECT_DIR/backend/target/monef-api-1.0.0.jar" "$PROJECT_DIR/release/"
printf '%s\n' 'Build complete. Run scripts/start-demo.sh or deploy with Docker Compose.'
