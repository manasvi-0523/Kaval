# Kaval

Kaval is an Android-first personal safety companion MVP built with Kotlin and Jetpack Compose.

## Status

MVP / Demo Build

Build status: `./gradlew assembleDebug` has been verified locally.

## Features

- SOS emergency mock flow
- 2-second hold-to-activate SOS button
- Trusted contacts
- Fake call simulation with selectable delay
- Activity log
- Appearance customization
- Demo mode
- Mock safety map
- Local-first storage

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- MVVM
- Room
- DataStore
- Navigation Compose

## MVP Scope

This version uses simulated emergency alerts and demo location data. Real SMS alerts, real-time tracking, backend services, and AI risk prediction are future enhancements.

## Screens

- Home
- Map
- Contacts
- Activity
- Settings
- Profile
- Appearance
- Fake Call
- Emergency Mode

## Screenshots

Screenshots should be added after the app UI is running and presentable:

- `screenshots/home.png`
- `screenshots/sos-flow.png`
- `screenshots/contacts.png`
- `screenshots/settings.png`
- `screenshots/activity-log.png`
- `screenshots/fake-call.png`

## How to Run

1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle sync and install any Android SDK packages requested by Android Studio.
4. Run the app on an Android emulator or physical Android device.

## Build From Terminal

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

If Java is not available on `PATH`, install JDK 17 or use Android Studio's bundled JDK.

## Mocked In MVP

- Emergency alert sending
- Contact notification
- Phone calls
- Location sharing
- Map and area risk data
- Battery percentage in alert message

## Future Enhancements

- Real SMS alerts
- Real-time location sharing
- Backend integration
- AI risk prediction
- Danger heatmap
- Admin dashboard

## Disclaimer

This MVP uses simulated emergency alerts and demo location data. It does not send real emergency messages or replace official emergency services.
