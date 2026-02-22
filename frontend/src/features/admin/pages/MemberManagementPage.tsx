import { useState } from 'react';
import { Plus } from 'lucide-react';
import { PageHeader } from '@/shared/components/PageHeader';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Badge } from '@/shared/components/Badge';
import { DataTable } from '@/shared/components/DataTable';
import { Spinner } from '@/shared/components/Spinner';
import { EmptyState } from '@/shared/components/EmptyState';
import { Modal } from '@/shared/components/Modal';
import {
  useOrgMembers,
  useInviteMember,
  useUpdateMemberRole,
  type OrgMember,
} from '@/api/org.api';
import { useToastStore } from '@/stores/toast.store';

const ROLES = ['OWNER', 'EDITOR', 'VIEWER'];

const roleColors: Record<string, string> = {
  OWNER: 'bg-red-100 text-red-800',
  EDITOR: 'bg-blue-100 text-blue-800',
  VIEWER: 'bg-gray-100 text-gray-700',
};

export function MemberManagementPage() {
  const addToast = useToastStore((s) => s.addToast);
  const { data: members, isLoading } = useOrgMembers();
  const inviteMember = useInviteMember();
  const updateRole = useUpdateMemberRole();

  const [showInvite, setShowInvite] = useState(false);
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState('VIEWER');

  const handleInvite = () => {
    const email = inviteEmail.trim();
    if (!email) return;
    inviteMember.mutate(
      { email, role: inviteRole },
      {
        onSuccess: () => {
          setShowInvite(false);
          setInviteEmail('');
          setInviteRole('VIEWER');
          addToast({ type: 'success', title: 'Member invited' });
        },
        onError: () => addToast({ type: 'error', title: 'Failed to invite member' }),
      },
    );
  };

  const handleRoleChange = (memberId: string, role: string) => {
    updateRole.mutate(
      { memberId, role },
      {
        onSuccess: () => addToast({ type: 'success', title: 'Role updated' }),
        onError: () => addToast({ type: 'error', title: 'Failed to update role' }),
      },
    );
  };

  const columns = [
    {
      key: 'fullName',
      header: 'Name',
      render: (m: OrgMember) => (
        <span className="font-medium text-gray-900">{m.fullName || '-'}</span>
      ),
    },
    {
      key: 'email',
      header: 'Email',
      render: (m: OrgMember) => (
        <span className="text-gray-600">{m.email}</span>
      ),
    },
    {
      key: 'role',
      header: 'Role',
      render: (m: OrgMember) => (
        <div className="flex items-center gap-2">
          <Badge className={roleColors[m.role] ?? 'bg-gray-100 text-gray-700'}>{m.role}</Badge>
          <Select
            value={m.role}
            onChange={(e) => handleRoleChange(m.id, e.target.value)}
            className="text-xs w-28"
          >
            {ROLES.map((r) => (
              <option key={r} value={r}>{r}</option>
            ))}
          </Select>
        </div>
      ),
    },
  ];

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Spinner size="lg" />
      </div>
    );
  }

  const memberList = members ?? [];

  return (
    <div>
      <PageHeader
        title="Members"
        description="Manage organization members and their roles."
        actions={
          <Button onClick={() => setShowInvite(true)}>
            <Plus className="h-4 w-4" />
            Invite Member
          </Button>
        }
      />

      {memberList.length === 0 ? (
        <EmptyState
          title="No members found"
          description="Invite team members to collaborate on rules."
        />
      ) : (
        <DataTable data={memberList} columns={columns} />
      )}

      <Modal
        open={showInvite}
        onClose={() => setShowInvite(false)}
        title="Invite Member"
        footer={
          <>
            <Button variant="outline" onClick={() => setShowInvite(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleInvite}
              disabled={!inviteEmail.trim() || inviteMember.isPending}
            >
              Send Invite
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
            <Input
              type="email"
              value={inviteEmail}
              onChange={(e) => setInviteEmail(e.target.value)}
              placeholder="colleague@example.com"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Role</label>
            <Select value={inviteRole} onChange={(e) => setInviteRole(e.target.value)}>
              {ROLES.map((r) => (
                <option key={r} value={r}>{r}</option>
              ))}
            </Select>
          </div>
        </div>
      </Modal>
    </div>
  );
}
