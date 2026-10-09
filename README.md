# AI Dots Android Vibration Test

This repository contains a small native Android app for verifying device vibration without an AI API or internet connection.

## Features

- Native Android vibration using the Android Vibrator API.
- Declares the required `android.permission.VIBRATE` permission.
- A one-tap vibration test button.
- An offline story that streams into the chat area.
- Repeating vibration while the story is being generated.
- Vibration stops when the story completes or the app leaves the foreground.
- A GitHub Actions workflow builds a debug APK.

## Build

The project uses Android Gradle Plugin 8.7.3, Gradle 8.9, and Java 17.

Run:

```bash
gradle assembleDebug
```

The APK is generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Build with GitHub Actions

Open the Actions tab and run the **Build Android APK** workflow, or push a commit to the main branch. Download the artifact named `ai-dots-vibration-test-apk` from a successful workflow run.

## Device testing

1. Install the debug APK on a physical Android phone.
2. Tap **TEST VIBRATION NOW** to verify a short native vibration.
3. Send any message to start the offline story.
4. Vibration should repeat while the story streams and stop when it finishes.

The app cannot force vibration if the device has no vibrator, system vibration is disabled, or device policy blocks it.
