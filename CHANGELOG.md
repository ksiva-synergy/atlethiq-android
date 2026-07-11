# Changelog

## [Milestone 4] - 2026-07-11

Built the daily loop and morning notification per design spec §6 (execution-plan-v2 §B M4), against the existing 45-day seeded mock history — no Health Connect, no real overnight sync.

Introduced a persisted `AppStateStore` (SharedPreferences) that models the daily loop on mock data: `frontierDay` is the app's current latest-with-data day (what Today opens on — defaulting to the latest real snapshot, day 44, so M2/M3's open-on-latest behavior is preserved), and `lastNotifiedDay` guards against posting more than once per day. A `MorningCallWorker` (`CoroutineWorker`, dependencies pulled through a Hilt `EntryPoint` so the default WorkManager factory constructs it — no `hilt-work` needed) fires the notification: it reads the **real** persisted snapshot for the frontier day and posts only if the frontier has advanced past `lastNotifiedDay` **and** a Call (go/hold/back_off) actually exists — never blind, and calibrating/no_data days are skipped. The worker is enqueued on every app open, so reopening on an already-notified day runs the job and it no-ops (frontier ≤ lastNotified). `CallNotifier` builds one variant per Call state — title carries the verbatim Call verb ("The Call: Go hard." / "Hold." / "Back off."), body is the state's one-line Decode summary, accent color bound to the state's ink (lime/cyan/orange), a white "A" monogram small icon (`ic_notification_monogram`), no image, no actions, and a fixed notification id so a later post replaces any earlier one (at most one visible). Tapping a notification carries the day as an intent extra; `MainActivity` (`singleTop`) persists it to `pendingNotifDay`, and `MainScreen` consumes it on resume — scoping Today to that day and forcing the Today tab (covers both cold and warm start). Added the `POST_NOTIFICATIONS` permission with a runtime request, and the channel is created in `AtlethiqApplication`.

Since there is no real overnight sync on mock data, the debug layer (behind the flask, alongside the day-picker) gains two controls: **"Simulate morning data arrival"** advances the frontier one day and enqueues the WorkManager job — the actual daily-loop mechanism, not a direct-post shortcut — and **"Arm arrival at day N"** repositions the frontier to (selected day − 1) so the next simulate lands on any chosen day and fires its Call, making every state reachable in one advance without walking the whole seed.

Also completed the obsolete scaffold `FakeMyModelRepository` in `MainScreenViewModelTest` (it had stopped compiling when M3 widened the `DataRepository` interface) so `testDebugUnitTest` is green again.

### Verification Results
Verified end-to-end on the emulator with real screenshots and adb/logcat evidence (not descriptions):
- **Go** (day 27, 2026-06-22): armed then simulated; logcat `MorningCallWorker: new data detected for day 27 (2026-06-22) call=go — posting notification`; shade showed "The Call: Go hard." / "You're recovered and trending up. Today's the day to push." with a lime-tinted "A" monogram.
- **Hold** (day 42, 2026-07-07): logcat confirmed `call=hold — posting notification`; shade showed "The Call: Hold." / "You're steady, not surging. Train — but don't chase a peak today." with a cyan-tinted monogram.
- **Back off** (day 34, 2026-06-29): logcat confirmed `call=back_off — posting notification`; shade showed "The Call: Back off." / "Recovery hasn't caught up with this week's load. Easy day — details inside." with an orange-tinted monogram. Each new post replaced the prior one (single fixed id).
- **WorkManager path (not a bypass):** every post was preceded by the worker's `run start: frontierDay=N lastNotifiedDay=N-1` → `new data detected` → `notification posted; lastNotifiedDay advanced to N` log sequence.
- **Deep link:** from the Trends tab on day 1, tapping the Hold notification switched to the Today tab scoped to day 42 (TUE 07 JUL).
- **No re-fire:** reopening the app on day 42 (frontier=42, lastNotified=42) logged `no new day beyond lastNotified (42 <= 42) — skip, not re-firing`; `dumpsys notification` reported 0 posted Atlethiq notifications. First launch likewise pins lastNotified to the latest real day, so no notification fires blind for pre-existing data.

## [Milestone 3] - 2026-07-11

Built Trends, Log, and Sources per design spec §5.4–§5.6, against the existing 45-day seeded mock history (execution-plan-v2 §B M3) — no Health Connect, no real ingestion.

Trends renders four Vico chart cards (HRV 7d-vs-30d, resting HR, sleep duration-vs-need, load 7d/28d directional) sourced entirely from the persisted `daily_snapshots.debug` payload — no readiness math duplicated client-side. A y-axis zoom (`AxisValueOverrider.fixed`, padded to the data's real range, excluding the 0.0 placeholder baseline the engine reports before a 30-day window exists) replaces Vico's zero-based default so the days 28–35 overload block reads as a visible HRV sag rather than a sliver against a 0–100 axis; the chart auto-scrolls (`Scroll.Absolute.x`) to center the day-picker's selected day, with a persistent marker (`DefaultCartesianMarker`, `LabelPosition.AroundPoint`) pinning it. The Drift status line reads `debug.drift_rising`/`consecutive_sag_days` off the selected day's snapshot and is the one place orange can glow independent of Today's ink, per spec.

Log adds a mono RPE 1–10 segmented selector (fills cyan-to-orange toward the high end) and Fresh/Normal/Flat/Wrecked feel chips; "Log it" writes real `rpe` and `subjective_feel` MetricSamples (`recording_method=manual`) to Room and upserts them to Supabase via a dedicated network-only DTO (`MetricSampleUpsert`) that mirrors the `metric_samples` columns exactly, keeping the Room entity's local-only fields (epoch millis, priority rank) out of the wire payload. History lists reverse-chronologically with Call pills, session summaries, and logged feel chips; tapping a row reuses the M2 Decode sheet read-only.

Sources shows a card per seeded source app (`mock_whoop`, `mock_oneplus`, `manual`) with a status dot computed relative to the day-picker's selected date (flowing/stale/never, not wall-clock — there's no real sync loop yet) and an attribution table. The attribution table is explicitly flagged as a placeholder priority config mirroring the dedup order the `compute-call` engine currently hardcodes — the real config-driven resolver is scoped to M6.

### Verification Results
- Trends on day 22 (2026-06-17, fresh/tapered block): "Drift: none — recovery is keeping pace with load", muted.
- Trends on day 35 (2026-06-30, overload block): "Drift: rising — 3 days of load outpacing recovery", orange with orange dot; HRV chart visibly sags from ~62ms to ~47ms across the block.
- All four charts render real 45-day series with dim-gray baseline/need lines and gridlines in the `Line` token only.
- Logged a real RPE 7 / Wrecked / note entry from the emulator; appeared immediately in the Log history list; confirmed live in Supabase via an authenticated REST query returning both the `rpe` and `subjective_feel` rows with the correct payload.
- Sources screen confirmed all three mock sources flowing (lime) after the log write refreshed source freshness; attribution table matches the seed data's actual dedup behavior.

## [Milestone 0] - 2026-07-11

Initialized the core Android application architecture and design system. Created Room database entities and separate DAOs for `MetricSample`, `DailySnapshot`, and `CallFeedback` conforming to the canonical model. Configured Hilt dependency injection modules for local database and Supabase client integration. Integrated Google Fonts provider for Inter, Space Grotesk, and JetBrains Mono fonts and extracted design tokens (Color, Type, Shape) from the reference HTML. Built the bottom navigation scaffold with custom selected-tab indicators and screen header placeholders, and wired a supabase-kt authentication flow with a customized login screen. Created the Supabase SQL schema migrations and RLS policies. Fixed a startup crash (`NoClassDefFoundError: Failed resolution of: Lio/ktor/client/plugins/HttpTimeout`) by upgrading the explicit Ktor client version pin to `3.0.0` in the versions catalog, aligning it with the runtime expectations of `supabase-kt 3.0.1`.

### Verification Results
Closed out Milestone 0 with full end-to-end integration and verification:
- Wired live Supabase credentials through `local.properties` (fully gitignored).
- Added `INTERNET` permission to `AndroidManifest.xml`.
- Successfully ran the SQL schema to create database tables and enable RLS with policies.
- Programmatically verified database connectivity: successfully performed queries on all four tables (`metric_samples`, `daily_snapshots`, `call_feedback`, and `engine_config`) via the Supabase client on app startup upon login.
- Validated login flow against the live Supabase instance with a real user.

