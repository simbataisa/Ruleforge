import { useState, useEffect } from 'react';
import { type Node } from '@xyflow/react';
import { X, Clock, MousePointerClick, Webhook, Radio, ArrowRightCircle, GitBranch, Copy, Check, CalendarDays, Trash2 } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Badge } from '@/shared/components/Badge';
import { cn } from '@/shared/utils/cn';
import { useRules } from '@/api/rules.api';
import { useHolidayCalendars, type SkipOnHoliday } from '@/api/holiday-calendars.api';

interface NodeEditPanelProps {
  node: Node;
  onClose: () => void;
  onNodeUpdate: (nodeId: string, data: Record<string, unknown>) => void;
  onDeleteNode?: (nodeId: string) => void;
  workflowId?: string;
}

const CRON_PRESETS = [
  { label: 'Every 5 min', cron: '*/5 * * * *' },
  { label: 'Hourly', cron: '0 * * * *' },
  { label: 'Daily', cron: '0 0 * * *' },
  { label: 'Weekly', cron: '0 9 * * 1' },
  { label: 'Monthly', cron: '0 0 1 * *' },
] as const;

const TRIGGER_TYPES = ['CRON', 'MANUAL', 'WEBHOOK', 'EVENT'] as const;
type TriggerTypeValue = (typeof TRIGGER_TYPES)[number];

const OUTPUT_TYPES = ['LOG', 'NOTIFY', 'STORE', 'WEBHOOK'] as const;

const NODE_META: Record<string, { icon: typeof Clock; color: string; badgeColor: 'green' | 'blue' | 'yellow' | 'gray' }> = {
  trigger: { icon: Clock, color: 'text-green-600', badgeColor: 'green' },
  rule: { icon: GitBranch, color: 'text-blue-600', badgeColor: 'blue' },
  condition: { icon: GitBranch, color: 'text-yellow-600', badgeColor: 'yellow' },
  output: { icon: ArrowRightCircle, color: 'text-purple-600', badgeColor: 'gray' },
};

function getTriggerIcon(type: TriggerTypeValue) {
  switch (type) {
    case 'MANUAL': return MousePointerClick;
    case 'WEBHOOK': return Webhook;
    case 'EVENT': return Radio;
    default: return Clock;
  }
}

export function NodeEditPanel({ node, onClose, onNodeUpdate, onDeleteNode, workflowId }: NodeEditPanelProps) {
  const nodeType = node.type ?? 'trigger';
  const data = node.data as Record<string, unknown>;
  const config = (data.config as Record<string, unknown>) ?? {};

  // Local form state
  const [label, setLabel] = useState((data.label as string) ?? '');
  const [triggerType, setTriggerType] = useState<TriggerTypeValue>(
    (config.triggerType as TriggerTypeValue) ?? 'CRON',
  );
  const [cronExpression, setCronExpression] = useState((config.cronExpression as string) ?? '');
  const [eventTopic, setEventTopic] = useState((config.eventTopic as string) ?? '');
  const [ruleId, setRuleId] = useState((data.ruleId as string) ?? '');
  const [conditionExpression, setConditionExpression] = useState(
    (config.expression as string) ?? '',
  );
  const [trueBranchLabel, setTrueBranchLabel] = useState((config.trueBranchLabel as string) ?? '');
  const [falseBranchLabel, setFalseBranchLabel] = useState((config.falseBranchLabel as string) ?? '');
  const [outputType, setOutputType] = useState((config.outputType as string) ?? 'LOG');
  const [outputDescription, setOutputDescription] = useState((config.description as string) ?? '');
  const [copied, setCopied] = useState(false);
  const [holidayCalendarId, setHolidayCalendarId] = useState(
    (config.holidayCalendarId as string) ?? '',
  );
  const [skipOnHoliday, setSkipOnHoliday] = useState<SkipOnHoliday>(
    (config.skipOnHoliday as SkipOnHoliday) ?? 'SKIP',
  );

  const { data: rulesData } = useRules({ size: 100 });
  const rules = rulesData?.content ?? [];
  const { data: calendarsData } = useHolidayCalendars();
  const calendars = calendarsData ?? [];

  // Reset form when node changes
  useEffect(() => {
    const d = node.data as Record<string, unknown>;
    const c = (d.config as Record<string, unknown>) ?? {};
    setLabel((d.label as string) ?? '');
    setTriggerType((c.triggerType as TriggerTypeValue) ?? 'CRON');
    setCronExpression((c.cronExpression as string) ?? '');
    setEventTopic((c.eventTopic as string) ?? '');
    setRuleId((d.ruleId as string) ?? '');
    setConditionExpression((c.expression as string) ?? '');
    setTrueBranchLabel((c.trueBranchLabel as string) ?? '');
    setFalseBranchLabel((c.falseBranchLabel as string) ?? '');
    setOutputType((c.outputType as string) ?? 'LOG');
    setOutputDescription((c.description as string) ?? '');
    setHolidayCalendarId((c.holidayCalendarId as string) ?? '');
    setSkipOnHoliday((c.skipOnHoliday as SkipOnHoliday) ?? 'SKIP');
    setCopied(false);
  }, [node.id, node.data]);

  const meta = NODE_META[nodeType] ?? NODE_META.trigger;
  const Icon = meta.icon;

  const webhookUrl = `/v1/workflows/${workflowId ?? 'unknown'}/webhook`;

  const handleCopyWebhook = async () => {
    await navigator.clipboard.writeText(webhookUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleApply = () => {
    const updated: Record<string, unknown> = { label };

    if (nodeType === 'trigger') {
      const triggerConfig: Record<string, unknown> = { triggerType };
      if (triggerType === 'CRON') {
        triggerConfig.cronExpression = cronExpression;
        if (holidayCalendarId) {
          triggerConfig.holidayCalendarId = holidayCalendarId;
          triggerConfig.skipOnHoliday = skipOnHoliday;
        }
      }
      if (triggerType === 'EVENT') triggerConfig.eventTopic = eventTopic;
      if (triggerType === 'WEBHOOK') triggerConfig.webhookUrl = webhookUrl;
      updated.config = triggerConfig;
    }

    if (nodeType === 'rule') {
      const selectedRule = rules.find((r) => r.id === ruleId);
      if (selectedRule) {
        updated.ruleId = selectedRule.id;
        updated.ruleName = selectedRule.name;
        updated.ruleType = selectedRule.type;
      }
    }

    if (nodeType === 'condition') {
      updated.config = {
        ...config,
        expression: conditionExpression,
        trueBranchLabel,
        falseBranchLabel,
      };
    }

    if (nodeType === 'output') {
      updated.config = {
        ...config,
        outputType,
        description: outputDescription,
      };
    }

    onNodeUpdate(node.id, updated);
  };

  return (
    <div className="w-80 border-l border-gray-200 bg-white flex flex-col overflow-hidden">
      {/* Header */}
      <div className="flex items-center gap-2 border-b border-gray-200 px-4 py-3">
        <Icon className={cn('h-4 w-4', meta.color)} />
        <Badge color={meta.badgeColor}>{nodeType}</Badge>
        <span className="text-sm font-semibold text-gray-900">
          Edit {nodeType.charAt(0).toUpperCase() + nodeType.slice(1)}
        </span>
        <button
          onClick={onClose}
          className="ml-auto rounded-md p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Form body */}
      <div className="flex-1 overflow-y-auto px-4 py-4 space-y-4">
        {/* Label — common to all types */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Label</label>
          <Input
            value={label}
            onChange={(e) => setLabel(e.target.value)}
            placeholder="Node label"
          />
        </div>

        {/* Trigger-specific fields */}
        {nodeType === 'trigger' && (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Trigger Type</label>
              <div className="grid grid-cols-2 gap-1.5">
                {TRIGGER_TYPES.map((t) => {
                  const TIcon = getTriggerIcon(t);
                  return (
                    <button
                      key={t}
                      type="button"
                      onClick={() => setTriggerType(t)}
                      className={cn(
                        'flex items-center justify-center gap-1.5 rounded-lg border px-3 py-2 text-xs font-medium transition-colors',
                        triggerType === t
                          ? 'bg-indigo-50 text-indigo-700 border-indigo-300 ring-1 ring-indigo-200'
                          : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50',
                      )}
                    >
                      <TIcon className="h-3.5 w-3.5 flex-shrink-0" />
                      {t}
                    </button>
                  );
                })}
              </div>
            </div>

            {triggerType === 'CRON' && (
              <>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">Cron Expression</label>
                  <div className="flex flex-wrap gap-1.5 mb-2">
                    {CRON_PRESETS.map((preset) => (
                      <button
                        key={preset.cron}
                        type="button"
                        onClick={() => setCronExpression(preset.cron)}
                        className={cn(
                          'rounded-full px-2.5 py-1 text-[11px] font-medium border transition-colors',
                          cronExpression === preset.cron
                            ? 'bg-indigo-100 text-indigo-700 border-indigo-300'
                            : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50',
                        )}
                      >
                        {preset.label}
                      </button>
                    ))}
                  </div>
                  <Input
                    value={cronExpression}
                    onChange={(e) => setCronExpression(e.target.value)}
                    placeholder="*/5 * * * *"
                  />
                </div>

                {/* Holiday Calendar — only for CRON triggers */}
                <div className="rounded-lg border border-gray-200 bg-gray-50 p-3 space-y-3">
                  <div className="flex items-center gap-1.5">
                    <CalendarDays className="h-3.5 w-3.5 text-gray-500" />
                    <label className="text-sm font-medium text-gray-700">Holiday Calendar</label>
                  </div>
                  <select
                    value={holidayCalendarId}
                    onChange={(e) => setHolidayCalendarId(e.target.value)}
                    className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500 bg-white"
                  >
                    <option value="">None — run on all days</option>
                    {calendars.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name} ({c.market})
                      </option>
                    ))}
                  </select>
                  {holidayCalendarId && (
                    <div>
                      <label className="block text-xs font-medium text-gray-600 mb-1.5">On holidays:</label>
                      <div className="space-y-1">
                        {([
                          { value: 'SKIP', label: 'Skip execution', desc: 'Do not run on holidays' },
                          { value: 'DEFER_NEXT', label: 'Defer to next business day', desc: 'Run on the next working day' },
                          { value: 'RUN_ANYWAY', label: 'Run anyway', desc: 'Ignore holiday calendar' },
                        ] as const).map((opt) => (
                          <label
                            key={opt.value}
                            className={cn(
                              'flex items-start gap-2 rounded-lg border px-3 py-2 cursor-pointer transition-colors',
                              skipOnHoliday === opt.value
                                ? 'border-indigo-300 bg-indigo-50'
                                : 'border-gray-200 bg-white hover:bg-gray-50',
                            )}
                          >
                            <input
                              type="radio"
                              name="skipOnHoliday"
                              value={opt.value}
                              checked={skipOnHoliday === opt.value}
                              onChange={() => setSkipOnHoliday(opt.value)}
                              className="mt-0.5 h-3.5 w-3.5 text-indigo-600 focus:ring-indigo-500"
                            />
                            <div>
                              <span className="text-xs font-medium text-gray-900">{opt.label}</span>
                              <p className="text-[10px] text-gray-500">{opt.desc}</p>
                            </div>
                          </label>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </>
            )}

            {triggerType === 'MANUAL' && (
              <p className="text-sm text-gray-500 italic">
                Trigger manually via API or UI.
              </p>
            )}

            {triggerType === 'WEBHOOK' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Webhook URL</label>
                <div className="flex items-center gap-2">
                  <code className="flex-1 text-xs text-gray-700 bg-gray-50 rounded-lg border border-gray-200 px-3 py-2 truncate">
                    {webhookUrl}
                  </code>
                  <button
                    type="button"
                    onClick={handleCopyWebhook}
                    className="rounded-lg border border-gray-200 p-2 text-gray-500 hover:bg-gray-50 hover:text-gray-700 transition-colors"
                  >
                    {copied ? <Check className="h-3.5 w-3.5 text-green-600" /> : <Copy className="h-3.5 w-3.5" />}
                  </button>
                </div>
              </div>
            )}

            {triggerType === 'EVENT' && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Event Topic</label>
                <Input
                  value={eventTopic}
                  onChange={(e) => setEventTopic(e.target.value)}
                  placeholder="event.topic.name"
                />
              </div>
            )}
          </>
        )}

        {/* Rule-specific fields */}
        {nodeType === 'rule' && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Rule</label>
            <select
              value={ruleId}
              onChange={(e) => setRuleId(e.target.value)}
              className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              <option value="">Select a rule...</option>
              {rules.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name} ({r.type})
                </option>
              ))}
            </select>
            {ruleId && (() => {
              const selected = rules.find((r) => r.id === ruleId);
              return selected ? (
                <div className="mt-2 flex items-center gap-2">
                  <Badge color="blue">{selected.type}</Badge>
                  <span className="text-xs text-gray-500">{selected.name}</span>
                </div>
              ) : null;
            })()}
          </div>
        )}

        {/* Condition-specific fields */}
        {nodeType === 'condition' && (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Condition Expression</label>
              <textarea
                value={conditionExpression}
                onChange={(e) => setConditionExpression(e.target.value)}
                placeholder="e.g. result.score > 80"
                rows={3}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">True Label</label>
                <Input
                  value={trueBranchLabel}
                  onChange={(e) => setTrueBranchLabel(e.target.value)}
                  placeholder="Yes"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">False Label</label>
                <Input
                  value={falseBranchLabel}
                  onChange={(e) => setFalseBranchLabel(e.target.value)}
                  placeholder="No"
                />
              </div>
            </div>
          </>
        )}

        {/* Output-specific fields */}
        {nodeType === 'output' && (
          <>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Output Type</label>
              <select
                value={outputType}
                onChange={(e) => setOutputType(e.target.value)}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              >
                {OUTPUT_TYPES.map((t) => (
                  <option key={t} value={t}>{t}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea
                value={outputDescription}
                onChange={(e) => setOutputDescription(e.target.value)}
                placeholder="Describe the output behavior..."
                rows={3}
                className="w-full rounded-lg border border-gray-300 px-3 py-2 text-sm text-gray-900 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
              />
            </div>
          </>
        )}
      </div>

      {/* Footer */}
      <div className="border-t border-gray-200 px-4 py-3">
        <Button onClick={handleApply} className="w-full">
          Apply Changes
        </Button>
        {onDeleteNode && (
          <button
            onClick={() => onDeleteNode(node.id)}
            className="w-full mt-2 rounded-lg border border-red-300 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50 transition-colors flex items-center justify-center gap-2"
          >
            <Trash2 className="h-4 w-4" />
            Delete Node
          </button>
        )}
      </div>
    </div>
  );
}
