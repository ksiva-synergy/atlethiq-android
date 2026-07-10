# Atlethiq — Brand & Design System
**Reusable reference for any Atlethiq design session.** Paste this whole file at the start of any Claude Design (or other) session, before the project-specific brief. It contains nothing specific to one surface — no screens, no product-specific layouts — so it holds true whether the deliverable is the landing page, the mobile app, a pitch deck, or a coach dashboard. Project-specific specs should assume this document and only add what's new.

---

## 1. What Atlethiq is

Sports performance intelligence. The market is saturated with dashboards, not decisions — twelve charts nobody reads. Atlethiq's wedge is **The Call**: one plain-language daily training directive (go hard / hold / back off), with the reasoning one tap away in **Decode**.

**Positioning statement:** For serious athletes and the coaches who push them, Atlethiq is the performance-intelligence platform that converts raw training data into clear, next-step decisions — because the edge isn't in measuring everything, it's in knowing what to do about it.

| Atlethiq is | Atlethiq is not |
|---|---|
| Decisive | A data dump |
| Premium and considered | Cheap or gamified |
| Built for people who train hard | A casual step-counter |
| Smart in a quiet, confident way | Loud, bro-y, hype-driven |

**Feel in one line:** a premium instrument, not a consumer app — the dashboard of a high-end car, dark and calm, where exactly one thing glows.

**Name:** Atlethiq (*ath-LEH-thick*) — athletic, spelled the way the brain gets there first. Not a typo to apologize for; the whole point. Never correct it, never apologize for it.

**Taglines:** Primary — *"Performance isn't always textbook."* Alternates: "Train smarter than your numbers." / "Less data. More decision." / "Your edge, decoded." / "Intelligence in motion."

---

## 2. Audience

- **Primary — The Committed Athlete (25–40).** Trains 5+ times a week, already wears a watch, already has the data, is quietly frustrated none of it tells them what to change. Wants an edge, not a hobby.
- **Secondary — The Performance Coach.** Manages a roster, needs to spot the one athlete trending toward injury or ready to peak.
- **Tertiary — The Crossover Pro.** Semi-pro/pro athletes and S&C staff — aspirational anchor even as a small revenue slice.

All three: they don't want to be coached by a cartoon. They want a tool as serious about performance as they are.

---

## 3. The five signature features

| Feature | What it is |
|---|---|
| **The Call** | The hero. One daily decision — go hard / hold / back off — stated as a verb, not twelve charts. |
| **Signal** | The readiness score sitting underneath The Call. |
| **Drift** | Early-warning detection for the slow ramp toward overtraining or injury. |
| **Peak Window** | Forecasts when you'll be at your physical best. |
| **Decode** | The plain-language "why" behind every number. |

## 4. Tiers

Named after the Base → Build → Peak periodization cycle every serious athlete already knows.

| | **Base** | **Build** | **Peak** | **Squad** |
|---|---|---|---|---|
| For | Curious / new | The committed athlete | Pros & semi-pros | Coaches & teams |
| Price (indicative) | Free | $12/mo | $29/mo | $19/athlete/mo |
| Palette | Signal | Signal | **Luxe** | Signal |

Base hooks with a free Call. Build is where most committed athletes land. Peak is the aspirational pro tier, visually rewarded with the Luxe gold palette. Squad opens the coach revenue line.

---

## 5. Color system — the highlighter-ink metaphor

**Core metaphor:** a highlighter marks the one line that matters on a dense page. Atlethiq's whole visual system is built on that single act — highlight what matters, ignore the rest. **The discipline: one highlight at a time.** A highlighter is useless if every line glows; exactly one ink appears live per view, everything else stays dark. A second ink may appear only desaturated (~40% opacity) as passive reference — never at full strength alongside another.

### 5.1 Signal palette (default — Base & Build tiers, all marketing, all surfaces unless a project explicitly calls for Luxe)

| Token | Hex | Role |
|---|---|---|
| Base | `#0B0E11` | Near-black background — the canvas |
| Surface | `#171B21` | Cards, panels |
| Surface deep | `#12161B` | Inset wells, chart backgrounds *(app-derived; safe to use in any dark UI)* |
| Surface raised | `#1F252D` | Elevated / pressed states |
| Line | `#2A313A` | Hairlines, dividers, gridlines |
| Text | `#EDEFF2` | Primary text (off-white) |
| Muted | `#7B848F` | Secondary text, labels (slate) |

### 5.2 The five highlighter inks

Each ink is both a **feature color** and a **functional state color** — this dual mapping is load-bearing and must stay consistent across every surface.

| Ink | Hex | Feature | State meaning |
|---|---|---|---|
| Signal Lime | `#C6F03C` | The Call | **Go** — push today |
| Electric Cyan | `#3CE0F0` | Signal | **Hold** — maintain |
| Flag Orange | `#FF7A1A` | Drift | **Back off** / caution |
| Ink Pink | `#FF5CA8` | Peak Window | Peak-forecast accents |
| Read Yellow | `#F0E84A` | Decode | Highlighted "why" text |

**On-ink text:** all five inks are light — text placed on an ink fill is always `#0B0E11` (base), never white.

### 5.3 Luxe palette (Peak tier and future high-end features only)

Same skeleton, re-skinned in champagne gold. Never mix with Signal on one surface; never use gold as a decorative accent outside an explicit Peak-tier context.

| Token | Hex | Role |
|---|---|---|
| Base | `#0C0C0E` | Warm near-black |
| Surface | `#161519` | Warm graphite |
| Accent | `#D4AF6A` | Champagne gold — the "act on this" color, premium |
| Accent 2 | `#A9B4BD` | Platinum — data, charts, hold states |
| Text | `#F2EFE9` | Bone |
| Muted | `#8A8378` | Taupe |

---

## 6. Typography

| Role | Face | Notes |
|---|---|---|
| Display / headlines / The Call verb | **Space Grotesk** 500–700 | Tight, confident, slightly condensed, athletic |
| Body / UI | **Inter** 400–600 | Clean, neutral, legible at small sizes in data-dense views |
| Data / numbers / stats / timestamps | **JetBrains Mono** 400–700 | Tabular numerals. Numbers should feel engineered — always mono, never proportional. |

Fonts import: `Space+Grotesk:wght@400;500;600;700` / `Inter:wght@400;500;600` / `JetBrains+Mono:wght@400;500;700` (Google Fonts).

---

## 7. Voice & tone

Atlethiq speaks like a sharp coach who doesn't waste your time — direct, knowledgeable, never hyped.

- **Say the decision, not the data.** "Back off today." — not "Your HRV is 12% below your 7-day baseline suggesting…"
- **Confident, not loud.** No exclamation marks. The brand is sure of itself; it doesn't need to yell.
- **Respect the athlete's intelligence.** No baby-talk, no gamified confetti, no "champ."
- **Plain over jargon — but precise when it counts.** Use the technical term when it's right, then translate it in the same breath.

| ❌ | ✅ |
|---|---|
| "🔥 Crush your goals today, champ!! You've got this!!!" | "You're peaking. Today's the day to go hard." |
| "Your acute:chronic workload ratio has exceeded threshold." | "You're ramping too fast. Ease off, or this becomes an injury." |

Applies everywhere: UI copy, notifications, marketing, error states, empty states. Sentence case by default; uppercase + letter-spacing reserved for short eyebrow labels only.

---

## 8. Logo & mark

**Direction:** the swapped letters, leaned into rather than hidden.

1. **The Transposition mark** — wordmark "ATLETHIQ" in the display grotesque, with the *L* and *T* subtly distinguished (a tick of lime, a slight offset, or a connecting stroke) — a quiet nod to letters in motion.
2. **The monogram** — an **A** built from two angled strokes reading as forward motion / a rising chart line. Works as an app icon on the dark base with a lime edge.
3. **The "iQ" lockup** — the final "iQ" treated as a distinct unit (intelligence) for compact placements.

Standing recommendation: wordmark + the **A** monogram as the icon.

---

## 9. Cross-surface design discipline

These rules apply regardless of what's being designed:

- **Dark-first, always.** No light theme has been designed or approved. If a project seems to need one, flag it as a new decision, not an assumption.
- **One ink glows.** Never two full-strength inks live on the same view.
- **Borders over shadows.** Elevation reads through `--line` hairlines and slightly lighter surfaces, not drop shadows — instrument, not paper.
- **No gauges, dials, or radial progress rings.** The instrument language here is typographic and linear, not automotive-dashboard-cliché.
- **No stock fitness photography, ever.**
- **No emoji, streaks, badges, or confetti** in-product or in marketing.
- **Numbers are always mono.** Any time a statistic, score, timestamp, or measurement appears, it's JetBrains Mono, tabular.
- **Motion is restrained.** One orchestrated signature moment per surface is plenty; everything else is a quiet 150–250ms fade or slide. Respect reduced-motion settings.
- **Radius:** roughly 16 for cards, 12 for inputs/chips, 28 for sheets/modals, full-round for pills — consistent across surfaces unless a project states otherwise.

---

## 10. How to use this document

- **Starting a new design session?** Paste this file first, then the project-specific brief (screens, pages, sections needed). Don't restate brand tokens in the brief — reference this doc instead so the two never drift apart.
- **Need something this doc doesn't cover** (e.g., a new component, an illustration style, a chart type)? Design it *from* these tokens and add the resulting pattern back here afterward, so the next session inherits it too.
- **Tempted to add a sixth ink, a light mode, or a new typeface?** That's a brand decision, not a layout decision — flag it explicitly rather than deciding it inside a single project spec.

---

*Atlethiq — Performance isn't always textbook.*
