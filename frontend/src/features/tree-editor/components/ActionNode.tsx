import { memo } from 'react';
import { Handle, Position } from '@xyflow/react';
import type { NodeProps } from '@xyflow/react';
import { Zap } from 'lucide-react';
import type { ActionNodeData } from '@/stores/tree-editor.store';

function ActionNodeComponent({ data, selected }: NodeProps) {
  const nodeData = data as unknown as ActionNodeData;
  const outputs = nodeData.outputs ?? {};
  const outputEntries = Object.entries(outputs);

  return (
    <div
      className={`rounded-lg border-2 bg-white shadow-sm min-w-[180px] transition-colors ${
        selected
          ? 'border-green-500 shadow-green-100'
          : 'border-green-300 hover:border-green-400'
      }`}
    >
      <Handle
        type="target"
        position={Position.Top}
        className="!w-3 !h-3 !bg-green-400 !border-2 !border-white"
      />
      <div className="bg-green-50 rounded-t-lg px-3 py-1.5 border-b border-green-200">
        <div className="flex items-center gap-1.5">
          <Zap className="h-3.5 w-3.5 text-green-600" />
          <span className="text-xs font-semibold text-green-700 uppercase tracking-wide">
            Action
          </span>
        </div>
      </div>
      <div className="px-3 py-2">
        <p className="text-sm font-medium text-gray-800">{nodeData.label}</p>
        {outputEntries.length > 0 && (
          <div className="mt-1 space-y-0.5">
            {outputEntries.map(([key, value]) => (
              <p key={key} className="text-xs text-gray-500">
                <span className="font-medium">{key}:</span> {value}
              </p>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export const ActionNode = memo(ActionNodeComponent);
