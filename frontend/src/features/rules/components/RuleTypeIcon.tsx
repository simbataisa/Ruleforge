import type { LucideIcon } from 'lucide-react';
import {
  Table2,
  GitBranch,
  Code,
  Workflow,
  Calculator,
  FileSpreadsheet,
  Activity,
  Layers,
} from 'lucide-react';
import type { RuleType } from '@/api/types/rule.types';

export const ruleTypeConfig: Record<
  string,
  { icon: LucideIcon; color: string; bgColor: string; label: string }
> = {
  TABLE: { icon: Table2, color: 'text-blue-500', bgColor: 'bg-blue-50 text-blue-700', label: 'Table' },
  TREE: { icon: GitBranch, color: 'text-green-500', bgColor: 'bg-green-50 text-green-700', label: 'Tree' },
  SCRIPT: { icon: Code, color: 'text-purple-500', bgColor: 'bg-purple-50 text-purple-700', label: 'Script' },
  FLOW: { icon: Workflow, color: 'text-orange-500', bgColor: 'bg-orange-50 text-orange-700', label: 'Flow' },
  SCORECARD: { icon: Calculator, color: 'text-teal-500', bgColor: 'bg-teal-50 text-teal-700', label: 'Scorecard' },
  DMN: { icon: FileSpreadsheet, color: 'text-indigo-500', bgColor: 'bg-indigo-50 text-indigo-700', label: 'DMN' },
  CEP: { icon: Activity, color: 'text-red-500', bgColor: 'bg-red-50 text-red-700', label: 'CEP' },
  ENSEMBLE: { icon: Layers, color: 'text-amber-500', bgColor: 'bg-amber-50 text-amber-700', label: 'Ensemble' },
};

interface RuleTypeIconProps {
  type: RuleType | string;
  size?: number;
  showLabel?: boolean;
}

export function RuleTypeIcon({ type, size = 16, showLabel = false }: RuleTypeIconProps) {
  const config = ruleTypeConfig[type];
  if (!config) return <span className="text-xs text-gray-500">{type}</span>;

  const Icon = config.icon;

  if (showLabel) {
    return (
      <span className={`inline-flex items-center gap-1 rounded-md px-1.5 py-0.5 text-[11px] font-medium ${config.bgColor}`}>
        <Icon size={size - 2} />
        {config.label}
      </span>
    );
  }

  return <Icon size={size} className={config.color} />;
}
