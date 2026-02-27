import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/test/test-utils';

/**
 * Tests for AbTestDetailPage acceptance criteria (US-3.3, US-3.4).
 * Uses the real exported component which accepts { results, onPromoteWinner } props.
 */

import { AbTestDetailPage } from '../AbTestDetailPage';
import type { AbTestResults } from '../AbTestDetailPage';

// ------------------------------------------------------------------
// Test data fixtures
// ------------------------------------------------------------------

const significantResults: AbTestResults = {
  testId: 'test-1',
  testName: 'Pricing Rule A/B Test',
  status: 'COMPLETED',
  significance: 97.2,
  significanceThreshold: 95,
  sufficientSamples: true,
  winner: 'Variant A',
  variants: [
    { variant: 'Variant A', executionCount: 5200, avgLatency: 42, errorRate: 0.012 },
    { variant: 'Variant B', executionCount: 4800, avgLatency: 58, errorRate: 0.034 },
  ],
};

const insufficientResults: AbTestResults = {
  testId: 'test-2',
  testName: 'Eligibility Test',
  status: 'ACTIVE',
  significance: 62.0,
  significanceThreshold: 95,
  sufficientSamples: false,
  winner: null,
  variants: [
    { variant: 'Variant A', executionCount: 320, avgLatency: 50, errorRate: 0.02 },
    { variant: 'Variant B', executionCount: 280, avgLatency: 55, errorRate: 0.03 },
  ],
};

const nearThresholdResults: AbTestResults = {
  testId: 'test-3',
  testName: 'Near Threshold Test',
  status: 'ACTIVE',
  significance: 93.0,
  significanceThreshold: 95,
  sufficientSamples: true,
  winner: 'Variant A',
  variants: [
    { variant: 'Variant A', executionCount: 4500, avgLatency: 44, errorRate: 0.015 },
    { variant: 'Variant B', executionCount: 4500, avgLatency: 52, errorRate: 0.028 },
  ],
};

// ------------------------------------------------------------------
// Tests
// ------------------------------------------------------------------

describe('AbTestDetailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('significance meter', () => {
    it('shows correct significance percentage toward threshold', () => {
      renderWithProviders(
        <AbTestDetailPage results={significantResults} onPromoteWinner={vi.fn()} />,
      );
      expect(screen.getByTestId('significance-value')).toHaveTextContent('97.2%');
      expect(screen.getByTestId('significance-threshold')).toHaveTextContent('/ 95%');
    });

    it('renders progress bar with correct aria value', () => {
      renderWithProviders(
        <AbTestDetailPage results={insufficientResults} onPromoteWinner={vi.fn()} />,
      );
      const progressBar = screen.getByRole('progressbar');
      expect(progressBar).toHaveAttribute('aria-valuenow', '62');
    });
  });

  describe('Promote Winner button', () => {
    it('is enabled when significance >= threshold', () => {
      renderWithProviders(
        <AbTestDetailPage results={significantResults} onPromoteWinner={vi.fn()} />,
      );
      const btn = screen.getByTestId('promote-winner-btn');
      expect(btn).toBeEnabled();
      expect(btn).toHaveTextContent('Promote Winner (Variant A)');
    });

    it('is disabled when significance < threshold', () => {
      renderWithProviders(
        <AbTestDetailPage results={nearThresholdResults} onPromoteWinner={vi.fn()} />,
      );
      const btn = screen.getByTestId('promote-winner-btn');
      expect(btn).toBeDisabled();
    });

    it('is not rendered when there is no winner', () => {
      renderWithProviders(
        <AbTestDetailPage results={insufficientResults} onPromoteWinner={vi.fn()} />,
      );
      expect(screen.queryByTestId('promote-winner-btn')).not.toBeInTheDocument();
    });

    it('calls onPromoteWinner with winning variant when clicked', async () => {
      const user = userEvent.setup();
      const onPromote = vi.fn();
      renderWithProviders(
        <AbTestDetailPage results={significantResults} onPromoteWinner={onPromote} />,
      );

      await user.click(screen.getByTestId('promote-winner-btn'));
      expect(onPromote).toHaveBeenCalledTimes(1);
      expect(onPromote).toHaveBeenCalledWith('Variant A');
    });
  });

  describe('metric comparison table', () => {
    it('displays per-variant execution counts', () => {
      renderWithProviders(
        <AbTestDetailPage results={significantResults} onPromoteWinner={vi.fn()} />,
      );
      expect(screen.getByText('5,200')).toBeInTheDocument();
      expect(screen.getByText('4,800')).toBeInTheDocument();
    });

    it('displays per-variant latency', () => {
      renderWithProviders(
        <AbTestDetailPage results={significantResults} onPromoteWinner={vi.fn()} />,
      );
      expect(screen.getByText('42ms')).toBeInTheDocument();
      expect(screen.getByText('58ms')).toBeInTheDocument();
    });

    it('displays per-variant error rates as percentages', () => {
      renderWithProviders(
        <AbTestDetailPage results={significantResults} onPromoteWinner={vi.fn()} />,
      );
      expect(screen.getByText('1.20%')).toBeInTheDocument();
      expect(screen.getByText('3.40%')).toBeInTheDocument();
    });

    it('shows winner indicators for each metric', () => {
      renderWithProviders(
        <AbTestDetailPage results={significantResults} onPromoteWinner={vi.fn()} />,
      );
      // Variant A has lower latency and lower error rate, so it wins those
      expect(screen.getByTestId('winner-latency')).toHaveTextContent('Variant A');
      expect(screen.getByTestId('winner-error-rate')).toHaveTextContent('Variant A');
    });

    it('shows "--" for winner indicators when no winner declared', () => {
      renderWithProviders(
        <AbTestDetailPage results={insufficientResults} onPromoteWinner={vi.fn()} />,
      );
      expect(screen.getByTestId('winner-latency')).toHaveTextContent('--');
      expect(screen.getByTestId('winner-error-rate')).toHaveTextContent('--');
    });
  });

  describe('insufficient samples', () => {
    it('shows insufficient samples message when sufficientSamples is false', () => {
      renderWithProviders(
        <AbTestDetailPage results={insufficientResults} onPromoteWinner={vi.fn()} />,
      );
      expect(screen.getByTestId('insufficient-samples')).toHaveTextContent(
        'Insufficient samples for statistical significance',
      );
    });

    it('does not show insufficient samples message when samples are sufficient', () => {
      renderWithProviders(
        <AbTestDetailPage results={significantResults} onPromoteWinner={vi.fn()} />,
      );
      expect(screen.queryByTestId('insufficient-samples')).not.toBeInTheDocument();
    });
  });

  describe('page rendering', () => {
    it('renders test name as heading', () => {
      renderWithProviders(
        <AbTestDetailPage results={significantResults} onPromoteWinner={vi.fn()} />,
      );
      expect(screen.getByText('Pricing Rule A/B Test')).toBeInTheDocument();
    });

    it('renders test status', () => {
      renderWithProviders(
        <AbTestDetailPage results={significantResults} onPromoteWinner={vi.fn()} />,
      );
      expect(screen.getByTestId('test-status')).toHaveTextContent('COMPLETED');
    });
  });
});
