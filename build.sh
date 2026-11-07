#!/bin/sh
# Builds MoveRevive.apk with the plain SDK tools (no Gradle). Xposed stubs stay out of the dex.
set -e
unset MSYS_NO_PATHCONV  # apksigner.bat needs $HOME converted to a Windows path
cd "$(dirname "$0")"

SDK="$LOCALAPPDATA/Android/Sdk"
BT="$SDK/build-tools/36.0.0"
JAR="$SDK/platforms/android-36/android.jar"

rm -rf build
mkdir -p build/stubs build/classes build/dex

"$BT/aapt2.exe" compile --dir res -o build/res.zip
"$BT/aapt2.exe" link -I "$JAR" --manifest AndroidManifest.xml -A assets build/res.zip \
    --min-sdk-version 28 --target-sdk-version 31 -o build/unsigned.apk

javac -nowarn -source 8 -target 8 -bootclasspath "$JAR" -d build/stubs \
    $(find stubs -name '*.java') 2>&1 | grep -v -E '^(warning|Note|1 warning)' || true
javac -nowarn -source 8 -target 8 -bootclasspath "$JAR" -cp build/stubs -d build/classes \
    $(find src -name '*.java') 2>&1 | grep -v -E '^(warning|Note|1 warning)' || true

"$BT/d8.bat" --lib "$JAR" --min-api 28 --output build/dex $(find build/classes -name '*.class')

jar uf build/unsigned.apk -C build/dex classes.dex
"$BT/zipalign.exe" -f 4 build/unsigned.apk build/aligned.apk
"$BT/apksigner.bat" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android \
    --key-pass pass:android --ks-key-alias androiddebugkey --out MoveRevive.apk build/aligned.apk

echo "built $(pwd)/MoveRevive.apk"
