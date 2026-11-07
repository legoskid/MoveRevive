# MoveRevive

MoveRevive is an Android/Xposed module that keeps the Oculus Move fitness app usable after its Graph API endpoints stop responding. It intercepts Move's GraphQL requests and serves the small set of responses required by the app from a loopback HTTP responder.

## How it works

- The Xposed hook targets `com.oculus.fitnesstracker`.
- Requests to `https://graph.oculus.com/graphql...` are redirected to a local responder bound to `127.0.0.1`.
- Known document IDs receive local JSON responses for account validation, dashboard data, and birthday information.
- Unknown document IDs receive an empty GraphQL response.
- The launcher activity opens Move's `FitnessTrackerActivity` and exits after a short delay, allowing MoveRevive to appear as the Move tile.

The module does not log request bodies or access tokens.

## Requirements

- An Oculus/Meta Quest device with Oculus Move installed.
- A compatible Xposed implementation, such as LSPosed, with support for modules targeting Android apps.
- Android SDK platform 36 and build-tools 36.0.0 for building.
- Java 8-compatible `javac`.
- A shell with `find` available, such as Git Bash on Windows.

## Build

The project intentionally does not use Gradle. `build.sh` invokes `aapt2`, `javac`, `d8`, `zipalign`, and `apksigner` directly.

On Windows, run the script from Git Bash:

```sh
./build.sh
```

The script expects the SDK at:

```text
$LOCALAPPDATA/Android/Sdk
```

It creates the signed debug APK at `MoveRevive.apk`. Build intermediates are written to `build/` and are ignored by Git.

To regenerate the launcher icon:

```sh
python make_icon.py
```

## Install and enable

Install the APK with ADB:

```sh
adb install -r MoveRevive.apk
```

Then enable **MoveRevive** in the Xposed manager and scope it to:

```text
com.oculus.fitnesstracker
```

Reboot or restart the affected app/process after changing the module state. Launching the MoveRevive tile opens the installed Oculus Move app.

## Birthday configuration

The birthday query reads the first valid ISO date in:

```text
/sdcard/MoveRevive/birthday.txt
```

Example:

```text
1990-01-01
```

The file is read on every birthday request, so changing it does not require rebuilding the APK. If the file is missing or contains no valid date, the responder logs the condition through Xposed and uses `1990-01-01`.

## Project layout

```text
src/dev/moverevive/
  LauncherActivity.java  Opens Oculus Move from the launcher tile
  MoveRevive.java        Xposed hook and Graph API URL redirect
  Responder.java         Loopback HTTP server and local JSON responses
stubs/                   Minimal Xposed API stubs used only at compile time
assets/xposed_init        Xposed module entry-point declaration
res/                     Launcher icon resources
AndroidManifest.xml      Android and Xposed module metadata
build.sh                 Plain Android SDK build
```

## Notes

- The module is specific to the current Oculus Move package, React Native networking method signature, and known GraphQL document IDs.
- The included Xposed stubs are compile-time-only and are not packaged into the APK.
- The APK is signed with the standard Android debug keystore by the build script; it is intended for local installation and testing.
