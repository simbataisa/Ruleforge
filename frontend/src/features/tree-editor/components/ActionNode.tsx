import { memo } from 'react';
import { Handle, Position } from '@xyflow/react';
import type { NodeProps } from '@xyflow/react';
import { Zap, Globe, FileCode, Terminal, MessageSquare } from 'lucide-react';
import type { ActionNodeData, CallableActionProtocol } from '@/stores/tree-editor.store';

const protocolIcons: Record<CallableActionProtocol, typeof Globe> = {
  http: Globe,
  rule: FileCode,
  script: Terminal,
  log: MessageSquare,
};

const protocolLabels: Record<CallableActionProtocol, string> = {
  http: 'HTTP',
  rule: 'Rule',
  script: 'Script',
  log: 'Log',
};

function ActionNodeComponent({ data, selected }: NodeProps) {
  const nodeData = data as unknown as ActionNodeData;
  const outputs = nodeData.outputs ?? {};
  const outputEntries = Object.entries(outputs);
  const callableActions = nodeData.callableActions ?? [];

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
        {callableActions.length > 0 && (
          <div className="mt-2 pt-2 border-t border-indigo-100">
            <p className="text-[10px] font-semibold text-indigo-500 uppercase tracking-wide mb-1">
              Actions
            </p>
            <div className="flex flex-wrap gap-1">
              {callableActions.map((action) => {
                const Icon = protocolIcons[action.protocol] ?? Globe;
                return (
                  <span
                    key={action.id}
                    className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-indigo-50 border border-indigo-200 text-[10px] text-indigo-700"
                    title={`${protocolLabels[action.protocol]}: ${action.target}`}
                  >
                    <Icon className="h-2.5 w-2.5" />
                    {protocolLabels[action.protocol]}
                  </span>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export const ActionNode = memo(ActionNodeComponent);
