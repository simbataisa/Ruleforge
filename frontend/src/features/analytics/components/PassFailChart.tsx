import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Legend,
  Tooltip,
} from 'recharts';

interface PassFailChartProps {
  passed: number;
  failed: number;
}

const COLORS = { passed: '#22c55e', failed: '#ef4444' };

export function PassFailChart({ passed, failed }: PassFailChartProps) {
  const total = passed + failed;
  const data = [
    { name: 'Passed', value: passed },
    { name: 'Failed', value: failed },
  ];

  const renderLabel = (props: { name?: string; value?: number }) => {
    const { name = '', value = 0 } = props;
    if (total === 0) return `${name} 0%`;
    return `${name} ${((value / total) * 100).toFixed(1)}%`;
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
          dataKey="value"
          label={renderLabel}
          labelLine={false}
        >
          <Cell fill={COLORS.passed} />
          <Cell fill={COLORS.failed} />
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
