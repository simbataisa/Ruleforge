import { Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './Layout';
import { ProtectedRoute } from './ProtectedRoute';
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { RegisterPage } from '@/features/auth/pages/RegisterPage';
import { RuleListPage } from '@/features/rules/pages/RuleListPage';
import { RuleEditorPage } from '@/features/rules/pages/RuleEditorPage';
import { ApiKeysPage } from '@/features/api-keys/pages/ApiKeysPage';
import { OrgSettingsPage } from '@/features/admin/pages/OrgSettingsPage';
import { AnalyticsDashboardPage } from '@/features/analytics/pages/AnalyticsDashboardPage';
import { RuleStatsPage } from '@/features/analytics/pages/RuleStatsPage';
import { SchedulerPage } from '@/features/scheduler/pages/SchedulerPage';
import { ConnectorsPage } from '@/features/connectors/pages/ConnectorsPage';
import { AbTestListPage } from '@/features/ab-testing/pages/AbTestListPage';

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
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/rules" element={<RuleListPage />} />
        <Route path="/rules/:id/edit" element={<RuleEditorPage />} />
        <Route path="/api-keys" element={<ApiKeysPage />} />
        <Route path="/settings" element={<OrgSettingsPage />} />
        <Route path="/analytics" element={<AnalyticsDashboardPage />} />
        <Route path="/analytics/rules/:ruleId" element={<RuleStatsPage />} />
        <Route path="/schedules" element={<SchedulerPage />} />
        <Route path="/connectors" element={<ConnectorsPage />} />
        <Route path="/ab-tests" element={<AbTestListPage />} />
      </Route>

      {/* Default redirect */}
      <Route path="/" element={<Navigate to="/rules" replace />} />
      <Route path="*" element={<Navigate to="/rules" replace />} />
    </Routes>
  );
}
