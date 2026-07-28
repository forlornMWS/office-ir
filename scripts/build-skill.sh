#!/usr/bin/env bash
# Builds office-ir.jar and bundles it with the skill's SKILL.md into dist/office-ir/.
# Usage: scripts/build-skill.sh [--tests]   (skips tests by default; pass --tests to run them)
#
# Output: dist/office-ir/office-ir.jar + dist/office-ir/SKILL.md  (a self-contained skill dir)
set -euo pipefail

# Resolve repo root (parent of scripts/).
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Pick a JDK 17. Prefer JAVA_HOME if it points at 17; else probe common locations.
pick_java() {
  if [ -n "${JAVA_HOME:-}" ]; then
    if "$JAVA_HOME/bin/java" -version 2>&1 | head -1 | grep -q '"17'; then
      echo "$JAVA_HOME"; return
    fi
  fi
  for c in /d/Program/java/java17 /c/Program/java/java17 /d/Program/java/jdk-17 /c/Program/Files/Java/jdk-17; do
    [ -x "$c/bin/java" ] && "$c/bin/java" -version 2>&1 | head -1 | grep -q '"17' && { echo "$c"; return; }
  done
  echo ""
}

JAVA17="$(pick_java || true)"
if [ -z "$JAVA17" ]; then
  echo "ERROR: no JDK 17 found. Set JAVA_HOME to a JDK 17 install." >&2
  exit 1
fi
export JAVA_HOME="$JAVA17"
echo "Using JAVA_HOME=$JAVA17"

# Build.
GOAL="package"
[ "${1:-}" = "--tests" ] || GOAL="package -DskipTests"
( cd "$ROOT" && mvn -o clean $GOAL )

JAR="$ROOT/target/office-ir.jar"
if [ ! -f "$JAR" ]; then
  echo "ERROR: build did not produce $JAR" >&2
  exit 1
fi

# Bundle jar + SKILL.md into a self-contained skill dir.
DIST="$ROOT/dist/office-ir"
mkdir -p "$DIST"
cp "$JAR" "$DIST/office-ir.jar"
cp "$ROOT/skills/office-ir/SKILL.md" "$DIST/SKILL.md"

echo
echo "Bundled skill to: $DIST"
echo "  $DIST/office-ir.jar"
echo "  $DIST/SKILL.md"
echo
echo "Run example:"
echo "  \"$JAVA17/bin/java\" -jar \"$DIST/office-ir.jar\" input.xlsx"
