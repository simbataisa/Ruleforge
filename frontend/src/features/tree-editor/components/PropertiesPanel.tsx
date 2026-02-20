import { useState, useEffect } from 'react';
import { X, Plus, Trash2 } from 'lucide-react';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Button } from '@/shared/components/Button';
import { useTreeEditorStore } from '@/stores/tree-editor.store';
import type { ConditionNodeData, ActionNodeData } from '@/stores/tree-editor.store';

const OPERATORS = [
  'EQ', 'NEQ', 'GT', 'GTE', 'LT', 'LTE', 'BETWEEN', 'IN', 'CONTAINS', 'MATCHES', 'IS_NULL',
];

export function PropertiesPanel() {
  const { nodes, edges, selectedNodeId, updateNode, updateEdgeLabel, setSelectedNodeId } =
    useTreeEditorStore();

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
  const data = selectedNode.data as unknown as ConditionNodeData | ActionNodeData;

  const handleAddOutput = () => {
    if (!outputKey.trim()) return;
    const actionData = data as ActionNodeData;
    updateNode(selectedNode.id, {
      outputs: { ...actionData.outputs, [outputKey.trim()]: outputValue },
    });
    setOutputKey('');
    setOutputValue('');
  };

  const handleRemoveOutput = (key: string) => {
    const actionData = data as ActionNodeData;
    const newOutputs = { ...actionData.outputs };
    delete newOutputs[key];
    updateNode(selectedNode.id, { outputs: newOutputs });
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
            onChange={(e) => updateNode(selectedNode.id, { label: e.target.value })}
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
                  updateNode(selectedNode.id, { field: e.target.value })
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
                  updateNode(selectedNode.id, { operator: e.target.value })
                }
              >
                {OPERATORS.map((op) => (
                  <option key={op} value={op}>
                    {op}
                  </option>
                ))}
              </Select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">
                Value
              </label>
              <Input
                value={(data as ConditionNodeData).value}
                onChange={(e) =>
                  updateNode(selectedNode.id, { value: e.target.value })
                }
                placeholder="Comparison value"
              />
            </div>
          </>
        )}

        {!isCondition && (
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-2">
              Outputs
            </label>
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
                        updateNode(selectedNode.id, {
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
                      onChange={(e) => updateEdgeLabel(edge.id, e.target.value)}
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
