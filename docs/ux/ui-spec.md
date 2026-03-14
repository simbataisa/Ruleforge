# UI Specification — Spaces Page Features

**Version:** 1.0
**Date:** 2026-03-14
**Author:** BMAD UX/UI Designer Agent (Phase 3c)
**Input:** `docs/prd.md`, `docs/architecture/solution-architecture.md`

---

## Personas

### Alex — Rule Author
- **Role:** Creates and manages rules within assigned spaces
- **Goal:** Quickly see available spaces, navigate to the right workspace, understand rule inventory
- **Pain Point:** Spaces list shows only a name — no indication of what's inside

### Sam — Space Admin (Org ADMIN role)
- **Role:** Creates spaces, configures them, manages approval policies
- **Goal:** Full lifecycle management of spaces without engineering support
- **Pain Point:** No edit/delete, no approval UI, "Manage" button goes nowhere

### Taylor — Org Owner
- **Role:** Top-level organizational authority
- **Goal:** Maintain clean space hygiene, delete unused spaces, control delete operations
- **Pain Point:** Can't delete spaces even when they're empty, no org-level view

---

## Screen 1: SpaceListPage (`/spaces`) — Enhanced

### Layout

```
┌──────────────────────────────────────────────────────────┐
│ [Spaces]                          [+ Create Space]       │
│ Manage workspaces to organize rules...                    │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  ┌─────────────────────────────────────────────────┐     │
│  │ 📁 Production Rules        [Open] [⚙] [🗑]      │     │
│  │ Rules for production deployment                  │     │
│  │ ●●● 42 rules                                     │     │
│  └─────────────────────────────────────────────────┘     │
│                                                           │
│  ┌─────────────────────────────────────────────────┐     │
│  │ 📁 Analytics               [Open] [⚙] [🗑]      │     │
│  │ No description                                   │     │
│  │ ●●● 7 rules                                      │     │
│  └─────────────────────────────────────────────────┘     │
│                                                           │
└──────────────────────────────────────────────────────────┘
```

### Card Component Spec

Each space card contains:

| Element | Content | Condition |
|---|---|---|
| Folder icon | `📁` (indigo-600) | Always |
| Space name | Bold, gray-900, 16px | Always |
| Description | gray-500, 14px | If null: "No description" in italic gray-400 |
| Rule count badge | `N rules` in indigo-100/indigo-800 | Always (shows 0 if empty) |
| "Open" button | `Button variant="outline"` | Always |
| Settings icon button | `⚙` icon, `variant="ghost"` | ADMIN/OWNER only |
| Delete icon button | `🗑` trash icon, `variant="ghost"` text-red-600 | OWNER only |

**Role-based visibility:**
- VIEWER/EDITOR: See card with name, description, rule count, "Open" button only
- ADMIN: All above + Settings gear icon
- OWNER: All above + Delete trash icon

### States

| State | Visual |
|---|---|
| Loading | 3 skeleton cards (pulsing gray rectangles, same card height) |
| Empty | `EmptyState` component: "No spaces yet" + "Create your first space" + `+ Create Space` button |
| Populated | Grid of cards (1 column on mobile, 2 on tablet, 3 on desktop ≥1280px) |
| Error | Alert banner: "Failed to load spaces" + Retry button |

### Create Space Modal

Existing modal extended with description field:

```
┌────────────────────────────────────────┐
│ Create Space                        [×] │
├────────────────────────────────────────┤
│ Space Name *                            │
│ ┌──────────────────────────────────┐   │
│ │ e.g., Production Rules           │   │
│ └──────────────────────────────────┘   │
│                                         │
│ Description (optional)                  │
│ ┌──────────────────────────────────┐   │
│ │                                  │   │
│ └──────────────────────────────────┘   │
│                                         │
│ [Cancel]                    [Create]   │
└────────────────────────────────────────┘
```

**Validation:**
- Name: Required, max 255 chars, empty = disabled Create button
- Description: Optional, max 2000 chars, character counter shown at 1800+

**Error state (409 slug conflict):**
- Inline error below Name field: "A space with this name already exists"
- Name field border turns red

### Delete Confirmation

Uses existing `ConfirmDialog` component:

```
┌────────────────────────────────────────────┐
│ Delete 'Production Rules'?              [×] │
├────────────────────────────────────────────┤
│  ⚠  This action cannot be undone.          │
│                                             │
│     The space and its configuration will   │
│     be permanently deleted.                 │
│                                             │
│  [Cancel]              [Delete Space] 🗑   │
└────────────────────────────────────────────┘
```

**Non-empty space (409 response):**
```
┌────────────────────────────────────────────┐
│ Cannot Delete Space                     [×] │
├────────────────────────────────────────────┤
│  🚫 This space contains 42 rules.          │
│                                             │
│     Remove all rules from this space       │
│     before deleting it.                    │
│                                             │
│                            [Got It]        │
└────────────────────────────────────────────┘
```

**Note:** Delete icon button should be `disabled` with tooltip "Space contains rules" when `ruleCount > 0`, to prevent the 409 altogether.

---

## Screen 2: SpaceDetailPage (`/spaces/:id`) — New

### Layout

```
┌───────────────────────────────────────────────────────────┐
│ ← Spaces / Production Rules                               │
│                                                           │
│ Production Rules                       [Space Settings →] │
│ Rules for production deployment                           │
│                                                           │
│  ┌────────────┐  ┌────────────┐  ┌────────────────────┐  │
│  │ 42         │  │ Jan 15     │  │ Mar 14             │  │
│  │ Rules      │  │ Created    │  │ Last Updated       │  │
│  └────────────┘  └────────────┘  └────────────────────┘  │
│                                                           │
│ ──────────────────────────────────────────────────────── │
│                                                           │
│  [Overview]  [Rules]                                      │
│                                                           │
│  ┌──────────────────────────────────────────────────┐    │
│  │  Overview                                        │    │
│  │                                                  │    │
│  │  This space is active and contains 42 rules.     │    │
│  │  Navigate to Rules to browse all content.        │    │
│  │                                                  │    │
│  │  [Browse Rules →]                                │    │
│  └──────────────────────────────────────────────────┘    │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

### Element Spec

| Element | Details |
|---|---|
| Breadcrumb | "← Spaces" (clickable link to /spaces) + " / {name}" |
| Page title | `<h1>` space name, text-2xl font-bold text-gray-900 |
| Description | text-gray-500 below title; hidden if null |
| Settings button | Top-right, `Button variant="outline"`, icon `⚙ Settings`, → `/spaces/:id/settings` |
| Stats bar | 3 stat cards: rule count (primary stat), createdAt (formatted date), updatedAt |
| Tabs | `[Overview]` | `[Rules]` — tab design matching rest of app |
| Overview tab | Static card with count summary + "Browse Rules →" button that links to /rules with space filter |

### Stats Bar Cards

```
┌────────────┐
│ 42         │  ← number text-3xl font-bold text-indigo-600
│ Rules      │  ← label text-sm text-gray-500
└────────────┘
```

### States

| State | Trigger | Visual |
|---|---|---|
| Loading | Page mount | Skeleton for title, stats bar, tab content |
| 404 | Space not found | "Space not found" message + "Back to Spaces" link |
| Error | API failure | Error alert + retry |
| Populated | Data loaded | Full layout |

---

## Screen 3: SpaceSettingsPage (`/spaces/:id/settings`) — New

### Layout

```
┌───────────────────────────────────────────────────────────┐
│ ← Spaces / Production Rules / Settings                    │
│                                                           │
│ Space Settings                                            │
│                                                           │
│  [General]  [Approval Settings]  [Danger Zone]           │
│                                                           │
│ ──────────────────────────────────────────────────────── │
│                                                           │
│  General                                                  │
│                                                           │
│  Space Name *                                             │
│  ┌──────────────────────────────────────────────────┐    │
│  │ Production Rules                                 │    │
│  └──────────────────────────────────────────────────┘    │
│                                                           │
│  Description                                              │
│  ┌──────────────────────────────────────────────────┐    │
│  │ Rules for production deployment                  │    │
│  │                                                  │    │
│  └──────────────────────────────────────────────────┘    │
│                                                           │
│                              [Cancel]  [Save Changes]    │
│                                                           │
└───────────────────────────────────────────────────────────┘
```

### General Tab Spec

| Element | Details |
|---|---|
| Space Name field | Pre-populated from API, `@NotBlank`, max 255 |
| Description field | Textarea, pre-populated (blank if null), max 2000, optional |
| Cancel button | `variant="outline"`, resets form to original values |
| Save Changes button | `variant="primary"`, disabled if no changes, shows loading spinner |
| Success feedback | Toast: "Space updated successfully" |
| Error (409) | Inline error below Name: "A space with this name already exists" |

**Unsaved changes guard:** If user navigates away with unsaved changes, show browser default `beforeunload` warning (standard behavior).

### Approval Settings Tab Spec (P1 — deferred)

```
┌──────────────────────────────────────────────────────────┐
│  Approval Settings                                       │
│                                                          │
│  Approvers                                               │
│  ┌──────────────────────────────────────┐               │
│  │ alex@company.com  ×                  │               │
│  │ sam@company.com   ×                  │               │
│  └──────────────────────────────────────┘               │
│  [Add approver...]                                       │
│                                                          │
│  Approval SLA (hours)                                    │
│  ┌─────────┐                                            │
│  │ 24      │                                            │
│  └─────────┘                                            │
│                                                          │
│  ○ Require all approvers                                 │
│  ● Any approver can approve                              │
│                                                          │
│                            [Cancel]  [Save Settings]    │
└──────────────────────────────────────────────────────────┘
```

Maps to `SpaceApprovalSettingsDto` from management-service. Calls `PUT /v1/spaces/:id/approval-settings`.

### Danger Zone Tab Spec

```
┌──────────────────────────────────────────────────────────┐
│  Danger Zone                                             │
│                                                          │
│ ┌────────────────────────────────────────────────────┐  │
│ │  Delete this space                                 │  │
│ │                                                    │  │
│ │  Once you delete a space, there is no going back.  │  │
│ │  This will permanently delete the space and all    │  │
│ │  its configuration.                                │  │
│ │                                        [Delete]   │  │
│ └────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

| Element | Details |
|---|---|
| Section border | `border border-red-200 rounded-lg p-4` |
| Delete button | `variant="destructive"` (red bg), triggers ConfirmDialog |
| Delete button disabled | When `ruleCount > 0`, tooltip: "Remove all rules first" |
| ConfirmDialog text | "Delete '{name}'? This cannot be undone." |
| Post-delete | Navigate to `/spaces`, toast: "Space deleted" |

**OWNER-only:** This entire tab is hidden for ADMIN users. Redirect to General tab if ADMIN visits `/spaces/:id/settings` with `#danger-zone` hash.

---

## OrgSettingsPage Fix

The "Manage" button in `OrgSettingsPage.tsx` needs `onClick`:

```tsx
// BEFORE (broken)
<Button variant="outline">Manage</Button>

// AFTER
<Button variant="outline" onClick={() => navigate(`/spaces/${space.id}/settings`)}>
  Manage
</Button>
```

---

## Interaction Specifications

### Navigation Flow

```
/spaces
  ├── [Open] → /spaces/:id
  │               ├── [Space Settings] → /spaces/:id/settings
  │               └── [Browse Rules] → /rules (filtered by space)
  │
  ├── [⚙ Settings icon] → /spaces/:id/settings
  │                           ├── [General tab] — default
  │                           ├── [Approval Settings tab]
  │                           └── [Danger Zone tab]
  │                               └── [Delete] → /spaces (post-delete)
  │
  └── [🗑 Delete icon] → ConfirmDialog → DELETE → /spaces (stay)

/settings → [Manage] → /spaces/:id/settings
```

### Loading States

All new pages use skeleton loading (not spinner), matching existing page patterns:

- SpaceDetailPage: Skeleton for breadcrumb, title, stats bar (3 placeholder boxes), tab content
- SpaceSettingsPage: Skeleton for form fields

### Error Handling

| Error | Response | UI Treatment |
|---|---|---|
| Space not found | 404 | Full-page "Space not found" with Back link |
| Auth error | 401 | Redirect to /login (existing auth flow) |
| Permission denied | 403 | Alert banner: "You don't have permission to perform this action" |
| Name conflict | 409 (SLUG_CONFLICT) | Inline error under name field |
| Non-empty delete | 409 (SPACE_NOT_EMPTY) | Error modal with rule count |
| Server error | 500 | Error banner + Retry button |
| Network | N/A | Toast: "Connection error. Please try again." |

---

## Accessibility

| Requirement | Implementation |
|---|---|
| Keyboard nav | Tab through card actions; Enter/Space triggers buttons |
| Focus management | Modal opens → focus first interactive element; modal closes → focus trigger button |
| ARIA labels | `aria-label="Settings for {name}"` on gear icon; `aria-label="Delete {name}"` on trash icon |
| Error association | Error messages linked to inputs via `aria-describedby` |
| Stats bar | Use `<dl>/<dt>/<dd>` semantic structure for stats |
| Icon buttons | Always paired with `aria-label` and tooltip |
| Delete dialog | Focus trap within dialog; `role="alertdialog"` |
| Color alone | Rule count badge uses text + background color (not color alone) |

---

## Responsive Behavior

| Breakpoint | SpaceListPage | SpaceDetailPage | SpaceSettingsPage |
|---|---|---|---|
| ≥1280px | 3-column card grid | Full layout | Full form |
| 768–1279px | 2-column grid | Full layout | Full form |
| <768px | 1-column stack | Stacked stats, single column | Stacked form, tabs become select |

On mobile (<768px), space card actions collapse to a `⋮` kebab menu:
```
┌────────────────────────────┐
│ Production Rules         ⋮ │  ← Opens action sheet
│ 42 rules                   │
└────────────────────────────┘
                    ┌──────────────┐
                    │ Open         │
                    │ Settings     │
                    │ Delete       │
                    └──────────────┘
```

---

## Cross-Review Notes (SA ↔ UX)

| UX Element | API Field | Status |
|---|---|---|
| Rule count badge | `SpaceDto.ruleCount` | ✅ Matches SA design |
| Description text | `SpaceDto.description` | ✅ Matches SA design |
| Settings tab form | `PUT /organizations/current/spaces/:id` | ✅ Matches SA design |
| Approval Settings tab | `GET/PUT /v1/spaces/:id/approval-settings` | ✅ Existing endpoints |
| Delete disabled when > 0 rules | `SpaceDto.ruleCount > 0` | ✅ Uses ruleCount from DTO |
| Slug field | Not shown in UI (internal only) | ✅ Computed server-side |
| Created/Updated dates | `SpaceDto.createdAt`, `SpaceDto.updatedAt` | ✅ Matches SA DTO |

---

**Status:** COMPLETE — Ready for Tech Lead Story Refinement
**UX/UI Designer Sign-Off:** BMAD UX Agent, 2026-03-14
