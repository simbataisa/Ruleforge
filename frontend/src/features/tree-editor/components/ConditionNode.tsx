import { memo } from 'react';
import { Handle, Position } from '@xyflow/react';
import type { NodeProps } from '@xyflow/react';
import { GitBranch } from 'lucide-react';
import type { ConditionNodeData } from '@/stores/tree-editor.store';

function formatCondition(nodeData: ConditionNodeData): string {
  const { field, operator, value, secondValue } = nodeData;
  if (!field) return '';
  if (operator === 'IS_NULL' || operator === 'IS_NOT_NULL') return `${field} ${operator}`;
  if (operator === 'BETWEEN' && secondValue) return `${field} BETWEEN ${value} AND ${secondValue}`;
  return `${field} ${operator} ${value}`;
}

function ConditionNodeComponent({ data, selected }: NodeProps) {
  const nodeData = data as unknown as ConditionNodeData;
  const conditionText = formatCondition(nodeData);
  return (
    <div
      className={`rounded-lg border-2 bg-white shadow-sm min-w-[180px] transition-colors ${
        selected
          ? 'border-blue-500 shadow-blue-100'
          : 'border-blue-300 hover:border-blue-400'
      }`}
    >
      <Handle
        type="target"
        position={Position.Top}
        className="!w-3 !h-3 !bg-blue-400 !border-2 !border-white"
      />
      <div className="bg-blue-50 rounded-t-lg px-3 py-1.5 border-b border-blue-200">
        <div className="flex items-center gap-1.5">
          <GitBranch className="h-3.5 w-3.5 text-blue-600" />
          <span className="text-xs font-semibold text-blue-700 uppercase tracking-wide">
            Condition
          </span>
        </div>
      </div>
      <div className="px-3 py-2">
        <p className="text-sm font-medium text-gray-800">{nodeData.label}</p>
        {conditionText && (
          <p className="text-xs text-gray-500 mt-1 font-mono">{conditionText}</p>
        )}
      </div>
      <Handle
        type="source"
        position={Position.Bottom}
        className="!w-3 !h-3 !bg-blue-400 !border-2 !border-white"
      />
    </div>
  );
}

export const ConditionNode = memo(ConditionNodeComponent);
