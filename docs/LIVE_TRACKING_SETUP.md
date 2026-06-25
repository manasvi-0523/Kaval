# Kaval Live Tracking Setup

Live tracking requires a hosted database and a guardian-facing web URL. A Google
Maps coordinate link is only a snapshot and cannot move after the SMS is sent.

## What You Need To Do

1. Create a free project at https://supabase.com/dashboard.
2. Open **Authentication > Providers > Anonymous** and enable anonymous sign-ins.
3. Open **SQL Editor**, paste `supabase/migrations/001_live_tracking.sql`, and run it.
4. Open **Project Settings > API** and collect:
   - Project URL
   - Publishable key (or legacy `anon` key)
5. Add these lines to the repository's uncommitted `local.properties`:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLISHABLE_KEY
GUARDIAN_WEB_BASE_URL=https://YOUR_GUARDIAN_SITE.vercel.app
```

Never provide or place the Supabase `service_role` key in the Android app,
`local.properties`, guardian website, GitHub, or chat. It belongs only in
server-side secrets such as Supabase Edge Functions.

6. Create a free Vercel account at https://vercel.com and connect the Kaval
   GitHub repository. The guardian site will be deployed from `guardian-web/`
   after that folder is implemented.
7. Send Codex the Project URL and publishable/anon key. These values are client
   configuration, not the service-role secret.

## How The Finished Flow Works

1. Kaval anonymously authenticates the installation with Supabase.
2. SOS creates an expiring `journey_sessions` row with a random token.
3. SMS contains `https://YOUR_SITE/track/TOKEN`.
4. The foreground service updates the session location every 10-15 seconds.
5. The guardian page requests only the row matching that token and moves the
   map marker as new updates arrive.
6. Marking safe completes the session, after which the link stops returning
   location data.

## Security Choice

The original prompt's public `SELECT` policy could expose all active tracking
rows to a badly written or malicious client. The migration keeps the table
private and exposes token-scoped functions instead. The first guardian page
will poll that function every 10 seconds. Private Realtime channels can replace
polling later without weakening row security.

## Required Physical Test

- Start an emergency session outdoors.
- Open its SMS link on another phone.
- Walk at least 50-100 metres.
- Confirm the guardian marker moves and the update timestamp changes.
- Tap **I am safe** and confirm the tracking link expires.
