#!/bin/bash
set -euo pipefail

GRADLE_FILE="build.gradle.kts"
PLUGIN_XML="src/main/resources/META-INF/plugin.xml"

current_version=$(grep -oP 'version = "\K[0-9]+\.[0-9]+\.[0-9]+' "$GRADLE_FILE")
if [ -z "$current_version" ]; then
  echo "Error: could not read version from $GRADLE_FILE"
  exit 1
fi

IFS='.' read -r major minor patch <<< "$current_version"

echo "Current version: $current_version"
echo ""
echo "Bump type:"
echo "  1) patch  → $major.$minor.$((patch + 1))"
echo "  2) minor  → $major.$((minor + 1)).0"
echo "  3) major  → $((major + 1)).0.0"
read -rp "Choose [1/2/3] (default: 1): " choice

case "${choice:-1}" in
  1) new_version="$major.$minor.$((patch + 1))" ;;
  2) new_version="$major.$((minor + 1)).0" ;;
  3) new_version="$((major + 1)).0.0" ;;
  *) echo "Invalid choice"; exit 1 ;;
esac

echo ""
echo "Bumping $current_version → $new_version"

sed -i '' "s/version = \"$current_version\"/version = \"$new_version\"/" "$GRADLE_FILE"
sed -i '' "s/<version>$current_version<\/version>/<version>$new_version<\/version>/" "$PLUGIN_XML"

git add "$GRADLE_FILE" "$PLUGIN_XML"
git commit -m "Bump version to $new_version"

git tag "v$new_version"

echo ""
echo "Pushing branch and tag..."
git push origin HEAD
git push origin "v$new_version"

echo ""
echo "Done! v$new_version pushed. GitHub Actions will publish to the marketplace."
