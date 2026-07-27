#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

APP_KEY=""
APP_LABEL=""
PROJECT_PATH=""
SCHEME=""
DERIVED_DATA_PATH=""

SIMULATOR_NAMES=()
SIMULATOR_UDIDS=()
SIMULATOR_STATES=()
SIMULATOR_RUNTIMES=()
PROMPT_SELECTION_INDEX=""
SELECTED_SIMULATOR_UDID=""

require_command() {
  local command_name="$1"
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "Missing required command: $command_name" >&2
    exit 1
  fi
}

choose_application() {
  while true; do
    cat <<'EOF'
Which application do you want to run?
1) sample
2) recipes
3) starter blueprint
4) list-detail blueprint
EOF
    printf "Enter selection [1-4]: "
    read -r selection

    case "$selection" in
      1)
        APP_KEY="sample"
        APP_LABEL="sample"
        PROJECT_PATH="$ROOT_DIR/sample/iosApp/iosApp.xcodeproj"
        SCHEME="iosApp"
        DERIVED_DATA_PATH="/tmp/app-platform-ios-run-sample"
        return
        ;;
      2)
        APP_KEY="recipes"
        APP_LABEL="recipes"
        PROJECT_PATH="$ROOT_DIR/recipes/recipesIosApp/recipesIosApp.xcodeproj"
        SCHEME="recipesIosApp"
        DERIVED_DATA_PATH="/tmp/app-platform-ios-run-recipes"
        return
        ;;
      3)
        APP_KEY="starter"
        APP_LABEL="starter blueprint"
        PROJECT_PATH="$ROOT_DIR/blueprints/starter/iosApp/iosApp.xcodeproj"
        SCHEME="iosApp"
        DERIVED_DATA_PATH="/tmp/app-platform-ios-run-starter"
        return
        ;;
      4)
        APP_KEY="list-detail"
        APP_LABEL="list-detail blueprint"
        PROJECT_PATH="$ROOT_DIR/blueprints/list-detail/app/ios/iosApp.xcodeproj"
        SCHEME="iosApp"
        DERIVED_DATA_PATH="/tmp/app-platform-ios-run-list-detail"
        return
        ;;
      *)
        echo "Invalid selection."
        ;;
    esac
  done
}

load_simulators() {
  local mode="$1"
  local current_runtime=""
  local line=""
  local parsed_line=""
  local device_name=""
  local device_udid=""
  local device_state=""

  SIMULATOR_NAMES=()
  SIMULATOR_UDIDS=()
  SIMULATOR_STATES=()
  SIMULATOR_RUNTIMES=()

  if [[ "$mode" == "available" ]]; then
    while IFS= read -r line; do
      parsed_line="$(printf '%s\n' "$line" | sed -nE 's/^-- (.+) --$/\1/p')"
      if [[ -n "$parsed_line" ]]; then
        current_runtime="$parsed_line"
        continue
      fi

      if [[ "$current_runtime" != iOS* ]]; then
        continue
      fi

      parsed_line="$(printf '%s\n' "$line" | sed -nE 's/^[[:space:]]+(.+) \(([A-F0-9-]+)\) \(([^)]+)\)[[:space:]]*$/\1\t\2\t\3/p')"
      if [[ -n "$parsed_line" ]]; then
        IFS=$'\t' read -r device_name device_udid device_state <<<"$parsed_line"
        SIMULATOR_NAMES+=("$device_name")
        SIMULATOR_UDIDS+=("$device_udid")
        SIMULATOR_STATES+=("$device_state")
        SIMULATOR_RUNTIMES+=("$current_runtime")
      fi
    done < <(xcrun simctl list devices available)
  else
    while IFS= read -r line; do
      parsed_line="$(printf '%s\n' "$line" | sed -nE 's/^-- (.+) --$/\1/p')"
      if [[ -n "$parsed_line" ]]; then
        current_runtime="$parsed_line"
        continue
      fi

      if [[ "$current_runtime" != iOS* ]]; then
        continue
      fi

      parsed_line="$(printf '%s\n' "$line" | sed -nE 's/^[[:space:]]+(.+) \(([A-F0-9-]+)\) \(([^)]+)\)[[:space:]]*$/\1\t\2\t\3/p')"
      if [[ -n "$parsed_line" ]]; then
        IFS=$'\t' read -r device_name device_udid device_state <<<"$parsed_line"
        if [[ "$device_state" != "Booted" ]]; then
          continue
        fi
        SIMULATOR_NAMES+=("$device_name")
        SIMULATOR_UDIDS+=("$device_udid")
        SIMULATOR_STATES+=("$device_state")
        SIMULATOR_RUNTIMES+=("$current_runtime")
      fi
    done < <(xcrun simctl list devices)
  fi
}

prompt_for_simulator_index() {
  local prompt="$1"
  local max_index="${#SIMULATOR_NAMES[@]}"
  local i=""
  local selection=""

  if (( max_index == 0 )); then
    echo "No matching simulators found." >&2
    exit 1
  fi

  echo "$prompt" >&2
  for (( i = 0; i < max_index; i++ )); do
    printf "%d) %s [%s] (%s)\n" \
      "$((i + 1))" \
      "${SIMULATOR_NAMES[$i]}" \
      "${SIMULATOR_RUNTIMES[$i]}" \
      "${SIMULATOR_STATES[$i]}" >&2
  done

  while true; do
    printf "Enter selection [1-%d]: " "$max_index" >&2
    read -r selection
    if [[ "$selection" =~ ^[0-9]+$ ]] && (( selection >= 1 && selection <= max_index )); then
      PROMPT_SELECTION_INDEX="$((selection - 1))"
      return
    fi
    echo "Invalid selection." >&2
  done
}

choose_simulator() {
  local selected_udid=""

  load_simulators "booted"

  case "${#SIMULATOR_UDIDS[@]}" in
    0)
      load_simulators "available"
      prompt_for_simulator_index "No simulator is booted. Which simulator should be booted?"
      selected_udid="${SIMULATOR_UDIDS[$PROMPT_SELECTION_INDEX]}"
      echo "Booting ${SIMULATOR_NAMES[$PROMPT_SELECTION_INDEX]}..." >&2
      xcrun simctl boot "$selected_udid"
      open -a Simulator --args -CurrentDeviceUDID "$selected_udid" >/dev/null 2>&1 || open -a Simulator >/dev/null 2>&1 || true
      xcrun simctl bootstatus "$selected_udid" -b
      SELECTED_SIMULATOR_UDID="$selected_udid"
      ;;
    1)
      echo "Using booted simulator: ${SIMULATOR_NAMES[0]} [${SIMULATOR_RUNTIMES[0]}]" >&2
      SELECTED_SIMULATOR_UDID="${SIMULATOR_UDIDS[0]}"
      ;;
    *)
      prompt_for_simulator_index "More than one simulator is booted. Which one should be used?"
      SELECTED_SIMULATOR_UDID="${SIMULATOR_UDIDS[$PROMPT_SELECTION_INDEX]}"
      ;;
  esac
}

find_built_app() {
  local products_dir="$1/Build/Products/Debug-iphonesimulator"
  find "$products_dir" -maxdepth 1 -type d -name '*.app' | head -n 1
}

main() {
  local app_path=""
  local bundle_id=""

  if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "This script requires macOS." >&2
    exit 1
  fi

  require_command xcodebuild
  require_command xcrun
  require_command open
  require_command /usr/libexec/PlistBuddy

  choose_application
  choose_simulator

  echo "Building $APP_LABEL for simulator $SELECTED_SIMULATOR_UDID..."
  xcodebuild \
    -project "$PROJECT_PATH" \
    -scheme "$SCHEME" \
    -configuration Debug \
    -destination "platform=iOS Simulator,id=$SELECTED_SIMULATOR_UDID" \
    -derivedDataPath "$DERIVED_DATA_PATH" \
    build

  app_path="$(find_built_app "$DERIVED_DATA_PATH")"
  if [[ -z "$app_path" ]]; then
    echo "Could not find the built .app bundle in $DERIVED_DATA_PATH." >&2
    exit 1
  fi

  bundle_id="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$app_path/Info.plist")"

  echo "Installing $app_path..."
  xcrun simctl install "$SELECTED_SIMULATOR_UDID" "$app_path"

  echo "Launching $bundle_id..."
  xcrun simctl launch "$SELECTED_SIMULATOR_UDID" "$bundle_id"
}

main "$@"
