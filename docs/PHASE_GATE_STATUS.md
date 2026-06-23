# Kaval Phase Gate Status

Updated: 2026-06-23

This file tracks what has actually passed. A phase is not complete unless both build checks and required physical-device checks pass.

## Phase 0 - Buildable Polished Scaffold

Status: Complete

Scope:
- Stable Android shell
- Package `com.kaval.app`
- Kotlin, Compose, Material 3
- Bottom navigation: Home, Map, Helplines, Profile
- Situation-first Home scaffold
- Professional theme colors
- No fake live sharing, fake route, fake ETA, or fake guardian success

Verification:
- Build pass 1: Passed (`.\gradlew.bat assembleDebug --no-daemon`)
- Build pass 2: Passed (`.\gradlew.bat assembleDebug --no-daemon`)
- Physical phone install/open: Passed on `CPH2613`
- Four tabs load: Passed on physical phone

Notes:
- Home shows `KAVAL`, `HOLD FOR SOS`, `Start Safe Journey`, `I feel unsafe`, and `Live Guardian Tracking: Off`.
- Bottom navigation is `Home`, `Map`, `Helplines`, `Profile`.
- Home no longer shows fake guardian success, fake live ETA, or fake route confidence.
- Map opens to `GPS Status`; detailed GPS control verification belongs to Phase 1.

## Phase 1 - Accurate Live Location Core

Status: In progress

Gate:
- Real GPS works on physical phone
- Accuracy and last updated time visible
- Location unavailable state honest

Implementation status:
- `FusedLocationProviderClient` is in use through `LocationTracker`.
- Coarse location explanation appears before the coarse system permission request.
- Fine location explanation appears only after coarse permission is granted.
- Map screen shows permission state, accuracy, last-updated time, refresh, recenter, and Google Maps link controls when a location is available.
- MapLibre location layer now renders a blue user dot and an accuracy radius from the real `Location.accuracy`.
- No fake Bengaluru/demo point is shown as the user's location.
- SOS message generation uses the transient latest Maps link when location is available.

Verification:
- Build pass 1: Passed (`.\gradlew.bat assembleDebug --no-daemon`)
- Build pass 2: Passed (`.\gradlew.bat assembleDebug --no-daemon`)
- Physical phone install: Passed on `CPH2613`
- Final Map screen phone check: Pending because the phone locked during verification
- MapLibre tile check: Pending because `MAPTILER_KEY` is not present in `local.properties`

Manual gate still pending:
- Unlock phone and open Map.
- Confirm current location appears.
- Confirm accuracy radius appears when MapTiler key is configured.
- Move slightly and confirm update.
- Turn device location off and confirm unavailable state.

## Phase 2 - Guardian Contacts and Live Sharing Session

Status: Not started

Gate:
- Real live-tracking guardian selected
- Session created locally/server-side
- UI shows real session state

## Phase 3 - ForegroundService Live Location Upload

Status: Not started

Gate:
- Background service uploads at least two real location updates
- Foreground notification remains visible

## Phase 4 - Guardian Web Tracking Page

Status: Not started

Gate:
- Guardian link opens on second device
- Marker appears and moves from live updates

## Phase 5 - Start Safe Journey Flow

Status: Not started

Gate:
- Destination, guardian, session, and journey end flow work procedurally

## Phase 6 - Journey Safety Timeline and Route Confidence

Status: Not started

Gate:
- Timeline uses real checkpoints, not decorative/fake status

## Phase 7 - SOS Integrated With Live Location

Status: Not started under this phase-gated run

Note:
- Older SOS/SMS/audio code exists, but it is not considered phase-complete until live guardian tracking works and the Phase 7 manual gate passes.

## Phase 8 - I Feel Unsafe Situation-First Flow

Status: Not started as a completed phase

Note:
- A scaffold entry exists on Home, but routes are not phase-complete.

## Phase 9 - Safety Call Mode

Status: Not started as a completed phase

Note:
- Older simulated-call behavior exists. It is not considered completed Safety Call Mode.

## Phase 10 - Emergency Suggestion Mode

Status: Not started as a completed phase

## Phase 11 - Helplines, Profile, Logs, and Settings

Status: Not started as a completed phase

## Phase 12 - Polish, Security, and Final Hardening

Status: Not started
