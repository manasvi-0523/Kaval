# Kaval Feature Roadmap

This roadmap tracks requested features after the first working Android MVP.

## Current MVP

- Home dashboard
- 2-second SOS hold flow
- Mock emergency alert
- Trusted contacts CRUD
- Activity log
- Fake call simulation
- Fake call ringtone
- Appearance settings
- Demo map preview
- Demo Mode
- Guardian/Passive/Journey UI states

## Next Build: Native Safety Integrations

### Real SMS SOS

Goal:

- Send SMS to trusted contacts during SOS.
- Work offline without internet.

Requirements:

- `SEND_SMS` permission.
- Runtime permission explanation.
- Clear user opt-in.
- Contact phone validation.
- Fallback if permission is denied.
- Activity log entry for success/failure.

### Live Location

Goal:

- Show real device location on Map.
- Include location link in SOS message.

Requirements:

- `ACCESS_FINE_LOCATION` permission.
- Runtime permission explanation.
- Fused location provider or platform location manager.
- Mock/demo fallback if location is unavailable.

### Google / Android Share

Goal:

- Prepare a safety message with a Google Maps link and open Android's system share sheet.

Requirements:

- Android share intent.
- Works with Google Messages, Gmail, WhatsApp, or any compatible installed app.
- No background messaging automation.

## Guardian Mode

Goal:

- Persistent guardian session during travel.
- Home-screen toggle.
- Journey state: Before, During, After.
- Quick updates: "I've boarded", "I've reached".

Future upgrades:

- ETA updates.
- Cab/auto vehicle number share.
- Arrived safely summary.

## Passive Safety Mode

Goal:

- Monitoring state that can be toggled from Home.
- Late Night Mode auto-activation after 9 PM.

Requirements:

- User-configurable quiet hours.
- Clear visible status.
- No hidden background tracking in MVP.

## Cab/Auto Verification

Goal:

- Enter vehicle number before boarding.
- Share the vehicle number with a guardian.

Requirements:

- Vehicle entry UI.
- Message preview.
- Trusted contact selection.

## Practical Escapes

### Quick Exit Script

Goal:

- Send or prepare a pre-typed excuse message so a trusted contact can call the user out of a situation.

### Decoy Screen

Allowed MVP direction:

- A decoy screen that visually looks minimal or locked while keeping Kaval controls accessible.

Important safety/legal boundary:

- Do not secretly record audio. Audio recording must require explicit user action, clear consent handling, and visible legal warnings.

## Presentation Notes

Kaval should avoid claiming real emergency delivery until SMS/location integrations are implemented and tested on a real Android phone.
