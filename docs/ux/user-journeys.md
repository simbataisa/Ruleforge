# User Journeys — RuleForge

> **Status:** Draft — seeded by /ux-designer:onboard on 2026-04-30. Journeys should be refined during UX brainstorming and linked to screen specs or wireframes as they mature.

## Journey 1: Space Admin Creates And Configures A Space

1. Sam opens `/spaces`.
2. Sam creates a space with a name and optional description.
3. RuleForge returns the generated slug, description, rule count, and timestamps.
4. Sam opens the space detail page and verifies the empty state.
5. Sam opens settings, updates details, and configures approval settings.
6. Sam confirms the space appears correctly in list, detail, settings, and org settings.

**Key screens:** `/spaces`, `/spaces/:id`, `/spaces/:id/settings`, `/settings`.

**State coverage:** loading, empty, populated, validation error, conflict, offline/degraded.

## Journey 2: Rule Author Finds Rules In A Space

1. Alex opens `/spaces` and scans space cards by name, description, and rule count.
2. Alex opens a specific space.
3. Alex filters or searches the rules table within that space.
4. Alex opens a rule editor or returns to the broader rules list with space context preserved.

**Key screens:** `/spaces`, `/spaces/:id`, `/rules`, `/rules/:id/edit`.

**State coverage:** space not found, no rules assigned, no filter matches, table populated, rule action menu.

## Journey 3: Org Owner Deletes An Empty Space

1. Taylor opens `/spaces` or `/spaces/:id/settings`.
2. Taylor sees delete affordances only where the role allows them.
3. RuleForge disables deletion or explains why deletion is blocked when a space contains rules.
4. For an empty space, Taylor confirms deletion in a dialog naming the space.
5. RuleForge returns Taylor to the space list and shows a success state.

**Key screens:** `/spaces`, `/spaces/:id/settings`, confirmation dialog.

**State coverage:** non-owner hidden action, non-empty blocked state, confirmation, API failure, successful redirect.

## Journey 4: Developer Configures Webhook Authentication

1. Morgan opens `/webhooks` and selects a webhook.
2. Morgan opens security/auth settings.
3. Morgan chooses no auth, bearer, basic, custom header, or OAuth2 client credentials.
4. Morgan saves configuration and tests delivery.
5. Morgan reviews metrics, delivery logs, AUTH_FAILURE state, and retry behavior.

**Key screens:** `/webhooks`, `/webhooks/:id`, webhook security tab, delivery logs tab, metrics tab.

**State coverage:** credential validation errors, token endpoint failure, auth failure status, retry reset, metrics loading.

## Journey 5: Validator Reviews Operational Evidence

1. Priya opens release-relevant screens and executes smoke test workflows.
2. Priya verifies role-specific visibility and keyboard access.
3. Priya checks audit events, webhook delivery outcomes, analytics dashboards, and error states.
4. Priya records gaps in release readiness docs.

**Key screens:** `/audit`, `/analytics`, `/approvals`, `/webhooks/:id`, `/spaces`.

**State coverage:** audit export, empty analytics, failed webhooks, approval queue empty/populated, inaccessible actions.

## Journeys To Author

- AI-assisted rule creation and review.
- Scheduler and workflow builder journey.
- Connector creation and enrichment testing.
- API key lifecycle and SDK onboarding.
- A/B test setup, monitoring, and promotion.
