#!/bin/bash
# Script to display current version information across the project

set -e

# Get current directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

echo "🔍 Current TRMNL Android Version Information"
echo "-------------------------------------------"

# Extract version from app/build.gradle.kts
VERSION_CODE=$(grep -o 'versionCode = [0-9]\+' "$ROOT_DIR/app/build.gradle.kts" | awk '{print $3}')
VERSION_NAME=$(grep -o 'versionName = "[^"]*"' "$ROOT_DIR/app/build.gradle.kts" | sed 's/versionName = "\(.*\)"/\1/')

echo "📱 From app/build.gradle.kts:"
echo "  - Version Code: $VERSION_CODE"
echo "  - Version Name: $VERSION_NAME"
echo

# Extract version from F-Droid metadata
if [ -f "$ROOT_DIR/metadata/ink.trmnl.android.yml" ]; then
  echo "🤖 From F-Droid metadata:"
  FDROID_VERSION=$(grep -o 'CurrentVersion: .*' "$ROOT_DIR/metadata/ink.trmnl.android.yml" | awk '{print $2}')
  FDROID_CODE=$(grep -o 'CurrentVersionCode: [0-9]*' "$ROOT_DIR/metadata/ink.trmnl.android.yml" | awk '{print $2}')
  FDROID_TAG=$(grep -o 'commit: .*' "$ROOT_DIR/metadata/ink.trmnl.android.yml" | awk '{print $2}')
  echo "  - Version Code: $FDROID_CODE"
  echo "  - Version Name: $FDROID_VERSION"
  echo "  - Git Tag: $FDROID_TAG"
  echo
fi

# Find and list changelog files
echo "📝 Changelog files:"
if [ -d "$ROOT_DIR/fastlane/metadata/android/en-US/changelogs" ]; then
  # Using find with sort -V to ensure version-sorted order of changelog files
  for file in $(find "$ROOT_DIR/fastlane/metadata/android/en-US/changelogs" -name "*.txt" | sort -V); do
    if [ -f "$file" ]; then
      VERSION=$(basename "$file" .txt)
      echo "  - Version $VERSION:"
      sed 's/^/    /' "$file"  # Indent the file content
      echo
    fi
  done
else
  echo "  No changelog files found in fastlane/metadata/android/en-US/changelogs"
fi

# Suggest next version
NEXT_VERSION_CODE=$((VERSION_CODE + 1))
# Simple semantic version increment (assumes X.Y.Z format)
IFS='.' read -ra VER_PARTS <<< "$VERSION_NAME"
NEXT_VERSION_NAME="${VER_PARTS[0]}.${VER_PARTS[1]}.$((${VER_PARTS[2]} + 1))"

echo "🚀 Suggested next version:"
echo "  - Version Code: $NEXT_VERSION_CODE"
echo "  - Version Name: $NEXT_VERSION_NAME"
echo

echo "To update version, run the GitHub Actions workflow 'Version Management' with:"
echo "  - Version name: $NEXT_VERSION_NAME"
echo "  - Version code: $NEXT_VERSION_CODE"
echo "  - Git tag: v$NEXT_VERSION_NAME (default)"
echo "  - Release notes: your comma-separated release notes"
