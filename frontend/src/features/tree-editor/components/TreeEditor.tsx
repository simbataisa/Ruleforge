import { useCallback, useMemo } from 'react';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  BackgroundVariant,
  Panel,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { GitBranch, Zap, Shield, Trash2 } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { useTreeEditorStore } from '@/stores/tree-editor.store';
import { useEditorStore } from '@/stores/editor.store';
import { ConditionNode } from './ConditionNode';
import { ActionNode } from './ActionNode';
import { GateNode } from './GateNode';
import { LabeledEdge } from './LabeledEdge';
import { PropertiesPanel } from './PropertiesPanel';
import type { NodeChange, EdgeChange, Connection, Node } from '@xyflow/react';

const nodeTypes = {
  conditionNode: ConditionNode,
  actionNode: ActionNode,
  gateNode: GateNode,
};

const edgeTypes = {
  labeledEdge: LabeledEdge,
};

export function TreeEditor() {
  const {
    nodes,
    edges,
    selectedNodeId,
    addConditionNode,
    addActionNode,
    addGateNode,
    deleteNode,
    onNodesChange,
    onEdgesChange,
    onConnect,
    setSelectedNodeId,
  } = useTreeEditorStore();

  const setDirty = useEditorStore((s) => s.setDirty);

  const handleNodesChange = useCallback(
    (changes: NodeChange[]) => {
      onNodesChange(changes);
      const hasMeaningfulChange = changes.some(
        (c) => c.type === 'position' || c.type === 'remove' || c.type === 'add',
      );
      if (hasMeaningfulChange) {
        setDirty(true);
      }
    },
    [onNodesChange, setDirty],
  );

  const handleEdgesChange = useCallback(
    (changes: EdgeChange[]) => {
      onEdgesChange(changes);
      setDirty(true);
    },
    [onEdgesChange, setDirty],
  );

  const handleConnect = useCallback(
    (connection: Connection) => {
      onConnect(connection);
      setDirty(true);
    },
    [onConnect, setDirty],
  );

  const handleNodeClick = useCallback(
    (_: React.MouseEvent, node: Node) => {
      setSelectedNodeId(node.id);
    },
    [setSelectedNodeId],
  );

  const handlePaneClick = useCallback(() => {
    setSelectedNodeId(null);
  }, [setSelectedNodeId]);

  const handleAddCondition = useCallback(() => {
    const x = 250 + Math.random() * 200;
    const y = 100 + Math.random() * 200;
    addConditionNode({ x, y });
    setDirty(true);
  }, [addConditionNode, setDirty]);

  const handleAddAction = useCallback(() => {
    const x = 250 + Math.random() * 200;
    const y = 300 + Math.random() * 200;
    addActionNode({ x, y });
    setDirty(true);
  }, [addActionNode, setDirty]);

  const handleAddGate = useCallback(() => {
    const x = 250 + Math.random() * 200;
    const y = 200 + Math.random() * 200;
    addGateNode({ x, y });
    setDirty(true);
  }, [addGateNode, setDirty]);

  const handleDeleteSelected = useCallback(() => {
    if (selectedNodeId) {
      deleteNode(selectedNodeId);
      setDirty(true);
    }
  }, [selectedNodeId, deleteNode, setDirty]);

  const defaultEdgeOptions = useMemo(
    () => ({
      type: 'labeledEdge',
      animated: false,
    }),
    [],
  );

  return (
    <div className="flex h-full">
      {/* Canvas */}
      <div className="flex-1 relative">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          nodeTypes={nodeTypes}
          edgeTypes={edgeTypes}
          onNodesChange={handleNodesChange}
          onEdgesChange={handleEdgesChange}
          onConnect={handleConnect}
          onNodeClick={handleNodeClick}
          onPaneClick={handlePaneClick}
          defaultEdgeOptions={defaultEdgeOptions}
          fitView
          deleteKeyCode={['Backspace', 'Delete']}
          className="bg-gray-50"
        >
          <Background
            variant={BackgroundVariant.Dots}
            gap={20}
            size={1}
            color="#d1d5db"
          />
          <Controls
            className="!bg-white !border-gray-200 !shadow-sm"
            showInteractive={false}
          />
          <MiniMap
            className="!bg-white !border-gray-200"
            nodeColor={(node) =>
              node.type === 'conditionNode' ? '#93c5fd' : node.type === 'gateNode' ? '#fcd34d' : '#86efac'
            }
            maskColor="rgba(0, 0, 0, 0.08)"
          />
          <Panel position="top-left">
            <div className="flex items-center gap-2 bg-white border border-gray-200 rounded-lg shadow-sm p-1.5">
              <Button
                size="sm"
                variant="outline"
                onClick={handleAddCondition}
                className="text-xs"
              >
                <GitBranch className="h-3.5 w-3.5 text-blue-600" />
                Condition
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={handleAddAction}
                className="text-xs"
              >
                <Zap className="h-3.5 w-3.5 text-green-600" />
                Action
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={handleAddGate}
                className="text-xs"
              >
                <Shield className="h-3.5 w-3.5 text-amber-600" />
                Gate
              </Button>
              <div className="h-5 w-px bg-gray-200" />
              <Button
                size="sm"
                variant="ghost"
                onClick={handleDeleteSelected}
                disabled={!selectedNodeId}
                className="text-xs text-red-600 hover:text-red-700 hover:bg-red-50"
              >
                <Trash2 className="h-3.5 w-3.5" />
                Delete
              </Button>
            </div>
          </Panel>
        </ReactFlow>
      </div>

      {/* Properties Panel */}
      <div className="w-72 flex-shrink-0 border-l border-gray-200 bg-white overflow-hidden">
        <PropertiesPanel />
      </div>
    </div>
  );
}
