#!/bin/bash
# Script to update version.properties for a new release

set -e

# Get current directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
VERSION_FILE="$ROOT_DIR/version.properties"

if [ ! -f "$VERSION_FILE" ]; then
  echo "Error: version.properties not found at $VERSION_FILE"
  exit 1
fi

# Parse current values
CURRENT_VERSION_NAME=$(grep "VERSION_NAME=" "$VERSION_FILE" | cut -d'=' -f2)
CURRENT_VERSION_CODE=$(grep "VERSION_CODE=" "$VERSION_FILE" | cut -d'=' -f2)
CURRENT_TAG=$(grep "GIT_TAG=" "$VERSION_FILE" | cut -d'=' -f2)

echo "Current version: $CURRENT_VERSION_NAME ($CURRENT_VERSION_CODE)"
echo "Current git tag: $CURRENT_TAG"
echo

# Ask for new version information
read -p "Enter new version name (semantic version, e.g. 1.9.5): " NEW_VERSION_NAME
read -p "Enter new version code (integer, e.g. $((CURRENT_VERSION_CODE + 1))): " NEW_VERSION_CODE
read -p "Enter git tag (default is v$NEW_VERSION_NAME): " NEW_TAG
NEW_TAG=${NEW_TAG:-v$NEW_VERSION_NAME}

# Ask for release notes
echo
echo "Enter release notes (comma or newline separated, empty line to finish):"
RELEASE_NOTES=""
while IFS= read -r line; do
  [[ -z "$line" ]] && break
  if [[ -z "$RELEASE_NOTES" ]]; then
    RELEASE_NOTES="$line"
  else
    RELEASE_NOTES="$RELEASE_NOTES, $line"
  fi
done

# Update version.properties
sed -i '' "s/VERSION_CODE=.*/VERSION_CODE=$NEW_VERSION_CODE/" "$VERSION_FILE"
sed -i '' "s/VERSION_NAME=.*/VERSION_NAME=$NEW_VERSION_NAME/" "$VERSION_FILE"
sed -i '' "s/GIT_TAG=.*/GIT_TAG=$NEW_TAG/" "$VERSION_FILE"
sed -i '' "s/RELEASE_NOTES=.*/RELEASE_NOTES=$RELEASE_NOTES/" "$VERSION_FILE"

echo
echo "✓ Updated version.properties to version $NEW_VERSION_NAME ($NEW_VERSION_CODE)"
echo "✓ Git tag set to $NEW_TAG"
echo "✓ Release notes added"
echo
echo "Next steps:"
echo "  1. Run './gradlew prepareRelease' to update all version references"
echo "  2. Review changes, commit, tag, and push"
