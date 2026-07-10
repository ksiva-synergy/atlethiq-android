# Atlethiq Mobile App — Design Spec (Dogfooding Build v1)

**Handoff document for Claude Design.** Everything needed to design the Atlethiq Android app is in this file: brand tokens, voice rules, navigation model, every screen with real copy, and every system state. Follow the tokens and copy exactly; where an axis is left free, it is marked as a design decision.

---

## 0. Brief

**Product:** Atlethiq — sports performance intelligence. The market is saturated with dashboards; Atlethiq ships a decision. Each morning the app issues **The Call**: one plain-language training directive — *go hard, hold, or back off* — with the reasoning one tap away (**Decode**).

**This build:** a single-user dogfooding app. The founder wears a WHOOP and a OnePlus Watch 2; data arrives via Android Health Connect. The app must serve two jobs at once: (1) a polished daily athlete experience good enough to preview the real product, and (2) a transparency/debug layer underneath that lets the founder audit every number that produced today's Call.

**Platform:** Android native (Jetpack Compose, Material 3 as the component base — but the visual identity below overrides Material defaults everywhere they conflict). Design at **412 × 915** (Pixel-class viewport), dark mode only. No light theme exists.

**Deliverable expected from Claude Design:** high-fidelity screen designs for every screen and state listed in §5–§7, plus the shared components in §4. A clickable flow (onboarding → home → decode → debug) is a bonus, not a requirement.

**Feel in one line:** a premium instrument, not a consumer app — the dashboard of a high-end car, dark and calm, where exactly one thing glows.

---

## 1. Brand tokens

### 1.1 Core surfaces & text (Signal palette — the only palette in this build)

| Token | Hex | Use |
|---|---|---|
| `--base` | `#0B0E11` | App background. The canvas everything sits on. |
| `--surface` | `#171B21` | Cards, panels, sheets |
| `--surface-deep` | `#12161B` | Inset wells, chart backgrounds |
| `--surface-raised` | `#1F252D` | Elevated elements, pressed states |
| `--line` | `#2A313A` | Hairline borders, dividers, chart gridlines |
| `--text` | `#EDEFF2` | Primary text |
| `--muted` | `#7B848F` | Secondary text, labels, captions |

### 1.2 The five highlighter inks

The brand metaphor: a highlighter marks the one line that matters on a dense page. Each ink maps to a feature **and** a functional state. The cardinal rule — **one highlight at a time.** Only one ink glows per screen; everything else stays dark.

| Ink | Hex | Feature | State meaning |
|---|---|---|---|
| Signal Lime | `#C6F03C` | The Call | **Go** — push today |
| Electric Cyan | `#3CE0F0` | Signal (readiness score) | **Hold** — maintain |
| Flag Orange | `#FF7A1A` | Drift | **Back off** — caution / warning |
| Ink Pink | `#FF5CA8` | Peak Window | Peak forecast accents |
| Read Yellow | `#F0E84A` | Decode | Highlighted "why" passages |

Ink usage in this app: today's Call takes exactly one of lime / cyan / orange and that ink tints the entire Today screen's accent slots. Yellow appears only inside Decode as the text-highlight treatment. Pink appears only on Peak Window elements (minimal in v1). Never show two inks at equal intensity on one screen — a secondary ink may appear only desaturated/dimmed (40% opacity) as a reference, e.g. gridline annotations.

On-ink text: all five inks are light enough that text placed **on** an ink fill must be `#0B0E11` (base), never white.

### 1.3 Luxe palette (reference only)

Peak tier re-skins the same skeleton in champagne gold (`#D4AF6A` accent, `#0C0C0E` base, `#F2EFE9` text, `#A9B4BD` data, `#8A8378` muted). **Not used in this build** — include nothing gold. Listed so no gold is improvised.

### 1.4 Typography

| Role | Face | Notes |
|---|---|---|
| Display / The Call verb / headlines | **Space Grotesk** 500–700 | Tight, slightly condensed, athletic. The Call verb is the largest type in the app. |
| Body / UI | **Inter** 400–600 | Legible at small sizes in data-dense views |
| Data / numbers / timestamps / debug | **JetBrains Mono** 400–700 | Tabular numerals. Numbers should feel engineered. Every metric value in the app is mono. |

Suggested scale (free to tune ±10%): Call verb 64/68, screen title 28/34, card title 17/22, body 15/22, label 12/16 (letter-spaced +4%, uppercase for eyebrows), data-large 40/44 mono, data-small 15/20 mono.

### 1.5 Shape, spacing, motion

- Radius: 16 for cards, 12 for inputs/chips, 28 for sheets, full-round for pills. Nothing sharp-cornered except hairline dividers.
- Spacing on an 8pt grid; screens breathe — density lives inside cards, not between them.
- Borders over shadows: elevation is expressed with `--line` hairlines and slightly lighter surfaces, not drop shadows. Instrument, not paper.
- Motion: restrained. One orchestrated moment per screen max — the morning reveal of The Call (ink sweep, ~400ms, see §5.2) is the app's only signature animation. Everything else is standard 150–250ms fades/slides. Respect reduced-motion.
- Icons: thin-stroke (1.5px) geometric set, muted by default, ink-tinted only when active.

### 1.6 The signature element

**The highlight stroke.** A horizontal band of today's ink behind key text — like a highlighter swipe, slightly overshooting the text box, with a subtly irregular edge (not a perfect rectangle). It appears behind The Call verb on Today, behind the key sentence in Decode (in yellow), and as the selected-state treatment in navigation. This is the one memorable device; use it in those three places and nowhere else.

---

## 2. Voice rules (copy is design material here)

Atlethiq speaks like a sharp coach who doesn't waste your time.

- **Say the decision, not the data.** "Back off today." — not "Your HRV is 12% below baseline suggesting…"
- **Confident, not loud.** No exclamation marks. No emoji. Ever.
- **Respect the athlete's intelligence.** No confetti, no streaks, no "champ."
- **Plain over jargon, precise when it counts.** Use the technical term when it's the right one, then translate it in the same breath.
- Sentence case everywhere except eyebrow labels (uppercase, letter-spaced).
- All copy in §5 is real and final unless marked *(placeholder)*. Do not paraphrase The Call verbs.

---

## 3. Navigation model

Bottom navigation, four destinations. The Call owns the home tab; everything deeper is deliberately buried — progressive disclosure is a brand behavior.

| Tab | Label | Contents |
|---|---|---|
| 1 | **Today** | The Call, Signal score, Decode entry, today's plan |
| 2 | **Trends** | HRV, RHR, sleep, load vs. baseline; Drift status |
| 3 | **Log** | Manual entry: RPE, subjective feel, journal, session history |
| 4 | **Sources** | Connected devices, per-metric attribution, sync status |

Persistent top bar on every screen: Atlethiq **A monogram** (left), screen title (center, only off-Today), and a small **flask/beaker icon** (right) that toggles **Debug mode** globally (§7). Settings lives behind the monogram tap → sheet.

Selected tab treatment: the highlight stroke (today's ink) under the label — not a filled pill.

---

## 4. Shared components

- **Call pill:** compact status chip — ink dot + verb ("Go" / "Hold" / "Back off") in mono. Used in history rows, trends header.
- **Metric card:** eyebrow label (muted, uppercase) → large mono value → delta vs. baseline line ("↑ 6% vs 30-day") in muted, arrow tinted with the relevant ink only when it warrants attention.
- **Confidence badge:** dot + word — "Calibrating" (muted outline), "Provisional" (cyan outline), "Reliable" (solid cyan dot), "High confidence" (solid lime dot). Appears wherever a model output shows.
- **Source chip:** tiny tag naming the data origin — "WHOOP", "OnePlus", "Manual" — mono 11px, muted, hairline border. Attached to every metric in Decode and Debug.
- **Sparkline:** 7-day mini chart, single ink line on `--surface-deep`, baseline band as a dim gray zone. No axes, no legends at this size.
- **Sheet:** bottom sheet, `--surface`, 28 radius top, grab handle, used for Decode detail, disagree flow, settings.

---

## 5. Screens

### 5.1 Onboarding (first run, 4 steps)

Dark, quiet, one idea per screen. Progress: four small dashes top-center, current one in lime.

**Step 1 — Identity.** Wordmark ATLETHIQ (Space Grotesk, the L and T subtly distinguished — a lime tick on the crossbar). Below: "Performance isn't always textbook." One field set: sport focus (chips: Endurance / Team sport / Racket sport / General), training days per week (stepper). CTA: "Continue".

**Step 2 — Connect your data.** Header: "Atlethiq reads what your devices already record." A list of what will be requested from Health Connect, each row: icon, metric name, why-line in muted:
- Sleep — "Duration and stages drive recovery."
- Heart rate variability — "The core readiness signal."
- Resting heart rate — "Your engine at idle."
- Workouts & heart rate — "Training load, measured not guessed."
- Steps — "Background load, nothing more."
CTA: "Connect Health Connect" (lime, full-width). Secondary text link: "What is Health Connect?"

**Step 3 — History permission.** Header: "Unlock your history." Body: "Health Connect shares the last 30 days by default. Granting history access lets Atlethiq build your baseline from everything your devices have recorded — your first Call gets smarter, faster." CTA: "Allow history access". Skip link: "Start from today instead" (muted).

**Step 4 — Calibration honesty.** This screen sets the expectation that The Call is not instant. A horizontal timeline graphic, four notches:
- Day 1 — "We start reading." (muted)
- Day 4 — "First provisional Call." (cyan)
- Day 14 — "Reliable." (cyan, solid)
- Day 30 — "High confidence." (lime)
Body: "Every readiness system needs a baseline. Yours is being built from night one — you'll see your data immediately, and The Call earns its confidence over your first month." CTA: "Start" (lime).

### 5.2 Today — The Call (the hero screen)

The most important screen in the product. Layout, top to bottom:

1. Top bar (monogram · date "THU 10 JUL" mono eyebrow · debug flask).
2. **The Call block** (~45% of viewport): eyebrow "THE CALL"; then the verb in Space Grotesk 700 at maximum size with the highlight stroke behind it in today's ink; beneath it one supporting line in text color.
3. **Signal row:** "SIGNAL" eyebrow, large mono score (0–100) tinted with today's ink, 7-day sparkline beside it, confidence badge.
4. **Decode teaser card:** the single strongest reason, one line, with a yellow highlight stroke on the key phrase; chevron → opens Decode sheet. E.g.: "HRV is running ==8% above== your monthly baseline."
5. **Today's plan card:** intensity target as plain language — "Quality work is on the table. Keep the hard session hard; skip junk volume." — with a small load context line in mono ("Load 7d: 412 · 28d avg: 465").

**The three Call states (design all three):**

| State | Ink | Verb | Supporting line |
|---|---|---|---|
| Go | Lime | **Go hard.** | "You're recovered and trending up. Today's the day to push." |
| Hold | Cyan | **Hold.** | "You're steady, not surging. Train — but don't chase a peak today." |
| Back off | Orange | **Back off.** | "You're ramping faster than you're recovering. Ease off, or this becomes an injury." |

**Calibration overlays on Today (design all three on top of the Hold state):**
- **Days 1–3 (Calibrating):** The Call block is replaced by a grayed state — verb slot shows "Calibrating" in muted, with "First Call in {n} days" beneath and the timeline notch graphic from onboarding, miniaturized. Sleep and HR data still show below — partial value from day one.
- **Days 4–13 (Provisional):** full Call, but the ink renders at ~70% saturation, confidence badge reads "Provisional", and a one-line caption sits under the verb: "Early read — your baseline is {n} days from reliable."
- **Day 14+ (Reliable / High confidence):** full ink, badge only.

**Signature motion:** on morning open, the highlight stroke sweeps left-to-right behind the verb (~400ms), the only time it animates that day.

**Empty/stale state:** if no overnight data by open — verb slot: "No read yet." Supporting: "Last night hasn't synced from Health Connect. Open WHOOP to push it, or pull to refresh." Muted, no ink.

### 5.3 Decode (sheet over Today)

The plain-language "why." Sheet title: "Decode" with eyebrow "WHY THIS CALL".

- **Lead sentence** restating the Call reasoning in one line, key phrase carrying the yellow highlight stroke.
- **Factor list**, 3–5 rows, each a metric card variant: factor name, direction word ("Lifting you" / "Neutral" / "Dragging you" — tinted lime/muted/orange), mono value + baseline comparison, source chip. Example rows:
  - HRV (RMSSD) — Lifting you — "62 ms · 7-day avg vs 57 ms baseline" — `WHOOP`
  - Sleep — Neutral — "7 h 12 m · need 7 h 30 m" — `WHOOP`
  - Resting HR — Dragging you — "54 bpm · +3 vs baseline" — `WHOOP`
  - Training load — Neutral — "7d 412 vs 28d 465 — directional only" — `Strava` *(placeholder)*
- **Honesty footnote** (muted, small): "Load from wrist data approximates court and racket sessions. Log RPE to sharpen it."
- Footer row: confidence badge + link "See the raw inputs →" (opens Debug pinned to today).

### 5.4 Trends

Header: current Drift status line — quiet when fine ("Drift: none — recovery is keeping pace with load", muted), loud when not ("Drift: rising — 9 days of load outpacing recovery", orange with orange dot; this is the one screen where orange may glow while Today shows another ink, because Drift *is* the orange feature).

Four full-width chart cards, each: eyebrow, 30-day line chart (single ink line — cyan for data by default — baseline band in dim gray, today marked), current mono value:
1. HRV 7-day vs 30-day baseline
2. Resting heart rate
3. Sleep (duration bars + need line)
4. Load (7d/28d, labeled "directional" in the eyebrow)

Charts follow the discipline: one line per chart, no rainbow multi-series, gridlines in `--line` only.

### 5.5 Log

Two zones:

**Quick log (top):** "How did today feel?" — RPE selector 1–10 as a horizontal band of segments (fills toward orange at high end, mono numerals), a feel row (chips: Fresh / Normal / Flat / Wrecked), optional note field. CTA "Log it". Caption: "Your read is the label the model learns from."

**History (below):** reverse-chron list, each row: date (mono), Call pill for that day, session summary if any ("Run · 52 min · load 86"), your logged feel chip. Tapping a row opens that day's Decode (read-only).

### 5.6 Sources

Header: sync status line — "Last sync 06:41 · next in 12 min" (mono, muted).

**Device list:** card per source — WHOOP (via Health Connect), OnePlus Watch 2 (via Health Connect), Manual entry. Each shows: status dot (lime = flowing, orange = stale >24h, muted = never), last data timestamp, data types received as small chips.

**Attribution table:** "Who wins what" — per-metric priority, mono table: HRV ← WHOOP · Sleep ← WHOOP (OnePlus deduped) · Steps ← OnePlus · Body comp ← Manual. Caption: "When two devices report the same thing, the higher-priority source wins. Everything is kept; one is trusted."

### 5.7 Debug mode (the dogfooding layer)

Toggled by the flask icon; a global mode, not a screen. Visual grammar when active: a 1px lime hairline frames the entire viewport, and every screen gains annotation layers. Design the **Today screen in debug mode** as the exemplar:

- Under the Call verb: the raw decision line in mono — `state=HOLD · score=71 · conf=0.82 · rule=hrv_z(-0.4)>θ_backoff, <θ_go`
- Every metric gains: raw value → normalized value → baseline → deviation (z-score) → weight, in a compact mono grid, each with its source chip and UTC timestamp.
- A **"Disagree with the Call"** button (outline, orange) → bottom sheet: "What was your read?" (chips: Should've been Go / Hold / Back off), "Why?" (chips: Felt fresh / Felt wrecked / Life stress / Illness / Other + note), CTA "Log disagreement". Confirmation toast: "Logged. This trains the model."
- Footer actions: "Export today (JSON)" · "Snapshot to Supabase" — both mono, muted.

Debug is allowed to be dense and engineered-looking — JetBrains Mono everywhere, tighter spacing — but still on-token. It should feel like the instrument's service menu, not a different app.

### 5.8 Settings (sheet, minimal)

Profile (sport, training days), notification time window, units, history permission status, "Re-run Health Connect permissions", app version. Nothing else in v1.

---

## 6. Notification (design as a component)

One guaranteed daily push, fired when overnight data lands. Android notification mockup:
- Title: **"The Call: Back off."**
- Body: "Recovery hasn't caught up with this week's load. Easy day — details inside."
- Small monogram icon; no image, no actions except open.
Design one per Call state. Never more than this daily push plus (later) Drift alerts — restraint is a feature.

---

## 7. State matrix (checklist for coverage)

| Screen | States to design |
|---|---|
| Today | Go · Hold · Back off · Calibrating (d1–3) · Provisional (d4–13) · No data/stale |
| Decode | Standard · Provisional (confidence caption) |
| Trends | Normal · Drift rising |
| Log | Empty history · Populated |
| Sources | All flowing · One stale · Never connected |
| Debug | Today-in-debug · Disagree sheet |
| Notification | ×3 Call states |

---

## 8. What not to do

- No light theme, no gold/Luxe, no gradients as decoration.
- Never two inks glowing at once; never white text on an ink fill.
- No gauges, dials, or radial progress rings — this brand's instrument language is typographic and linear.
- No emoji, exclamation marks, streaks, badges, or confetti.
- No stock-fitness photography anywhere in the app.
- Don't soften the orange state — "Back off" should feel like a coach's hand on your chest, not an error message.

---

*Atlethiq — Performance isn't always textbook.*
