import { Clock, FileText, GitBranch, ArrowRightCircle } from 'lucide-react';
import type { DragEvent } from 'react';

const NODE_TYPES = [
  { type: 'trigger', label: 'Trigger', icon: Clock, color: 'text-green-600 bg-green-50 border-green-200' },
  { type: 'rule', label: 'Rule', icon: FileText, color: 'text-blue-600 bg-blue-50 border-blue-200' },
  { type: 'condition', label: 'Condition', icon: GitBranch, color: 'text-yellow-600 bg-yellow-50 border-yellow-200' },
  { type: 'output', label: 'Output', icon: ArrowRightCircle, color: 'text-purple-600 bg-purple-50 border-purple-200' },
] as const;

export function NodePalette() {
  const onDragStart = (event: DragEvent, nodeType: string) => {
    event.dataTransfer.setData('application/reactflow', nodeType);
    event.dataTransfer.effectAllowed = 'move';
  };

  return (
    <div className="w-48 border-r border-gray-200 bg-white p-3 space-y-2">
      <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wider mb-3">
        Node Types
      </h3>
      {NODE_TYPES.map((item) => (
        <div
          key={item.type}
          draggable
          onDragStart={(e) => onDragStart(e, item.type)}
          className={`flex items-center gap-2 rounded-lg border px-3 py-2 cursor-grab active:cursor-grabbing transition-colors hover:shadow-sm ${item.color}`}
        >
          <item.icon className="h-4 w-4 flex-shrink-0" />
          <span className="text-sm font-medium">{item.label}</span>
        </div>
      ))}
    </div>
  );
}
