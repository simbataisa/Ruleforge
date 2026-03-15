# UI Spec — Space Context Banners

**Date:** 2026-03-15 | **UX Designer:** (BMAD)

## Banner Designs

### Sandbox Space Banner
- **Color:** Amber/Yellow (warning tone — experimental environment)
- **Icon:** `FlaskConical` (Lucide) — conveys experimental/lab context  
- **Text:** "This is the Sandbox space. Any rules created here will be visible to all spaces."
- **Placement:** Below the space header, above the stats bar
- **Style:** `bg-amber-50 border border-amber-200 text-amber-800`

### Production Space Banner
- **Color:** Blue (informational — production environment)
- **Icon:** `Shield` (Lucide) — conveys production/protected context
- **Text:** "This is a production space. Any rules created here will be visible to all spaces."
- **Placement:** Below the space header, above the stats bar
- **Style:** `bg-blue-50 border border-blue-200 text-blue-800`

## Wireframe (ASCII)

```
┌────────────────────────────────────────────────────────────────┐
│  [← Back] Default Space             [⚙ Settings]              │
├────────────────────────────────────────────────────────────────┤
│  🛡 This is a production space. Any rules created here will    │ ← ProductionBanner
│     be visible to all spaces.                                  │
├────────────────────────────────────────────────────────────────┤
│  📋 48 rules   📅 Created Jan 1, 2026                          │ ← stats bar
├────────────────────────────────────────────────────────────────┤
│  Rules                                                         │
│  ...                                                           │
└────────────────────────────────────────────────────────────────┘
```

```
┌────────────────────────────────────────────────────────────────┐
│  [← Back] Sandbox                   [⚙ Settings]              │
├────────────────────────────────────────────────────────────────┤
│  🧪 This is the Sandbox space. Any rules created here will     │ ← SandboxBanner
│     be visible to all spaces.                                  │
├────────────────────────────────────────────────────────────────┤
│  📋 0 rules   📅 Created Jan 1, 2026                           │ ← stats bar
└────────────────────────────────────────────────────────────────┘
```

## Accessibility
- Banner div: `role="status"` for non-critical informational content
- Icon: `aria-hidden="true"` (decorative)
- Text contrast ratio: amber-800 on amber-50 ≥ 4.5:1 ✅, blue-800 on blue-50 ≥ 4.5:1 ✅

