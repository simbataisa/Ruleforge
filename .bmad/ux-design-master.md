# UX Design Master — RuleForge

**Last updated:** 2026-04-30
**Design Tool:** Pencil
**Master File:** `docs/ux/rulefoge.pen`
**Design System:** `docs/ux/DESIGN.md`

## Detected UX Stack

| Category | Detected |
|---|---|
| Frontend framework | React 18 + TypeScript + Vite |
| Styling | Tailwind CSS 3.4 with default theme extension |
| Component library | Custom shared components in `frontend/src/shared/components` |
| Icons | Lucide React |
| Data/UI support | React Query, Zustand, React Flow, Monaco Editor, Recharts |
| Storybook | Not detected |
| A11y tooling | WCAG requirements in docs; dedicated axe/pa11y package not detected |

## Existing UX Artifacts

| Artifact | Purpose |
|---|---|
| `docs/ux/DESIGN.md` | Canonical Google Stitch design system seed |
| `docs/ux/ui-spec.md` | Spaces Page screen spec |
| `docs/ux/ui-spec-spaces-rules.md` | Space-scoped rules UX spec |
| `docs/ux/ui-spec-space-banners.md` | Space context banner UX spec |
| `docs/ux/ui-spec-space-assignment.md` | Space assignment UX spec |
| `docs/ux/ui-spec-webhooks-auth.md` | Webhook auth UX spec |
| `docs/ux/ui-spec-webhooks-oauth2-customheader.md` | Webhook OAuth2/custom header UX spec |
| `docs/ux/rulefoge.pen` | Pencil design file detected during onboard |

## Screen Inventory

| Route | Primary page/component |
|---|---|
| `/login`, `/register` | Auth pages |
| `/rules`, `/rules/:id/edit` | Rule list and editor |
| `/approvals` | Approval queue |
| `/spaces`, `/spaces/:id`, `/spaces/:id/settings` | Space list, detail, and settings |
| `/webhooks`, `/webhooks/:id` | Webhook list and detail |
| `/analytics`, `/analytics/rules/:ruleId` | Analytics dashboard and rule stats |
| `/schedules`, `/schedules/:id`, `/workflows/:id`, `/holiday-calendars/:id` | Scheduler and workflow surfaces |
| `/connectors` | Connector management |
| `/ab-tests`, `/ab-tests/:id` | A/B testing |
| `/audit` | Audit log |
| `/api-keys`, `/settings`, `/settings/sso`, `/settings/members`, `/profile` | Admin and account settings |
| `/tags` | Tag management |

## Notes

- Tailwind has no custom theme extension yet; `DESIGN.md` maps the observed default Tailwind colors and spacing used in components.
- Existing screen specs are curated and should not be replaced by onboarding. Extend them per epic.
- New screen specs should include loading, empty, populated, error, and offline/degraded states.
- Consider adding Storybook or a lightweight component gallery once the shared component layer stabilizes.
