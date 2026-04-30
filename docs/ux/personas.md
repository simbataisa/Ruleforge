# UX Personas — RuleForge

> **Status:** Draft — seeded by /ux-designer:onboard on 2026-04-30. Source: project brief, PRD, and existing UX specs.

## Alex — Rule Author

Alex creates, edits, tests, and publishes rules inside assigned spaces.

- **Primary goals:** Find the right workspace quickly, understand rule inventory, edit rules without breaking approvals, and test behavior before publishing.
- **Pain points:** Fragmented navigation, unclear space context, missing metadata, and slow confidence checks before changes go live.
- **UX needs:** Clear breadcrumbs, visible rule status, fast filters, stable editor layouts, and explicit save/test/publish outcomes.

## Sam — Space Admin

Sam configures spaces, manages approval workflows, and maintains workspace hygiene.

- **Primary goals:** Create and update spaces, configure approval policy, assign or move rules, and resolve workflow bottlenecks.
- **Pain points:** Missing settings affordances, uncertainty around delete safety, and limited visibility into who can approve what.
- **UX needs:** Role-aware actions, confirmation flows, approval settings screens, conflict messaging, and audit-friendly changes.

## Taylor — Org Owner

Taylor owns organization-level governance and enterprise adoption.

- **Primary goals:** Enforce access control, remove unused spaces, verify auditability, and keep the platform suitable for regulated operations.
- **Pain points:** High risk from silent destructive actions, unclear tenant boundaries, and insufficient release confidence.
- **UX needs:** Strong permission cues, destructive-action guardrails, audit visibility, and concise operational dashboards.

## Morgan — Developer / Integrator

Morgan integrates RuleForge through APIs, SDKs, webhooks, connectors, and embedded rule execution.

- **Primary goals:** Use reliable contracts, configure external delivery, inspect failures, and automate rule execution safely.
- **Pain points:** Implicit API contracts, hard-to-debug webhook auth failures, and uncertainty around retry or token behavior.
- **UX needs:** API key management, webhook status and metrics, connector test feedback, copyable technical values, and precise error states.

## Priya — QA / Release Validator

Priya validates new rule workflows, smoke tests, accessibility, and release readiness.

- **Primary goals:** Confirm acceptance criteria, reproduce errors, check role-based behavior, and verify state coverage.
- **Pain points:** Missing empty/error/loading states, undocumented edge cases, and inconsistent component behavior across screens.
- **UX needs:** Explicit state specs, predictable test data hooks, accessible modals, and clear release-readiness evidence.

## Personas To Refine

- Business analyst segment beyond rule authoring: [populate]
- Read-only viewer and executive/reporting user: [populate]
- Security or compliance reviewer: [populate]
