# Design Tokens Reference - Design System Foundation Guide

## What Are Design Tokens?

### Definition

Design tokens are the single source of truth for design decisions in your product. They are reusable, named values that represent colors, typography, spacing, shadows, animations, and other design properties. Instead of hardcoding values like `color: #2563EB`, you use a token name like `color-primary-500`, which points to the value.

### Why Design Tokens Matter

1. **Consistency:** Every product uses the same color, spacing, and typography values. No color drift where some parts of app are slightly different shades of blue.

2. **Theming:** Easy to switch between light and dark mode. Each token maps to different values in each theme (light blue → dark blue for dark mode).

3. **Efficiency:** Change a color once in the token definition, and it updates everywhere it's used. No searching through codebase for all instances of `#2563EB`.

4. **Accessibility:** Tokens ensure contrast ratios are maintained. All instances of text-primary use a color that meets WCAG AA standards.

5. **Collaboration:** Designers and developers speak same language. Designer says "use primary-500", developer knows exactly what color/size that is.

6. **Scalability:** As product grows, centralizing design decisions prevents chaos and keeps design cohesive.

### Token Hierarchy

Design tokens are organized in three tiers:

```
┌─────────────────────────────────────────────────────────┐
│  Tier 3: Component-Specific Tokens                      │
│  (e.g., button-primary-background, input-focus-border)  │
├─────────────────────────────────────────────────────────┤
│  Tier 2: Semantic Tokens (Context-Aware Aliases)        │
│  (e.g., interactive-primary, surface-default)           │
├─────────────────────────────────────────────────────────┤
│  Tier 1: Global/Primitive Tokens (Raw Values)           │
│  (e.g., color-blue-500, space-16, font-size-16)         │
└─────────────────────────────────────────────────────────┘
```

**Tier 1 (Global):** The base colors, sizes, and values. Rarely change. Example: `primary-50 through primary-900` for all shades of blue.

**Tier 2 (Semantic):** Named by purpose, not value. Example: `surface-default` might use `primary-50` in light mode and `primary-900` in dark mode.

**Tier 3 (Component):** Specific to components. Example: `button-primary-background` uses `interactive-primary` token.

---

## Token Naming Convention

### Format: `[CATEGORY]-[PROPERTY]-[VARIANT]`

Examples:
- `color-primary-500` — Color category, primary property, 500 variant
- `space-16` — Spacing category, 16px variant
- `font-size-body-lg` — Font size category, body property, large variant
- `duration-short` — Duration category, short variant

### Naming Rules

1. **Use kebab-case (lowercase with hyphens)**, not camelCase or PascalCase
   - Good: `color-primary-500`
   - Bad: `colorPrimary500` or `ColorPrimary500`

2. **Category names are nouns** (plural for clarity)
   - Good: `colors`, `spacing`, `typography`
   - Bad: `color-list`, `space-sizes`

3. **Be specific about context** (not just "blue")
   - Good: `color-primary-500`, `color-semantic-error`
   - Bad: `color-blue` (which blue? What context?)

4. **Include modifier for scale (if applicable)**
   - Good: `font-size-12`, `space-8`, `duration-300`
   - Bad: `font-small` (vague, what size exactly?)

5. **Reserved names for common scales**
   - Colors: `-50, -100, -200, ..., -900` (Tailwind-style)
   - Spacing: `-4, -8, -12, -16, -24, -32, -40` (4px base unit)
   - Typography: `-xs, -sm, -md, -lg, -xl, -2xl` (T-shirt sizes)
   - Duration: `-instant, -micro, -short, -medium, -long, -extra-long`

---

## Colour Tokens

### Primitive Palette (Base Colors)

Primitive colors are the raw, unfiltered color values. They form the color ramp from light to dark.

#### Primary Color Ramp (Brand Blue)

| Token | Hex | Usage | Light Mode | Dark Mode |
|-------|-----|-------|-----------|-----------|
| `color-primary-50` | #EFF6FF | Very light backgrounds, subtle hover states | Primary ramp base | N/A (too light) |
| `color-primary-100` | #DBEAFE | Light backgrounds, disabled states | Primary ramp | N/A |
| `color-primary-200` | #BFDBFE | Very light hover, selected states | Primary ramp | N/A |
| `color-primary-300` | #93C5FD | Light interactive hover | Primary ramp | Primary shade 800 (inverted) |
| `color-primary-400` | #60A5FA | Lighter interactive elements | Primary ramp | Primary shade 700 |
| `color-primary-500` | #2563EB | Primary brand color, buttons, links | Used everywhere | Used for hover/focus |
| `color-primary-600` | #1D4ED8 | Darker interactive, hover state | Used for hover | Primary shade 400 |
| `color-primary-700` | #1E40AF | Dark interactive, pressed state | Used for pressed | Primary shade 300 |
| `color-primary-800` | #1E3A8A | Very dark interactive | Used for very dark | Primary shade 200 |
| `color-primary-900` | #172554 | Darkest shade (rarely used) | Primary ramp end | Primary shade 50 |

**Usage Rule:** In light mode, use primary-500 as main. Use primary-600/700 for hover/pressed. In dark mode, invert: use primary-400 as main, primary-500 for hover.

#### Secondary Color Ramp (Accent Purple)

| Token | Hex | Usage |
|-------|-----|-------|
| `color-secondary-50` | #FAF5FF | Light secondary backgrounds |
| `color-secondary-100` | #F3E8FF | Secondary disabled states |
| `color-secondary-200` | #E9D5FF | Secondary light |
| `color-secondary-300` | #D8B4FE | Secondary light-medium |
| `color-secondary-400` | #C084FC | Secondary medium |
| `color-secondary-500` | #A855F7 | Secondary brand accent |
| `color-secondary-600` | #9333EA | Secondary hover |
| `color-secondary-700` | #7E22CE | Secondary pressed |
| `color-secondary-800` | #6B21A8 | Secondary dark |
| `color-secondary-900` | #581C87 | Secondary very dark |

**Usage:** Secondary colors for accents, highlights, complementary brand color. Used less frequently than primary.

#### Neutral Color Ramp (Gray)

| Token | Hex | Usage |
|-------|-----|-------|
| `color-neutral-0` | #FFFFFF | White background (light mode) |
| `color-neutral-50` | #F9FAFB | Very light gray background |
| `color-neutral-100` | #F3F4F6 | Light gray background, alternate rows |
| `color-neutral-200` | #E5E7EB | Gray borders, dividers |
| `color-neutral-300` | #D1D5DB | Medium gray border |
| `color-neutral-400` | #9CA3AF | Medium gray text |
| `color-neutral-500` | #6B7280 | Medium-dark gray text (secondary) |
| `color-neutral-600` | #4B5563 | Dark gray text |
| `color-neutral-700` | #374151 | Dark gray, secondary text (light mode) |
| `color-neutral-800` | #1F2937 | Very dark gray, primary text (light mode) |
| `color-neutral-900` | #111827 | Darkest gray (almost black) |
| `color-neutral-1000` | #000000 | True black |

#### Semantic Colors (Semantic by Purpose)

| Token | Light Mode Hex | Dark Mode Hex | Usage |
|-------|---|---|-------|
| `color-semantic-success` | #10B981 | #34D399 | Success states, checkmarks, positive feedback |
| `color-semantic-warning` | #F59E0B | #FBBF24 | Warnings, caution, alerts (not critical) |
| `color-semantic-error` | #EF4444 | #F87171 | Errors, failures, destructive actions |
| `color-semantic-info` | #3B82F6 | #60A5FA | Info, neutral notifications, tips |

---

## Semantic Colour Tokens (Light & Dark Mode Mapping)

Semantic tokens map to different primitives depending on theme. This allows dark mode to work without changing component code.

### Light Mode Mapping

| Token | Value | Usage |
|-------|-------|-------|
| `color-surface-default` | `color-neutral-0` (#FFFFFF) | Default page background |
| `color-surface-subtle` | `color-neutral-50` (#F9FAFB) | Subtle background (alternate rows, cards) |
| `color-surface-strong` | `color-neutral-100` (#F3F4F6) | Strong background (focused containers) |
| `color-surface-inverse` | `color-neutral-900` (#111827) | Inverted background (dark on light) |
| `color-text-primary` | `color-neutral-900` (#1F2937) | Primary text, headings |
| `color-text-secondary` | `color-neutral-600` (#4B5563) | Secondary text, helper text |
| `color-text-tertiary` | `color-neutral-500` (#6B7280) | Tertiary text, placeholders |
| `color-text-disabled` | `color-neutral-400` (#9CA3AF) | Disabled text, inactive elements |
| `color-text-inverse` | `color-neutral-0` (#FFFFFF) | Text on dark backgrounds |
| `color-text-on-primary` | `color-neutral-0` (#FFFFFF) | Text on primary-colored backgrounds (buttons) |
| `color-border-default` | `color-neutral-300` (#D1D5DB) | Standard borders, dividers |
| `color-border-strong` | `color-neutral-400` (#9CA3AF) | Strong borders (emphasized dividers) |
| `color-border-focus` | `color-primary-500` (#2563EB) | Focus indicator border color |
| `color-interactive-primary` | `color-primary-500` (#2563EB) | Primary buttons, primary interactive |
| `color-interactive-primary-hover` | `color-primary-600` (#1D4ED8) | Primary button on hover |
| `color-interactive-primary-pressed` | `color-primary-700` (#1E40AF) | Primary button when pressed/active |
| `color-interactive-primary-disabled` | `color-neutral-200` (#E5E7EB) | Primary button disabled state |
| `color-interactive-secondary` | `color-secondary-500` (#A855F7) | Secondary buttons |
| `color-interactive-secondary-hover` | `color-secondary-600` (#9333EA) | Secondary button on hover |

### Dark Mode Mapping (Same token names, different values)

| Token | Value | Why Different |
|-------|-------|---|
| `color-surface-default` | `color-neutral-900` (#111827) | Dark background for dark mode |
| `color-surface-subtle` | `color-neutral-800` (#1F2937) | Subtle dark background |
| `color-surface-strong` | `color-neutral-700` (#374151) | Strong dark background |
| `color-surface-inverse` | `color-neutral-0` (#FFFFFF) | Inverted is light in dark mode |
| `color-text-primary` | `color-neutral-0` (#FFFFFF) | Light text for dark mode |
| `color-text-secondary` | `color-neutral-300` (#D1D5DB) | Lighter secondary text |
| `color-text-tertiary` | `color-neutral-400` (#9CA3AF) | Lighter tertiary text |
| `color-interactive-primary` | `color-primary-400` (#60A5FA) | Lighter blue for dark mode contrast |
| `color-interactive-primary-hover` | `color-primary-300` (#93C5FD) | Even lighter on hover in dark mode |

---

## Typography Tokens

### Font Families

| Token | Font Stack | Usage |
|-------|-----------|-------|
| `font-family-display` | "Inter", system-ui, sans-serif | Headings, titles, display text |
| `font-family-body` | "Inter", system-ui, sans-serif | Body text, paragraphs, default |
| `font-family-mono` | "Monaco" or "Courier New", monospace | Code snippets, terminal text, API responses |

**Why Inter?** Open-source, highly legible, good metrics, widely used in modern design systems.

**System Font Fallback:** If Inter doesn't load, fall back to system UI fonts (San Francisco on Mac, Segoe on Windows), then generic sans-serif.

### Font Weights

| Token | Value | Usage |
|-------|-------|-------|
| `font-weight-regular` | 400 | Body text, default weight |
| `font-weight-medium` | 500 | Labels, semi-bold text |
| `font-weight-semibold` | 600 | Subheadings, emphasized text |
| `font-weight-bold` | 700 | Headings, strong emphasis |

### Font Sizes

Defined in absolute pixels, converted to rem/em in actual CSS (1rem = 16px base).

| Token | Size | Base Conversion |
|-------|------|---|
| `font-size-xs` | 12px | 0.75rem |
| `font-size-sm` | 14px | 0.875rem |
| `font-size-md` | 16px | 1rem |
| `font-size-lg` | 18px | 1.125rem |
| `font-size-xl` | 20px | 1.25rem |
| `font-size-2xl` | 24px | 1.5rem |
| `font-size-3xl` | 30px | 1.875rem |
| `font-size-4xl` | 36px | 2.25rem |
| `font-size-5xl` | 48px | 3rem |

### Line Heights

| Token | Value | Usage | When to Use |
|-------|-------|-------|------------|
| `line-height-tight` | 1.2 | Headings, short text blocks | Titles, headings where tightness is OK |
| `line-height-normal` | 1.5 | Body text, standard | Default for paragraphs and body copy |
| `line-height-relaxed` | 1.75 | Long-form content, accessibility | Blog posts, documentation, accessibility focus |

**Why these ratios?** Below 1.2 feels cramped. Above 1.75 feels spacious. 1.5 is the Goldilocks zone for readability.

### Letter Spacing (Tracking)

| Token | Value | Usage |
|-------|-------|-------|
| `letter-spacing-tight` | -0.02em | Headings, large display text (tighter letter spacing looks elegant) |
| `letter-spacing-normal` | 0em | Default, most text |
| `letter-spacing-wide` | 0.05em | UI labels, all-caps, visual emphasis |

### Typographic Compositions (Tier 3: Component-Specific)

Composed tokens that combine size, weight, and line-height for specific uses:

| Token | Size | Weight | Line Height | Usage |
|-------|------|--------|-------------|-------|
| **Headings** | | | | |
| `heading-1` | 48px | Bold (700) | Tight (1.2) | Page title (h1) |
| `heading-2` | 36px | Bold (700) | Tight (1.2) | Section heading (h2) |
| `heading-3` | 24px | Bold (700) | Tight (1.2) | Subsection heading (h3) |
| `heading-4` | 20px | Semibold (600) | Tight (1.2) | Minor heading (h4) |
| `heading-5` | 16px | Semibold (600) | Normal (1.5) | Small heading (h5) |
| `heading-6` | 14px | Semibold (600) | Normal (1.5) | Smallest heading (h6) |
| **Body** | | | | |
| `body-lg` | 18px | Regular (400) | Normal (1.5) | Large body text, intro text |
| `body-md` | 16px | Regular (400) | Normal (1.5) | Standard body text, default |
| `body-sm` | 14px | Regular (400) | Normal (1.5) | Small body text, secondary |
| **UI** | | | | |
| `ui-label` | 14px | Medium (500) | Normal (1.5) | Form labels, button text |
| `ui-caption` | 12px | Regular (400) | Tight (1.2) | Captions, hints, small UI text |
| `ui-overline` | 12px | Semibold (600) | Tight (1.2) | Overlines, section dividers |
| **Code** | | | | |
| `code-snippet` | 12px | Regular (400) | Relaxed (1.6) | Inline code or code blocks |

---

## Spacing Tokens

Spacing uses a 4px base unit. All spacing increments are multiples of 4 (4, 8, 12, 16, 24, 32, 40, 48, 56, 64).

### Spacing Scale

| Token | Size | Usage |
|-------|------|-------|
| `space-0` | 0px | Remove default margins (reset) |
| `space-1` | 4px | Micro spacing (icon inside button, etc) |
| `space-2` | 8px | Small spacing (input padding, small gaps) |
| `space-3` | 12px | Small-medium spacing |
| `space-4` | 16px | Standard spacing (most common, default margin between elements) |
| `space-6` | 24px | Medium spacing (between sections) |
| `space-8` | 32px | Large spacing (between major sections) |
| `space-10` | 40px | Extra large spacing |
| `space-12` | 48px | XXL spacing (hero sections, big gaps) |
| `space-14` | 56px | Very large spacing |
| `space-16` | 64px | Extreme spacing (rarely used) |
| `space-20` | 80px | Huge spacing (full-page section gaps) |

### Common Patterns

| Pattern | Value | Reasoning |
|---------|-------|-----------|
| **Button padding** | `space-2` (8px) vertical, `space-4` (16px) horizontal | Small padding for button feel |
| **Input padding** | `space-2` (8px) vertical, `space-2` (8px) horizontal | Compact but comfortable |
| **Form field gap** | `space-4` (16px) | Standard gap between form fields |
| **Card padding** | `space-6` (24px) | Breathing room inside cards |
| **Section margin** | `space-8` (32px) | Space between major sections |
| **Page margin** | `space-8` to `space-10` | Outer page margins |

---

## Border Radius Tokens

| Token | Size | Radius | Usage |
|-------|------|--------|-------|
| `border-radius-none` | 0px | 0px | Sharp corners (buttons, inputs with style) |
| `border-radius-sm` | 2px | 2px | Minimal rounding (slight edge softening) |
| `border-radius-md` | 4px | 4px | Standard rounding (buttons, small components) |
| `border-radius-lg` | 8px | 8px | Medium rounding (cards, larger components) |
| `border-radius-xl` | 12px | 12px | Large rounding (modals, drawers) |
| `border-radius-2xl` | 16px | 16px | Extra large rounding (pills, badges) |
| `border-radius-full` | 9999px | 50% | Fully rounded (circles, pill-shaped buttons) |

### Usage Rules

- Buttons: `border-radius-md` (4px)
- Inputs: `border-radius-md` (4px)
- Cards: `border-radius-lg` (8px)
- Modals: `border-radius-xl` (12px)
- Badge/pill: `border-radius-full` (circle)
- Avatar images: `border-radius-full` (circle) or `border-radius-lg` (slightly rounded square)

---

## Elevation / Shadow Tokens

Shadows create depth and hierarchy. We use material design-inspired shadows.

| Token | CSS Box-Shadow | Elevation | Usage |
|-------|---|---|-------|
| `elevation-0` | none | Flat | No shadow, baseline element |
| `elevation-1` | `0 1px 2px 0 rgba(0, 0, 0, 0.05)` | Subtle | Subtle card, input focus |
| `elevation-2` | `0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)` | Low | Hovered card, button hover |
| `elevation-3` | `0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)` | Medium | Modal, dropdown, popover |
| `elevation-4` | `0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)` | High | Tooltip, menu |
| `elevation-5` | `0 25px 50px -12px rgba(0, 0, 0, 0.25)` | Extra High | Overlay, floating action button |

### When to Use Each

- **elevation-0:** Button, input, flat containers
- **elevation-1:** Subtle card, input border focus
- **elevation-2:** Hovered button, active card
- **elevation-3:** Modal dialog, dropdown, popover
- **elevation-4:** Tooltip, context menu
- **elevation-5:** Floating action button, sticky header

---

## Motion Tokens

### Duration Tokens

| Token | Value | Usage |
|-------|-------|-------|
| `duration-instant` | 0ms | No animation (instant) |
| `duration-micro` | 100ms | Quick feedback (hover, focus) |
| `duration-short` | 200ms | Tooltip show, quick transitions |
| `duration-medium` | 300ms | Modal entrance, standard transition |
| `duration-long` | 500ms | Page transition, major layout change |
| `duration-extra-long` | 700ms | Complex animation, celebratory motion |

### Easing Functions

| Token | Cubic-Bezier | Usage | Feel |
|-------|---|---|---|
| `easing-ease-in` | `cubic-bezier(0.4, 0, 1, 1)` | Accelerating motion (ease-in to rest) | Object picks up speed |
| `easing-ease-out` | `cubic-bezier(0, 0, 0.2, 1)` | Decelerating motion (ease-out) | Object slows to stop |
| `easing-ease-in-out` | `cubic-bezier(0.4, 0, 0.2, 1)` | Accelerate then decelerate | Natural, balanced motion |
| `easing-spring` | `cubic-bezier(0.34, 1.56, 0.64, 1)` | Bouncy, playful | Springy, elastic feel |

### Animation Compositions (Component-Specific)

| Token | Duration | Easing | Usage |
|-------|----------|--------|-------|
| `animation-fade-in` | 200ms | ease-out | Fade in from opacity 0 → 1 |
| `animation-fade-out` | 200ms | ease-in | Fade out from opacity 1 → 0 |
| `animation-slide-in-up` | 300ms | ease-out | Slide up from bottom |
| `animation-slide-in-down` | 300ms | ease-out | Slide down from top |
| `animation-bounce` | 300ms | spring | Bouncy entrance |
| `animation-pulse` | 2000ms | ease-in-out | Pulsing opacity (repeating) |
| `animation-spin` | 1000ms | linear | Continuous rotation (loading spinner) |

---

## Z-Index Scale

| Token | Value | Usage |
|-------|-------|-------|
| `z-index-base` | 0 | Default stacking (page content) |
| `z-index-dropdown` | 100 | Dropdown menus (above base content) |
| `z-index-sticky` | 150 | Sticky headers, floating buttons (above dropdowns) |
| `z-index-overlay` | 200 | Page overlays, semi-transparent dark background |
| `z-index-modal` | 250 | Modal dialogs (above overlays) |
| `z-index-tooltip` | 300 | Tooltips (above modals) |
| `z-index-popover` | 300 | Popovers (same as tooltip) |
| `z-index-notification` | 400 | Toast notifications (above everything) |

### Hierarchy

```
Notification (400) ← Toast should be on top
Tooltip (300)      ← Hovering info
Modal (250)        ← Dialog boxes
Overlay (200)      ← Semi-transparent background
Sticky (150)       ← Header stays visible when scrolling
Dropdown (100)     ← Menu expands below
Base (0)           ← Normal page content
```

---

## Implementation Guidance

### CSS Variables

**Light Mode (root selector):**
```css
:root {
  --color-primary-500: #2563EB;
  --color-primary-600: #1D4ED8;
  --color-surface-default: #FFFFFF;
  --color-text-primary: #1F2937;
  --space-4: 16px;
  --font-size-md: 16px;
  --duration-short: 200ms;
  --easing-ease-out: cubic-bezier(0, 0, 0.2, 1);
}
```

**Dark Mode (dark class selector):**
```css
:root[data-theme="dark"] {
  --color-primary-500: #60A5FA;
  --color-surface-default: #111827;
  --color-text-primary: #FFFFFF;
}
```

**Usage in Components:**
```css
.button-primary {
  background-color: var(--color-interactive-primary);
  color: var(--color-text-on-primary);
  padding: var(--space-2) var(--space-4);
  font-size: var(--font-size-md);
  border-radius: var(--border-radius-md);
  transition: background-color var(--duration-micro) var(--easing-ease-out);
}

.button-primary:hover {
  background-color: var(--color-interactive-primary-hover);
}
```

### Tailwind Config

If using Tailwind CSS, extend config with custom tokens:

```javascript
module.exports = {
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#EFF6FF',
          500: '#2563EB',
          600: '#1D4ED8',
        },
        semantic: {
          error: '#EF4444',
          success: '#10B981',
        },
      },
      spacing: {
        4: '16px',  // Override default if needed
      },
      fontSize: {
        md: '16px',
        lg: '18px',
      },
      borderRadius: {
        md: '4px',
        lg: '8px',
      },
    },
  },
}
```

### React/TypeScript

Create token constants file:

```typescript
// tokens.ts
export const colors = {
  primary: {
    50: '#EFF6FF',
    500: '#2563EB',
    600: '#1D4ED8',
  },
  semantic: {
    error: '#EF4444',
    success: '#10B981',
  },
};

export const spacing = {
  2: '8px',
  4: '16px',
  6: '24px',
};

export const typography = {
  heading1: {
    fontSize: '48px',
    fontWeight: 700,
    lineHeight: '1.2',
  },
  bodyMd: {
    fontSize: '16px',
    fontWeight: 400,
    lineHeight: '1.5',
  },
};

export const motion = {
  durationShort: '200ms',
  easingEaseOut: 'cubic-bezier(0, 0, 0.2, 1)',
};
```

**Usage in Component:**
```typescript
import styled from 'styled-components';
import { colors, spacing, typography } from './tokens';

const Button = styled.button`
  background-color: ${colors.primary[500]};
  padding: ${spacing[2]} ${spacing[4]};
  font-size: ${typography.bodyMd.fontSize};
  font-weight: ${typography.bodyMd.fontWeight};
  transition: background-color 200ms ease-out;

  &:hover {
    background-color: ${colors.primary[600]};
  }
`;
```

### iOS/Android (React Native / Native)

**iOS (SwiftUI):**
```swift
struct DesignTokens {
  static let colorPrimary500 = Color(red: 0.15, green: 0.39, blue: 0.93)
  static let spacingMedium: CGFloat = 16.0
  static let borderRadiusMd: CGFloat = 4.0
}

struct Button: View {
  var body: some View {
    Text("Save")
      .foregroundColor(.white)
      .padding(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
      .background(DesignTokens.colorPrimary500)
      .cornerRadius(DesignTokens.borderRadiusMd)
  }
}
```

**Android (Jetpack Compose):**
```kotlin
object DesignTokens {
  val colorPrimary500 = Color(0xFF2563EB)
  val spacingMedium = 16.dp
  val borderRadiusMd = 4.dp
}

@Composable
fun MyButton() {
  Button(
    onClick = { },
    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.colorPrimary500),
    modifier = Modifier
      .padding(DesignTokens.spacingMedium)
      .clip(RoundedCornerShape(DesignTokens.borderRadiusMd))
  ) {
    Text("Save")
  }
}
```

---

## Token Maintenance and Updates

### When to Create New Tokens

✓ **DO create tokens for:**
- New brand colors or palette
- Repeated spacing values
- New typography style used multiple times
- New animation or transition pattern

✗ **DON'T create tokens for:**
- One-off values used in single component
- Very large tokens (space-200, space-300) — probably a layout issue
- Temporary values during dev (remove before shipping)

### Versioning Tokens

Document changes when tokens change:

```markdown
## Design System v2.1 (2024-03-14)

### Added
- New `color-semantic-warning` token for warning states

### Changed
- `duration-short` increased from 150ms to 200ms (better UX)
- `heading-1` font-size increased from 44px to 48px

### Deprecated
- `color-blue-dark` (use `color-primary-700` instead)

### Removed
- `color-custom-overlay` (no longer needed)
```

### Breakchange Policy

If changing a token value:
1. Mark as deprecated first (one release cycle)
2. Update all usages in codebase
3. Remove deprecated token in next major version
4. Communicate to teams before breaking

---

## Quick Reference Cheat Sheet

### Most Common Tokens to Use

```
Colors:
  Light mode: primary-500, text-primary, surface-default, border-default
  Dark mode: primary-400 (lighter for contrast), text-inverse, surface-default (dark)
  Semantic: error (#EF4444), success (#10B981), warning (#F59E0B)

Spacing:
  Component padding: space-2 (8px) to space-4 (16px)
  Between elements: space-4 (16px) default
  Section gaps: space-6 (24px) to space-8 (32px)

Typography:
  Headings: heading-1 through heading-6
  Body text: body-md (16px) default, body-sm for secondary
  UI text: ui-label (14px bold) for labels, ui-caption for hints

Motion:
  Most interactions: duration-short (200ms)
  Modals/dialogs: duration-medium (300ms)
  Easing: ease-out for entrances, ease-in for exits

Radius:
  Buttons/inputs: border-radius-md (4px)
  Cards: border-radius-lg (8px)
  Badges: border-radius-full (circle)
```

### Common Component Recipes

**Button:**
```
Background: color-interactive-primary
Text: color-text-on-primary
Padding: space-2 (vertical) × space-4 (horizontal)
Border radius: border-radius-md
Hover: color-interactive-primary-hover + elevation-2
```

**Input Field:**
```
Background: color-surface-default
Border: 1px color-border-default
Focus border: 2px color-border-focus
Padding: space-2
Border radius: border-radius-md
```

**Card:**
```
Background: color-surface-default
Border: 1px color-border-default
Padding: space-6
Border radius: border-radius-lg
Shadow: elevation-1
```

**Heading:**
```
Heading 1: heading-1 (48px, bold, 1.2 line-height)
Heading 2: heading-2 (36px, bold, 1.2 line-height)
Color: color-text-primary
```
