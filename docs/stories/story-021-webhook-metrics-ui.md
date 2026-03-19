# Story 021 — Frontend: WebhookMetricsTab + WebhookHealthBadge

**Story ID:** story-021
**Epic:** Webhooks Authentication Enhancement (Epic 5)
**Prepared by:** Tech Lead
**Date:** 2026-03-18
**Assigned to:** Frontend Engineer
**Priority:** P1 — Sprint 5.3
**Estimate:** 3 story points
**Depends on:** Story 018 (backend `GET /v1/webhooks/{id}/metrics` must exist), Story 017 (Webhook type includes `authType`)

---

## User Story

**As a** DevOps engineer or org owner,
**I want** a Metrics tab on the webhook detail page showing delivery success rate, failure counts, and a 7-day timeline chart,
**And** a health badge on the webhook list page,
**So that** I can immediately identify unhealthy webhooks without drilling into delivery logs.

---

## Acceptance Criteria

1. `WebhookDetailPage` has a fifth tab: "Metrics" (between "Events" and any trailing tabs).
2. `WebhookMetricsTab` renders 5 stat cards: Success Rate, Total Deliveries (7d), Failures (7d), DLQ Depth, Avg Latency.
3. Success Rate card variant changes based on health: ≥95% = default, 80–94% = amber border, <80% = destructive border.
4. DLQ Depth card shows warning icon if depth > 0.
5. A `<BarChart>` (Recharts, already in project) shows per-day success vs failed deliveries for last 7 days.
6. Chart X-axis: day labels (Mon, Tue…). Y-axis: delivery count. Green bars = success, red bars = failed.
7. Empty state: rendered when `totalDeliveries === 0` with text "No delivery data yet." + "Test Webhook →" link to Delivery Logs tab.
8. Loading state: skeleton cards + skeleton chart while `useWebhookMetrics` is loading.
9. Error state: alert with "Unable to load metrics" + Retry button.
10. `WebhookHealthBadge` component renders: green dot (Healthy), amber dot (Degraded), red triangle (Unhealthy), gray dash (No data).
11. `WebhookListPage` shows `WebhookHealthBadge` in each row alongside the status badge.
12. Health badge has a `<Tooltip>` showing success rate + DLQ depth on hover.
13. `WebhookHealthBadge` uses `useWebhookMetrics` with `staleTime: 60_000` — metrics refresh every 60s.
14. `npm run build` 0 TypeScript errors.
15. Vitest tests: `WebhookMetricsTab` renders loading/empty/error/data states, `WebhookHealthBadge` renders correct variant.

---

## Files to Create

### `frontend/src/features/webhooks/components/WebhookMetricsTab.tsx`

```typescript
interface WebhookMetricsTabProps {
  webhookId: string
}

export function WebhookMetricsTab({ webhookId }: WebhookMetricsTabProps): JSX.Element {
  const { data: metrics, isLoading, isError, refetch } = useWebhookMetrics(webhookId)

  if (isLoading) return <MetricsSkeleton />
  if (isError) return <MetricsError onRetry={refetch} />
  if (!metrics || metrics.totalDeliveries === 0) return <MetricsEmptyState />

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-medium text-muted-foreground">Last 7 days</h3>
        <Button variant="ghost" size="sm" onClick={() => refetch()}>
          <RefreshCw className="h-3 w-3 mr-1" /> Refresh
        </Button>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-5 gap-4">
        <SuccessRateCard value={metrics.successRate} />
        <StatCard label="Deliveries" value={metrics.totalDeliveries} />
        <StatCard label="Failures" value={metrics.failedDeliveries} />
        <DlqDepthCard depth={metrics.dlqDepth} />
        <StatCard label="Avg Latency" value={metrics.avgLatencyMs ? `${metrics.avgLatencyMs}ms` : '—'} />
      </div>

      {/* Timeline chart */}
      <DeliveryTimelineChart data={metrics.deliveryTimeline} />
    </div>
  )
}
```

**`SuccessRateCard` sub-component:**
```typescript
function SuccessRateCard({ value }: { value: number }) {
  const borderClass = value >= 95 ? '' : value >= 80 ? 'border-yellow-400' : 'border-destructive'
  return (
    <Card className={cn('border-2', borderClass)}>
      <CardContent className="pt-4">
        <p className="text-2xl font-bold">{value.toFixed(1)}%</p>
        <p className="text-xs text-muted-foreground mt-1">Success Rate</p>
      </CardContent>
    </Card>
  )
}
```

**`DeliveryTimelineChart` sub-component:**
```typescript
function DeliveryTimelineChart({ data }: { data: DeliveryTimelineEntry[] }) {
  return (
    <BarChart width="100%" height={200} data={data}>
      <CartesianGrid strokeDasharray="3 3" />
      <XAxis dataKey="date" tickFormatter={(d) => format(new Date(d), 'EEE')} />
      <YAxis />
      <Tooltip />
      <Legend />
      <Bar dataKey="success" fill="#22c55e" name="Success" stackId="a" />
      <Bar dataKey="failed" fill="#ef4444" name="Failed" stackId="a" />
    </BarChart>
  )
}
```

### `frontend/src/features/webhooks/components/WebhookHealthBadge.tsx`

```typescript
interface WebhookHealthBadgeProps {
  webhookId: string
}

export function WebhookHealthBadge({ webhookId }: WebhookHealthBadgeProps): JSX.Element {
  const { data: metrics } = useWebhookMetrics(webhookId)

  const health = computeHealth(metrics)

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <span role="status" aria-label={`Health: ${health.label}`}>
          {health.icon}
        </span>
      </TooltipTrigger>
      <TooltipContent>
        <p className="font-medium">{health.label}</p>
        {metrics && (
          <>
            <p className="text-xs">7d success rate: {metrics.successRate.toFixed(1)}%</p>
            <p className="text-xs">DLQ: {metrics.dlqDepth} messages</p>
          </>
        )}
      </TooltipContent>
    </Tooltip>
  )
}

function computeHealth(metrics?: WebhookMetrics): { label: string; icon: ReactNode } {
  if (!metrics || metrics.totalDeliveries === 0)
    return { label: 'No data', icon: <span className="text-muted-foreground">–</span> }
  if (metrics.successRate >= 95 && metrics.dlqDepth === 0)
    return { label: 'Healthy', icon: <span className="text-green-500">●</span> }
  if (metrics.successRate >= 80 && metrics.dlqDepth <= 5)
    return { label: 'Degraded', icon: <span className="text-yellow-500">●</span> }
  return { label: 'Unhealthy', icon: <span className="text-destructive">▲</span> }
}
```

---

## Files to Modify

### `frontend/src/api/webhooks.api.ts`

Add `useWebhookMetrics` hook:
```typescript
export function useWebhookMetrics(webhookId: string) {
  return useQuery({
    queryKey: ['webhooks', webhookId, 'metrics'],
    queryFn: () => api.get<WebhookMetrics>(`/v1/webhooks/${webhookId}/metrics`),
    staleTime: 60_000,
    enabled: !!webhookId,
  })
}
```

Add `WebhookMetrics` and `DeliveryTimelineEntry` interfaces (per Story 018 response shape).

### `frontend/src/features/webhooks/pages/WebhookDetailPage.tsx`

Add "Metrics" as fifth tab:
```tsx
<TabsList>
  <TabsTrigger value="settings">Settings</TabsTrigger>
  <TabsTrigger value="security">Security</TabsTrigger>
  <TabsTrigger value="delivery-logs">Delivery Logs</TabsTrigger>
  <TabsTrigger value="events">Events</TabsTrigger>
  <TabsTrigger value="metrics">Metrics</TabsTrigger>   {/* NEW */}
</TabsList>
// ...
<TabsContent value="metrics">
  <WebhookMetricsTab webhookId={webhook.id} />
</TabsContent>
```

### `frontend/src/features/webhooks/pages/WebhookListPage.tsx`

Add `WebhookHealthBadge` to each row (after status badge in table row):
```tsx
<TableCell>
  <WebhookHealthBadge webhookId={webhook.id} />
</TableCell>
```

Add column header "Health" in `<TableHead>`.

---

## Definition of Done

- [ ] `WebhookMetricsTab` renders loading / empty / error / data states correctly
- [ ] 5 stat cards with conditional styling for success rate and DLQ depth
- [ ] `DeliveryTimelineChart` renders stacked bar chart using Recharts
- [ ] `WebhookHealthBadge` renders correct icon + tooltip for all 4 health states
- [ ] 5th "Metrics" tab added to `WebhookDetailPage`
- [ ] `WebhookHealthBadge` added to `WebhookListPage` table
- [ ] `useWebhookMetrics` hook in `webhooks.api.ts` with 60s stale time
- [ ] Vitest tests: all render states for `WebhookMetricsTab`, badge variants
- [ ] `tsc --noEmit` 0 errors; `npm run lint` passes
- [ ] `npm run build` 0 errors

---

## Notes for Implementer

- Recharts is already a project dependency (`recharts` in `package.json`) — no new install needed.
- `format(new Date(d), 'EEE')` requires `date-fns` — verify it's installed (likely is given existing analytics charts). If not, use `new Date(d).toLocaleDateString('en-US', { weekday: 'short' })` instead.
- `WebhookHealthBadge` in the list page fires one `useWebhookMetrics` request per visible webhook. With React Query's `staleTime: 60_000`, this means N requests on initial page load but zero re-fetches for 60 seconds. For orgs with >50 webhooks, consider lazy loading badges only for the first visible page.
- The chart should be **responsive** — use `<ResponsiveContainer width="100%" height={200}>` wrapper from Recharts.
