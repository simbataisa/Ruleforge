import { useState, useCallback, memo } from 'react';
import { useFlowEditorStore } from '../../../stores/flow-editor.store';
import { FlowNodeType, FlowNodeData } from '../types';
import { AddEdgeModal } from './AddEdgeModal';

const NODE_COLORS: Record<FlowNodeType, string> = {
  CONDITION: 'bg-blue-100 border-blue-400',
  ACTION: 'bg-green-100 border-green-400',
  GATE: 'bg-yellow-100 border-yellow-400',
  FORK: 'bg-purple-100 border-purple-400',
  JOIN: 'bg-purple-100 border-purple-400',
  SUB_RULE: 'bg-orange-100 border-orange-400',
};

const NODE_ICONS: Record<FlowNodeType, string> = {
  CONDITION: '?',
  ACTION: '\u2192',
  GATE: '\u2227',
  FORK: '\u2442',
  JOIN: '\u2443',
  SUB_RULE: '\u229e',
};

interface FlowNodeCardProps {
  node: FlowNodeData;
  isStart: boolean;
  isSelected: boolean;
  onSelect: () => void;
  onUpdate: (data: Partial<FlowNodeData>) => void;
  onDelete: () => void;
  onSetStart: () => void;
}

const FlowNodeCard = memo(function FlowNodeCard({ node, isStart, isSelected, onSelect, onUpdate, onDelete, onSetStart }: FlowNodeCardProps) {
  return (
    <div
      className={`relative border-2 rounded-lg p-3 cursor-pointer transition-all ${NODE_COLORS[node.type]} ${isSelected ? 'ring-2 ring-blue-500' : ''}`}
      onClick={onSelect}
    >
      {isStart && (
        <span className="absolute -top-2 -left-2 bg-green-500 text-white text-xs px-1 rounded">START</span>
      )}
      <div className="flex items-center gap-2 mb-1">
        <span className="text-lg font-bold">{NODE_ICONS[node.type]}</span>
        <span className="text-xs font-semibold text-gray-600 uppercase">{node.type}</span>
      </div>
      <input
        className="w-full text-sm font-medium bg-transparent border-none outline-none"
        value={node.label || ''}
        onChange={e => onUpdate({ label: e.target.value })}
        onClick={e => e.stopPropagation()}
        placeholder="Node label"
      />
      {node.type === 'CONDITION' && (
        <input
          className="w-full text-xs bg-white/50 border rounded px-1 mt-1"
          value={node.expression || ''}
          onChange={e => onUpdate({ expression: e.target.value })}
          onClick={e => e.stopPropagation()}
          placeholder="Expression (e.g. input.age >= 18)"
        />
      )}
      {node.type === 'ACTION' && (
        <textarea
          className="w-full text-xs bg-white/50 border rounded px-1 mt-1 font-mono"
          value={JSON.stringify(node.outputs || {}, null, 2)}
          onChange={e => {
            try { onUpdate({ outputs: JSON.parse(e.target.value) }); } catch (_) { /* ignore invalid JSON during editing */ }
          }}
          onClick={e => e.stopPropagation()}
          rows={2}
          placeholder='{"result": true}'
        />
      )}
      <div className="flex gap-1 mt-2">
        {!isStart && (
          <button
            className="text-xs px-1 py-0.5 bg-green-200 rounded hover:bg-green-300"
            onClick={e => { e.stopPropagation(); onSetStart(); }}
          >Set Start</button>
        )}
        <button
          className="text-xs px-1 py-0.5 bg-red-200 rounded hover:bg-red-300 ml-auto"
          onClick={e => { e.stopPropagation(); onDelete(); }}
        >x</button>
      </div>
    </div>
  );
});

interface FlowEditorProps {
  readOnly?: boolean;
}

export function FlowEditor({ readOnly = false }: FlowEditorProps) {
  const { definition, selectedNodeId, addNode, updateNode, deleteNode, addEdge, deleteEdge, setStartNode, selectNode } = useFlowEditorStore();
  const [showAddEdgeModal, setShowAddEdgeModal] = useState(false);

  const handleAddNode = useCallback((type: FlowNodeType) => {
    if (!readOnly) addNode(type);
  }, [readOnly, addNode]);

  const handleUpdateNode = useCallback((nodeId: string, data: Partial<FlowNodeData>) => {
    if (!readOnly) updateNode(nodeId, data);
  }, [readOnly, updateNode]);

  const handleDeleteNode = useCallback((nodeId: string) => {
    if (!readOnly) deleteNode(nodeId);
  }, [readOnly, deleteNode]);

  const handleSetStartNode = useCallback((nodeId: string) => {
    if (!readOnly) setStartNode(nodeId);
  }, [readOnly, setStartNode]);

  const handleSelectNode = useCallback((nodeId: string) => {
    selectNode(nodeId);
  }, [selectNode]);

  const handleAddEdge = useCallback(({ from, to, condition }: { from: string; to: string; condition?: string }) => {
    addEdge({ from, to, condition });
  }, [addEdge]);

  const handleCloseEdgeModal = useCallback(() => {
    setShowAddEdgeModal(false);
  }, []);

  const nodeTypes: FlowNodeType[] = ['CONDITION', 'ACTION', 'GATE', 'FORK', 'JOIN', 'SUB_RULE'];

  return (
    <div className="flex flex-col h-full gap-4">
      {!readOnly && (
        <div className="flex flex-wrap gap-2 p-3 bg-gray-50 border rounded-lg">
          <span className="text-sm font-medium text-gray-700 self-center">Add node:</span>
          {nodeTypes.map(type => (
            <button
              key={type}
              onClick={() => handleAddNode(type)}
              className={`text-xs px-2 py-1 rounded border font-medium ${NODE_COLORS[type]}`}
            >
              {NODE_ICONS[type]} {type}
            </button>
          ))}
          <button
            onClick={() => setShowAddEdgeModal(true)}
            className="text-xs px-2 py-1 rounded border bg-gray-100 border-gray-300 ml-auto"
          >
            + Edge
          </button>
        </div>
      )}

      <div className="flex gap-4 flex-1 overflow-auto">
        <div className="flex-1">
          <div className="text-xs font-semibold text-gray-500 mb-2 uppercase">Nodes ({definition.nodes.length})</div>
          <div className="grid grid-cols-2 gap-3">
            {definition.nodes.map(node => (
              <FlowNodeCard
                key={node.id}
                node={node}
                isStart={node.id === definition.startNodeId}
                isSelected={selectedNodeId === node.id}
                onSelect={() => handleSelectNode(node.id)}
                onUpdate={(data) => handleUpdateNode(node.id, data)}
                onDelete={() => handleDeleteNode(node.id)}
                onSetStart={() => handleSetStartNode(node.id)}
              />
            ))}
          </div>
        </div>

        <div className="w-56 shrink-0">
          <div className="text-xs font-semibold text-gray-500 mb-2 uppercase">Edges ({definition.edges.length})</div>
          <div className="space-y-2">
            {definition.edges.map((edge, i) => (
              <div key={i} className="flex items-center gap-1 p-2 bg-gray-50 border rounded text-xs">
                <span className="font-mono">{edge.from}</span>
                <span>{'\u2192'}</span>
                <span className="font-mono">{edge.to}</span>
                {edge.condition && <span className="text-gray-500 ml-1">[{edge.condition}]</span>}
                {!readOnly && (
                  <button
                    className="ml-auto text-red-400 hover:text-red-600"
                    onClick={() => deleteEdge(edge.from, edge.to)}
                  >x</button>
                )}
              </div>
            ))}
          </div>
          <div className="mt-4 p-2 bg-blue-50 border border-blue-200 rounded text-xs">
            <div className="font-medium text-blue-700">Start Node</div>
            <div className="font-mono mt-1">{definition.startNodeId || 'None'}</div>
          </div>
        </div>
      </div>

      <AddEdgeModal
        open={showAddEdgeModal}
        onClose={handleCloseEdgeModal}
        onSubmit={handleAddEdge}
        nodes={definition.nodes}
      />
    </div>
  );
}

export default FlowEditor;
