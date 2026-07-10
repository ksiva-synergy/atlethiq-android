# Atlethiq Dogfooding App — Execution Plan & Antigravity Handoff (v1)

**Goal:** a full-fledged, daily-usable native Android app for single-user dogfooding — Health Connect ingestion, The Call + Decode, calibration states, Debug/Transparency layer — built via Antigravity with milestone-scoped prompts, backed by Supabase.

**Inputs to this plan:**
- `atlethiq-app-design-spec.md` — screen-by-screen design spec (source of truth for UX)
- `atlethiq-brand-system.md` — brand tokens, voice, discipline (source of truth for visual identity)
- `Atlethiq_-_Standalone.html` — Claude Design export (source of truth for *rendered look*; place in repo at `/design/reference.html`)

---

## 1. Architecture & tool roles

```
┌─────────────────────────────┐
│  Android app (Kotlin/Compose)│  ← Antigravity builds this. The ONLY native piece.
│  · Health Connect ingestion  │
│  · Canonical local store     │
│  · All app UI (Today→Debug)  │
└──────────────┬──────────────┘
               │ sync (idempotent upserts)
┌──────────────▼──────────────┐
│  Supabase (shared spine)     │
│  · Postgres: canonical data  │
│  · Edge Fn: readiness engine │  ← The Call computed HERE (TypeScript)
│  · Auth (single user), RLS   │
└──────────────┬──────────────┘
               │ read-only (optional, later)
┌──────────────▼──────────────┐
│  Lovable web app (OPTIONAL)  │  ← Debug console / future coach dashboard.
│  reads Supabase directly     │     NOT on the critical path. No code moves
└─────────────────────────────┘     from here to Android — different stacks.
```

**Why the readiness engine lives in a Supabase Edge Function, not in the app:**
1. The algorithm is the product; iterating it must not require an APK rebuild. Tuning weights = redeploy one TypeScript function.
2. Both the Android app and any future web surface (Lovable debug console, Squad dashboard) consume the same computed Call — one implementation, no drift.
3. A/B hooks from the dogfooding spec (swap parameters, recompute over history) are trivial server-side, painful on-device.

The app remains functional offline for *viewing* (last computed Call is cached locally); computation requires the function. Acceptable for dogfooding.

**Tool roles, stated plainly:**

| Tool | Role | What does NOT happen here |
|---|---|---|
| Claude Design export | Visual reference. Tokens + layout extracted into Compose theme. | It is not app code; nothing is "converted." |
| Antigravity | Builds the entire Android app + Supabase schema/functions, one milestone at a time. | Never builds ahead of the current milestone. |
| Lovable | *Optional, post-M5:* web debug console over the same Supabase. | Prototyping the mobile app. No React→Kotlin path exists. |
| Supabase | Postgres, Auth, Edge Functions, RLS. Free tier. | No aggregator webhooks yet (M6, only if needed). |

---

## 2. Antigravity Rules file

Create as `.antigravity/rules.md` (or the platform's equivalent) in the repo root before the first prompt. This is the architectural constitution across all agent runs:

```markdown
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
```

---

## 3. Supabase schema (created in M0)

```sql
-- canonical samples (mirror of the app's Room table)
create table metric_samples (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users not null,
  metric_type text not null,          -- 'hrv_rmssd' | 'resting_hr' | 'sleep_session' | 'heart_rate' | 'exercise_session' | 'steps' | 'rpe' | 'subjective_feel' | ...
  value numeric,
  unit text,
  start_time timestamptz not null,
  end_time timestamptz,
  zone_offset text,
  source_app text not null,           -- e.g. 'com.whoop.android', 'com.heytap.health', 'manual'
  source_device text,
  recording_method text not null,     -- 'automatic' | 'manual'
  payload jsonb,                      -- stages, raw extras
  client_sample_id text not null,     -- HC record id or generated; dedup key
  created_at timestamptz default now(),
  unique (user_id, metric_type, client_sample_id)
);

-- one row per day: the computed Call
create table daily_snapshots (
  user_id uuid references auth.users not null,
  day date not null,
  call text not null,                 -- 'go' | 'hold' | 'back_off' | 'calibrating' | 'no_data'
  signal_score int,                   -- 0–100
  confidence text not null,           -- 'calibrating' | 'provisional' | 'reliable' | 'high'
  decode jsonb not null,              -- factor list: name, direction, value, baseline, source
  debug jsonb not null,               -- z-scores, weights, thresholds, engine version
  engine_version text not null,
  computed_at timestamptz default now(),
  primary key (user_id, day)
);

-- founder feedback: the labeled dataset
create table call_feedback (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users not null,
  day date not null,
  issued_call text not null,
  user_verdict text not null,         -- 'go' | 'hold' | 'back_off'
  reasons text[],                     -- 'felt_fresh' | 'felt_wrecked' | 'life_stress' | 'illness' | 'other'
  note text,
  created_at timestamptz default now()
);

-- tunable engine parameters (A/B hooks)
create table engine_config (
  version text primary key,
  params jsonb not null,              -- weights, windows, thresholds
  active boolean default false
);
```
RLS on all tables: `user_id = auth.uid()`. Single user, but write the policies anyway — this is the same schema Squad inherits later.

---

## 4. Readiness engine v1 (Edge Function `compute-call`)

Deterministic spec so every milestone builds against the same contract. All numbers are **v1 defaults stored in `engine_config`**, expected to be tuned via dogfooding — none are gospel.

**Inputs (per day):** overnight HRV RMSSD (nightly value), resting HR, sleep session (duration, need = 7.5h default), exercise load (session duration × intensity proxy from HR zones; steps as background load).

**Baselines:** HRV 7-day rolling mean vs 30-day mean and std (z-score). RHR 7-day vs 30-day. Load: 7-day sum vs 28-day weekly average (directional only — ACWR's "sweet spot" is scientifically contested; never a hard gate).

**Signal score (0–100):** weighted composite — HRV z 50%, sleep (duration vs need + debt) 30%, RHR deviation 10%, load direction 10%.

**Call mapping:**
- `go` — score ≥ 75 AND HRV z ≥ −0.5 AND load not spiking (7d ≤ 1.4 × 28d avg)
- `back_off` — score < 45 OR HRV z ≤ −1.0 OR (HRV 7d mean ≥ 15% below 30d baseline for ≥ 3 consecutive days) → the last condition also sets **Drift = rising**
- `hold` — everything else

**Confidence:** days with valid HRV+sleep data: < 4 `calibrating` (no Call issued), 4–13 `provisional`, 14–29 `reliable`, ≥ 30 `high`.

**Output:** upsert into `daily_snapshots` with full `decode` (factor list with per-factor direction/value/baseline/source) and `debug` (z-scores, weights, thresholds evaluated, engine_version) — the Debug screen renders this verbatim; the exit criterion is *every Call fully explainable from the debug payload*.

**Trigger:** invoked by the app after each morning sync; also a scheduled cron at 09:00 IST as backstop; also re-runnable over historical days for A/B comparison.

---

## 5. Milestones & Antigravity prompts

One prompt per milestone, pasted verbatim, one at a time. Do not start Mn+1 until Mn's exit criteria pass.

### M0 — Scaffold, theme, data model *(2–3 days)*
**Prompt:** "Read `.antigravity/rules.md`, `/design/reference.html`, and `atlethiq-app-design-spec.md` §1 (tokens). Create the Android project per the pinned stack. Extract the design tokens from the reference HTML into `ui/theme/` (Color.kt, Type.kt with the three font roles via Google Fonts, Shape.kt). Implement the `MetricSample` canonical model as a Room entity + DAO, and the `daily_snapshots`/`call_feedback` mirror models. Set up Hilt, Navigation Compose with the four-tab scaffold (Today/Trends/Log/Sources) using placeholder screens that render the correct top bar (monogram, date, debug flask icon). Apply the SQL in §3 to Supabase and wire supabase-kt auth with a single email login. Exit: app compiles, tabs navigate, theme visibly matches the reference HTML side-by-side, Supabase login works."

### M1 — Health Connect Inspector *(2–3 days)*
**Prompt:** "Milestone M1 only. Implement the Health Connect permission flow requesting: SleepSession, HeartRateVariabilityRmssd, RestingHeartRate, HeartRate, ExerciseSession, Steps — plus the history-read permission (Android 15) and background-read permission. Build a temporary 'Inspector' screen (behind the debug flask for now): for each record type, list which source app wrote it, timestamps, and values, reading the last 30 days. Implement the Health Connect → MetricSample adapters in `data/adapters/healthconnect/` with unit tests. Exit: I can see on-device exactly which apps (WHOOP / OHealth / Zepp) write which record types."
**Decision gate:** this milestone decides M6. If WHOOP writes HRV + sleep to Health Connect with usable fidelity → M6 is cancelled. If not → M6 (WHOOP cloud API) is confirmed.

### M2 — Ingestion, dedup, Supabase sync *(3–4 days)*
**Prompt:** "Milestone M2 only. Add a WorkManager periodic sync (15 min) plus sync-on-app-open: read new Health Connect records since last token, map via adapters, upsert into Room (idempotent on client_sample_id), then push to Supabase `metric_samples` (upsert on the unique constraint). Implement the source-priority dedup resolver in `domain/dedup/` as a config-driven per-metric priority table (WHOOP > OnePlus/OHealth > Zepp > manual for HRV/sleep/RHR; watch > phone for steps), with unit tests covering overlapping sleep sessions from two sources. Build the real Sources screen per spec §5.6: device list with status dots, last-sync line, and the 'who wins what' attribution table driven by the live dedup config. Exit: two-source sleep resolves correctly; data visible in Supabase with correct timestamps."

### M3 — Readiness engine + The Call *(1 week)*
**Prompt (server):** "Implement the Supabase Edge Function `compute-call` exactly per §4 of the execution plan, parameterized by the active `engine_config` row, with unit tests over synthetic 45-day datasets covering all three Calls, Drift trigger, and all four confidence states. Seed `engine_config` v1 with the §4 defaults."
**Prompt (app):** "Milestone M3 only. Build the Today screen per design spec §5.2, rendering the persisted `daily_snapshots` row: all three Call states with the correct ink, the highlight-stroke behind the verb (400ms sweep on first daily open), Signal row with sparkline, Decode teaser card, plan card. Implement the three calibration overlays (calibrating / provisional at 70% ink / full) and the no-data state. Build the Decode bottom sheet per §5.3 rendering the `decode` payload with source chips and the yellow key-phrase highlight. Trigger `compute-call` after morning sync completes. Exit: on my real data, a provisional Call renders by day 4 of wear; verbs and copy match the spec verbatim."

### M4 — Daily loop: notification, Trends, Log *(3–4 days)*
**Prompt:** "Milestone M4 only. (a) Morning notification: after the first successful sync that lands overnight sleep+HRV, post exactly one notification with the Call verb and Decode one-liner per spec §6 — never a blind alarm, never more than one per day. (b) Trends screen per §5.4: Drift status line (orange treatment when rising), four Vico chart cards (HRV vs baseline band, RHR, sleep, load-directional), one series per chart. (c) Log screen per §5.5: RPE 1–10 segment selector, feel chips, note; writes `rpe`/`subjective_feel` MetricSamples (recording_method=manual) and syncs; history list with per-day Call pills opening read-only Decode. Exit: I wake to one correct notification; my manual RPE appears in Supabase."

### M5 — Debug layer, disagree loop, export *(3–4 days)*
**Prompt:** "Milestone M5 only. Implement global Debug mode per spec §5.7: flask toggle framing the viewport in a lime hairline; Today gains the raw decision line and the per-metric grid (raw → normalized → baseline → z → weight, with source chips and UTC timestamps) rendered from the `debug` payload. 'Disagree with the Call' sheet writing to `call_feedback`. 'Export today (JSON)' via the system share sheet (full raw + derived day). Retire the M1 Inspector into a Debug sub-screen. Exit: every number on Today is traceable in Debug to a source record; a disagreement round-trips to Supabase."

### M6 — WHOOP/Strava cloud adapters *(conditional, ~1 week)*
Only if the M1 gate found Health Connect's WHOOP data insufficient. Supabase Edge Function webhook receivers (WHOOP OAuth with rotating refresh tokens + `offline` scope; Strava standard tier), writing into `metric_samples` server-side as new sources — the app changes only by listing the new sources. Strava constraint: its data never feeds ML training and is never shown to other users (API terms).

### M7 — Optional Lovable web debug console *(post-M5, parallel-safe)*
A Lovable project connected to the same Supabase: read-only dashboard over `daily_snapshots`, `metric_samples`, `call_feedback` — desktop-sized charts, A/B comparison of engine_config versions over history (re-invoking `compute-call` per version). This is the seed of the Squad coach dashboard, in the correct stack for it (web). **Nothing here blocks or feeds the Android build.**

---

## 6. Distribution & device setup

- **Install:** sideload the release APK (or a private Play internal-testing track). Sideloading skips Play's Health Connect declaration review entirely — that review is only needed at public launch.
- **Day-0 device checklist:** WHOOP app updated + Health Connect sync enabled in WHOOP settings; OHealth (OnePlus) → Health Connect enabled; Zepp → Health Connect enabled (the Amazfit workaround to verify); Health Connect app: grant Atlethiq all requested types + history + background.
- **Battery:** exempt Atlethiq from battery optimization, or the 15-min WorkManager sync will be deferred aggressively. If Health Connect background sync still proves unreliable → per the standing threshold, treat WHOOP as source of truth and demote HC data to gap-filling (this is what M6 is for).

## 7. Risks & standing thresholds

| Risk | Mitigation |
|---|---|
| WHOOP's Health Connect write is shallow (no HRV/stages) | M1 gate → M6 direct API |
| HC 30-day history cap limits baseline warm-up | History permission + accept that day-30 high-confidence is reached in real time; optionally backfill via WHOOP API in M6 |
| Background sync throttling on OxygenOS | Battery exemption; sync-on-open as floor; morning notification waits for data, never fires blind |
| Lived experience contradicts The Call repeatedly | Stop feature work; tune `engine_config` (HRV weighting, baseline window) first — model trust is the whole product |
| Scope creep toward Squad/coach view | Explicitly out of scope until The Call is validated; the M7 web console is its future home |

## 8. Timeline

M0–M5 ≈ **4–5 weeks** of milestone-scoped Antigravity runs to a daily-usable app; +1 week if M6 triggers. M7 whenever useful, in parallel after M5.

---
*Engine numbers in §4 are starting hypotheses. The dogfooding build exists to falsify them.*
