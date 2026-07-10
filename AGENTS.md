# Atlethiq Android — Agent Rules

## Stack (pinned — do not substitute)
- Kotlin 2.x, Jetpack Compose, Material 3, single-activity
- Hilt (DI), Room (local store), WorkManager (sync), Navigation Compose
- androidx.health.connect:connect-client (Health Connect)
- supabase-kt (Auth, Postgrest, Functions)
- Vico for charts
- minSdk 28, targetSdk 35. No Google Fit APIs ever (sunset end 2026).

## Architecture (non-negotiable)
1. CANONICAL MODEL: every data point is a `MetricSample`
   (value, unit, startTime/endTime UTC + zoneOffset, metricType,
   sourceApp, sourceDevice, recordingMethod, priorityRank, confidence).
   Product/UI code reads ONLY the canonical model.
2. ADAPTER PATTERN: Health Connect records are mapped to MetricSample
   in `data/adapters/healthconnect/`. No HC types leak past adapters.
   Future sources (WHOOP API, Strava) are new adapters, nothing else changes.
3. DEDUP: source-priority resolution lives in `domain/dedup/`,
   config-driven (per-metric priority table), never inline in UI or adapters.
4. READINESS: The Call is computed by the Supabase Edge Function
   `compute-call`. The app NEVER computes readiness locally; it renders
   the persisted daily snapshot. (Cache last snapshot for offline view.)
5. Layers: ui/ → domain/ → data/. UI never touches data/ directly.

## Design system (source: /design/reference.html + brand doc)
- Dark only. Colors, type, radius EXACTLY per the token object in
  ui/theme/ — extract once from the reference, never invent values.
- Fonts: Space Grotesk (display), Inter (body), JetBrains Mono (ALL numbers).
- One ink glows per screen: lime #C6F03C / cyan #3CE0F0 / orange #FF7A1A
  bound to Call state; yellow #F0E84A only inside Decode; pink #FF5CA8
  only on Peak Window elements. On-ink text is always #0B0E11.
- No gauges/dials/radial rings. No emoji. No exclamation marks in copy.
- Copy comes verbatim from the design spec; do not paraphrase Call verbs.

## Process
- Build ONLY the current milestone. Do not scaffold future milestones.
- Every milestone ends with: compiling app, listed manual test steps, and
  a one-paragraph CHANGELOG.md entry.
- Unit tests required for: adapters, dedup resolver, and (server-side)
  the readiness function. UI tests not required in dogfooding phase.
- Secrets in local.properties / Supabase env — never committed.
