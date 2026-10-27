#!/usr/bin/env bash
# Keep private signing files outside the source tree; never pass passwords as text.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK directory}"
: "${ROUTIA_KEYSTORE:?Set ROUTIA_KEYSTORE to your private keystore path}"
: "${ROUTIA_PASSWORD_FILE:?Set ROUTIA_PASSWORD_FILE to a private password file}"

python3 - "$PWD" "$ROUTIA_KEYSTORE" "$ROUTIA_PASSWORD_FILE" <<'PY'
from pathlib import Path
import sys
root = Path(sys.argv[1]).resolve()
for label, raw in zip(("keystore", "password file"), sys.argv[2:]):
    path = Path(raw).expanduser().resolve()
    if not path.is_file() or path.is_relative_to(root):
        raise SystemExit("Signing " + label + " must be an existing file outside the repository.")
PY

./gradlew --no-daemon assembleRelease
readarray -t apk_info < <(python3 - <<'PY'
import json, re
from pathlib import Path
folder = Path("app/build/outputs/apk/release")
elements = json.loads((folder / "output-metadata.json").read_text())["elements"]
if len(elements) != 1:
    raise SystemExit("Expected exactly one release APK.")
item = elements[0]
version = item["versionName"]
source = folder / item["outputFile"]
if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", version) or not source.is_file():
    raise SystemExit("Invalid release metadata.")
print(version)
print(source)
PY
)
if [[ ${#apk_info[@]} != 2 ]]; then
    echo "Could not read release APK metadata." >&2
    exit 1
fi
version="${apk_info[0]}"
source_apk="${apk_info[1]}"
signer="$(find "$ANDROID_HOME/build-tools" -mindepth 2 -maxdepth 2 -type f -name apksigner | sort -V | tail -1)"
if [[ -z "$signer" ]]; then
    echo "No Android apksigner was found." >&2
    exit 1
fi
# Optional output directory supports isolated signing checks without replacing a release.
output_dir="${ROUTIA_OUTPUT_DIR:-build/deliverables}"
mkdir -p "$output_dir"
output_apk="$output_dir/routia-$version.apk"
"$signer" sign --ks "$ROUTIA_KEYSTORE" --ks-key-alias "${ROUTIA_KEY_ALIAS:-routia}" --ks-pass "file:$ROUTIA_PASSWORD_FILE" --out "$output_apk" "$source_apk"
"$signer" verify --verbose "$output_apk"
printf 'Signed APK: %s\n' "$output_apk"
