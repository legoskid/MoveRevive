# MoveRevive
![Banner. you can see the cleverness that I did with the 9 squares icon being the move rings.](Untitled.png)

MoveRevive lets you use Oculus Move in 2026 via Xposed! Please star my repo if you enjoy this!

**Will work on v74 and below GUARANTEED. Newer may not be supported**

**v2 versions definitely don't work right now (the move package is gone completely)**

## Requirements for setup

- Your Quest *with root access*
- LSPosed (or other) installed and working

## Install

Get the APK from Releases and install (here we do it with ADB):

```sh
adb install MoveRevive.apk
```

Open LSPosed and enable the module for Move `com.oculus.fitnesstracker`

**Optional:** Create a birthday.txt file in `/sdcard/MoveRevive/` with your birthday in ISO format (YYYY-MM-DD). This is used to calculate your age for the Move app. If you don't create this file, the default date of `1990-01-01` will be used.

Go to Unknown Sources and launch, go through the setup to start tracking!

## Doesn't Work
* Sharing
* Fitness Friends
* Phone sync
* Cloud backup
* Achievements

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

## Notes

```
- The module is specific to the current Oculus Move package, React Native networking method signature, and known GraphQL document IDs.
- The included Xposed stubs are compile-time-only and are not packaged into the APK.
```
