import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { AbTestCard } from '../AbTestCard';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('@/api/analytics.api', () => ({
  useAbTestResults: () => ({ data: undefined, isLoading: false }),
}));

const activeTest = {
  id: 'test-1',
  name: 'Pricing Rule A/B',
  versionA: 3,
  versionB: 4,
  trafficSplit: 70,
  status: 'ACTIVE',
  startDate: '2026-02-20T10:00:00Z',
};

const completedTest = {
  id: 'test-2',
  name: 'Eligibility Check Test',
  versionA: 1,
  versionB: 2,
  trafficSplit: 50,
  status: 'COMPLETED',
  startDate: '2026-01-15T08:00:00Z',
  endDate: '2026-02-15T08:00:00Z',
};

const stoppedTest = {
  id: 'test-3',
  name: 'Stopped Test',
  versionA: 5,
  versionB: 6,
  trafficSplit: 60,
  status: 'STOPPED',
};

describe('AbTestCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders test name and version comparison', () => {
    renderWithProviders(<AbTestCard test={activeTest} />);
    expect(screen.getByText('Pricing Rule A/B')).toBeInTheDocument();
    expect(screen.getByText('v3 vs v4')).toBeInTheDocument();
  });

  it('renders status badge', () => {
    renderWithProviders(<AbTestCard test={activeTest} />);
    expect(screen.getByText('ACTIVE')).toBeInTheDocument();
  });

  it('renders traffic split bar with correct percentages', () => {
    renderWithProviders(<AbTestCard test={activeTest} />);
    expect(screen.getByText('v3 (70%)')).toBeInTheDocument();
    expect(screen.getByText('v4 (30%)')).toBeInTheDocument();
  });

  it('renders 50/50 split correctly', () => {
    renderWithProviders(<AbTestCard test={completedTest} />);
    expect(screen.getByText('v1 (50%)')).toBeInTheDocument();
    expect(screen.getByText('v2 (50%)')).toBeInTheDocument();
  });

  it('renders date range when dates are provided', () => {
    renderWithProviders(<AbTestCard test={activeTest} />);
    // startDate is Feb 20, 2026
    expect(screen.getByText('Feb 20, 2026')).toBeInTheDocument();
    // No endDate => "No end date"
    expect(screen.getByText('No end date')).toBeInTheDocument();
  });

  it('renders both start and end dates', () => {
    renderWithProviders(<AbTestCard test={completedTest} />);
    expect(screen.getByText('Jan 15, 2026')).toBeInTheDocument();
    expect(screen.getByText('Feb 15, 2026')).toBeInTheDocument();
  });

  it('shows Stop Test button only for ACTIVE tests', () => {
    const onStop = vi.fn();
    renderWithProviders(<AbTestCard test={activeTest} onStop={onStop} />);
    expect(screen.getByRole('button', { name: /stop test/i })).toBeInTheDocument();
  });

  it('does not show Stop Test button for COMPLETED tests', () => {
    const onStop = vi.fn();
    renderWithProviders(<AbTestCard test={completedTest} onStop={onStop} />);
    expect(screen.queryByRole('button', { name: /stop test/i })).not.toBeInTheDocument();
  });

  it('does not show Stop Test button for STOPPED tests', () => {
    const onStop = vi.fn();
    renderWithProviders(<AbTestCard test={stoppedTest} onStop={onStop} />);
    expect(screen.queryByRole('button', { name: /stop test/i })).not.toBeInTheDocument();
  });

  it('calls onStop with test ID when Stop Test is clicked', async () => {
    const user = userEvent.setup();
    const onStop = vi.fn();
    renderWithProviders(<AbTestCard test={activeTest} onStop={onStop} />);

    await user.click(screen.getByRole('button', { name: /stop test/i }));
    expect(onStop).toHaveBeenCalledTimes(1);
    expect(onStop).toHaveBeenCalledWith('test-1');
  });

  it('renders View Results button and navigates to detail page on click', async () => {
    const user = userEvent.setup();
    renderWithProviders(<AbTestCard test={activeTest} />);

    const btn = screen.getByRole('button', { name: /view results/i });
    expect(btn).toBeInTheDocument();
    await user.click(btn);
    expect(mockNavigate).toHaveBeenCalledTimes(1);
    expect(mockNavigate).toHaveBeenCalledWith('/ab-tests/test-1');
  });

  it('always renders View Results button and hides Stop when no onStop handler', () => {
    renderWithProviders(<AbTestCard test={activeTest} />);
    expect(screen.queryByRole('button', { name: /stop test/i })).not.toBeInTheDocument();
    // View Results is always rendered (uses navigate, not callback)
    expect(screen.getByRole('button', { name: /view results/i })).toBeInTheDocument();
  });
});
