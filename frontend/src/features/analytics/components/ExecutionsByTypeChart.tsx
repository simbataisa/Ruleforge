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

export function ExecutionsByTypeChart({ data }: ExecutionsByTypeChartProps) {
  return (
    <ResponsiveContainer width="100%" height={260}>
      <BarChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" vertical={false} />
        <XAxis
          dataKey="type"
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
        <Bar dataKey="count" name="Executions" radius={[4, 4, 0, 0]}>
          {data.map((entry) => (
            <Cell
              key={entry.type}
              fill={TYPE_COLORS[entry.type] ?? '#94a3b8'}
            />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
