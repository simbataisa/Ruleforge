# Story 004 — Space Detail Page

**Epic:** Spaces Page Features
**Phase:** Frontend
**Assignee:** Frontend Engineer
**Priority:** P0
**Depends on:** Story 001, Story 002 (GET /spaces/:id)

---

## User Story

As a rule author,
I want to open a dedicated Space Detail page by clicking a space,
So that I have a workspace landing page with stats and navigation to rules.

---

## Acceptance Criteria

1. Route `/spaces/:id` renders `SpaceDetailPage`
2. Page shows breadcrumb: "← Spaces" (link) + "/ {space name}"
3. Page title shows space name; description shown below (hidden if null)
4. Stats bar shows: rule count (prominent), created date, updated date
5. "Space Settings" button top-right → navigates to `/spaces/:id/settings`
6. Tab bar: "Overview" (default) | "Rules" (placeholder or link)
7. Overview tab shows a summary card with rule count and "Browse Rules →" button
8. Loading state: skeleton placeholders
9. 404 state: "Space not found" message + "Back to Spaces" link
10. Error state: error banner + retry button

---

## Technical Implementation

### New File: `frontend/src/features/spaces/pages/SpaceDetailPage.tsx`

```typescript
import { useParams, Link, useNavigate } from 'react-router-dom';
import { Settings } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Spinner } from '@/shared/components/Spinner';
import { useSpace } from '@/api/org.api';

export function SpaceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: space, isLoading, isError } = useSpace(id!);

  if (isLoading) {
    return <SpaceDetailSkeleton />;
  }

  if (isError || !space) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500 mb-4">Space not found.</p>
        <Button variant="outline" onClick={() => navigate('/spaces')}>
          Back to Spaces
        </Button>
      </div>
    );
  }

  return (
    <div>
      {/* Breadcrumb */}
      <nav className="text-sm text-gray-500 mb-4">
        <Link to="/spaces" className="hover:text-gray-700">Spaces</Link>
        <span className="mx-2">/</span>
        <span className="text-gray-900">{space.name}</span>
      </nav>

      {/* Header */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{space.name}</h1>
          {space.description && (
            <p className="text-gray-500 mt-1">{space.description}</p>
          )}
        </div>
        <Button variant="outline" onClick={() => navigate(`/spaces/${id}/settings`)}>
          <Settings className="h-4 w-4 mr-2" />
          Settings
        </Button>
      </div>

      {/* Stats Bar */}
      <dl className="grid grid-cols-3 gap-4 mb-8">
        <div className="bg-white border border-gray-200 rounded-lg p-4">
          <dt className="text-sm text-gray-500">Rules</dt>
          <dd className="text-3xl font-bold text-indigo-600 mt-1">{space.ruleCount}</dd>
        </div>
        <div className="bg-white border border-gray-200 rounded-lg p-4">
          <dt className="text-sm text-gray-500">Created</dt>
          <dd className="text-lg font-medium text-gray-900 mt-1">
            {new Date(space.createdAt).toLocaleDateString()}
          </dd>
        </div>
        <div className="bg-white border border-gray-200 rounded-lg p-4">
          <dt className="text-sm text-gray-500">Last Updated</dt>
          <dd className="text-lg font-medium text-gray-900 mt-1">
            {new Date(space.updatedAt).toLocaleDateString()}
          </dd>
        </div>
      </dl>

      {/* Tabs */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="-mb-px flex space-x-8">
          <span className="border-b-2 border-indigo-600 pb-3 text-sm font-medium text-indigo-600">
            Overview
          </span>
        </nav>
      </div>

      {/* Overview Content */}
      <div className="bg-white border border-gray-200 rounded-lg p-6">
        <p className="text-gray-600 mb-4">
          This space is active and contains <strong>{space.ruleCount}</strong> rule{space.ruleCount !== 1 ? 's' : ''}.
        </p>
        <Button variant="outline" onClick={() => navigate('/rules')}>
          Browse Rules →
        </Button>
      </div>
    </div>
  );
}

function SpaceDetailSkeleton() {
  return (
    <div className="animate-pulse">
      <div className="h-4 bg-gray-200 rounded w-32 mb-4" />
      <div className="h-8 bg-gray-200 rounded w-64 mb-2" />
      <div className="h-4 bg-gray-200 rounded w-96 mb-6" />
      <div className="grid grid-cols-3 gap-4 mb-8">
        {[1, 2, 3].map(i => (
          <div key={i} className="h-20 bg-gray-200 rounded-lg" />
        ))}
      </div>
      <div className="h-32 bg-gray-200 rounded-lg" />
    </div>
  );
}
```

### Modified: `frontend/src/app/routes.tsx`

Add lazy import:
```typescript
const SpaceDetailPage = lazy(() =>
  import('@/features/spaces/pages/SpaceDetailPage').then((m) => ({ default: m.SpaceDetailPage })),
);
```

Add route in children array:
```typescript
{ path: '/spaces/:id', element: <Lazy><SpaceDetailPage /></Lazy> },
```

Place before the existing `/spaces` route so `:id` doesn't conflict.

---

## Definition of Done

- [ ] `/spaces/:id` route registered in `routes.tsx`
- [ ] `SpaceDetailPage` component created
- [ ] Breadcrumb renders with working link to `/spaces`
- [ ] Page title shows space name
- [ ] Description shown when present, hidden when null
- [ ] Stats bar shows rule count, created date, updated date
- [ ] Settings button navigates to `/spaces/:id/settings`
- [ ] Skeleton loading state shown during fetch
- [ ] 404/error state renders with "Back to Spaces" link
- [ ] No TypeScript errors

---

## Dependencies

- Story 001 (enriched SpaceDto)
- Story 002 (GET /spaces/:id endpoint)
- `useSpace` hook from Story 003

**Estimated Complexity:** Simple (1.5 days)
