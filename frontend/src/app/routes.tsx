import { Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './Layout';
import { ProtectedRoute } from './ProtectedRoute';
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { RegisterPage } from '@/features/auth/pages/RegisterPage';
import { RuleListPage } from '@/features/rules/pages/RuleListPage';
import { RuleEditorPage } from '@/features/rules/pages/RuleEditorPage';
import { ApiKeysPage } from '@/features/api-keys/pages/ApiKeysPage';
import { OrgSettingsPage } from '@/features/admin/pages/OrgSettingsPage';

// Stub pages for routes that are not yet implemented
const DashboardPage = () => <div className="p-8">Analytics Dashboard - Coming Soon</div>;

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
        <Route path="/analytics" element={<DashboardPage />} />
      </Route>

      {/* Default redirect */}
      <Route path="/" element={<Navigate to="/rules" replace />} />
      <Route path="*" element={<Navigate to="/rules" replace />} />
    </Routes>
  );
}
