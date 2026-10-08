#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

KTFMT_VERSION="$(sed -n 's/^ktfmt = "\(.*\)"$/\1/p' gradle/libs.versions.toml)"
if [[ -z "$KTFMT_VERSION" ]]; then
  echo "Could not find ktfmt version in gradle/libs.versions.toml" >&2
  exit 1
fi

if [[ -z "${KTFMT_JAR:-}" ]]; then
  KTFMT_CACHE_DIR="${KTFMT_CACHE_DIR:-${RUNNER_TEMP:-$ROOT_DIR/.gradle/ktfmt}}"
  mkdir -p "$KTFMT_CACHE_DIR"
  KTFMT_JAR="$KTFMT_CACHE_DIR/ktfmt-${KTFMT_VERSION}-with-dependencies.jar"

  if [[ ! -f "$KTFMT_JAR" ]]; then
    curl -fsSL \
      "https://github.com/Kotlin/ktfmt/releases/download/v${KTFMT_VERSION}/ktfmt-${KTFMT_VERSION}-with-dependencies.jar" \
      -o "$KTFMT_JAR"
  fi
fi

files=()
while IFS= read -r file; do
  files+=("$file")
done < <(
  git ls-files -- \
    "*.kt" \
    ":!metro-extensions/contribute/impl-compiler-plugin/src/test/resources/**"
)

if [[ ${#files[@]} -eq 0 ]]; then
  exit 0
fi

exec java -jar "$KTFMT_JAR" --google-style "$@" "${files[@]}"
