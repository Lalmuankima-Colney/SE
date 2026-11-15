#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper script for Unix
# ----------------------------------------------------------------------------

set -e

MAVEN_VERSION="3.9.6"
MAVEN_DIST_DIR="$HOME/.m2/wrapper/dists/apache-maven-$MAVEN_VERSION-bin"
MAVEN_HOME="$MAVEN_DIST_DIR/apache-maven-$MAVEN_VERSION"
MAVEN_CMD="$MAVEN_HOME/bin/mvn"

if [ ! -x "$MAVEN_CMD" ]; then
    echo "Installing Maven $MAVEN_VERSION wrapper..."
    mkdir -p "$MAVEN_DIST_DIR"
    ZIP_PATH="$MAVEN_DIST_DIR/apache-maven-$MAVEN_VERSION-bin.zip"
    DOWNLOAD_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"
    
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "$DOWNLOAD_URL" -o "$ZIP_PATH"
    elif command -v wget >/dev/null 2>&1; then
        wget -q "$DOWNLOAD_URL" -O "$ZIP_PATH"
    else
        echo "Error: curl or wget required to download Maven wrapper."
        exit 1
    fi
    
    unzip -q "$ZIP_PATH" -d "$MAVEN_DIST_DIR"
    rm -f "$ZIP_PATH"
    chmod +x "$MAVEN_CMD"
fi

exec "$MAVEN_CMD" "$@"
