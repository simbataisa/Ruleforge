# UX/UI Specification — Space-Rule Assignment (Epic 4)

**Prepared by:** UX/UI Designer
**Date:** 2026-03-15
**Epic:** Space-Rule Assignment (Epic 4)
**Input:** `docs/prd-space-assignment.md`, `docs/architecture/solution-architecture-space-assignment.md`
**Status:** APPROVED

---

## 1. Design Principles

- **Single-responsibility surfaces**: SpaceDetailPage manages space contents; Rules page manages rule creation. The UI must reinforce this separation.
- **Assignment ≠ Creation**: The visual language for "Assign Rules" must be distinct from create actions. Use an additive/link icon, not a `+` create icon.
- **Progressive disclosure**: Bulk assignment is accessible but not prominent; primary CTA is single-item assignment.
- **Consistent modal patterns**: Modals follow the established patterns from `MoveRuleModal` (Epic 2): header, body, footer with Cancel + primary action.

---

## 2. SpaceDetailPage Changes

### 2.1 Remove "Create Rule" Button

- Remove the "Create Rule" button entirely from the page header toolbar.
- Do not replace it with a disabled state or a tooltip — the absence should be clean.
- The page header CTA area now contains only: **[Assign Rules]** (EDITOR+ only).

### 2.2 "Assign Rules" Button

**Placement:** Top-right of the rules section header (same position the Create button occupied).

**Appearance:**
```
[ ↗ Assign Rules ]
```
- Style: `variant="outline"` (Shadcn/ui Button), medium size
- Icon: `LinkIcon` or `ArrowUpRightIcon` from Lucide (additive/link semantic, not create)
- Visibility: Hidden for VIEWER role (`currentUser.role === 'VIEWER'` → render null)

**Tooltip (on hover):** "Assign existing rules to this space"

**Empty state clarification:** When the space has no rules, the empty state message reads:
> "No rules in this space yet. Click Assign Rules to add existing rules, or create rules on the Rules page."

---

## 3. AssignRulesModal

### 3.1 Layout

```
┌─────────────────────────────────────────────────────────────────┐
│  Assign Rules to [Space Name]                            [✕]    │
├───────────────────────────────────────┬─────────────────────────┤
│  AVAILABLE RULES                      │  SELECTED (N)           │
│  ┌─────────────────────────────────┐  │                         │
│  │ 🔍 Search rules...              │  │  [empty state or list   │
│  └─────────────────────────────────┘  │   of selected rule      │
│                                       │   names with ✕]         │
│  ┌──────────────────────────────────┐ │                         │
│  │ ☐  Credit Score Rule       TABLE │ │  • Credit Score Rule ✕  │
│  │     /Underwriting/Credit         │ │  • Income Check Rule ✕  │
│  │     Updated Mar 14, 2026         │ │                         │
│  ├──────────────────────────────────┤ │                         │
│  │ ☑  Income Check Rule      SCRIPT │ │                         │
│  │     /Underwriting/Income         │ │                         │
│  │     Updated Mar 10, 2026         │ │                         │
│  ├──────────────────────────────────┤ │                         │
│  │ ☐  Fraud Detection Rule    TREE  │ │                         │
│  │     /Fraud/Detection             │ │                         │
│  │     Updated Mar 8, 2026          │ │                         │
│  └──────────────────────────────────┘ │                         │
│  Showing 8 of 47 rules                │                         │
├───────────────────────────────────────┴─────────────────────────┤
│                                    [Cancel]  [Assign (2)]       │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 Component Specs

**Modal dimensions:** `max-w-3xl` (768px), `h-[600px]` with scrollable left panel

**Left panel — Available Rules:**
- Search input: `placeholder="Search rules by name..."`, `aria-label="Search available rules"`
- Rule list items (left panel):
  - Checkbox (`aria-label="Select [rule name]"`)
  - Rule name in bold
  - Rule type badge: colored pill matching existing rule type color scheme (`TABLE` = blue, `TREE` = green, `SCRIPT` = amber, etc.)
  - Folder path in `text-muted-foreground text-sm`
  - Updated date in `text-muted-foreground text-xs`
- Empty search result: "No rules match '[query]'"
- Loading state: 3 skeleton rows

**Right panel — Selected:**
- Header: "Selected (N)" where N updates live
- Empty state: "No rules selected yet"
- Each selected rule: rule name + `✕` button to deselect (`aria-label="Remove [rule name] from selection"`)
- Right panel is `min-w-[200px]`

**Footer:**
- Cancel: `variant="ghost"`, closes without action
- Assign button: `variant="default"`, label = "Assign (N)" where N = selection count
- Assign button disabled when N = 0
- Assign button shows loading spinner during PATCH calls: "Assigning..."

### 3.3 States

| State | Behavior |
|-------|----------|
| Loading | Skeleton rows in left panel; Assign button disabled |
| Empty (all rules already assigned) | Left panel: "All org rules are already in this space" |
| Error (fetch failed) | "Failed to load rules. [Retry]" link |
| Populated | Default state as per layout above |
| Submitting | Assign button shows spinner "Assigning..."; inputs disabled |
| Success | Modal closes; toast: "N rules assigned to [Space Name]" |
| Partial failure | Modal closes; warning toast: "N of M rules assigned. Failed: [names]" |

---

## 4. RuleListPage Changes

### 4.1 Single-Rule Action Menu

**Trigger:** The `⋯` (MoreHorizontal) icon dropdown per row.

**New menu item added after "Move to Space":**
```
┌─────────────────┐
│  Edit           │
│  Duplicate      │
│  Move to Space  │
│  Assign to Space│  ← NEW (shown after Move)
│  ──────────────  │
│  Delete         │
└─────────────────┘
```

**Visibility:** Hidden for VIEWER role (same condition as "Move to Space").

**Note:** "Move to Space" (existing from Epic 2) and "Assign to Space" (new) serve similar purposes. Consider renaming "Move to Space" to "Change Space" and using "Assign to Space" only from the bulk bar to reduce redundancy. This decision is left to the implementing engineer in consultation with the Tech Lead.

### 4.2 Bulk Action Bar

When ≥ 1 rows are checked, the bulk action bar appears:

```
┌──────────────────────────────────────────────────────────────────┐
│  ✓ 5 rules selected   [Assign to Space]  [Delete]  [Clear]      │
└──────────────────────────────────────────────────────────────────┘
```

**"Assign to Space" button:**
- Style: `variant="outline"`, small size
- Icon: `LinkIcon`
- Opens `AssignToSpaceModal` with all selected rule IDs

---

## 5. AssignToSpaceModal

### 5.1 Layout

**Single rule:**
```
┌──────────────────────────────────────────────────┐
│  Assign to Space                          [✕]    │
├──────────────────────────────────────────────────┤
│  Assign "Credit Score Rule" to a space.          │
│                                                  │
│  Target Space                                    │
│  ┌────────────────────────────────────────────┐  │
│  │  Select a space...                     ▼  │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
├──────────────────────────────────────────────────┤
│                          [Cancel]  [Assign]      │
└──────────────────────────────────────────────────┘
```

**Bulk (N rules):**
- Title: "Assign to Space"
- Body text: "Assign **N rules** to a space."
- Rest of layout identical

### 5.2 Component Specs

**Modal dimensions:** `max-w-sm` (384px)

**Space dropdown:**
- Shadcn/ui `Select` component
- Populated from `GET /spaces` (org-scoped)
- Sorted alphabetically by space name
- Placeholder: "Select a space..."
- `aria-label="Target space"`

**Assign button:**
- `variant="default"`
- Disabled until a space is selected
- Loading state: "Assigning..."

### 5.3 States

| State | Behavior |
|-------|----------|
| Loading spaces | Dropdown shows "Loading spaces..." and is disabled |
| Error loading spaces | "Failed to load spaces. [Retry]" |
| No space selected | Assign button disabled |
| Space selected | Assign button enabled |
| Submitting | Spinner; inputs disabled |
| Success | Modal closes; toast: "Rule assigned to [Space Name]" / "N rules assigned to [Space Name]" |
| Failure | Error message inline: "Assignment failed: [reason]"; modal stays open |

---

## 6. Toast Notifications

All toasts use the existing `useToast` hook (Shadcn/ui Sonner pattern).

| Scenario | Toast Type | Message |
|----------|------------|---------|
| Single rule assigned | Success | "[Rule Name] assigned to [Space Name]" |
| N rules assigned (all success) | Success | "N rules assigned to [Space Name]" |
| Partial failure | Warning | "N of M rules assigned. Failed: [Rule A], [Rule B]" |
| All failed | Error | "Assignment failed. Please try again." |

---

## 7. Accessibility Requirements

| Requirement | Implementation |
|-------------|----------------|
| Focus trap in modals | `FocusTrap` or `Dialog` from Radix (already used in existing modals) |
| ESC closes modal | Handled by Radix Dialog `onEscapeKeyDown` |
| Focus on open | First focusable element: search input (AssignRulesModal) / space dropdown (AssignToSpaceModal) |
| Checkbox accessible name | `aria-label="Select [rule name]"` |
| List count announced | `aria-live="polite"` region: "N rules selected" |
| Confirm button state | `aria-disabled="true"` when disabled (not HTML disabled, to keep tab order) |
| Role for modal | `role="dialog"` with `aria-labelledby` pointing to modal title |
| Error messages | `role="alert"` with `aria-live="assertive"` |
| Color-only information | Rule type badge uses both color AND text label |

---

## 8. Responsive Behavior

| Breakpoint | AssignRulesModal | AssignToSpaceModal |
|------------|-----------------|-------------------|
| Desktop (≥ 768px) | Two-panel layout as specified | Centered modal |
| Tablet (< 768px) | Single panel; selected count in header | Centered modal |
| Mobile (< 640px) | Full-screen sheet; selected count in header | Full-screen sheet |

---

## 9. Design Tokens and Components (Shadcn/ui)

| UI Element | Component |
|------------|-----------|
| Modal container | `Dialog`, `DialogContent`, `DialogHeader`, `DialogFooter` |
| Search input | `Input` with `SearchIcon` prefix |
| Checkbox | `Checkbox` |
| Rule type badge | `Badge variant="outline"` with type-specific color class |
| Space dropdown | `Select`, `SelectTrigger`, `SelectContent`, `SelectItem` |
| Assign button | `Button variant="default"` |
| Cancel button | `Button variant="ghost"` |
| Loading skeleton | `Skeleton` (3 rows) |
| Toast | `useToast` (existing hook) |

---

*Handoff to Tech Lead: UX spec complete. Proceed to story refinement (story-012, story-013).*
