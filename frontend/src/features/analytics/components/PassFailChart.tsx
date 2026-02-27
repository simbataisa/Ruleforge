import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  Legend,
} from 'recharts';

interface PassFailChartProps {
  passed: number;
  failed: number;
  timelineData?: { time: string; passed: number; failed: number }[];
}

export function PassFailChart({ passed, failed, timelineData }: PassFailChartProps) {
  // If timeline data is available, show stacked bar chart over time
  if (timelineData && timelineData.length > 0) {
    return (
      <ResponsiveContainer width="100%" height={260}>
        <BarChart data={timelineData} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
          <XAxis
            dataKey="time"
            tick={{ fontSize: 12, fill: '#6b7280' }}
            tickLine={false}
            axisLine={{ stroke: '#e5e7eb' }}
          />
          <YAxis
            tick={{ fontSize: 12, fill: '#6b7280' }}
            tickLine={false}
            axisLine={false}
            allowDecimals={false}
          />
          <Tooltip
            contentStyle={{
              fontSize: 13,
              borderRadius: 8,
              border: '1px solid #e5e7eb',
            }}
          />
          <Legend
            verticalAlign="bottom"
            iconType="circle"
            formatter={(value: string) => (
              <span className="text-sm text-gray-600">{value}</span>
            )}
          />
          <Bar dataKey="passed" stackId="status" fill="#22c55e" name="Passed" radius={[0, 0, 0, 0]} />
          <Bar dataKey="failed" stackId="status" fill="#ef4444" name="Failed" radius={[4, 4, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    );
  }

  // Fallback: simple stacked bar
  const data = [
    { name: 'Passed', value: passed, fill: '#22c55e' },
    { name: 'Failed', value: failed, fill: '#ef4444' },
  ];
  const total = passed + failed;

  return (
    <div className="flex flex-col items-center justify-center h-[260px] gap-4">
      <div className="flex h-4 w-full overflow-hidden rounded-full bg-gray-100">
        {total > 0 && (
          <>
            <div
              className="bg-green-500 transition-all"
              style={{ width: `${(passed / total) * 100}%` }}
            />
            <div
              className="bg-red-500 transition-all"
              style={{ width: `${(failed / total) * 100}%` }}
            />
          </>
        )}
      </div>
      <div className="flex gap-6 text-sm">
        {data.map((d) => (
          <div key={d.name} className="flex items-center gap-2">
            <div className="h-3 w-3 rounded-full" style={{ backgroundColor: d.fill }} />
            <span className="text-gray-600">
              {d.name}: {d.value.toLocaleString()}{' '}
              {total > 0 && (
                <span className="text-gray-400">
                  ({((d.value / total) * 100).toFixed(1)}%)
                </span>
              )}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
