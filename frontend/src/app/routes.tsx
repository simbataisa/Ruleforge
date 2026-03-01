import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
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
import { ToastContainer } from '@/shared/components/Toast';

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
const ScheduleDetailPage = lazy(() =>
  import('@/features/scheduler/pages/ScheduleDetailPage').then((m) => ({ default: m.ScheduleDetailPage })),
);
const ConnectorsPage = lazy(() =>
  import('@/features/connectors/pages/ConnectorsPage').then((m) => ({ default: m.ConnectorsPage })),
);
const AbTestListPage = lazy(() =>
  import('@/features/ab-testing/pages/AbTestListPage').then((m) => ({ default: m.AbTestListPage })),
);
const AbTestDetailPage = lazy(() =>
  import('@/features/ab-testing/pages/AbTestDetailPage').then((m) => ({ default: m.AbTestDetailPageRoute })),
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

function Lazy({ children }: { children: React.ReactNode }) {
  return <Suspense fallback={<LazyFallback />}>{children}</Suspense>;
}

export const router = createBrowserRouter([
  /* Public routes */
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },

  /* Protected routes with layout */
  {
    element: (
      <ProtectedRoute>
        <ErrorBoundary>
          <Layout />
          <ToastContainer />
        </ErrorBoundary>
      </ProtectedRoute>
    ),
    children: [
      { path: '/rules', element: <RuleListPage /> },
      { path: '/rules/:id/edit', element: <RuleEditorPage /> },
      { path: '/approvals', element: <Lazy><ApprovalQueuePage /></Lazy> },
      { path: '/api-keys', element: <ApiKeysPage /> },
      { path: '/settings', element: <OrgSettingsPage /> },
      { path: '/analytics', element: <Lazy><AnalyticsDashboardPage /></Lazy> },
      { path: '/analytics/rules/:ruleId', element: <Lazy><RuleStatsPage /></Lazy> },
      { path: '/schedules', element: <Lazy><SchedulerPage /></Lazy> },
      { path: '/schedules/:id', element: <Lazy><ScheduleDetailPage /></Lazy> },
      { path: '/connectors', element: <Lazy><ConnectorsPage /></Lazy> },
      { path: '/ab-tests', element: <Lazy><AbTestListPage /></Lazy> },
      { path: '/ab-tests/:id', element: <Lazy><AbTestDetailPage /></Lazy> },
      { path: '/profile', element: <Lazy><ProfilePage /></Lazy> },
      { path: '/audit', element: <Lazy><AuditLogPage /></Lazy> },
      { path: '/webhooks', element: <Lazy><WebhookListPage /></Lazy> },
      { path: '/tags', element: <Lazy><TagListPage /></Lazy> },
      { path: '/spaces', element: <Lazy><SpaceListPage /></Lazy> },
      { path: '/settings/sso', element: <Lazy><SsoSettingsPage /></Lazy> },
      { path: '/settings/members', element: <Lazy><MemberManagementPage /></Lazy> },
    ],
  },

  /* Default redirect */
  { path: '/', element: <Navigate to="/rules" replace /> },
  { path: '*', element: <Navigate to="/rules" replace /> },
]);
