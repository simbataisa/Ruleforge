import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './Layout';
import { ProtectedRoute } from './ProtectedRoute';
import { ErrorBoundary } from './ErrorBoundary';
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { RegisterPage } from '@/features/auth/pages/RegisterPage';
import { RuleListPage } from '@/features/rules/pages/RuleListPage';
import { RuleEditorPage } from '@/features/rules/pages/RuleEditorPage';
import { ApiKeysPage } from '@/features/api-keys/pages/ApiKeysPage';
import { OrgSettingsPage } from '@/features/admin/pages/OrgSettingsPage';
import { Spinner } from '@/shared/components/Spinner';

const AuditLogPage = lazy(() =>
  import('@/features/audit/pages/AuditLogPage').then((m) => ({ default: m.AuditLogPage })),
);
const WebhookListPage = lazy(() =>
  import('@/features/webhooks/pages/WebhookListPage').then((m) => ({ default: m.WebhookListPage })),
);
const TagListPage = lazy(() =>
  import('@/features/tags/pages/TagListPage').then((m) => ({ default: m.TagListPage })),
);
const SpaceListPage = lazy(() =>
  import('@/features/spaces/pages/SpaceListPage').then((m) => ({ default: m.SpaceListPage })),
);
const SsoSettingsPage = lazy(() =>
  import('@/features/admin/pages/SsoSettingsPage').then((m) => ({ default: m.SsoSettingsPage })),
);
const MemberManagementPage = lazy(() =>
  import('@/features/admin/pages/MemberManagementPage').then((m) => ({
    default: m.MemberManagementPage,
  })),
);
const AnalyticsDashboardPage = lazy(() =>
  import('@/features/analytics/pages/AnalyticsDashboardPage').then((m) => ({ default: m.AnalyticsDashboardPage })),
);
const RuleStatsPage = lazy(() =>
  import('@/features/analytics/pages/RuleStatsPage').then((m) => ({ default: m.RuleStatsPage })),
);
const SchedulerPage = lazy(() =>
  import('@/features/scheduler/pages/SchedulerPage').then((m) => ({ default: m.SchedulerPage })),
);
const ConnectorsPage = lazy(() =>
  import('@/features/connectors/pages/ConnectorsPage').then((m) => ({ default: m.ConnectorsPage })),
);
const AbTestListPage = lazy(() =>
  import('@/features/ab-testing/pages/AbTestListPage').then((m) => ({ default: m.AbTestListPage })),
);
const ProfilePage = lazy(() =>
  import('@/features/admin/pages/ProfilePage').then((m) => ({ default: m.ProfilePage })),
);
const ApprovalQueuePage = lazy(() =>
  import('@/features/rules/pages/ApprovalQueuePage').then((m) => ({ default: m.ApprovalQueuePage })),
);

function LazyFallback() {
  return (
    <div className="flex items-center justify-center h-64">
      <Spinner size="lg" />
    </div>
  );
}

export function AppRoutes() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Protected routes with layout */}
      <Route
        element={
          <ProtectedRoute>
            <ErrorBoundary>
              <Layout />
            </ErrorBoundary>
          </ProtectedRoute>
        }
      >
        <Route path="/rules" element={<RuleListPage />} />
        <Route path="/rules/:id/edit" element={<RuleEditorPage />} />
        <Route path="/approvals" element={<Suspense fallback={<LazyFallback />}><ApprovalQueuePage /></Suspense>} />
        <Route path="/api-keys" element={<ApiKeysPage />} />
        <Route path="/settings" element={<OrgSettingsPage />} />
        <Route path="/analytics" element={<Suspense fallback={<LazyFallback />}><AnalyticsDashboardPage /></Suspense>} />
        <Route path="/analytics/rules/:ruleId" element={<Suspense fallback={<LazyFallback />}><RuleStatsPage /></Suspense>} />
        <Route path="/schedules" element={<Suspense fallback={<LazyFallback />}><SchedulerPage /></Suspense>} />
        <Route path="/connectors" element={<Suspense fallback={<LazyFallback />}><ConnectorsPage /></Suspense>} />
        <Route path="/ab-tests" element={<Suspense fallback={<LazyFallback />}><AbTestListPage /></Suspense>} />
        <Route path="/profile" element={<Suspense fallback={<LazyFallback />}><ProfilePage /></Suspense>} />
        <Route
          path="/audit"
          element={
            <Suspense fallback={<LazyFallback />}>
              <AuditLogPage />
            </Suspense>
          }
        />
        <Route
          path="/webhooks"
          element={
            <Suspense fallback={<LazyFallback />}>
              <WebhookListPage />
            </Suspense>
          }
        />
        <Route
          path="/tags"
          element={
            <Suspense fallback={<LazyFallback />}>
              <TagListPage />
            </Suspense>
          }
        />
        <Route
          path="/spaces"
          element={
            <Suspense fallback={<LazyFallback />}>
              <SpaceListPage />
            </Suspense>
          }
        />
        <Route
          path="/settings/sso"
          element={
            <Suspense fallback={<LazyFallback />}>
              <SsoSettingsPage />
            </Suspense>
          }
        />
        <Route
          path="/settings/members"
          element={
            <Suspense fallback={<LazyFallback />}>
              <MemberManagementPage />
            </Suspense>
          }
        />
      </Route>

      {/* Default redirect */}
      <Route path="/" element={<Navigate to="/rules" replace />} />
      <Route path="*" element={<Navigate to="/rules" replace />} />
    </Routes>
  );
}
