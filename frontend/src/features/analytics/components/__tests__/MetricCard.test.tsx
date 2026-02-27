import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';
import { MetricCard } from '../MetricCard';

describe('MetricCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders label and value', () => {
    renderWithProviders(
      <MetricCard label="Total Decisions" value="12,345" />,
    );
    expect(screen.getByText('Total Decisions')).toBeInTheDocument();
    expect(screen.getByText('12,345')).toBeInTheDocument();
  });

  it('renders description when provided', () => {
    renderWithProviders(
      <MetricCard label="Error Rate" value="2.3%" description="Last 24h" />,
    );
    expect(screen.getByText('Last 24h')).toBeInTheDocument();
  });

  it('does not render description when omitted', () => {
    renderWithProviders(
      <MetricCard label="Error Rate" value="2.3%" />,
    );
    expect(screen.queryByText('Last 24h')).not.toBeInTheDocument();
  });

  it('renders icon when provided', () => {
    renderWithProviders(
      <MetricCard
        label="Latency"
        value="45ms"
        icon={<span data-testid="test-icon">icon</span>}
      />,
    );
    expect(screen.getByTestId('test-icon')).toBeInTheDocument();
  });

  it('does not render icon container when icon is omitted', () => {
    const { container } = renderWithProviders(
      <MetricCard label="Latency" value="45ms" />,
    );
    // The icon wrapper div has specific classes; when no icon, it should not exist
    const iconWrappers = container.querySelectorAll('.bg-indigo-50');
    expect(iconWrappers).toHaveLength(0);
  });

  it('renders numeric value correctly', () => {
    renderWithProviders(
      <MetricCard label="Count" value={42} />,
    );
    expect(screen.getByText('42')).toBeInTheDocument();
  });

  it('applies custom className', () => {
    const { container } = renderWithProviders(
      <MetricCard label="Test" value="0" className="custom-class" />,
    );
    const card = container.firstElementChild;
    expect(card).toHaveClass('custom-class');
  });

  it('fires onClick handler when clicked (drill-down)', async () => {
    const user = userEvent.setup();
    const onClick = vi.fn();
    renderWithProviders(
      <MetricCard label="Error Rate" value="2.3%" onClick={onClick} />,
    );
    const card = screen.getByRole('button');
    await user.click(card);
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('has role=button and is keyboard accessible when onClick is provided', () => {
    renderWithProviders(
      <MetricCard label="Latency" value="45ms" onClick={vi.fn()} />,
    );
    const card = screen.getByRole('button');
    expect(card).toHaveAttribute('tabindex', '0');
  });

  it('does not have role=button when onClick is not provided', () => {
    renderWithProviders(
      <MetricCard label="Count" value="0" />,
    );
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
