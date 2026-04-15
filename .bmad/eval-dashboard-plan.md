# Implementation Plan: BMAD Eval Dashboard

**Spec:** `docs/superpowers/specs/2026-03-26-bmad-eval-dashboard-design.md`
**Output:** `.bmad/bmad-agent-eval-dashboard.html`

## Steps

1. Write HTML skeleton — dark theme, header, filter bar, 3 metric sections, stats banner
2. Implement FileReader-based JSONL loader — file picker, parse, display record count
3. Implement filter pills — dynamic generation from data, AND-combined filtering
4. Implement metric tile rendering — averaged values, formatted units, progress bars
5. Implement delta calculation — baseline vs assisted comparison per metric, directional colouring
6. Implement Welch's t-test — inline JS, degrees of freedom + p-value approximation, significance table
7. Wire reactivity — recompute all tiles + banner on every filter change
8. Verify with eval-log.jsonl — load the existing 1-record file, confirm rendering
