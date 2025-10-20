#!/bin/bash
# Script to display current version information across the project
# https://semver.org/spec/v2.0.0.html
# 
# 📚 Read following for additional context:
# - https://github.com/usetrmnl/trmnl-android/blob/main/RELEASE_CHECKLIST.md

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

# Extract version from metadata (optional)
if [ -f "$ROOT_DIR/metadata/ink.trmnl.android.yml" ]; then
  echo "📄 From metadata/ink.trmnl.android.yml:"
  METADATA_VERSION=$(grep -o 'CurrentVersion: .*' "$ROOT_DIR/metadata/ink.trmnl.android.yml" | awk '{print $2}' || echo "N/A")
  METADATA_CODE=$(grep -o 'CurrentVersionCode: [0-9]*' "$ROOT_DIR/metadata/ink.trmnl.android.yml" | awk '{print $2}' || echo "N/A")
  METADATA_TAG=$(grep -o 'commit: .*' "$ROOT_DIR/metadata/ink.trmnl.android.yml" | awk '{print $2}' || echo "N/A")
  echo "  - Version Code: $METADATA_CODE"
  echo "  - Version Name: $METADATA_VERSION"
  echo "  - Git Tag: $METADATA_TAG"
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
# Increment version code by 1
NEXT_VERSION_CODE=$((VERSION_CODE + 1))

# Increment minor version and reset patch version to 0 (assumes X.Y.Z format)
# Example: 2.1.0 -> 2.2.0, or 2.1.3 -> 2.2.0
IFS='.' read -ra VER_PARTS <<< "$VERSION_NAME"
MAJOR="${VER_PARTS[0]}"
MINOR="${VER_PARTS[1]}"
NEXT_MINOR=$((MINOR + 1))
NEXT_VERSION_NAME="${MAJOR}.${NEXT_MINOR}.0"

echo "🚀 Suggested next version:"
echo "  - Version Code: $NEXT_VERSION_CODE"
echo "  - Version Name: $NEXT_VERSION_NAME"
echo

echo "To update version, run the GitHub Actions workflow 'Version Management' with:"
echo "  - Version name: $NEXT_VERSION_NAME"
echo "  - Version code: $NEXT_VERSION_CODE"
echo "  - Git tag: v$NEXT_VERSION_NAME (default)"
echo "  - Release notes: your comma-separated release notes"
