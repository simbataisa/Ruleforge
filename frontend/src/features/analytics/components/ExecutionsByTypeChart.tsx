import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
} from 'recharts';

interface TypeCount {
  type: string;
  count: number;
}

interface ExecutionsByTypeChartProps {
  data: TypeCount[];
}

const TYPE_COLORS: Record<string, string> = {
  TABLE: '#3b82f6',
  TREE: '#8b5cf6',
  SCRIPT: '#f97316',
  FLOW: '#06b6d4',
  SCORECARD: '#10b981',
  DMN: '#ec4899',
  CEP: '#eab308',
  ENSEMBLE: '#64748b',
};

const FALLBACK_COLORS = ['#94a3b8', '#a78bfa', '#fbbf24', '#34d399', '#f87171'];

export function ExecutionsByTypeChart({ data }: ExecutionsByTypeChartProps) {
  const total = data.reduce((sum, d) => sum + d.count, 0);

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const renderLabel = (props: any) => {
    const type = props.type ?? props.name ?? '';
    const count = props.count ?? props.value ?? 0;
    if (total === 0) return `${type} 0%`;
    return `${type} ${((count / total) * 100).toFixed(0)}%`;
  };

  return (
    <ResponsiveContainer width="100%" height={260}>
      <PieChart>
        <Pie
          data={data}
          cx="50%"
          cy="50%"
          innerRadius={55}
          outerRadius={85}
          paddingAngle={2}
          dataKey="count"
          nameKey="type"
          label={renderLabel}
          labelLine={false}
        >
          {data.map((entry, idx) => (
            <Cell
              key={entry.type}
              fill={TYPE_COLORS[entry.type] ?? FALLBACK_COLORS[idx % FALLBACK_COLORS.length]}
            />
          ))}
        </Pie>
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
      </PieChart>
    </ResponsiveContainer>
  );
}
