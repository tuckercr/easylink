#!/usr/bin/env bash
# Wipes the debug app's state and reinstalls it — the two ways to get back to
# a "fresh install" for testing:
#
#   ./scripts/fresh-install.sh          uninstall + reinstall (real fresh
#                                        install: exercises Auto Backup
#                                        restore, matches what a new user sees)
#   ./scripts/fresh-install.sh --clear  pm clear only (faster: wipes app data
#                                        and revokes permissions, but skips
#                                        the backup-restore path)
#
# Flags:
#   -f, --flavor <standard|safety>   which product flavor to install (default: standard)
#   -n, --no-build                   skip the Gradle build, install the APK already on disk
#   -s, --serial <device-id>         target a specific device/emulator (adb -s)
#
# Requires exactly one adb device connected unless -s is given.

set -euo pipefail

MODE="reinstall"
FLAVOR="standard"
DO_BUILD=1
SERIAL=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --clear)
            MODE="clear"
            shift
            ;;
        -f|--flavor)
            FLAVOR="$2"
            shift 2
            ;;
        -n|--no-build)
            DO_BUILD=0
            shift
            ;;
        -s|--serial)
            SERIAL="$2"
            shift 2
            ;;
        -h|--help)
            grep '^#' "$0" | sed 's/^# \{0,1\}//'
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            exit 1
            ;;
    esac
done

if [[ "$FLAVOR" != "standard" && "$FLAVOR" != "safety" ]]; then
    echo "Unknown flavor '$FLAVOR' — expected 'standard' or 'safety'" >&2
    exit 1
fi

PACKAGE="com.fangjet.launcher.debug"
APK="app/build/outputs/apk/${FLAVOR}/debug/app-${FLAVOR}-debug.apk"
ACTIVITY="com.fangjet.launcher.presentation.main.MainActivity"

cd "$(dirname "$0")/.."

ADB=(adb)
if [[ -n "$SERIAL" ]]; then
    ADB=(adb -s "$SERIAL")
fi

if [[ $DO_BUILD -eq 1 ]]; then
    echo "==> Building ${FLAVOR}Debug"
    ./gradlew "assemble$(tr '[:lower:]' '[:upper:]' <<< "${FLAVOR:0:1}")${FLAVOR:1}Debug" -q
fi

if [[ "$MODE" == "clear" ]]; then
    echo "==> pm clear ${PACKAGE}"
    "${ADB[@]}" shell pm clear "$PACKAGE"
else
    echo "==> Uninstalling ${PACKAGE} (ignore 'not installed' on first run)"
    "${ADB[@]}" uninstall "$PACKAGE" || true
    echo "==> Installing ${APK}"
    "${ADB[@]}" install "$APK"
fi

echo "==> Launching"
"${ADB[@]}" shell am start -n "${PACKAGE}/${ACTIVITY}"

echo "==> Done"
