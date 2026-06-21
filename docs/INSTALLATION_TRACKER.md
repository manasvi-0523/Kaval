# Installation Tracker

This file tracks local tools, SDK packages, emulators, and generated artifacts installed or created while building and testing Kaval on this machine.

## Project Location

```text
C:\Users\Lenovo\Desktop\kaval\Kaval
```

## Repository

```text
https://github.com/manasvi-0523/Kaval
```

## Local Project Tools

These are local helper tools under the project folder and are ignored by Git.

```text
C:\Users\Lenovo\Desktop\kaval\Kaval\.tools\jdk17
C:\Users\Lenovo\Desktop\kaval\Kaval\.tools\gradle-8.7
```

Purpose:

- JDK 17 for Gradle/Android builds when Java is not available on PATH.
- Gradle 8.7 local distribution used before the Gradle wrapper was verified.

Removed after extraction:

```text
C:\Users\Lenovo\Desktop\kaval\Kaval\.tools\jdk17.zip
C:\Users\Lenovo\Desktop\kaval\Kaval\.tools\gradle-8.7-bin.zip
```

## Gradle Wrapper

The project Gradle wrapper is committed to the repository.

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

Verified build command:

```powershell
.\gradlew.bat assembleDebug
```

## Android SDK Location

```text
C:\Users\Lenovo\AppData\Local\Android\Sdk
```

`local.properties` points to this SDK path and is intentionally ignored by Git.

Android Platform Tools was added to the current Windows user's `PATH` on June 20, 2026:

```text
C:\Users\Lenovo\AppData\Local\Android\Sdk\platform-tools
```

New terminal windows can run `adb` directly. Existing terminal windows must be reopened or have the path added to their current session.

## Android SDK Packages Installed/Present

Observed installed packages:

```text
build-tools;34.0.0
build-tools;36.0.0
build-tools;36.1.0
build-tools;37.0.0
cmdline-tools;latest
emulator;36.6.11
extras;google;Android_Emulator_Hypervisor_Driver;2.2.0
platform-tools;37.0.0
platforms;android-35
platforms;android-36.1
sources;android-36.1
system-images;android-35;google_apis;x86_64
system-images;android-35;google_apis;arm64-v8a
```

Notes:

- Android command-line tools were installed manually into the existing SDK.
- Android Emulator was reinstalled once to repair a suspected emulator runtime issue.
- `emulator.backup` was created by SDK package repair/reinstall behavior and may be present under the SDK folder.

## Android Virtual Devices Created

```text
C:\Users\Lenovo\.android\avd\KavalPixel.avd
C:\Users\Lenovo\.android\avd\KavalPixel.ini
C:\Users\Lenovo\.android\avd\KavalArm.avd
C:\Users\Lenovo\.android\avd\KavalArm.ini
```

Purpose:

- `KavalPixel`: Android 35 Google APIs x86_64 image.
- `KavalArm`: Android 35 Google APIs ARM64 fallback image.

Status:

- `KavalPixel` requires hardware acceleration. The Android Emulator Hypervisor Driver service `aehd` is installed but not running.
- `KavalArm` cannot run on this x86_64 host with QEMU2 because the system image architecture does not match the host architecture.

## Emulator Blocker

Current connection status:

```text
adb devices -l
List of devices attached
```

No Android device or emulator is currently connected.

Observed emulator errors:

```text
x86_64 emulation currently requires hardware acceleration
Android Emulator hypervisor driver is not installed on this machine
Avd's CPU Architecture 'arm64' is not supported by the QEMU2 emulator on x86_64 host
```

Driver/service checked:

```text
sc.exe query aehd
```

Observed status:

```text
SERVICE_NAME: aehd
TYPE: KERNEL_DRIVER
STATE: STOPPED
```

## Emulator Graphics Config Attempts

Created/updated:

```text
C:\Users\Lenovo\.android\advancedFeatures.ini
```

Content used to reduce emulator graphics crashes:

```text
Vulkan = off
GLDirectMem = off
VulkanNullOptionalStrings = off
VulkanShaderFloat16Int8 = off
```

Updated `KavalPixel` AVD config:

```text
hw.gpu.enabled = yes
hw.gpu.mode = swiftshader_indirect
```

## Generated Build Artifact

Debug APK:

```text
C:\Users\Lenovo\Desktop\kaval\Kaval\app\build\outputs\apk\debug\app-debug.apk
```

## Phase 1 GPS Dependencies

Added to the Android application through Gradle on June 20, 2026:

```text
com.google.android.gms:play-services-location:21.3.0
org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0
```

Purpose:

- Google Play services fused location provider for last-known and fresh device location.
- Coroutine support for safely awaiting Google Play services location tasks.

These artifacts are stored in the normal Gradle cache; no separate executable or global program was installed.

## Safety Log Maintenance Dependency

Added through Gradle on June 21, 2026:

```text
androidx.work:work-runtime-ktx:2.9.1
```

Purpose:

- Schedule weekly privacy cleanup for completed Safety Logs while retaining active emergency records.

## Phase 1 Map Dependency

Added through Gradle on June 21, 2026:

```text
org.maplibre.gl:android-sdk:11.0.0
```

Purpose:

- Render the real-location map with MapLibre and Maptiler tiles without using the Google Maps SDK.

Runtime requirement:

```text
MAPTILER_KEY=<key> in local.properties
```

The key is local-only and must never be committed. It was not present when the Phase 1 code was compiled, so tile rendering still requires local key configuration.

## Diagnostic Logs

Temporary emulator logs generated during debugging:

```text
emulator.err.log
emulator.out.log
emulator-current.err.log
emulator-current.out.log
emulator-arm.err.log
emulator-arm.out.log
```

These are ignored by Git via `.gitignore`.

## Next Local Run Path

Fastest path:

1. Connect a physical Android phone.
2. Enable Developer Options.
3. Enable USB debugging.
4. Run:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell monkey -p com.kaval.app 1
```

Alternative path:

- Start the Android Emulator Hypervisor Driver as Administrator, or enable a supported Windows emulator acceleration stack, then retry `KavalPixel`.
