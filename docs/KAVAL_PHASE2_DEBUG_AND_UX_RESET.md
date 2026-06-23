# Kaval Phase 2 Debug and UX Reset

Date: 2026-06-23

## Current pause point

We are still in Phase 2 validation. Phase 1 real GPS is accepted. Phase 2 is implemented but not production-stable yet because three things failed user validation:

- The latest UI/navigation reframe was not convincing and should be rolled back.
- SOS SMS is still failing on the real phone.
- SOS audio recording exists, but rotation makes the recording experience unreliable and the UI alignment shifts.

## Immediate rollback decision

The latest home/navigation reframe should be revoked.

Rolled back locally:

- `app/src/main/java/com/kaval/app/presentation/KavalApp.kt`
- `app/src/main/java/com/kaval/app/presentation/screens/Screens.kt`

Kept for now because they help diagnose real Phase 2 bugs:

- `IncidentDao.kt` delivery update guard, which preserves the real SMS send failure reason.
- `KavalActivityCard` recording row, which makes local recordings visible in Incident Log.

If we decide the recording row also hurts the UI, it should be redesigned, not silently removed, because users need a clear way to find local SOS evidence.

## Evidence from phone

ADB connection was working on device:

`adb-5cd434fc-dHuQTG._adb-tls-connect._tcp`

SMS permission is allowed:

`SEND_SMS: allow`

Local recordings were created under app-private storage:

`/data/user/0/com.kaval.app/files/sos_recordings/`

This is intentionally not visible in Gallery or normal file manager. The app must expose recordings through Incident Log with play/share/export controls.

Latest Room data showed:

- Latest SOS incident had `smsStatus = failed`
- `contactsAttempted = 1`
- `failedCount = 1`
- Recording path was saved for the latest SOS incident
- Latest SMS delivery row:
  - `sentStatus = FAILED`
  - `deliveryStatus = PENDING`
  - `failureReason = SMS failed with result code 0`
  - `resultCode = 0`

This is useful because previous builds overwrote the real send failure with a delivery receipt message. The current result means the failure is happening at the send callback, not only at carrier delivery receipt.

## SMS failure analysis

Current implementation:

- Uses `divideMessage`.
- Uses `sendMultipartTextMessage`.
- Uses sent and delivered `PendingIntent`s.
- Stores per-contact send/delivery status in Room.
- Includes custom emergency message, profile name, Google Maps link, coordinates, timestamp, and Kaval attribution.

Likely failure causes, ordered by probability:

1. Default SMS subscription is not being selected correctly on this Android version or dual-SIM device.
2. Multipart message is too long or carrier/device is rejecting it under repeated test conditions.
3. Phone number formatting is too loose. Existing contact is stored as a local 10-digit number; previous shorter messages worked, but production SMS should normalize Indian numbers to `+91`.
4. PendingIntent/result handling needs richer logging for result code `0` and per-part failures.
5. Carrier spam/rate limiting after repeated SOS tests.

Planned SMS fix:

1. Add an SMS preflight layer:
   - Check `SEND_SMS` permission.
   - Check active SIM/subscription.
   - Check default SMS subscription ID.
   - Store preflight status in incident logs.
2. Send through the subscription-specific manager:
   - Prefer `SmsManager.getSmsManagerForSubscriptionId(defaultSmsSubscriptionId)`.
   - Fall back only when the default subscription is unavailable.
3. Normalize trusted contact numbers:
   - Indian 10-digit numbers become `+91XXXXXXXXXX`.
   - Keep already international numbers unchanged.
   - Block invalid numbers before SOS and show the contact causing failure.
4. Shorten the production SOS body:
   - Keep custom emergency note.
   - Keep Google Maps link.
   - Remove duplicate coordinate line from SMS body if link is present.
   - Keep message concise to reduce multipart risk.
5. Improve result-code mapping:
   - Treat `Activity.RESULT_CANCELED` / `0` as "SMS send canceled by system or subscription unavailable".
   - Log subscription ID, part count, and normalized destination.
6. Add one internal diagnostic button or dev-only command:
   - Send a tiny test SMS to the selected trusted contact.
   - This separates carrier/SIM failure from long SOS-message failure.

## Recording and rotation bug analysis

Current implementation:

- Recording starts only after real SOS trigger.
- It runs through `AudioRecordingService`.
- It uses a foreground service notification.
- It writes local `.m4a` files into private app storage.
- It persists the file path into the incident log.

Observed issue:

- User reports recording stops or feels broken when the phone is rotated.
- UI alignment shifts on rotation.

Likely root causes:

1. `AudioRecordingService` returns `START_NOT_STICKY`, so if the system kills/recreates the service it will not recover.
2. The active recording state is not represented as durable app state.
3. Emergency UI is composed from current nav/activity state and can visually reset on rotation.
4. The service notification is visible, but the app UI does not show a strong "recording active / saved locally / tap to manage" state.

Planned recording fix:

1. Change the service to `START_STICKY`.
2. Persist active recording metadata:
   - active incident ID
   - file path
   - start time
   - max stop time
3. Add notification actions:
   - Stop recording
   - Open Emergency Mode
4. Make Emergency Mode rotation-safe:
   - Use stable state from Room/service instead of transient activity state.
   - Keep layout constraints fixed for portrait and landscape.
5. Add recording finalization states:
   - Recording active
   - Saved locally
   - Failed to start
   - Export/share available
6. Keep files local unless the user explicitly shares or exports.

## UX reset analysis

The rejected UI pass failed because it changed navigation structure without establishing a better visual system first.

What went wrong:

- It removed the bottom bar too early.
- It made the home screen more command-heavy but not more premium.
- It did not introduce a strong enough visual hierarchy.
- It did not solve the core mental model: Before, During, After safety phases.
- It made the app feel rearranged, not elevated.

New UX direction:

Kaval should feel like a premium operational safety companion, not a demo checklist. The design should be calm, serious, and high-trust.

Principles:

- SOS remains the central action.
- Map is supporting infrastructure, not the main product.
- Guardian, Journey, and SOS must share one safety-state language.
- Logs and recordings must be findable without feeling scary or technical.
- Real features should be visually separated from prototype/demo features.

Recommended information architecture:

1. Home
   - Current safety state
   - SOS
   - Guardian Mode toggle
   - Journey start/check-ins
   - quick actions: Fake Call, GPS, Helplines
2. Journey
   - Before / During / After phase tabs
   - Start Journey
   - Vehicle number
   - Boarded / Reached quick confirms
   - ETA/status card
3. SOS / Emergency Mode
   - live SOS status
   - SMS per-contact state
   - local recording state
   - stop emergency
4. Profile
   - personal info
   - emergency note
   - trusted contacts
   - incident log
   - recording exports
5. Settings
   - appearance
   - permissions
   - privacy
   - log retention

## Elite UI plan

Do not restart with random colors or glass effects.

The next UI pass should first create a small design system:

- Status colors:
  - Safe
  - Watch
  - Alert
  - Offline
- Surfaces:
  - background
  - elevated panel
  - emergency panel
  - quiet row
- Components:
  - safety state header
  - SOS control
  - phase segmented control
  - contact delivery row
  - recording evidence row
  - permission readiness row
  - quick action tile

Visual target:

- Premium Android Material 3, not web landing-page design.
- Clean, high-contrast, restrained motion.
- Fewer cards.
- More structured bands and rows.
- Clear active/inactive states.
- No decorative UI that does not explain safety state.

## What must be solved before Phase 3

1. SMS must send successfully to at least one trusted contact on the real phone.
2. SMS failure reason must be specific and actionable.
3. Recording must continue across rotation.
4. Recording must be clearly visible in Incident Log.
5. Emergency Mode layout must not shift badly on rotation.
6. Home must return to a stable usable navigation structure.
7. Demo/prototype features must be clearly labeled or hidden.

## What can wait until after Phase 2 is stable

- Full helpline screen with all 15 numbers.
- Supabase.
- Guardian live link backend.
- Embedded Google Maps SDK.
- Decoy screen.
- Auto late-night monitoring.
- Full cab/auto verification workflow.

## Next implementation order

1. Build and reinstall the rollback build.
2. Fix SMS subscription/number/message handling.
3. Run one short diagnostic SMS test.
4. Run one real SOS test.
5. Fix recording service stickiness and rotation-safe state.
6. Rebuild and reinstall.
7. Only then start the premium UX redesign pass from a proper design-system layer.

