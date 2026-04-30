---
version: alpha
name: RuleForge
description: Enterprise Business Rules Management System for authoring, managing, and executing business rules at scale.
colors:
  primary: "#4F46E5"
  primary-hover: "#4338CA"
  surface: "#FFFFFF"
  surface-muted: "#F9FAFB"
  surface-raised: "#F3F4F6"
  text: "#111827"
  text-muted: "#6B7280"
  border: "#E5E7EB"
  border-strong: "#D1D5DB"
  success: "#15803D"
  success-bg: "#DCFCE7"
  info: "#1D4ED8"
  info-bg: "#DBEAFE"
  warning: "#B45309"
  warning-bg: "#FEF3C7"
  danger: "#DC2626"
  danger-hover: "#B91C1C"
  danger-bg: "#FEE2E2"
typography:
  heading-lg:
    fontFamily: Inter, ui-sans-serif, system-ui, sans-serif
    fontSize: 24px
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: 0px
  heading-md:
    fontFamily: Inter, ui-sans-serif, system-ui, sans-serif
    fontSize: 20px
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: 0px
  body-md:
    fontFamily: Inter, ui-sans-serif, system-ui, sans-serif
    fontSize: 16px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0px
  body-sm:
    fontFamily: Inter, ui-sans-serif, system-ui, sans-serif
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0px
  label-sm:
    fontFamily: Inter, ui-sans-serif, system-ui, sans-serif
    fontSize: 14px
    fontWeight: 500
    lineHeight: 1.4
    letterSpacing: 0px
  metadata:
    fontFamily: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace
    fontSize: 12px
    fontWeight: 400
    lineHeight: 1.4
    letterSpacing: 0px
spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  page: 24px
rounded:
  sm: 4px
  md: 6px
  lg: 8px
  full: 9999px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.surface}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.lg}"
    padding: "{spacing.md}"
  button-primary-hover:
    backgroundColor: "{colors.primary-hover}"
  button-secondary:
    backgroundColor: "{colors.surface-raised}"
    textColor: "{colors.text}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.lg}"
    padding: "{spacing.md}"
  button-outline:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text-muted}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.lg}"
    padding: "{spacing.md}"
  button-danger:
    backgroundColor: "{colors.danger}"
    textColor: "{colors.surface}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.lg}"
    padding: "{spacing.md}"
  button-danger-hover:
    backgroundColor: "{colors.danger-hover}"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    rounded: "{rounded.lg}"
    padding: "{spacing.lg}"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.text}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.lg}"
    padding: "{spacing.sm}"
  input-disabled:
    backgroundColor: "{colors.surface-muted}"
    textColor: "{colors.text-muted}"
    typography: "{typography.body-sm}"
    rounded: "{rounded.lg}"
    padding: "{spacing.sm}"
  badge:
    backgroundColor: "{colors.info-bg}"
    textColor: "{colors.info}"
    typography: "{typography.metadata}"
    rounded: "{rounded.full}"
    padding: "{spacing.xs}"
  badge-success:
    backgroundColor: "{colors.success-bg}"
    textColor: "{colors.success}"
    typography: "{typography.metadata}"
    rounded: "{rounded.full}"
    padding: "{spacing.xs}"
  badge-warning:
    backgroundColor: "{colors.warning-bg}"
    textColor: "{colors.warning}"
    typography: "{typography.metadata}"
    rounded: "{rounded.full}"
    padding: "{spacing.xs}"
  badge-danger:
    backgroundColor: "{colors.danger-bg}"
    textColor: "{colors.danger-hover}"
    typography: "{typography.metadata}"
    rounded: "{rounded.full}"
    padding: "{spacing.xs}"
  divider:
    backgroundColor: "{colors.border}"
    height: 1px
  divider-strong:
    backgroundColor: "{colors.border-strong}"
    height: 1px
---

# RuleForge Design System

## Overview

RuleForge is a work-focused enterprise BRMS used by business analysts, rule authors, developers, and administrators who need confidence when managing decision logic. The interface should feel dense, precise, and operational: clear hierarchy, fast scanning, predictable controls, and restrained decoration.

The product ships as a React 18 and Tailwind CSS application with custom shared primitives in `frontend/src/shared/components`. Current UX artifacts include screen-level specs for Spaces, space-rule management, space assignment, space banners, webhooks authentication, and OAuth2/custom header webhook auth.

## Colors

The palette is grounded in neutral surfaces and text, with indigo reserved for primary actions and active navigation.

- **Primary (#4F46E5):** Primary actions, focus affordances, selected navigation, and key metrics.
- **Neutral surfaces (#FFFFFF, #F9FAFB, #F3F4F6):** Page backgrounds, cards, table headers, and disabled controls.
- **Text (#111827, #6B7280, #9CA3AF):** Main content, secondary metadata, placeholders, and low-emphasis labels.
- **Status colors:** Green for success, blue for informational states, amber for warnings, and red for destructive or failed states.
- **Borders (#E5E7EB, #D1D5DB):** Low-contrast containment for cards, tables, modals, and inputs.

## Typography

The app relies on the Tailwind default system sans stack and a monospace stack for slugs, IDs, timestamps, and other technical metadata.

- **Heading large:** Page titles and primary screen headings.
- **Heading medium:** Panel titles, card section headings, and modal titles.
- **Body:** Reading text, descriptions, and workflow instructions.
- **Labels:** Form labels, button labels, table headers, and compact controls.
- **Metadata:** Slugs, IDs, version numbers, timestamps, and API-style identifiers.

## Layout

Screens use application-shell layouts with constrained page padding and dense, scannable content regions. Route inventory is driven by `frontend/src/app/routes.tsx`, including `/rules`, `/approvals`, `/analytics`, `/schedules`, `/connectors`, `/ab-tests`, `/audit`, `/webhooks`, `/tags`, `/spaces`, and settings routes.

Use an 8px spacing rhythm. Page padding defaults to 24px, cards use 24px internal padding, and dense lists/tables use 12px to 16px row spacing. Responsive grids use one column on mobile, two on tablet, and three only when the content is genuinely comparable at wide desktop widths.

## Elevation & Depth

Depth is intentionally quiet. Use borders and tonal surface changes before shadows. Cards and modals may use `shadow-sm` or a modal shadow, but routine page sections should remain unframed unless they are repeated items, dialogs, tables, or tool containers.

## Shapes

The shape language uses small radii for operational clarity. Cards, inputs, modals, and buttons typically use 8px radius. Compact menu items and icon buttons may use 4px to 6px. Pills and badges use a full radius only for compact metadata chips.

## Components

### Buttons

Use icon buttons for common tool actions such as settings, delete, copy, move, refresh, and close. Use labeled buttons for commands that benefit from explicit text, such as Save Changes, Create Space, Run Test, or Retry.

Primary buttons use indigo and should be limited to the main action in a region. Danger buttons use red and require confirmation for destructive actions. Outline and ghost buttons handle secondary actions without competing with the primary path.

### Inputs

Inputs pair a visible label with inline error text. Validation errors use red borders and red helper text. Disabled fields use muted surfaces and text; read-only technical values such as slugs may use the metadata typography token.

### Cards

Cards are for repeated entities, stats, modals, and contained tools. Do not nest cards inside cards. For dashboards, keep cards compact and comparable; avoid marketing-style hero treatments.

### Tables and Lists

Tables use gray header backgrounds, muted uppercase or compact labels, and stable columns. Rows should support hover affordances when clickable. Empty, loading, populated, error, and offline/degraded states must be specified for new screens.

### Badges and Status

Badges represent status, rule type, role, count, and scope. Keep them compact, text-first, and color-coded by semantic meaning. Do not use badges as buttons unless the component is explicitly interactive and keyboard accessible.

### Modals and Confirmations

Modals must trap focus, close on Escape where safe, and return focus to the invoking control. Destructive confirmations must name the resource and explain the consequence before enabling the destructive action.

## Do's and Don'ts

- Do optimize for scanning, comparison, and repeated operational workflows.
- Do use icons from Lucide React for familiar tool actions.
- Do define loading, empty, populated, error, and offline/degraded states for each new screen.
- Do maintain WCAG AA contrast and keyboard access for modals, menus, forms, and tables.
- Don't use decorative gradients, oversized hero layouts, or marketing composition inside the product UI.
- Don't create new one-off component styles when a shared primitive already exists.
- Don't put UI cards inside other cards.
- Don't rely on color alone to communicate status or permissions.

## Changelog

| Date | Version | Change | Author |
|---|---:|---|---|
| 2026-04-30 | 0.1.0 | Initial seed from /ux-designer:onboard | UX Designer - brownfield onboarding |
