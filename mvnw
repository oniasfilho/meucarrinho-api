#!/bin/sh

set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WRAPPER_DIR="$BASE_DIR/.mvn/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
WRAPPER_VERSION=3.3.4

if [ ! -f "$WRAPPER_JAR" ]; then
  WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/${WRAPPER_VERSION}/maven-wrapper-${WRAPPER_VERSION}.jar"
  mkdir -p "$WRAPPER_DIR"
  if command -v curl >/dev/null 2>&1; then
    curl --fail --location --silent --show-error "$WRAPPER_URL" --output "$WRAPPER_JAR"
  elif command -v wget >/dev/null 2>&1; then
    wget --quiet "$WRAPPER_URL" --output-document="$WRAPPER_JAR"
  else
    echo "Error: curl or wget is required to download the Maven Wrapper." >&2
    exit 1
  fi
fi

exec java \
  -Dmaven.multiModuleProjectDirectory="$BASE_DIR" \
  -classpath "$WRAPPER_JAR" \
  org.apache.maven.wrapper.MavenWrapperMain "$@"

