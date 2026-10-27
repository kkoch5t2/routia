#!/usr/bin/env bash
# Export committed source only. Never zip the entire working directory.
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -n "$(git status --porcelain --untracked-files=normal)" ]]; then
    echo "Commit or set aside pending source changes before exporting." >&2
    exit 1
fi
python3 scripts/check-publication.py --ref HEAD
version="$(git show HEAD:app/build.gradle | sed -n "s/^[[:space:]]*versionName '\([^']*\)'.*/\1/p")"
if [[ ! "$version" =~ ^[A-Za-z0-9][A-Za-z0-9._-]*$ ]]; then
    echo "Could not determine the committed version." >&2
    exit 1
fi
mkdir -p build/deliverables
output="build/deliverables/routia-$version-source.zip"
git archive --format=zip --prefix=Routia/ --output="$output" HEAD
printf 'Source archive: %s\n' "$output"
