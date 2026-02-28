import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { ApprovalQueuePage } from '../ApprovalQueuePage';
import type { ApprovalRequestDto } from '@/api/rules.api';

const mockRefetch = vi.fn();
const mockAddToast = vi.fn();
const mockApproveMutate = vi.fn();
const mockRejectMutate = vi.fn();
const mockCancelMutate = vi.fn();

function makeRequest(overrides: Partial<ApprovalRequestDto> = {}): ApprovalRequestDto {
  return {
    id: 'req-1',
    ruleId: 'rule-1',
    ruleName: 'Pricing Rule',
    ruleType: 'TABLE',
    version: 3,
    requestedBy: 'user-1',
    requestedByName: 'John Doe',
    requestedAt: '2026-02-25T10:00:00Z',
    note: null,
    status: 'PENDING',
    resolvedBy: null,
    resolvedAt: null,
    deadline: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(), // 24h from now
    assignees: [
      { memberId: 'm1', name: 'Alice Admin', status: 'PENDING', respondedAt: null },
    ],
    ...overrides,
  };
}

const pendingRequests = [
  makeRequest({ id: 'req-1', ruleName: 'Pricing Rule' }),
  makeRequest({
    id: 'req-2',
    ruleName: 'Overdue Rule',
    deadline: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), // 2h ago
  }),
];

let mockUserRole = 'OWNER';
let mockRequests: ApprovalRequestDto[] = pendingRequests;

vi.mock('@/api/rules.api', () => ({
  useApprovalRequests: vi.fn(() => ({
    data: { content: mockRequests },
    isLoading: false,
    refetch: mockRefetch,
  })),
  useApproveVersion: vi.fn(() => ({ mutate: mockApproveMutate, isPending: false })),
  useRejectVersion: vi.fn(() => ({ mutate: mockRejectMutate, isPending: false })),
  useCancelApprovalRequest: vi.fn(() => ({ mutate: mockCancelMutate, isPending: false })),
}));

vi.mock('@/stores/toast.store', () => ({
  useToastStore: vi.fn((sel: (s: { addToast: typeof mockAddToast }) => unknown) =>
    sel({ addToast: mockAddToast }),
  ),
}));

vi.mock('@/stores/auth.store', () => ({
  useAuthStore: vi.fn((sel: (s: { user: { id: string; role: string } }) => unknown) =>
    sel({ user: { id: 'user-1', role: mockUserRole } }),
  ),
}));

// Mock ApprovalDetailPanel since it has its own logic
vi.mock('../../components/ApprovalDetailPanel', () => ({
  ApprovalDetailPanel: ({ rule }: { rule: { name: string } }) => (
    <div data-testid="detail-panel">{rule.name}</div>
  ),
}));

describe('ApprovalQueuePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockUserRole = 'OWNER';
    mockRequests = pendingRequests;
  });

  it('renders the Approvals heading', () => {
    renderWithProviders(<ApprovalQueuePage />);
    expect(screen.getByText('Approvals')).toBeInTheDocument();
  });

  it('shows My Queue tab for OWNER role', () => {
    renderWithProviders(<ApprovalQueuePage />);
    expect(screen.getByText('My Queue')).toBeInTheDocument();
  });

  it('shows My Requests tab for all roles', () => {
    renderWithProviders(<ApprovalQueuePage />);
    expect(screen.getByText('My Requests')).toBeInTheDocument();
  });

  it('shows All Requests tab for OWNER role', () => {
    renderWithProviders(<ApprovalQueuePage />);
    expect(screen.getByText('All Requests')).toBeInTheDocument();
  });

  it('hides My Queue and All Requests tabs for EDITOR role', () => {
    mockUserRole = 'EDITOR';
    renderWithProviders(<ApprovalQueuePage />);
    expect(screen.queryByText('My Queue')).not.toBeInTheDocument();
    expect(screen.queryByText('All Requests')).not.toBeInTheDocument();
    expect(screen.getByText('My Requests')).toBeInTheDocument();
  });

  it('shows All Requests tab for ADMIN role', () => {
    mockUserRole = 'ADMIN';
    renderWithProviders(<ApprovalQueuePage />);
    expect(screen.getByText('All Requests')).toBeInTheDocument();
  });

  it('shows My Queue tab for APPROVER role but not All Requests', () => {
    mockUserRole = 'APPROVER';
    renderWithProviders(<ApprovalQueuePage />);
    expect(screen.getByText('My Queue')).toBeInTheDocument();
    expect(screen.queryByText('All Requests')).not.toBeInTheDocument();
  });

  it('displays pending count in summary', () => {
    renderWithProviders(<ApprovalQueuePage />);
    // The summary bar shows "2 pending" -- find the container with "pending" text
    const pendingSpan = screen.getByText('pending');
    expect(pendingSpan).toBeInTheDocument();
    // The count "2" is inside a sibling span with class "font-semibold"
    const countSpan = pendingSpan.closest('span')!.querySelector('.font-semibold');
    expect(countSpan?.textContent).toBe('2');
  });

  it('shows overdue count when there are overdue requests', () => {
    renderWithProviders(<ApprovalQueuePage />);
    expect(screen.getByText('1')).toBeInTheDocument(); // 1 overdue
    expect(screen.getByText('overdue')).toBeInTheDocument();
  });

  it('renders SLA bar with "Overdue" text for past-deadline requests', () => {
    renderWithProviders(<ApprovalQueuePage />);
    // The overdue request should show "Overdue by Xh"
    expect(screen.getByText(/overdue by/i)).toBeInTheDocument();
  });

  it('shows Approve and Reject buttons on My Queue cards for PENDING requests', () => {
    renderWithProviders(<ApprovalQueuePage />);
    // My Queue is default tab for OWNER, and requests are PENDING
    const approveButtons = screen.getAllByText('Approve');
    const rejectButtons = screen.getAllByText('Reject');
    expect(approveButtons.length).toBeGreaterThanOrEqual(1);
    expect(rejectButtons.length).toBeGreaterThanOrEqual(1);
  });

  it('shows Cancel button on My Requests tab for PENDING requests', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ApprovalQueuePage />);

    // Switch to My Requests tab
    const myRequestsTab = screen.getByText('My Requests');
    await user.click(myRequestsTab);

    const cancelButtons = screen.getAllByRole('button', { name: /cancel/i });
    expect(cancelButtons.length).toBeGreaterThanOrEqual(1);
  });

  it('does not show Cancel button for non-PENDING requests on My Requests', async () => {
    mockRequests = [
      makeRequest({ id: 'req-1', status: 'APPROVED' }),
    ];
    const user = userEvent.setup();
    renderWithProviders(<ApprovalQueuePage />);

    const myRequestsTab = screen.getByText('My Requests');
    await user.click(myRequestsTab);

    // No Cancel button for APPROVED request
    expect(screen.queryByRole('button', { name: /^cancel$/i })).not.toBeInTheDocument();
  });

  it('renders empty state when no requests', async () => {
    mockRequests = [];
    renderWithProviders(<ApprovalQueuePage />);
    expect(screen.getByText('All caught up')).toBeInTheDocument();
  });

  it('shows status filter dropdown', () => {
    renderWithProviders(<ApprovalQueuePage />);
    expect(screen.getByText('All Statuses')).toBeInTheDocument();
  });

  it('renders rule names in the request list', () => {
    renderWithProviders(<ApprovalQueuePage />);
    expect(screen.getByText('Pricing Rule')).toBeInTheDocument();
    expect(screen.getByText('Overdue Rule')).toBeInTheDocument();
  });
});
