import { useState, useMemo } from 'react';

interface TimezoneSelectProps {
  value: string;
  onChange: (tz: string) => void;
  id?: string;
}

const FALLBACK_TIMEZONES = [
  'UTC', 'America/New_York', 'America/Chicago', 'America/Denver', 'America/Los_Angeles',
  'Europe/London', 'Europe/Paris', 'Europe/Berlin', 'Asia/Tokyo', 'Asia/Shanghai',
  'Asia/Singapore', 'Asia/Kolkata', 'Australia/Sydney', 'Pacific/Auckland',
];

export function TimezoneSelect({ value, onChange, id }: TimezoneSelectProps) {
  const [search, setSearch] = useState('');

  const timezones: string[] = useMemo(() => {
    try {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      return (Intl as any).supportedValuesOf('timeZone') as string[];
    } catch {
      return FALLBACK_TIMEZONES;
    }
  }, []);

  const filtered = search
    ? timezones.filter((tz: string) => tz.toLowerCase().includes(search.toLowerCase()))
    : timezones;

  return (
    <div className="relative">
      <input
        id={id}
        type="text"
        value={search || value}
        onChange={(e) => {
          setSearch(e.target.value);
          const exact = timezones.find((tz: string) => tz.toLowerCase() === e.target.value.toLowerCase());
          if (exact) {
            onChange(exact);
            setSearch('');
          }
        }}
        onFocus={() => setSearch(value)}
        onBlur={() => setTimeout(() => setSearch(''), 150)}
        placeholder="Select timezone..."
        className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        list={`${id ?? 'tz'}-list`}
      />
      <datalist id={`${id ?? 'tz'}-list`}>
        {filtered.slice(0, 50).map((tz: string) => (
          <option key={tz} value={tz} />
        ))}
      </datalist>
    </div>
  );
}
