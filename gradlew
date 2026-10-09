#!/usr/bin/env sh
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ -f "$WRAPPER_JAR" ]; then
  exec java -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
fi
GRADLE_VERSION=8.7
CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gooskers-bootstrap/gradle-$GRADLE_VERSION"
DIST="$CACHE_DIR/gradle-$GRADLE_VERSION-bin.zip"
HOME_DIR="$CACHE_DIR/gradle-$GRADLE_VERSION"
mkdir -p "$CACHE_DIR"
if [ ! -x "$HOME_DIR/bin/gradle" ]; then
  if [ ! -f "$DIST" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$DIST"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$DIST" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    else
      echo "curl or wget is required for first-time Gradle bootstrap." >&2
      exit 1
    fi
  fi
  rm -rf "$HOME_DIR"
  unzip -q "$DIST" -d "$CACHE_DIR"
fi
exec "$HOME_DIR/bin/gradle" -p "$APP_HOME" "$@"
