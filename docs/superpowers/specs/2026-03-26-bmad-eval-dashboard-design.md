# BMAD Agent Eval Dashboard — Design Spec

**Date:** 2026-03-26
**Status:** Approved

## Overview

A single self-contained HTML file (`bmad-agent-eval-dashboard.html`) placed in `.bmad/` that visualises BMAD productivity metrics from `eval-log.jsonl`. The user opens it directly in a browser (`file://`) and loads the JSONL via a file picker. No server, no dependencies, no internet required.

## Data Source

- **File:** `.bmad/eval-log.jsonl` — one JSON object per line, schema defined by `/bmad-eval`
- **Loading:** User clicks "Load eval-log.jsonl" button; browser FileReader API parses each line as JSON and discards malformed lines silently
- **Schema fields consumed:**

| Field | Type | Section |
|---|---|---|
| `timeToArtifact` | hours (float) | Speed |
| `timeToDraft` | hours (float) | Speed |
| `iterTurnaround` | hours (float) | Speed |
| `firstPassRate` | 0.0–1.0 | Quality |
| `nfrCoverage` | 0.0–1.0 | Quality |
| `archDebt` | integer count | Quality |
| `alternatives` | integer count | Coverage |
| `risks` | integer count | Coverage |
| `scenarios` | integer count | Coverage |
| `week` | integer | Filter |
| `phase` | "baseline" \| "assisted" | Filter |
| `practitioner` | string | Filter |
| `_collectedAt` | ISO-8601 | Display only |

## Layout

### Header

- Title: "BMAD Agent Productivity" / subtitle "Evaluation Dashboard"
- "📂 Load eval-log.jsonl" button (triggers `<input type="file" accept=".jsonl">`)
- Badge: "{N} records loaded" — updates after file is parsed

### Filter Bar

Three pill-group filters, all rendered dynamically from loaded data:

- **Week** — "All" + one pill per distinct `week` value (sorted ascending)
- **Phase** — "All" | "Baseline" | "Assisted"
- **Practitioner** — "All" + one pill per distinct `practitioner` value

Active pill is highlighted (sky blue). Filters are AND-combined. "All" is selected by default.

### Metric Sections

Three sections, each with 3 tiles in a 3-column grid:

**⚡ SPEED** (blue left border, `#38bdf8`)
- Time to Artifact (`timeToArtifact`) — unit: "h"
- Time to Draft (`timeToDraft`) — unit: "h"
- Iter. Turnaround (`iterTurnaround`) — unit: "h"

**✦ QUALITY** (mixed accents)
- First Pass Rate (`firstPassRate`) — displayed as percentage; green left border
- NFR Coverage (`nfrCoverage`) — displayed as percentage; amber left border
- Arch Debt (`archDebt`) — unit: "items"; purple left border

**◎ COVERAGE** (orange left border, `#fb923c`)
- Alternatives (`alternatives`) — unit: "ADRs"
- Risks Identified (`risks`) — no unit suffix
- Scenarios (`scenarios`) — unit: "stories"

### Tile Structure

Each tile contains:
1. **Metric name** (small label, muted)
2. **Value** — average of filtered records; formatted to 1 decimal place for floats, integer for counts
3. **Delta line** — when both baseline and assisted records exist in the filtered set:
   - Formula: `delta = (mean_assisted - mean_baseline) / mean_baseline * 100`
   - Lower-is-better metrics (`timeToArtifact`, `timeToDraft`, `iterTurnaround`, `archDebt`): negative delta shown as "▼ X%" in green; positive as "▲ X%" in red
   - Higher-is-better metrics (`firstPassRate`, `nfrCoverage`, `alternatives`, `risks`, `scenarios`): positive delta shown as "▲ X%" in green; negative as "▼ X%" in red
   - When no baseline records in the filtered set: "— no baseline yet" in muted grey
4. **Progress bar** — thin (3px) bar showing the tile's current value as a proportion of the max value for that metric across **all loaded records** (unfiltered), so the scale stays stable when filters change

### Statistics Banner

Fixed at the bottom of the metric grid. Three states:

1. **No data loaded:** "Load a file to begin."
2. **Insufficient data:** "Need ≥4 baseline and ≥4 assisted records for significance testing. Currently: {N} assisted, {M} baseline." Counts reflect active filter.
3. **Sufficient data:** Runs Welch's t-test on each metric (baseline vs assisted). Shows a table: metric name | mean baseline | mean assisted | p-value | significant (p < 0.05). Row colour: green if significant improvement, red if significant regression, grey otherwise.

## Behaviour

- **Empty state** (no file loaded): all tile values show "—", filters show only "All", banner shows state 1
- **Filter interaction:** all tile values, deltas, progress bars, and the stats banner recompute instantly on every filter change
- **Multi-practitioner:** when "All" practitioners selected, values are averaged across all matching records
- **Malformed JSONL lines:** silently skipped; total record count reflects only valid parsed lines
- **File re-load:** clicking the button again replaces all data (no merge)

## Visual Design

- **Theme:** Dark (`#0f172a` background, `#1e293b` cards, `#334155` borders)
- **Typography:** System font stack; metric values in 700 weight
- **Accent colours:**
  - Speed: `#38bdf8` (sky blue)
  - Quality — First Pass Rate: `#34d399` (emerald); NFR Coverage: `#f59e0b` (amber); Arch Debt: `#a78bfa` (violet)
  - Coverage: `#fb923c` (orange)
  - Positive delta: `#34d399`; Negative delta: `#f87171`; Neutral: `#475569`
- **Active filter pill:** `#0ea5e9` background, white text
- **No external fonts, no CDN assets**

## File Location

`.bmad/bmad-agent-eval-dashboard.html` — co-located with `eval-log.jsonl` so the file picker defaults to the same directory.

## Implementation Notes

- Single HTML file; all CSS in `<style>`, all JS in `<script>` at bottom of `<body>`
- FileReader API used for JSONL parsing — no `fetch`, no `XMLHttpRequest`
- Welch's t-test implemented inline (no library): degrees of freedom via Welch–Satterthwaite, p-value via numerical approximation of the t-distribution CDF
- No build step, no minification required
