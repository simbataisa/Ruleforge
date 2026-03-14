export const CRON_PRESETS = [
  { label: 'Every 5 min', cron: '*/5 * * * *' },
  { label: 'Hourly', cron: '0 * * * *' },
  { label: 'Daily', cron: '0 0 * * *' },
  { label: 'Weekly', cron: '0 9 * * 1' },
  { label: 'Monthly', cron: '0 0 1 * *' },
] as const;

const CRON_MAP: Record<string, string> = {
  '*/5 * * * *': 'Every 5 minutes',
  '*/10 * * * *': 'Every 10 minutes',
  '*/15 * * * *': 'Every 15 minutes',
  '*/30 * * * *': 'Every 30 minutes',
  '0 * * * *': 'Every hour',
  '0 */2 * * *': 'Every 2 hours',
  '0 */6 * * *': 'Every 6 hours',
  '0 0 * * *': 'Daily at midnight',
  '0 9 * * *': 'Daily at 9:00 AM',
  '0 9 * * 1': 'Weekly on Monday at 9 AM',
  '0 0 1 * *': 'Monthly on the 1st',
};

export function cronToHuman(cron: string): string {
  return CRON_MAP[cron] ?? cron;
}

/**
 * Basic cron expression validation.
 * Returns null if valid, or an error message string if invalid.
 */
export function validateCron(cron: string): string | null {
  if (!cron.trim()) return 'Cron expression is required';
  const parts = cron.trim().split(/\s+/);
  if (parts.length < 5 || parts.length > 7) {
    return `Expected 5-7 fields, got ${parts.length}`;
  }
  // Basic range checks for standard 5-field cron
  const ranges = [
    { name: 'minute', min: 0, max: 59 },
    { name: 'hour', min: 0, max: 23 },
    { name: 'day of month', min: 1, max: 31 },
    { name: 'month', min: 1, max: 12 },
    { name: 'day of week', min: 0, max: 7 },
  ];
  for (let i = 0; i < Math.min(parts.length, 5); i++) {
    const part = parts[i];
    if (part === '*' || part === '?') continue;
    // Allow step expressions like */5, ranges like 1-5, lists like 1,3,5
    const match = part.match(/^(\*|\d+)(?:\/(\d+))?$/);
    if (match) {
      const val = match[1] === '*' ? null : parseInt(match[1], 10);
      if (val !== null && (val < ranges[i].min || val > ranges[i].max)) {
        return `${ranges[i].name}: ${val} is out of range (${ranges[i].min}-${ranges[i].max})`;
      }
      continue;
    }
    // Allow range/list patterns without strict validation
    if (/^[\d,\-\*\/\?]+$/.test(part)) continue;
    // Allow named months/days (MON, JAN, etc.)
    if (/^[A-Za-z,\-]+$/.test(part)) continue;
    return `Invalid ${ranges[i].name} field: "${part}"`;
  }
  return null;
}
