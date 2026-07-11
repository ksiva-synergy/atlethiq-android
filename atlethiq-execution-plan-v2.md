# Atlethiq Dogfooding App — Execution Plan v2 (Mock-First Build Order)

**Supersedes v1's §5 milestones.** Sections 1–4 of v1 (architecture, AGENTS.md rules, Supabase schema, readiness engine spec) remain in force unchanged — this document reorders the build so the complete app (all screens, engine, states, debug layer) is built and verified against **seeded mock data for the existing mock user on the emulator first**. Real Health Connect ingestion and physical-device installation move to the end.

**Status:** M0 complete and verified (scaffold, theme, canonical model, Supabase live, login round-trip confirmed).

**Why this order works:** the app never knows where data came from — everything downstream of ingestion reads the canonical `metric_samples` and computed `daily_snapshots`. Seeding those tables directly is indistinguishable from real ingestion to every screen and to the engine. When Health Connect lands in M7, it plugs into an already-finished app.

---

## §A. Seed dataset specification (new — input to M1)

One SQL seed script (`supabase_seed.sql`, kept in repo, idempotent: deletes + reinserts for the mock user) generating **45 days of synthetic history ending yesterday**, engineered to exercise every app state:

**Metric coverage per day:** nightly HRV RMSSD, resting HR, sleep session (with stage payload), 0–2 exercise sessions, daily steps, occasional manual RPE + subjective feel entries.

**Two synthetic sources everywhere it matters:** `mock_whoop` (priority 1) and `mock_oneplus` (priority 2) both write sleep and HR data with slightly different values and overlapping windows — so the dedup resolver (M6 in this plan) has real material, and Sources/Decode attribution chips render meaningfully from day one.

**Scenario blocks (chronological):**
| Days | Block | Engineered outcome |
|---|---|---|
| 1–3 | Sparse start | `calibrating` — no Call issued |
| 4–13 | Baseline building, steady values | `provisional` Calls, mostly Hold |
| 14–20 | Steady training | `reliable` confidence, Hold with one Go |
| 21–27 | Fresh + tapered: HRV climbing, good sleep, moderate load | **Go** days |
| 28–35 | Overload ramp: load 7d > 1.4× 28d, HRV sagging ≥15% below baseline 3+ consecutive days, RHR creeping up | **Back off** + **Drift = rising** |
| 36–40 | Recovery: load cut, HRV rebounding, one bad-sleep night | Back off → Hold transition |
| 41–45 | Return to form | Hold → Go, `high` confidence (day 30+ crossed) |

Values stay physiologically plausible (HRV 40–90ms band around a personal baseline ~60ms, RHR 50–60, sleep 5.5–8.5h) so charts look real, not synthetic.

---

## §B. Revised milestones

### M1 — Seed data + engine backfill *(2–3 days)*
Write `supabase_seed.sql` per §A and apply it. Implement the `compute-call` Edge Function exactly per v1 §4 (weights/thresholds from `engine_config`), plus a **backfill mode**: recompute `daily_snapshots` for every historical day in order (so calibration/confidence progresses day by day as it would have live). Run backfill over the seeded 45 days. Unit tests on the engine against the scenario blocks: assert the engineered outcomes in §A's table actually come out (Go days are Go, Drift triggers in the overload block, no Call before day 4).
**Exit:** `daily_snapshots` holds 45 rows for the mock user; spot-checks match the scenario table; engine tests pass.

### M2 — Today + Decode *(1 week)*
Design spec §5.2–§5.3, rendering real `daily_snapshots` rows. All three Call states with correct ink + highlight-stroke sweep, Signal row + sparkline (fed from `metric_samples`), Decode sheet with factor rows, source chips, yellow key-phrase highlight, confidence badges, the three calibration overlays, and the no-data state.
**Includes a debug day-picker** (temporary, behind the flask icon): select any of the 45 seeded days and the app renders Today/Decode *as of* that day — this is how every state gets visually verified on the emulator without waiting for real time to pass. It stays useful for the entire dogfooding phase.
**Exit:** paging through the 45 days shows every state in the design spec's §7 matrix rendering correctly; copy matches spec verbatim.

### M3 — Trends + Log + Sources *(4–5 days)*
Design spec §5.4–§5.6. Trends: Drift status line (verify it goes orange across days 28–35 via the day-picker), four Vico charts with baseline bands. Log: RPE segments + feel chips + note writing real manual `metric_samples` (recording_method=manual) for the mock user, synced to Supabase; history list with Call pills opening read-only Decode. Sources: renders the synthetic sources with status dots, last-data timestamps, and the live dedup priority table.
**Exit:** a manual RPE logged in the emulator appears in Supabase; Drift line verified orange in the overload block; charts match reference.html styling.

### M4 — Daily loop + notification *(2–3 days)*
Morning notification per spec §6 — but since there's no real overnight sync yet, trigger = a WorkManager job that fires when a new `daily_snapshot` appears for "today" (for testing: a debug button "simulate morning data arrival" that advances the seed by one day and lets the notification fire naturally). One per day, never blind, correct verb + Decode one-liner per Call state.
**Exit:** all three notification variants observed on the emulator via simulation.

### M5 — Debug mode, disagree, export *(3–4 days)*
Design spec §5.7 in full: global flask toggle, lime hairline frame, raw decision line, per-metric grid (raw → normalized → baseline → z → weight, source chips, UTC timestamps) rendered from the `debug` payload, "Disagree with the Call" sheet writing `call_feedback`, JSON day-export via share sheet. Absorb the day-picker into this mode permanently.
**Exit:** every number on Today traceable in Debug to a seeded record; a disagreement lands in Supabase; export produces a complete, valid JSON for any picked day.

### M6 — Dedup resolver + ingestion plumbing *(3–4 days)*
The pieces from v1's M2 that don't need a device: source-priority dedup resolver in `domain/dedup/` (config-driven, unit-tested against the seeded overlapping `mock_whoop`/`mock_oneplus` sleep sessions), the WorkManager periodic sync skeleton, and the Health Connect → MetricSample adapters in `data/adapters/healthconnect/` with unit tests against synthetic HC records. No permissions UI yet; adapters are exercised only by tests.
**Exit:** dedup resolves the seeded overlaps per the priority table; adapter + resolver tests green.

### M7 — Health Connect live + physical device *(3–4 days)*
Former v1 M1+M2 device-facing work: permission flow (incl. history + background read), HC-not-installed and partial-denial handling, wiring adapters + dedup + sync loop to real Health Connect, Inspector as a Debug sub-screen. Build release APK, **sideload onto the real phone** (WHOOP + OnePlus paired), grant permissions, wear for 2–3 days.
**Decision gate (unchanged from v1):** the Inspector shows whether WHOOP writes HRV/sleep to Health Connect with usable fidelity → decides M8.
**Cutover detail:** real data and seed data coexist under different source names; once real data flows, run the seed script's delete-only section to remove mock rows for the real user (or use a separate real user account from the start — simplest: keep the mock user for the emulator, create your real account on the phone).
**Exit:** a real overnight Call computed from your actual wearables.

### M8 — WHOOP/Strava cloud adapters *(conditional, ~1 week)*
Unchanged from v1 M6. Only if the M7 gate finds Health Connect insufficient.

### M9 — Optional Lovable web debug console
Unchanged from v1 M7. Parallel-safe any time after M5.

---

## §C. What this reordering changes and what it doesn't

- **Unchanged:** architecture, rules file, schema, engine spec, design spec, the M6-cancellation gate (now at M7), all v1 risks.
- **Changed:** every screen and the engine are now verified against a complete, state-covering dataset *before* any device dependency; the physical phone enters exactly once, at M7, receiving a finished app.
- **New asset:** the seed script + day-picker become permanent test infrastructure — future engine tuning (post-disagreements) gets validated by re-running backfill over the same 45 days and paging through the results.

**Timeline:** M1–M5 ≈ 3.5–4 weeks to a fully loaded app on the emulator; M6–M7 ≈ 1 week to live data on your phone; +1 week if M8 triggers.
