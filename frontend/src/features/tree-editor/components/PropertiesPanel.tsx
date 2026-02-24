import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { X, Plus, Trash2, Globe, FileCode, Terminal, MessageSquare, ChevronDown, ChevronUp, Shield, Key, Info, Check } from 'lucide-react';
import Editor from '@monaco-editor/react';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Button } from '@/shared/components/Button';
import { useTreeEditorStore } from '@/stores/tree-editor.store';
import { useEditorStore } from '@/stores/editor.store';
import type { ConditionNodeData, ActionNodeData, GateNodeData, CallableAction, CallableActionProtocol } from '@/stores/tree-editor.store';

const PROTOCOL_OPTIONS: { value: CallableActionProtocol; label: string; icon: typeof Globe; help: string }[] = [
  { value: 'http', label: 'HTTP', icon: Globe, help: 'Enter the URL to call (e.g., https://api.example.com/webhook)' },
  { value: 'rule', label: 'Rule', icon: FileCode, help: 'Enter the rule alias or ID to invoke' },
  { value: 'script', label: 'Script', icon: Terminal, help: 'Enter inline JavaScript. Must define an evaluate(params) function' },
  { value: 'log', label: 'Log', icon: MessageSquare, help: 'Enter a message template. Use ${varName} for variables' },
];

const OPERATORS = [
  'EQ', 'NEQ', 'GT', 'GTE', 'LT', 'LTE', 'BETWEEN', 'IN', 'CONTAINS', 'MATCHES', 'IS_NULL',
];

export function PropertiesPanel() {
  const { nodes, edges, selectedNodeId, updateNode, updateEdgeLabel, setSelectedNodeId } =
    useTreeEditorStore();
  const setDirty = useEditorStore((s) => s.setDirty);

  const selectedNode = nodes.find((n) => n.id === selectedNodeId);
  const nodeEdges = edges.filter(
    (e) => e.source === selectedNodeId || e.target === selectedNodeId,
  );

  const [outputKey, setOutputKey] = useState('');
  const [outputValue, setOutputValue] = useState('');

  // Reset new output fields when node changes
  useEffect(() => {
    setOutputKey('');
    setOutputValue('');
  }, [selectedNodeId]);

  if (!selectedNode) {
    return (
      <div className="flex items-center justify-center h-full text-sm text-gray-400 p-4 text-center">
        Select a node to edit its properties
      </div>
    );
  }

  const isCondition = selectedNode.type === 'conditionNode';
  const isGate = selectedNode.type === 'gateNode';
  const isAction = selectedNode.type === 'actionNode';
  const data = selectedNode.data as unknown as ConditionNodeData | ActionNodeData | GateNodeData;

  const updateNodeAndDirty = (id: string, data: Record<string, unknown>) => {
    updateNode(id, data);
    setDirty(true);
  };

  const updateEdgeLabelAndDirty = (id: string, label: string) => {
    updateEdgeLabel(id, label);
    setDirty(true);
  };

  const handleAddOutput = () => {
    if (!outputKey.trim()) return;
    const actionData = data as ActionNodeData;
    updateNodeAndDirty(selectedNode.id, {
      outputs: { ...actionData.outputs, [outputKey.trim()]: outputValue },
    });
    setOutputKey('');
    setOutputValue('');
  };

  const handleRemoveOutput = (key: string) => {
    const actionData = data as ActionNodeData;
    const newOutputs = { ...actionData.outputs };
    delete newOutputs[key];
    updateNodeAndDirty(selectedNode.id, { outputs: newOutputs });
  };

  return (
    <div className="flex flex-col h-full">
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
        <h3 className="text-sm font-semibold text-gray-900">Properties</h3>
        <button
          onClick={() => setSelectedNodeId(null)}
          className="p-1 rounded hover:bg-gray-100"
        >
          <X className="h-4 w-4 text-gray-400" />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-1">
            Label
          </label>
          <Input
            value={data.label}
            onChange={(e) => updateNodeAndDirty(selectedNode.id, { label: e.target.value })}
            placeholder="Node label"
          />
        </div>

        {isCondition && (
          <>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">
                Field
              </label>
              <Input
                value={(data as ConditionNodeData).field}
                onChange={(e) =>
                  updateNodeAndDirty(selectedNode.id, { field: e.target.value })
                }
                placeholder="e.g. age, income"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">
                Operator
              </label>
              <Select
                value={(data as ConditionNodeData).operator}
                onChange={(e) =>
                  updateNodeAndDirty(selectedNode.id, { operator: e.target.value })
                }
              >
                {OPERATORS.map((op) => (
                  <option key={op} value={op}>
                    {op}
                  </option>
                ))}
              </Select>
            </div>
            {(data as ConditionNodeData).operator === 'BETWEEN' ? (
              <div className="flex gap-2">
                <div className="flex-1">
                  <label className="block text-xs font-medium text-gray-600 mb-1">
                    Lower Bound
                  </label>
                  <Input
                    value={(data as ConditionNodeData).value}
                    onChange={(e) =>
                      updateNodeAndDirty(selectedNode.id, { value: e.target.value })
                    }
                    placeholder="Min value"
                  />
                </div>
                <div className="flex-1">
                  <label className="block text-xs font-medium text-gray-600 mb-1">
                    Upper Bound
                  </label>
                  <Input
                    value={(data as ConditionNodeData).secondValue ?? ''}
                    onChange={(e) =>
                      updateNodeAndDirty(selectedNode.id, { secondValue: e.target.value })
                    }
                    placeholder="Max value"
                  />
                </div>
              </div>
            ) : (data as ConditionNodeData).operator === 'IS_NULL' ? null : (
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">
                  Value
                </label>
                <Input
                  value={(data as ConditionNodeData).value}
                  onChange={(e) =>
                    updateNodeAndDirty(selectedNode.id, { value: e.target.value })
                  }
                  placeholder="Comparison value"
                />
              </div>
            )}
          </>
        )}

        {isGate && (
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">
              Expression (RFL)
            </label>
            <textarea
              value={(data as GateNodeData).expression}
              onChange={(e) =>
                updateNodeAndDirty(selectedNode.id, { expression: e.target.value })
              }
              placeholder="e.g. age > 18 && income >= 30000"
              rows={3}
              className="block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 text-xs font-mono"
            />
            <p className="mt-1 text-xs text-gray-400">
              Use && for AND, || for OR. Connect two children: first edge = true, second = false.
            </p>
          </div>
        )}

        {isAction && (
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-2">
              Outputs
            </label>
            {Object.keys((data as ActionNodeData).outputs ?? {}).length === 0 && (
              <div className="mb-2 px-3 py-2 rounded-md bg-amber-50 border border-amber-200 text-amber-700 text-xs">
                At least one output is required for this action node.
              </div>
            )}
            <div className="space-y-2">
              {Object.entries((data as ActionNodeData).outputs ?? {}).map(
                ([key, value]) => (
                  <div key={key} className="flex items-center gap-2">
                    <Input
                      value={key}
                      readOnly
                      className="flex-1 bg-gray-50 text-xs"
                    />
                    <Input
                      value={value}
                      onChange={(e) => {
                        const actionData = data as ActionNodeData;
                        updateNodeAndDirty(selectedNode.id, {
                          outputs: {
                            ...actionData.outputs,
                            [key]: e.target.value,
                          },
                        });
                      }}
                      className="flex-1 text-xs"
                    />
                    <button
                      onClick={() => handleRemoveOutput(key)}
                      className="p-1 rounded hover:bg-red-100 text-gray-400 hover:text-red-500"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                ),
              )}
              <div className="flex items-end gap-2">
                <div className="flex-1">
                  <Input
                    value={outputKey}
                    onChange={(e) => setOutputKey(e.target.value)}
                    placeholder="Key"
                    className="text-xs"
                  />
                </div>
                <div className="flex-1">
                  <Input
                    value={outputValue}
                    onChange={(e) => setOutputValue(e.target.value)}
                    placeholder="Value"
                    className="text-xs"
                  />
                </div>
                <Button
                  size="sm"
                  variant="outline"
                  onClick={handleAddOutput}
                  disabled={!outputKey.trim()}
                >
                  <Plus className="h-3 w-3" />
                </Button>
              </div>
            </div>
          </div>
        )}

        {/* Callable Actions */}
        {isAction && (
          <CallableActionsEditor
            actions={(data as ActionNodeData).callableActions ?? []}
            onChange={(actions) => updateNodeAndDirty(selectedNode.id, { callableActions: actions })}
          />
        )}

        {/* Edge labels */}
        {nodeEdges.length > 0 && (
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-2">
              Connected Edges
            </label>
            <div className="space-y-2">
              {nodeEdges.map((edge) => {
                const isSource = edge.source === selectedNodeId;
                const otherNode = nodes.find(
                  (n) => n.id === (isSource ? edge.target : edge.source),
                );
                return (
                  <div
                    key={edge.id}
                    className="flex items-center gap-2 text-xs"
                  >
                    <span className="text-gray-500 flex-shrink-0">
                      {isSource ? 'To' : 'From'}:{' '}
                      <span className="text-gray-700">
                        {(otherNode?.data as Record<string, unknown>)?.label as string ?? 'Unknown'}
                      </span>
                    </span>
                    <Input
                      value={(edge.label as string) ?? ''}
                      onChange={(e) => updateEdgeLabelAndDirty(edge.id, e.target.value)}
                      placeholder="Edge label"
                      className="flex-1 text-xs"
                    />
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// --- Callable Actions Editor ---

const DEFAULT_SCRIPT_TEMPLATE = `function evaluate(params) {
  // Access params passed from the Parameters section below
  // Return an object to merge into rule outputs
  return { result: params.value };
}`;

const HTTP_METHODS = ['GET', 'POST', 'PUT', 'DELETE'] as const;
const AUTH_TYPES = ['none', 'bearer', 'api_key'] as const;

const HEADER_SUGGESTIONS = [
  'Content-Type',
  'Accept',
  'Authorization',
  'X-API-Key',
  'Cache-Control',
  'X-Request-ID',
  'X-Correlation-ID',
  'X-Forwarded-For',
  'User-Agent',
];

interface HeaderRow {
  key: string;
  value: string;
  enabled: boolean;
}

type AuthType = typeof AUTH_TYPES[number];

interface HttpAuthorization {
  type: AuthType;
  token?: string;
  header?: string;
  value?: string;
}

function getHttpMethod(params: Record<string, unknown>): string {
  return (params.method as string) || 'POST';
}

function getHttpHeaders(params: Record<string, unknown>): HeaderRow[] {
  const raw = params.headers;
  if (Array.isArray(raw)) {
    return raw as HeaderRow[];
  }
  // Migrate from old Record<string, string> format
  if (raw && typeof raw === 'object') {
    return Object.entries(raw as Record<string, string>).map(([key, value]) => ({
      key,
      value,
      enabled: true,
    }));
  }
  return [];
}

function getHttpAuth(params: Record<string, unknown>): HttpAuthorization {
  return (params.authorization as HttpAuthorization) || { type: 'none' };
}

function getHttpBody(params: Record<string, unknown>): string {
  return (params.body as string) || '';
}

function getHttpQueryParams(params: Record<string, unknown>): Record<string, string> {
  return (params.queryParams as Record<string, string>) || {};
}

// Generic key-value param editor used by log/rule protocols and script parameters
function KeyValueParamEditor({
  params,
  onChange,
}: {
  params: Record<string, unknown>;
  onChange: (params: Record<string, unknown>) => void;
}) {
  const [paramKey, setParamKey] = useState('');
  const [paramValue, setParamValue] = useState('');

  // Filter out protocol-specific nested keys so we only show flat key-value pairs
  const flatEntries = Object.entries(params).filter(
    ([, v]) => typeof v === 'string' || typeof v === 'number' || typeof v === 'boolean',
  );

  return (
    <div>
      <label className="block text-[10px] font-medium text-gray-500 mb-0.5">Parameters</label>
      {flatEntries.map(([k, v]) => (
        <div key={k} className="flex items-center gap-1 mb-1">
          <Input value={k} readOnly className="flex-1 bg-gray-50 text-xs" />
          <Input
            value={String(v)}
            onChange={(e) => onChange({ ...params, [k]: e.target.value })}
            className="flex-1 text-xs"
          />
          <button
            onClick={() => {
              const p = { ...params };
              delete p[k];
              onChange(p);
            }}
            className="p-0.5 rounded hover:bg-red-100 text-gray-400 hover:text-red-500"
          >
            <Trash2 className="h-2.5 w-2.5" />
          </button>
        </div>
      ))}
      <div className="flex items-center gap-1">
        <Input
          value={paramKey}
          onChange={(e) => setParamKey(e.target.value)}
          placeholder="Key"
          className="flex-1 text-xs"
        />
        <Input
          value={paramValue}
          onChange={(e) => setParamValue(e.target.value)}
          placeholder="Value"
          className="flex-1 text-xs"
        />
        <Button
          size="sm"
          variant="outline"
          onClick={() => {
            if (paramKey.trim()) {
              onChange({ ...params, [paramKey.trim()]: paramValue });
              setParamKey('');
              setParamValue('');
            }
          }}
          disabled={!paramKey.trim()}
          className="px-1"
        >
          <Plus className="h-2.5 w-2.5" />
        </Button>
      </div>
    </div>
  );
}

// Postman-like header editor with enable/disable checkboxes and autocomplete
function HeaderEditor({
  headers,
  onChange,
}: {
  headers: HeaderRow[];
  onChange: (headers: HeaderRow[]) => void;
}) {
  const [focusedIdx, setFocusedIdx] = useState<number | null>(null);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const suggestionsRef = useRef<HTMLDivElement>(null);

  const usedKeys = useMemo(() => new Set(headers.map((h) => h.key.toLowerCase())), [headers]);

  const filteredSuggestions = useMemo(() => {
    if (focusedIdx === null) return [];
    const current = headers[focusedIdx]?.key.toLowerCase() ?? '';
    return HEADER_SUGGESTIONS.filter(
      (s) => s.toLowerCase().includes(current) && !usedKeys.has(s.toLowerCase()),
    );
  }, [focusedIdx, headers, usedKeys]);

  const updateRow = (idx: number, patch: Partial<HeaderRow>) => {
    const updated = headers.map((h, i) => (i === idx ? { ...h, ...patch } : h));
    onChange(updated);
  };

  const removeRow = (idx: number) => {
    onChange(headers.filter((_, i) => i !== idx));
  };

  const addRow = () => {
    onChange([...headers, { key: '', value: '', enabled: true }]);
  };

  const applySuggestion = (idx: number, suggestion: string) => {
    updateRow(idx, { key: suggestion });
    setShowSuggestions(false);
    setFocusedIdx(null);
  };

  return (
    <div className="space-y-1">
      {headers.length > 0 && (
        <div className="flex items-center gap-1 text-[9px] uppercase tracking-wider text-gray-400 px-0.5">
          <span className="w-5" />
          <span className="flex-1">Key</span>
          <span className="flex-1">Value</span>
          <span className="w-5" />
        </div>
      )}
      {headers.map((row, idx) => (
        <div key={idx} className="flex items-center gap-1 group relative">
          <button
            onClick={() => updateRow(idx, { enabled: !row.enabled })}
            className={`w-5 h-5 flex items-center justify-center rounded border transition-colors ${
              row.enabled
                ? 'bg-indigo-500 border-indigo-500 text-white'
                : 'bg-white border-gray-300 text-transparent hover:border-gray-400'
            }`}
            title={row.enabled ? 'Disable header' : 'Enable header'}
          >
            <Check className="h-3 w-3" />
          </button>
          <div className="flex-1 relative">
            <Input
              value={row.key}
              onChange={(e) => updateRow(idx, { key: e.target.value })}
              onFocus={() => {
                setFocusedIdx(idx);
                setShowSuggestions(true);
              }}
              onBlur={() => {
                // Delay to allow suggestion click
                setTimeout(() => {
                  setShowSuggestions(false);
                  setFocusedIdx(null);
                }, 150);
              }}
              placeholder="Header name"
              className={`text-xs ${!row.enabled ? 'opacity-50' : ''}`}
            />
            {showSuggestions && focusedIdx === idx && filteredSuggestions.length > 0 && (
              <div
                ref={suggestionsRef}
                className="absolute z-10 top-full left-0 right-0 mt-0.5 bg-white border border-gray-200 rounded-md shadow-lg max-h-32 overflow-y-auto"
              >
                {filteredSuggestions.map((s) => (
                  <button
                    key={s}
                    onMouseDown={(e) => {
                      e.preventDefault();
                      applySuggestion(idx, s);
                    }}
                    className="block w-full text-left px-2 py-1 text-xs hover:bg-indigo-50 text-gray-700 hover:text-indigo-700"
                  >
                    {s}
                  </button>
                ))}
              </div>
            )}
          </div>
          <Input
            value={row.value}
            onChange={(e) => updateRow(idx, { value: e.target.value })}
            placeholder="Value"
            className={`flex-1 text-xs ${!row.enabled ? 'opacity-50' : ''}`}
          />
          <button
            onClick={() => removeRow(idx)}
            className="p-0.5 rounded hover:bg-red-100 text-gray-400 hover:text-red-500 opacity-0 group-hover:opacity-100 transition-opacity"
          >
            <Trash2 className="h-2.5 w-2.5" />
          </button>
        </div>
      ))}
      <Button
        size="sm"
        variant="outline"
        onClick={addRow}
        className="w-full text-xs mt-1"
      >
        <Plus className="h-2.5 w-2.5 mr-1" />
        Add Header
      </Button>
    </div>
  );
}

// HTTP-specific fields for an action
function HttpActionFields({
  action,
  onUpdate,
}: {
  action: CallableAction;
  onUpdate: (updates: Partial<CallableAction>) => void;
}) {
  const params = action.parameters;
  const method = getHttpMethod(params);
  const headers = getHttpHeaders(params);
  const queryParams = getHttpQueryParams(params);
  const auth = getHttpAuth(params);
  const body = getHttpBody(params);
  const showBody = method === 'POST' || method === 'PUT' || method === 'DELETE';

  const [headersOpen, setHeadersOpen] = useState(headers.length > 0);

  const [queryParamsOpen, setQueryParamsOpen] = useState(method === 'GET' || Object.keys(queryParams).length > 0);
  const [qpKey, setQpKey] = useState('');
  const [qpValue, setQpValue] = useState('');

  const pathParams = useMemo(() => {
    const matches = (action.target || '').match(/\$\{([^}]+)\}/g);
    return matches ? matches.map((m) => m.slice(2, -1)) : [];
  }, [action.target]);

  const updateParams = useCallback(
    (patch: Record<string, unknown>) => {
      onUpdate({ parameters: { ...params, ...patch } });
    },
    [params, onUpdate],
  );

  return (
    <>
      {/* Method */}
      <div>
        <label className="block text-[10px] font-medium text-gray-500 mb-0.5">Method</label>
        <Select
          value={method}
          onChange={(e) => {
            updateParams({ method: e.target.value });
            if (e.target.value === 'GET') setQueryParamsOpen(true);
          }}
          className="text-xs"
        >
          {HTTP_METHODS.map((m) => (
            <option key={m} value={m}>{m}</option>
          ))}
        </Select>
      </div>

      {/* URL */}
      <div>
        <label className="block text-[10px] font-medium text-gray-500 mb-0.5">URL</label>
        <Input
          value={action.target}
          onChange={(e) => onUpdate({ target: e.target.value })}
          placeholder="https://api.example.com/endpoint"
          className="text-xs"
        />
        {pathParams.length > 0 && (
          <p className="mt-0.5 text-[10px] text-indigo-500 flex items-center gap-1">
            <Info className="h-2.5 w-2.5 flex-shrink-0" />
            Path params: {pathParams.join(', ')}
          </p>
        )}
      </div>

      {/* Query Parameters (collapsible) */}
      <div>
        <button
          onClick={() => setQueryParamsOpen(!queryParamsOpen)}
          className="flex items-center gap-1 text-[10px] font-medium text-gray-500 mb-0.5 hover:text-gray-700"
        >
          {queryParamsOpen ? <ChevronUp className="h-2.5 w-2.5" /> : <ChevronDown className="h-2.5 w-2.5" />}
          Query Parameters {Object.keys(queryParams).length > 0 && `(${Object.keys(queryParams).length})`}
        </button>
        {queryParamsOpen && (
          <div className="ml-1 space-y-1">
            {Object.entries(queryParams).map(([k, v], idx) => (
              <div key={idx} className="flex items-center gap-1">
                <Input
                  value={k}
                  onChange={(e) => {
                    const newQp = { ...queryParams };
                    const val = newQp[k];
                    delete newQp[k];
                    newQp[e.target.value] = val;
                    updateParams({ queryParams: newQp });
                  }}
                  className="flex-1 text-xs"
                />
                <Input
                  value={v}
                  onChange={(e) => {
                    updateParams({ queryParams: { ...queryParams, [k]: e.target.value } });
                  }}
                  className="flex-1 text-xs"
                />
                <button
                  onClick={() => {
                    const qp = { ...queryParams };
                    delete qp[k];
                    updateParams({ queryParams: qp });
                  }}
                  className="p-0.5 rounded hover:bg-red-100 text-gray-400 hover:text-red-500"
                >
                  <Trash2 className="h-2.5 w-2.5" />
                </button>
              </div>
            ))}
            <div className="flex items-center gap-1">
              <Input
                value={qpKey}
                onChange={(e) => setQpKey(e.target.value)}
                placeholder="Param name"
                className="flex-1 text-xs"
              />
              <Input
                value={qpValue}
                onChange={(e) => setQpValue(e.target.value)}
                placeholder="Param value"
                className="flex-1 text-xs"
              />
              <Button
                size="sm"
                variant="outline"
                onClick={() => {
                  if (qpKey.trim()) {
                    updateParams({ queryParams: { ...queryParams, [qpKey.trim()]: qpValue } });
                    setQpKey('');
                    setQpValue('');
                  }
                }}
                disabled={!qpKey.trim()}
                className="px-1"
              >
                <Plus className="h-2.5 w-2.5" />
              </Button>
            </div>
            <p className="text-[10px] text-gray-400 mt-0.5">
              Use {'${factPath}'} expressions to inject working memory values.
            </p>
          </div>
        )}
      </div>

      {/* Headers (collapsible, Postman-style) */}
      <div>
        <button
          onClick={() => setHeadersOpen(!headersOpen)}
          className="flex items-center gap-1 text-[10px] font-medium text-gray-500 mb-0.5 hover:text-gray-700"
        >
          {headersOpen ? <ChevronUp className="h-2.5 w-2.5" /> : <ChevronDown className="h-2.5 w-2.5" />}
          Headers {headers.length > 0 && `(${headers.filter((h) => h.enabled).length}/${headers.length})`}
        </button>
        {headersOpen && (
          <div className="ml-1">
            <HeaderEditor
              headers={headers}
              onChange={(updated) => updateParams({ headers: updated })}
            />
          </div>
        )}
      </div>

      {/* Authorization */}
      <div>
        <label className="block text-[10px] font-medium text-gray-500 mb-0.5">Authorization</label>
        <Select
          value={auth.type}
          onChange={(e) => {
            const type = e.target.value as AuthType;
            if (type === 'none') {
              updateParams({ authorization: { type: 'none' } });
            } else if (type === 'bearer') {
              updateParams({ authorization: { type: 'bearer', token: auth.token || '' } });
            } else {
              updateParams({ authorization: { type: 'api_key', header: auth.header || 'X-API-Key', value: auth.value || '' } });
            }
          }}
          className="text-xs"
        >
          <option value="none">None</option>
          <option value="bearer">Bearer Token</option>
          <option value="api_key">API Key</option>
        </Select>

        {auth.type === 'bearer' && (
          <div className="mt-1">
            <div className="flex items-center gap-1">
              <Shield className="h-3 w-3 text-gray-400 flex-shrink-0" />
              <Input
                value={auth.token || ''}
                onChange={(e) => updateParams({ authorization: { ...auth, token: e.target.value } })}
                placeholder="Bearer token"
                className="text-xs flex-1"
                type="password"
              />
            </div>
          </div>
        )}

        {auth.type === 'api_key' && (
          <div className="mt-1 space-y-1">
            <div className="flex items-center gap-1">
              <Key className="h-3 w-3 text-gray-400 flex-shrink-0" />
              <Input
                value={auth.header || 'X-API-Key'}
                onChange={(e) => updateParams({ authorization: { ...auth, header: e.target.value } })}
                placeholder="Header name"
                className="text-xs flex-1"
              />
            </div>
            <Input
              value={auth.value || ''}
              onChange={(e) => updateParams({ authorization: { ...auth, value: e.target.value } })}
              placeholder="API key value"
              className="text-xs"
              type="password"
            />
          </div>
        )}
      </div>

      {/* Request Body (POST/PUT/DELETE only) */}
      {showBody && (
        <div>
          <label className="block text-[10px] font-medium text-gray-500 mb-0.5">Request Body (JSON)</label>
          <div className="border border-gray-300 rounded-md overflow-hidden">
            <Editor
              height="150px"
              language="json"
              value={body}
              onChange={(val) => updateParams({ body: val || '' })}
              options={{
                minimap: { enabled: false },
                lineNumbers: 'off',
                scrollBeyondLastLine: false,
                fontSize: 11,
                tabSize: 2,
                wordWrap: 'on',
              }}
              theme="vs-light"
            />
          </div>
          <p className="mt-0.5 text-[10px] text-gray-400">
            Use {'${factPath}'} expressions to reference working memory values.
          </p>
        </div>
      )}

      {/* Output Mapping */}
      <div>
        <label className="block text-[10px] font-medium text-gray-500 mb-0.5">Output Mapping (optional)</label>
        <Input
          value={action.outputMapping ?? ''}
          onChange={(e) => onUpdate({ outputMapping: e.target.value || undefined })}
          placeholder="Key to store response under"
          className="text-xs"
        />
      </div>
    </>
  );
}

// Script-specific fields for an action
function ScriptActionFields({
  action,
  onUpdate,
}: {
  action: CallableAction;
  onUpdate: (updates: Partial<CallableAction>) => void;
}) {
  const params = action.parameters;

  // Filter out non-flat params for the key-value editor
  const scriptParams = Object.fromEntries(
    Object.entries(params).filter(([, v]) => typeof v === 'string' || typeof v === 'number' || typeof v === 'boolean'),
  );

  return (
    <>
      {/* Monaco JS editor for script body (stored in target) */}
      <div>
        <label className="block text-[10px] font-medium text-gray-500 mb-0.5">Script</label>
        <div className="border border-gray-300 rounded-md overflow-hidden">
          <Editor
            height="200px"
            language="javascript"
            value={action.target || DEFAULT_SCRIPT_TEMPLATE}
            onChange={(val) => onUpdate({ target: val || '' })}
            options={{
              minimap: { enabled: false },
              lineNumbers: 'on',
              scrollBeyondLastLine: false,
              fontSize: 11,
              tabSize: 2,
              wordWrap: 'on',
            }}
            theme="vs-light"
          />
        </div>

        {/* Hint panel */}
        <div className="mt-1.5 rounded-md bg-blue-50 border border-blue-200 p-2 space-y-0.5">
          <div className="flex items-start gap-1">
            <Info className="h-3 w-3 text-blue-500 mt-0.5 flex-shrink-0" />
            <div className="text-[10px] text-blue-700 space-y-0.5">
              <p>Function must be named <code className="bg-blue-100 px-0.5 rounded">evaluate</code> and accept a <code className="bg-blue-100 px-0.5 rounded">params</code> object.</p>
              <p>Return an object to add to rule outputs, or return nothing for side-effect-only scripts.</p>
              <p>Runs in a sandboxed environment -- no network or file access.</p>
            </div>
          </div>
        </div>
      </div>

      {/* Parameters (passed to the script) */}
      <KeyValueParamEditor
        params={scriptParams}
        onChange={(newParams) => onUpdate({ parameters: newParams })}
      />

      {/* Output Mapping */}
      <div>
        <label className="block text-[10px] font-medium text-gray-500 mb-0.5">Output Mapping (optional)</label>
        <Input
          value={action.outputMapping ?? ''}
          onChange={(e) => onUpdate({ outputMapping: e.target.value || undefined })}
          placeholder="Key to store result under"
          className="text-xs"
        />
      </div>
    </>
  );
}

// Generic fields for log/rule protocols
function GenericActionFields({
  action,
  onUpdate,
}: {
  action: CallableAction;
  onUpdate: (updates: Partial<CallableAction>) => void;
}) {
  const proto = PROTOCOL_OPTIONS.find((p) => p.value === action.protocol);

  return (
    <>
      <div>
        <label className="block text-[10px] font-medium text-gray-500 mb-0.5">Target</label>
        <Input
          value={action.target}
          onChange={(e) => onUpdate({ target: e.target.value })}
          placeholder={proto?.help}
          className="text-xs"
        />
      </div>
      <KeyValueParamEditor
        params={action.parameters}
        onChange={(newParams) => onUpdate({ parameters: newParams })}
      />
      <div>
        <label className="block text-[10px] font-medium text-gray-500 mb-0.5">Output Mapping (optional)</label>
        <Input
          value={action.outputMapping ?? ''}
          onChange={(e) => onUpdate({ outputMapping: e.target.value || undefined })}
          placeholder="Key to store result under"
          className="text-xs"
        />
      </div>
    </>
  );
}

function CallableActionsEditor({
  actions,
  onChange,
}: {
  actions: CallableAction[];
  onChange: (actions: CallableAction[]) => void;
}) {
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [showNewForm, setShowNewForm] = useState(false);
  const [newProtocol, setNewProtocol] = useState<CallableActionProtocol>('http');

  const handleAddAction = () => {
    const isScript = newProtocol === 'script';
    const action: CallableAction = {
      id: `ca_${Date.now()}`,
      protocol: newProtocol,
      target: isScript ? DEFAULT_SCRIPT_TEMPLATE : '',
      parameters: newProtocol === 'http' ? { method: 'POST' } : {},
    };
    onChange([...actions, action]);
    setExpandedId(action.id);
    setShowNewForm(false);
  };

  const handleDeleteAction = (id: string) => {
    onChange(actions.filter((a) => a.id !== id));
    if (expandedId === id) setExpandedId(null);
  };

  const handleUpdateAction = useCallback(
    (id: string, updates: Partial<CallableAction>) => {
      onChange(actions.map((a) => (a.id === id ? { ...a, ...updates } : a)));
    },
    [actions, onChange],
  );

  return (
    <div>
      <div className="flex items-center justify-between mb-2">
        <label className="block text-xs font-medium text-indigo-600">
          Callable Actions
        </label>
        <Button
          size="sm"
          variant="outline"
          onClick={() => setShowNewForm(!showNewForm)}
          className="text-indigo-600 border-indigo-300 hover:bg-indigo-50"
        >
          <Plus className="h-3 w-3 mr-1" />
          Add
        </Button>
      </div>

      {actions.length === 0 && !showNewForm && (
        <p className="text-xs text-gray-400 italic">
          No callable actions. Add one to execute HTTP calls, sub-rules, scripts, or logging.
        </p>
      )}

      {/* Existing actions */}
      <div className="space-y-2">
        {actions.map((action) => {
          const proto = PROTOCOL_OPTIONS.find((p) => p.value === action.protocol);
          const Icon = proto?.icon ?? Globe;
          const isExpanded = expandedId === action.id;
          const actionLabel = action.protocol === 'script'
            ? 'Script'
            : action.protocol === 'http'
              ? `${getHttpMethod(action.parameters)} ${action.target || '(no url)'}`
              : action.target || '(empty)';

          return (
            <div
              key={action.id}
              className="rounded-md border border-indigo-200 bg-indigo-50/50"
            >
              <div className="flex items-center gap-2 px-2 py-1.5">
                <Icon className="h-3.5 w-3.5 text-indigo-500 flex-shrink-0" />
                <span className="text-xs font-medium text-indigo-700 flex-1 truncate">
                  {proto?.label}: {actionLabel}
                </span>
                <button
                  onClick={() => setExpandedId(isExpanded ? null : action.id)}
                  className="p-0.5 rounded hover:bg-indigo-100"
                >
                  {isExpanded ? (
                    <ChevronUp className="h-3 w-3 text-indigo-400" />
                  ) : (
                    <ChevronDown className="h-3 w-3 text-indigo-400" />
                  )}
                </button>
                <button
                  onClick={() => handleDeleteAction(action.id)}
                  className="p-0.5 rounded hover:bg-red-100 text-gray-400 hover:text-red-500"
                >
                  <Trash2 className="h-3 w-3" />
                </button>
              </div>

              {isExpanded && (
                <div className="px-2 pb-2 space-y-2 border-t border-indigo-200">
                  <div className="pt-2">
                    <label className="block text-[10px] font-medium text-gray-500 mb-0.5">Protocol</label>
                    <Select
                      value={action.protocol}
                      onChange={(e) => {
                        const protocol = e.target.value as CallableActionProtocol;
                        const updates: Partial<CallableAction> = { protocol };
                        // Reset parameters and clear stale target when switching protocols
                        if (protocol === 'http') {
                          updates.target = '';
                          updates.parameters = { method: 'POST' };
                        } else if (protocol === 'script') {
                          updates.target = action.target || DEFAULT_SCRIPT_TEMPLATE;
                          updates.parameters = {};
                        } else {
                          updates.target = '';
                          updates.parameters = {};
                        }
                        handleUpdateAction(action.id, updates);
                      }}
                      className="text-xs"
                    >
                      {PROTOCOL_OPTIONS.map((p) => (
                        <option key={p.value} value={p.value}>{p.label}</option>
                      ))}
                    </Select>
                  </div>

                  {action.protocol === 'http' && (
                    <HttpActionFields
                      action={action}
                      onUpdate={(updates) => handleUpdateAction(action.id, updates)}
                    />
                  )}

                  {action.protocol === 'script' && (
                    <ScriptActionFields
                      action={action}
                      onUpdate={(updates) => handleUpdateAction(action.id, updates)}
                    />
                  )}

                  {(action.protocol === 'log' || action.protocol === 'rule') && (
                    <GenericActionFields
                      action={action}
                      onUpdate={(updates) => handleUpdateAction(action.id, updates)}
                    />
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* New action form — simplified to just protocol picker */}
      {showNewForm && (
        <div className="mt-2 rounded-md border border-indigo-300 bg-indigo-50 p-2 space-y-2">
          <div>
            <label className="block text-[10px] font-medium text-gray-500 mb-0.5">Protocol</label>
            <Select
              value={newProtocol}
              onChange={(e) => setNewProtocol(e.target.value as CallableActionProtocol)}
              className="text-xs"
            >
              {PROTOCOL_OPTIONS.map((p) => (
                <option key={p.value} value={p.value}>{p.label}</option>
              ))}
            </Select>
            <p className="mt-1 text-[10px] text-gray-400">
              {PROTOCOL_OPTIONS.find((p) => p.value === newProtocol)?.help}
            </p>
          </div>
          <div className="flex gap-2 pt-1">
            <Button size="sm" onClick={handleAddAction} className="bg-indigo-600 hover:bg-indigo-700 text-white">
              Add Action
            </Button>
            <Button size="sm" variant="outline" onClick={() => setShowNewForm(false)}>
              Cancel
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
