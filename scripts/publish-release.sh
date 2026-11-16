#!/usr/bin/env bash
# Uses the developer's GitHub CLI login. Never uploads a private signing key.
set -euo pipefail
cd "$(dirname "$0")/.."
command -v gh >/dev/null || { echo 'Instale o GitHub CLI oficial e execute gh auth login.'; exit 1; }
VERSION_NAME="$(sed -n 's/^versionName=//p' version.properties)"
NOTES="${1:-RELEASE_NOTES.md}"
python3 scripts/prepare-release.py --notes-file "$NOTES"
gh release create "v$VERSION_NAME" "dist/neon-shift-$VERSION_NAME.apk" \
    "dist/neon-shift-$VERSION_NAME.apk.sha256" dist/update.json \
    --repo levymtmr/menatwork --verify-tag --title "Menatwork $VERSION_NAME" --notes-file "$NOTES"
