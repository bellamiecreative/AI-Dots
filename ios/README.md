# AI Dots — iOS Haptics Test

A small native SwiftUI iPhone test app that mirrors the Android vibration-test build.

## Included

- Native iOS interface with an AI Dots title and chat-style message bubbles.
- Offline sample story streamed one character at a time.
- Light UIKit haptic feedback for each non-whitespace character.
- Longer pauses after punctuation.
- A **TEST VIBRATION NOW** button.
- No network connection or AI API required.

## Build

Open `ios/AIDotsVibrationTest.xcodeproj` in Xcode on a Mac and run the `AIDotsVibrationTest` scheme on an iPhone.

The GitHub Actions workflow builds a simulator version automatically. The simulator artifact is for simulator testing only and cannot be installed directly on a physical iPhone.

## Installing on a physical iPhone

A physical-device build must be signed with an Apple development certificate and provisioning profile. This repository does not contain signing credentials. Once the user has an Apple Developer signing setup, a signed device build can be produced and installed through an appropriate Apple-supported testing workflow.
