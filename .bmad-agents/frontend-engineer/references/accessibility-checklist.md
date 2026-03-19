# WCAG 2.2 AA Accessibility Checklist & Reference

## Quick Reference: 25 Key WCAG 2.2 AA Success Criteria

| # | Criterion | Level | Category | Implementation Notes |
|---|---|---|---|---|
| 1 | 1.1.1 Non-text Content | A | Perceivable | All images need descriptive `alt` text or `alt=""` if decorative |
| 2 | 1.3.1 Info and Relationships | A | Perceivable | Use semantic HTML headings, lists, labels; avoid using color alone |
| 3 | 1.4.3 Contrast (Minimum) | AA | Perceivable | Text 4.5:1 ratio (normal), 3:1 (large text, UI components) |
| 4 | 1.4.4 Resize Text | AA | Perceivable | Text must be readable at 200% zoom without scrolling |
| 5 | 1.4.5 Images of Text | AA | Perceivable | Avoid images containing text (use real text instead) |
| 6 | 1.4.11 Non-text Contrast | AA | Perceivable | UI components, focus indicators: 3:1 contrast minimum |
| 7 | 2.1.1 Keyboard | A | Operable | All functionality must be accessible via keyboard (no mouse required) |
| 8 | 2.1.2 No Keyboard Trap | A | Operable | Focus must be able to leave any component (trap only in modals) |
| 9 | 2.4.3 Focus Order | A | Operable | Logical tab order (left-to-right, top-to-bottom, natural flow) |
| 10 | 2.4.7 Focus Visible | AA | Operable | Focus indicator visible on all interactive elements |
| 11 | 2.5.4 Motion Actuation | A | Operable | Don't trigger actions via device motion/sensors alone |
| 12 | 3.1.1 Language of Page | A | Understandable | Document `<html lang="en">` attribute set |
| 13 | 3.2.1 On Focus | A | Understandable | Components don't change content on focus alone |
| 14 | 3.2.2 On Input | A | Understandable | No unexpected context change on input change |
| 15 | 3.3.1 Error Identification | A | Understandable | Errors clearly identified in text (not color alone) |
| 16 | 3.3.2 Labels or Instructions | A | Understandable | All form inputs have associated labels or aria-labels |
| 17 | 3.3.4 Error Prevention | AA | Understandable | Confirm before submission (especially for important actions) |
| 18 | 4.1.2 Name, Role, Value | A | Robust | Components have accessible name, role, state properties |
| 19 | 4.1.3 Status Messages | AA | Robust | Live region announcements for dynamic content updates |
| 20 | 2.4.2 Page Titled | A | Operable | Page has descriptive `<title>` tag |
| 21 | 2.4.6 Headings and Labels | AA | Understandable | Descriptive headings and labels (not just "Click here") |
| 22 | 2.4.8 Focus Purpose | AAA | Operable | Purpose of focus visible (for keyboard users) |
| 23 | 1.4.13 Content on Hover | AA | Perceivable | Hoverable content dismissible, not blocking interaction |
| 24 | 2.5.3 Label in Name | A | Operable | Text visible in component matches accessible name |
| 25 | 3.3.5 Help | AAA | Understandable | Help text provided for complex inputs or fields |

---

## Perceivable: Make Content Accessible to Senses

### Images & Alt Text

**Rule:** All images need descriptive `alt` text or `alt=""` if decorative.

**Bad Examples:**
```html
<!-- No alt attribute -->
<img src="graph.png">

<!-- Alt text is filename -->
<img src="chart-revenue-2026.png" alt="chart-revenue-2026">

<!-- Alt text same as surrounding text (redundant) -->
<figure>
  <img src="chart.png" alt="Revenue Chart">
  <figcaption>Revenue Chart</figcaption>
</figure>
```

**Good Examples:**
```html
<!-- Descriptive alt text -->
<img src="graph.png" alt="Revenue increased 25% from Jan to Mar 2026">

<!-- Decorative image explicitly marked empty -->
<img src="divider.png" alt="">

<!-- Complex image with linked description -->
<img src="complex-chart.png" alt="Product sales by region (see detailed table below)">
<table>
  <!-- Detailed data table follows -->
</table>

<!-- SVG with title -->
<svg alt="Chart">
  <title>Monthly Sales Trend</title>
  <rect x="0" y="0" width="100" height="100"/>
</svg>
```

### Color Contrast Requirements

**Text Contrast:**
- Normal text (< 18pt): **4.5:1** ratio minimum
- Large text (18pt+ or 14pt+ bold): **3:1** ratio minimum

**UI Components:**
- Focus indicators: **3:1** contrast ratio minimum
- Graphical elements (borders, icons): **3:1** ratio minimum

**Testing:**
```
// Use WebAIM contrast checker
// https://webaim.org/resources/contrastchecker/

// Or use axe DevTools browser extension
// Or use Lighthouse in Chrome DevTools
```

**Example:**
```css
/* Good: 4.5:1 contrast (black on light gray) */
.text {
  color: #000000;
  background-color: #ffffff;
  /* Passes 4.5:1 for normal text */
}

/* Bad: Only 2.8:1 contrast */
.text {
  color: #666666;
  background-color: #ffffff;
  /* Fails WCAG AA for normal text */
}

/* Good: 3:1 contrast for focus indicator */
button:focus-visible {
  outline: 3px solid #0066cc;
  outline-offset: 2px;
  /* 0066cc on white = 8.6:1, well above 3:1 minimum */
}
```

### Text Resize to 200%

Users must be able to zoom to 200% without:
- Content being clipped or hidden
- Horizontal scrolling required
- Loss of functionality

**Implementation:**
```css
/* Use relative units (em, rem) not fixed pixels */
body {
  font-size: 16px; /* Base size */
}

button {
  font-size: 1em; /* Scales with zoom */
  padding: 0.5em 1em; /* Relative padding */
}

/* Bad: Fixed pixels don't scale with zoom */
button {
  font-size: 12px;
  padding: 6px 12px;
}
```

### Captions & Transcripts

**Video:** Synchronized captions must be provided.
**Audio:** Transcript required.

```html
<video controls>
  <source src="movie.mp4" type="video/mp4">
  <track kind="captions" src="captions.vtt" srclang="en">
  <track kind="descriptions" src="descriptions.vtt" srclang="en">
</video>

<!-- Transcript link -->
<a href="/transcripts/movie.txt">Video Transcript</a>
```

---

## Operable: Allow Keyboard Navigation

### Keyboard Accessibility

**Rule:** Every interactive element must be keyboard accessible.

**Keyboard Requirements:**
- Tab: Move forward through focusable elements
- Shift+Tab: Move backward
- Enter: Activate button, submit form
- Space: Toggle checkbox, toggle button
- Arrow keys: Navigate menus, lists, tabs (custom)
- Escape: Close modal, cancel operation

**Implementation:**
```typescript
// Make custom components keyboard-accessible
export function CustomButton({ onActivate }: Props) {
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onActivate();
    }
  };

  return (
    <div
      role="button"
      tabIndex={0}
      onKeyDown={handleKeyDown}
      onClick={onActivate}
    >
      Click me
    </div>
  );
}

// Custom menu navigation
export function CustomMenu({ items }: Props) {
  const [focusedIndex, setFocusedIndex] = useState(0);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setFocusedIndex((i) => (i + 1) % items.length);
        break;
      case 'ArrowUp':
        e.preventDefault();
        setFocusedIndex((i) => (i - 1 + items.length) % items.length);
        break;
      case 'Home':
        e.preventDefault();
        setFocusedIndex(0);
        break;
      case 'End':
        e.preventDefault();
        setFocusedIndex(items.length - 1);
        break;
    }
  };

  return (
    <ul role="menu" onKeyDown={handleKeyDown}>
      {items.map((item, i) => (
        <li key={i} role="none">
          <button
            role="menuitem"
            autoFocus={i === focusedIndex}
            onClick={() => selectItem(item)}
          >
            {item.label}
          </button>
        </li>
      ))}
    </ul>
  );
}
```

### No Keyboard Trap

**Rule:** Focus must be able to escape any interactive component (except intentional traps like modals).

**Bad Example:**
```html
<!-- User can't Tab out of this textbox -->
<input
  onKeyDown={(e) => {
    if (e.key === 'Tab') {
      e.preventDefault(); // Traps focus!
    }
  }}
/>
```

**Good Example (Modal):**
```typescript
// Modal intentionally traps focus
export function Modal({ children, onClose }: Props) {
  const modalRef = useRef<HTMLDivElement>(null);
  const firstButtonRef = useRef<HTMLButtonElement>(null);
  const lastButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    // Focus first element when modal opens
    firstButtonRef.current?.focus();
  }, []);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Tab') {
      if (e.shiftKey && document.activeElement === firstButtonRef.current) {
        // Move focus to last element
        e.preventDefault();
        lastButtonRef.current?.focus();
      } else if (!e.shiftKey && document.activeElement === lastButtonRef.current) {
        // Move focus to first element
        e.preventDefault();
        firstButtonRef.current?.focus();
      }
    }
    if (e.key === 'Escape') {
      onClose();
    }
  };

  return (
    <div
      ref={modalRef}
      role="dialog"
      aria-modal="true"
      onKeyDown={handleKeyDown}
    >
      <h2>Confirm Action</h2>
      <button ref={firstButtonRef} onClick={onClose}>
        Cancel
      </button>
      <button ref={lastButtonRef} onClick={() => handleConfirm()}>
        Confirm
      </button>
    </div>
  );
}
```

### Focus Order (Tab Order)

**Rule:** Tab order must follow logical visual flow (left-to-right, top-to-bottom).

**Implementation:**
```html
<!-- Natural order: semantic HTML provides correct focus order -->
<form>
  <label for="first">First Name:</label>
  <input id="first" type="text"> <!-- Tab 1 -->

  <label for="last">Last Name:</label>
  <input id="last" type="text"> <!-- Tab 2 -->

  <button type="submit">Submit</button> <!-- Tab 3 -->
</form>

<!-- Bad: Using tabindex="1, 2, 3..." is difficult to maintain -->
<input tabindex="3"> <!-- Third in tab order -->
<input tabindex="1"> <!-- First in tab order -->
<input tabindex="2"> <!-- Second in tab order -->

<!-- Good: Only use tabindex for exceptional cases -->
<div tabindex="-1">Not in tab order; but focusable via JS</div>
<button tabindex="0">Last interactive element</button> <!-- Default tab order -->
```

### Focus Visible Indicator

**Rule:** Focus indicator must be visible with minimum 3px offset or border.

**Implementation:**
```css
/* Clear focus indicator */
button:focus-visible {
  outline: 3px solid #0066cc;
  outline-offset: 2px;
}

/* Or use box-shadow for rounded elements */
input:focus-visible {
  box-shadow: 0 0 0 3px rgba(0, 102, 204, 0.3);
  border: 2px solid #0066cc;
}

/* Bad: No visible focus indicator */
button:focus {
  outline: none;
}

/* Bad: Low contrast focus indicator */
button:focus {
  outline: 1px solid #999999; /* Only 2.9:1 contrast */
}
```

---

## Understandable: Make Content Understandable

### Language Declaration

**Rule:** Document language must be set in `<html>` tag.

```html
<!-- Good: Primary language -->
<html lang="en">

<!-- Good: With regional dialect -->
<html lang="en-US">

<!-- Good: Changing language within document -->
<html lang="en">
  <body>
    <p>Welcome to our site</p>
    <p lang="es">Bienvenido a nuestro sitio</p>
  </body>
</html>
```

### Error Identification

**Rule:** Errors must be clearly identified in text, not by color alone.

**Bad Example:**
```html
<!-- Error only indicated by red color -->
<input style="border-color: red;">
<!-- Screen reader user has no idea this field has an error -->
```

**Good Example:**
```html
<input
  id="email-input"
  type="email"
  aria-invalid="true"
  aria-errormessage="email-error"
/>
<p id="email-error" role="alert" style="color: red;">
  <!-- Text message, not just color -->
  Invalid email format. Please use example@domain.com
</p>
```

### Form Labels

**Rule:** Every form input must have an associated label.

**Good Examples:**
```html
<!-- Explicit label with for attribute -->
<label for="email">Email Address:</label>
<input id="email" type="email">

<!-- Implicit label (input inside label) -->
<label>
  <input type="checkbox">
  I agree to the terms
</label>

<!-- ARIA label for icon buttons -->
<button aria-label="Close dialog">×</button>

<!-- aria-describedby for help text -->
<input
  id="password"
  type="password"
  aria-describedby="password-help"
>
<p id="password-help">At least 8 characters, one uppercase, one number</p>
```

### Headings

**Rule:** Use semantic heading hierarchy; don't skip levels.

**Good Example:**
```html
<!-- Proper hierarchy -->
<h1>Site Title</h1>
<h2>Main Section</h2>
<h3>Subsection</h3>
<h3>Another Subsection</h3>
<h2>Another Section</h2>

<!-- Screen readers can navigate by heading level -->
```

**Bad Example:**
```html
<!-- Skipping levels -->
<h1>Title</h1>
<h3>Subsection</h3> <!-- Should be <h2> -->

<!-- Using heading for styling (should use CSS) -->
<h1 style="font-size: 12px;">Not really a heading</h1>
```

---

## Robust: Support Assistive Technologies

### Semantic HTML

**Rule:** Use semantic HTML elements; avoid `<div role="button">` when `<button>` exists.

| Content | Semantic | Anti-pattern |
|---|---|---|
| Navigation | `<nav>` | `<div id="navigation">` |
| Main content | `<main>` | `<div id="main">` |
| Article/post | `<article>` | `<div class="post">` |
| Section/grouping | `<section>` | `<div class="section">` |
| Sidebar | `<aside>` | `<div id="sidebar">` |
| List | `<ul>`, `<ol>`, `<li>` | `<div class="list">` |
| Form input | `<input>` with `<label>` | `<div>` with text |
| Button | `<button>` | `<div onclick="">` |
| Link | `<a href="">` | `<span onclick="">` |
| Quote | `<blockquote>` | `<div class="quote">` |
| Emphasized text | `<em>`, `<strong>` | `<span style="italic">` |

### ARIA (Accessible Rich Internet Applications)

**Golden Rule:** Use ARIA to supplement, not replace, semantic HTML.

**Landmark Roles:**
```html
<nav aria-label="Main navigation">
  <!-- Navigation links -->
</nav>

<main>
  <!-- Main content -->
</main>

<aside aria-labelledby="sidebar-title">
  <h2 id="sidebar-title">Related Links</h2>
</aside>

<footer aria-label="Site footer">
  <!-- Footer content -->
</footer>
```

**Live Regions:**
```html
<!-- Announce dynamic content updates -->
<div aria-live="polite" aria-atomic="true" id="status">
  <!-- Content updates here; screen reader announces changes -->
</div>

<!-- Assertive for urgent announcements -->
<div aria-live="assertive" role="alert">
  <!-- High-priority updates like errors -->
</div>

<!-- Update item count after filtering -->
<div aria-live="polite">
  {filteredItems.length} items found
</div>
```

**Button States:**
```html
<!-- Toggle button -->
<button
  aria-pressed="false"
  onClick={() => setIsPressed(!isPressed)}
>
  {isPressed ? 'Favorited' : 'Add to Favorites'}
</button>

<!-- Expanded/collapsed control -->
<button
  aria-expanded="false"
  aria-controls="menu"
  onClick={() => setOpen(!open)}
>
  Menu
</button>
<menu id="menu" hidden={!open}>
  <!-- Menu items -->
</menu>

<!-- Loading state -->
<button aria-busy="true" disabled>
  Loading...
</button>

<!-- Disabled state -->
<button aria-disabled="true">
  Not available
</button>
```

### Focus Management in React

```typescript
// Manage focus after DOM changes
export function Modal({ isOpen, onClose }: Props) {
  const triggerRef = useRef<HTMLButtonElement>(null);
  const modalRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isOpen) {
      // Focus modal when it opens
      modalRef.current?.focus();
    } else {
      // Restore focus to trigger when modal closes
      triggerRef.current?.focus();
    }
  }, [isOpen]);

  return (
    <>
      <button ref={triggerRef} onClick={() => setOpen(true)}>
        Open Modal
      </button>
      {isOpen && (
        <div
          ref={modalRef}
          role="dialog"
          aria-modal="true"
          tabIndex={-1}
        >
          {/* Modal content */}
        </div>
      )}
    </>
  );
}
```

---

## React-Specific Patterns

### Custom Component ARIA Patterns

**Tabs:**
```typescript
export function Tabs() {
  const [activeTab, setActiveTab] = useState(0);

  const tabs = ['Profile', 'Settings', 'Logout'];

  return (
    <div>
      <div role="tablist" aria-label="Account settings">
        {tabs.map((label, i) => (
          <button
            key={i}
            role="tab"
            aria-selected={i === activeTab}
            aria-controls={`panel-${i}`}
            tabIndex={i === activeTab ? 0 : -1}
            onClick={() => setActiveTab(i)}
          >
            {label}
          </button>
        ))}
      </div>
      {tabs.map((label, i) => (
        <div
          key={i}
          id={`panel-${i}`}
          role="tabpanel"
          aria-labelledby={`tab-${i}`}
          hidden={i !== activeTab}
        >
          {/* Panel content */}
        </div>
      ))}
    </div>
  );
}
```

**Combobox (Autocomplete):**
```typescript
export function Combobox({ options }: Props) {
  const [value, setValue] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const [focusedIndex, setFocusedIndex] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  const filtered = options.filter((o) =>
    o.toLowerCase().includes(value.toLowerCase())
  );

  const handleKeyDown = (e: React.KeyboardEvent) => {
    switch (e.key) {
      case 'ArrowDown':
        e.preventDefault();
        setFocusedIndex((i) => Math.min(i + 1, filtered.length - 1));
        break;
      case 'ArrowUp':
        e.preventDefault();
        setFocusedIndex((i) => Math.max(i - 1, -1));
        break;
      case 'Enter':
        e.preventDefault();
        if (focusedIndex >= 0) {
          setValue(filtered[focusedIndex]);
          setIsOpen(false);
        }
        break;
      case 'Escape':
        setIsOpen(false);
        break;
    }
  };

  return (
    <div role="combobox" aria-expanded={isOpen}>
      <input
        ref={inputRef}
        role="searchbox"
        value={value}
        onChange={(e) => {
          setValue(e.target.value);
          setIsOpen(true);
        }}
        onKeyDown={handleKeyDown}
        aria-controls="listbox"
        aria-autocomplete="list"
      />
      {isOpen && (
        <ul
          id="listbox"
          ref={listRef}
          role="listbox"
        >
          {filtered.map((option, i) => (
            <li
              key={option}
              role="option"
              aria-selected={i === focusedIndex}
              onClick={() => {
                setValue(option);
                setIsOpen(false);
              }}
            >
              {option}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
```

---

## Testing Accessibility

### Automated Testing Tools

**Limitations:**
```
Tool            | Can Detect | Limitations
─────────────────────────────────────────────────
axe-core        | 70%        | Can't check contrast in images, can't verify alt text relevance
jest-axe        | 70%        | Same as axe; unit test integration
Lighthouse      | 50%        | Best for quick audits; many false positives
WAVE            | 60%        | Browser extension; good for quick checks
```

**Implementation:**
```typescript
// Jest + jest-axe
import { axe, toHaveNoViolations } from 'jest-axe';

expect.extend(toHaveNoViolations);

test('should not have accessibility violations', async () => {
  const { container } = render(<MyComponent />);
  const results = await axe(container);
  expect(results).toHaveNoViolations();
});

// Cypress + axe
import { checkA11y } from 'cypress-axe';

describe('Accessibility', () => {
  it('should pass axe checks', () => {
    cy.visit('/');
    cy.injectAxe();
    cy.checkA11y();
  });
});
```

### Manual Testing: Keyboard Only

1. Disconnect mouse/trackpad
2. Use Tab/Shift+Tab to navigate
3. Use Enter/Space to activate buttons
4. Use Arrow keys for menus/lists
5. Use Escape to close modals
6. Verify all content is reachable and usable

### Manual Testing: Screen Reader Commands

**NVDA (Windows) Common Commands:**
```
H             | Next heading
1-6           | Heading level 1-6
L             | Next list
I             | Next list item
N             | Next navigation landmark
M             | Next main landmark
D             | Next landmark
B             | Next button
F             | Next form field
T             | Next table
Alt+Shift+N   | Toggle reading of links vs headings
Alt+Shift+A   | Toggle announcement of annotations
```

**VoiceOver (macOS/iOS):**
```
VO + Right    | Next item
VO + Left     | Previous item
VO + H        | Next heading
VO + X        | Read all
VO + W        | Show web rotor (headings, links, etc)
```

---

## Common Accessibility Violations & Fixes

| Violation | Bad Example | Good Example | Impact |
|---|---|---|---|
| **Missing alt text** | `<img src="chart.png">` | `<img src="chart.png" alt="Q1 revenue: $2.5M">` | Screen reader users can't see image content |
| **Low contrast** | Black (#000) on dark gray (#333) = 1.3:1 | Black (#000) on white (#FFF) = 21:1 | Low vision users can't read text |
| **No focus indicator** | `button:focus { outline: none; }` | `button:focus-visible { outline: 3px solid blue; }` | Keyboard users can't see focus |
| **Missing form label** | `<input type="email">` | `<label>Email:</label><input id="email" type="email">` | Screen reader users don't know input purpose |
| **Keyboard trap** | Event listener preventing Tab from leaving | Removing preventDefault on Tab key | Keyboard users can't navigate away |
| **Color only** | Error shown only in red | Error in red text + error message | Color-blind users can't identify errors |
| **Inaccessible modal** | Modal without focus trap or backdrop | Focus trap + aria-modal="true" | Screen reader users confused by context |
| **Semantic confusion** | `<div role="button" onclick="">` | `<button>` | Screen readers don't announce as button |
| **Missing heading hierarchy** | `<h1>` then `<h3>` (skip h2) | `<h1>`, `<h2>`, `<h3>` | Screen reader users miss structure |
| **Dynamic content not announced** | List updates without aria-live | `<div aria-live="polite">` updates | Screen reader users don't know about changes |
