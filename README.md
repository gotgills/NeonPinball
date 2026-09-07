# Neon Pinball for Android

A dependency-free Android pinball game written in Java using a custom View and Canvas physics.

## Features
- Portrait pinball table
- Dual touch flippers (multi-touch supported)
- 3 bumpers with score hits
- Side rails and posts
- Gravity, wall, bumper and flipper collision physics
- 3-ball game, score display and restart screen
- No internet permission and no external libraries

## Build APK in Android Studio
1. Open this folder in Android Studio.
2. Let Android Studio install Android SDK 35 if prompted.
3. Select **Build > Build APK(s)**.
4. The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Build from terminal
With Android SDK and Gradle installed:

```bash
gradle assembleDebug
```

Then install:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
