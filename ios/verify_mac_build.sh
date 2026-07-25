#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"

if ! command -v xcodebuild >/dev/null 2>&1; then
  echo "xcodebuild was not found. Install Xcode 16+ and select it with xcode-select." >&2
  exit 1
fi

xcode_version="$(xcodebuild -version | awk '/^Xcode / { print $2 }')"
xcode_major="${xcode_version%%.*}"
case "$xcode_major" in
  ''|*[!0-9]*)
    echo "Unable to determine the selected Xcode version." >&2
    exit 1
    ;;
esac
if [ "$xcode_major" -lt 16 ]; then
  echo "Xcode 16+ is required; selected version is $xcode_version." >&2
  exit 1
fi

echo "[1/3] Selected Xcode $xcode_version"
xcodebuild -version

echo "[2/3] Static source validation"
python3 ../scripts/validate_delivery.py

echo "[3/3] Xcode 16+ simulator build"
xcodebuild \
  -project KinVoice.xcodeproj \
  -scheme KinVoice \
  -configuration Debug \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  clean build

echo "KinVoice simulator build passed."
