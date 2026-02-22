import { memo } from 'react';
import { Handle, Position } from '@xyflow/react';
import type { NodeProps } from '@xyflow/react';
import { Shield } from 'lucide-react';
import type { GateNodeData } from '@/stores/tree-editor.store';

function GateNodeComponent({ data, selected }: NodeProps) {
  const nodeData = data as unknown as GateNodeData;
  return (
    <div
      className={`rounded-lg border-2 bg-white shadow-sm min-w-[180px] transition-colors ${
        selected
          ? 'border-amber-500 shadow-amber-100'
          : 'border-amber-300 hover:border-amber-400'
      }`}
    >
      <Handle
        type="target"
        position={Position.Top}
        className="!w-3 !h-3 !bg-amber-400 !border-2 !border-white"
      />
      <div className="bg-amber-50 rounded-t-lg px-3 py-1.5 border-b border-amber-200">
        <div className="flex items-center gap-1.5">
          <Shield className="h-3.5 w-3.5 text-amber-600" />
          <span className="text-xs font-semibold text-amber-700 uppercase tracking-wide">
            Gate
          </span>
        </div>
      </div>
      <div className="px-3 py-2">
        <p className="text-sm font-medium text-gray-800">{nodeData.label}</p>
        {nodeData.expression && (
          <p className="text-xs text-gray-500 mt-1 font-mono break-all">
            {nodeData.expression}
          </p>
        )}
      </div>
      <Handle
        type="source"
        position={Position.Bottom}
        className="!w-3 !h-3 !bg-amber-400 !border-2 !border-white"
      />
    </div>
  );
}

export const GateNode = memo(GateNodeComponent);
