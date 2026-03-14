import type { BadgeColor } from './types';

export const RULE_TYPE_COLORS: Record<string, { bg: string; text: string }> = {
  TABLE: { bg: 'bg-blue-100', text: 'text-blue-700' },
  TREE: { bg: 'bg-emerald-100', text: 'text-emerald-700' },
  SCRIPT: { bg: 'bg-orange-100', text: 'text-orange-700' },
  FLOW: { bg: 'bg-violet-100', text: 'text-violet-700' },
  SCORECARD: { bg: 'bg-teal-100', text: 'text-teal-700' },
  DMN: { bg: 'bg-indigo-100', text: 'text-indigo-700' },
  ENSEMBLE: { bg: 'bg-rose-100', text: 'text-rose-700' },
  CEP: { bg: 'bg-amber-100', text: 'text-amber-700' },
};

export const MARKET_FLAGS: Record<string, string> = {
  US: '\u{1F1FA}\u{1F1F8}', UK: '\u{1F1EC}\u{1F1E7}', SG: '\u{1F1F8}\u{1F1EC}', AU: '\u{1F1E6}\u{1F1FA}',
  JP: '\u{1F1EF}\u{1F1F5}', DE: '\u{1F1E9}\u{1F1EA}', IN: '\u{1F1EE}\u{1F1F3}', GLOBAL: '\u{1F30C}',
  GB: '\u{1F1EC}\u{1F1E7}', HK: '\u{1F1ED}\u{1F1F0}', FR: '\u{1F1EB}\u{1F1F7}', CN: '\u{1F1E8}\u{1F1F3}',
  KR: '\u{1F1F0}\u{1F1F7}', CA: '\u{1F1E8}\u{1F1E6}', BR: '\u{1F1E7}\u{1F1F7}',
};

export function statusColor(status: string): BadgeColor {
  switch (status) {
    case 'ACTIVE': return 'green';
    case 'PAUSED': return 'yellow';
    case 'COMPLETED': return 'blue';
    case 'DRAFT': return 'gray';
    case 'FAILED': return 'red';
    default: return 'gray';
  }
}

export function runStatusColor(status: string): BadgeColor {
  switch (status) {
    case 'SUCCESS': return 'green';
    case 'FAILED': return 'red';
    case 'RUNNING': return 'blue';
    case 'CANCELLED': return 'gray';
    default: return 'gray';
  }
}

export function formatDuration(startedAt: string, completedAt?: string | null): string {
  if (!completedAt || !startedAt) return '-';
  const ms = new Date(completedAt).getTime() - new Date(startedAt).getTime();
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

export function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}
