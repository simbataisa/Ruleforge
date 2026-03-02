import { Handle, Position, type NodeProps, type Node } from '@xyflow/react';
import { cn } from '@/shared/utils/cn';

const RULE_TYPE_COLORS: Record<string, { bg: string; text: string }> = {
  TABLE: { bg: 'bg-blue-100', text: 'text-blue-700' },
  TREE: { bg: 'bg-emerald-100', text: 'text-emerald-700' },
  SCRIPT: { bg: 'bg-orange-100', text: 'text-orange-700' },
  FLOW: { bg: 'bg-violet-100', text: 'text-violet-700' },
  SCORECARD: { bg: 'bg-teal-100', text: 'text-teal-700' },
  DMN: { bg: 'bg-indigo-100', text: 'text-indigo-700' },
  ENSEMBLE: { bg: 'bg-rose-100', text: 'text-rose-700' },
  CEP: { bg: 'bg-amber-100', text: 'text-amber-700' },
};

type RuleNodeData = {
  label: string;
  ruleId?: string;
  ruleName?: string;
  ruleType?: string;
  config?: Record<string, unknown>;
};

type RuleNodeType = Node<RuleNodeData, 'rule'>;

export function RuleNode({ data, selected }: NodeProps<RuleNodeType>) {
  const colors = data.ruleType
    ? RULE_TYPE_COLORS[data.ruleType] ?? { bg: 'bg-gray-100', text: 'text-gray-600' }
    : { bg: 'bg-gray-100', text: 'text-gray-600' };

  return (
    <div className={cn(
      "rounded-lg border border-gray-200 bg-white shadow-md min-w-[180px] cursor-pointer hover:shadow-lg transition-shadow",
      selected && "ring-2 ring-indigo-500 ring-offset-2"
    )}>
      <Handle type="target" position={Position.Top} className="!bg-blue-500" />
      <div className="bg-blue-50 border-b border-blue-100 px-3 py-1.5 rounded-t-lg">
        <span className="text-xs font-semibold text-blue-700">Rule</span>
      </div>
      <div className="px-3 py-2 space-y-1">
        <p className="text-sm font-medium text-gray-900 truncate">{data.ruleName ?? data.label}</p>
        {data.ruleType && (
          <span
            className={cn(
              'inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold',
              colors.bg,
              colors.text,
            )}
          >
            {data.ruleType}
          </span>
        )}
      </div>
      <Handle type="source" position={Position.Bottom} className="!bg-blue-500" />
    </div>
  );
}
