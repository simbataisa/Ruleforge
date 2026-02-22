import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  Cell,
} from 'recharts';

interface LatencyBucket {
  percentile: string;
  ms: number;
}

interface LatencyChartProps {
  data: LatencyBucket[];
}

const PERCENTILE_COLORS: Record<string, string> = {
  p50: '#6366f1',
  p75: '#8b5cf6',
  p90: '#a855f7',
  p95: '#d946ef',
  p99: '#ec4899',
};

export function LatencyChart({ data }: LatencyChartProps) {
  return (
    <ResponsiveContainer width="100%" height={260}>
      <BarChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
        <XAxis
          dataKey="percentile"
          tick={{ fontSize: 12, fill: '#6b7280' }}
          tickLine={false}
          axisLine={{ stroke: '#e5e7eb' }}
        />
        <YAxis
          tick={{ fontSize: 12, fill: '#6b7280' }}
          tickLine={false}
          axisLine={false}
          unit=" ms"
        />
        <Tooltip
          contentStyle={{
            fontSize: 13,
            borderRadius: 8,
            border: '1px solid #e5e7eb',
          }}
          formatter={(value) => [`${value} ms`, 'Latency']}
        />
        <Bar dataKey="ms" name="Latency" radius={[4, 4, 0, 0]}>
          {data.map((entry) => (
            <Cell
              key={entry.percentile}
              fill={PERCENTILE_COLORS[entry.percentile] ?? '#94a3b8'}
            />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
