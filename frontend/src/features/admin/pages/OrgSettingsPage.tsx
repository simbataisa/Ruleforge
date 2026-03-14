import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Plus, Mail, Shield } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Badge } from '@/shared/components/Badge';
import { Modal } from '@/shared/components/Modal';
import { Spinner } from '@/shared/components/Spinner';
import { useToastStore } from '@/stores/toast.store';
import { useAuthStore } from '@/stores/auth.store';
import {
  useOrganization,
  useOrgMembers,
  useOrgSpaces,
  useInviteMember,
  useUpdateMemberRole,
  useCreateSpace,
} from '@/api/org.api';
import { SsoSettings } from '../components/SsoSettings';

type SettingsTab = 'general' | 'members' | 'spaces' | 'sso';

const tabs: { key: SettingsTab; label: string }[] = [
  { key: 'general', label: 'General' },
  { key: 'members', label: 'Members' },
  { key: 'spaces', label: 'Spaces' },
  { key: 'sso', label: 'SSO' },
];

const roleColors: Record<string, string> = {
  OWNER: 'bg-red-100 text-red-700',
  ADMIN: 'bg-purple-100 text-purple-700',
  EDITOR: 'bg-blue-100 text-blue-700',
  VIEWER: 'bg-gray-100 text-gray-600',
};

export function OrgSettingsPage() {
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);
  const user = useAuthStore((s) => s.user);

  const [activeTab, setActiveTab] = useState<SettingsTab>('general');
  const [showInvite, setShowInvite] = useState(false);
  const [showCreateSpace, setShowCreateSpace] = useState(false);

  // Invite form state
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('EDITOR');

  // Create space form state
  const [spaceName, setSpaceName] = useState('');

  // API hooks
  const { data: org, isLoading: orgLoading } = useOrganization();
  const { data: members, isLoading: membersLoading } = useOrgMembers();
  const { data: spaces, isLoading: spacesLoading } = useOrgSpaces();
  const inviteMember = useInviteMember();
  const updateMemberRole = useUpdateMemberRole();
  const createSpace = useCreateSpace();

  const handleInvite = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inviteEmail.trim()) return;
    try {
      await inviteMember.mutateAsync({ email: inviteEmail.trim(), role: inviteRole });
      addToast({ type: 'success', title: `Invitation sent to ${inviteEmail}` });
      setInviteEmail('');
      setInviteRole('EDITOR');
      setShowInvite(false);
    } catch {
      addToast({ type: 'error', title: 'Failed to send invitation' });
    }
  };

  const handleRoleChange = async (memberId: string, newRole: string) => {
    try {
      await updateMemberRole.mutateAsync({ memberId, role: newRole });
      addToast({ type: 'success', title: 'Role updated successfully' });
    } catch {
      addToast({ type: 'error', title: 'Failed to update role' });
    }
  };

  const handleCreateSpace = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!spaceName.trim()) return;
    try {
      await createSpace.mutateAsync({ name: spaceName.trim() });
      addToast({ type: 'success', title: `Space "${spaceName}" created` });
      setSpaceName('');
      setShowCreateSpace(false);
    } catch {
      addToast({ type: 'error', title: 'Failed to create space' });
    }
  };

  const isOwnerOrAdmin = user?.role === 'OWNER' || user?.role === 'ADMIN';

  return (
    <div className="flex flex-col h-full">
      <PageHeader title="Organization Settings" />

      {/* Tabs */}
      <div className="border-b border-gray-200 bg-white px-6">
        <nav className="flex gap-6" aria-label="Settings tabs">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key)}
              className={`py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === tab.key
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </nav>
      </div>

      {/* Tab Content */}
      <div className="flex-1 overflow-auto p-6">
        {/* General Tab */}
        {activeTab === 'general' && (
          <div className="max-w-lg space-y-6">
            {orgLoading ? (
              <div className="flex justify-center py-8">
                <Spinner />
              </div>
            ) : (
              <>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Organization Name
                  </label>
                  <Input value={org?.name ?? ''} readOnly className="bg-gray-50" />
                  <p className="text-xs text-gray-400 mt-1">
                    Contact support to change your organization name.
                  </p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Organization Slug
                  </label>
                  <Input value={org?.slug ?? ''} readOnly className="bg-gray-50" />
                  <p className="text-xs text-gray-400 mt-1">
                    This is used in URLs and API endpoints. It cannot be changed.
                  </p>
                </div>
              </>
            )}
          </div>
        )}

        {/* Members Tab */}
        {activeTab === 'members' && (
          <div className="max-w-3xl">
            {membersLoading ? (
              <div className="flex justify-center py-8">
                <Spinner />
              </div>
            ) : (
              <>
                <div className="flex items-center justify-between mb-4">
                  <p className="text-sm text-gray-600">
                    {members?.length ?? 0} member{(members?.length ?? 0) !== 1 ? 's' : ''} in
                    your organization
                  </p>
                  {isOwnerOrAdmin && (
                    <Button size="sm" onClick={() => setShowInvite(true)}>
                      <Plus className="h-4 w-4" />
                      Invite Member
                    </Button>
                  )}
                </div>
                <div className="border border-gray-200 rounded-lg overflow-hidden">
                  <table className="w-full">
                    <thead>
                      <tr className="bg-gray-50 border-b border-gray-200">
                        <th className="px-4 py-2.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Member
                        </th>
                        <th className="px-4 py-2.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Email
                        </th>
                        <th className="px-4 py-2.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                          Role
                        </th>
                        {isOwnerOrAdmin && (
                          <th className="px-4 py-2.5 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                            Actions
                          </th>
                        )}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200">
                      {(members ?? []).map((member) => (
                        <tr key={member.id} className="hover:bg-gray-50">
                          <td className="px-4 py-3">
                            <div className="flex items-center gap-3">
                              <div className="h-8 w-8 rounded-full bg-indigo-100 flex items-center justify-center">
                                <span className="text-xs font-semibold text-indigo-700">
                                  {member.fullName
                                    .split(' ')
                                    .map((n) => n[0])
                                    .join('')
                                    .toUpperCase()}
                                </span>
                              </div>
                              <span className="text-sm font-medium text-gray-900">
                                {member.fullName}
                              </span>
                            </div>
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex items-center gap-1.5 text-sm text-gray-500">
                              <Mail className="h-3.5 w-3.5" />
                              {member.email}
                            </div>
                          </td>
                          <td className="px-4 py-3">
                            <Badge
                              className={
                                roleColors[member.role] ?? 'bg-gray-100 text-gray-600'
                              }
                            >
                              <Shield className="h-3 w-3" />
                              {member.role}
                            </Badge>
                          </td>
                          {isOwnerOrAdmin && (
                            <td className="px-4 py-3">
                              {member.role !== 'OWNER' && (
                                <Select
                                  value={member.role}
                                  onChange={(e) =>
                                    handleRoleChange(member.id, e.target.value)
                                  }
                                  className="text-xs h-7 w-28"
                                >
                                  <option value="ADMIN">Admin</option>
                                  <option value="EDITOR">Editor</option>
                                  <option value="VIEWER">Viewer</option>
                                </Select>
                              )}
                            </td>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            )}
          </div>
        )}

        {/* Spaces Tab */}
        {activeTab === 'spaces' && (
          <div className="max-w-3xl">
            {spacesLoading ? (
              <div className="flex justify-center py-8">
                <Spinner />
              </div>
            ) : (
              <>
                <div className="flex items-center justify-between mb-4">
                  <p className="text-sm text-gray-600">
                    {spaces?.length ?? 0} space{(spaces?.length ?? 0) !== 1 ? 's' : ''} in your
                    organization
                  </p>
                  {isOwnerOrAdmin && (
                    <Button size="sm" onClick={() => setShowCreateSpace(true)}>
                      <Plus className="h-4 w-4" />
                      Create Space
                    </Button>
                  )}
                </div>
                <div className="grid gap-3">
                  {(spaces ?? []).map((space) => (
                    <div
                      key={space.id}
                      className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:border-gray-300 transition-colors"
                    >
                      <div>
                        <h3 className="text-sm font-medium text-gray-900">
                          {space.name}
                        </h3>
                        <p className="text-xs text-gray-500 mt-0.5">
                          Slug: {space.slug}
                        </p>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="text-xs text-gray-500">
                          {space.ruleCount} rule{space.ruleCount !== 1 ? 's' : ''}
                        </span>
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => navigate(`/spaces/${space.id}`)}
                        >
                          Manage
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>
        )}

        {/* SSO Tab */}
        {activeTab === 'sso' && <SsoSettings />}
      </div>

      {/* Invite Member Modal */}
      <Modal
        open={showInvite}
        onClose={() => setShowInvite(false)}
        title="Invite Member"
      >
        <form onSubmit={handleInvite} className="space-y-4">
          <div>
            <label
              htmlFor="invite-email"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Email Address
            </label>
            <Input
              id="invite-email"
              type="email"
              value={inviteEmail}
              onChange={(e) => setInviteEmail(e.target.value)}
              placeholder="colleague@company.com"
              autoFocus
              required
            />
          </div>
          <div>
            <label
              htmlFor="invite-role"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Role
            </label>
            <Select
              id="invite-role"
              value={inviteRole}
              onChange={(e) => setInviteRole(e.target.value)}
            >
              <option value="ADMIN">Admin</option>
              <option value="EDITOR">Editor</option>
              <option value="VIEWER">Viewer</option>
            </Select>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => setShowInvite(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              loading={inviteMember.isPending}
              disabled={!inviteEmail.trim()}
            >
              Send Invitation
            </Button>
          </div>
        </form>
      </Modal>

      {/* Create Space Modal */}
      <Modal
        open={showCreateSpace}
        onClose={() => setShowCreateSpace(false)}
        title="Create Space"
      >
        <form onSubmit={handleCreateSpace} className="space-y-4">
          <div>
            <label
              htmlFor="space-name"
              className="block text-sm font-medium text-gray-700 mb-1"
            >
              Space Name
            </label>
            <Input
              id="space-name"
              value={spaceName}
              onChange={(e) => setSpaceName(e.target.value)}
              placeholder="e.g. Production, Staging"
              autoFocus
              required
            />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <Button
              type="button"
              variant="outline"
              onClick={() => setShowCreateSpace(false)}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              loading={createSpace.isPending}
              disabled={!spaceName.trim()}
            >
              Create Space
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
