# BluetoothAudio Prototype

## Summary
A minimal Android Java app demonstrating consent-based audio streaming between two phones using Bluetooth RFCOMM sockets.

## Requirements
- Android Studio
- Two physical Android devices (paired via system Bluetooth)
- Android SDK Platform 34
- USB debugging enabled on devices

## Build
1. Open project in Android Studio.
2. Let Gradle sync.
3. Connect device and Run app.

## Run
1. On Device A: Launch app → Tap "Start Server (Accept Connections)" → grant permissions.
2. On Device B: Launch app → Tap "Scan & Connect (Client)" → app connects to a paired device and streams mic to Device A.

## Notes
- Both devices must grant RECORD_AUDIO and Bluetooth permissions.
- This is a prototype. Add robust error handling, discovery UI, pairing flow, and security for production.
