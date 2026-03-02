import { useCallback, useRef, useMemo, useState, useEffect, type DragEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ReactFlow,
  Controls,
  MiniMap,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  Handle,
  Position,
  type Node,
  type Edge,
  type NodeTypes,
  type OnConnect,
  type NodeProps,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { ArrowLeft, Save, ToggleLeft, ToggleRight, Clock, ArrowRightCircle, Webhook, MousePointerClick, Radio } from 'lucide-react';
import { Button } from '@/shared/components/Button';
import { Spinner } from '@/shared/components/Spinner';
import { useToastStore } from '@/stores/toast.store';
import { cn } from '@/shared/utils/cn';
import { useWorkflow, useUpdateWorkflow, useCreateWorkflow, type WorkflowNode, type TriggerType } from '@/api/workflows.api';
import { RuleNode } from '../components/RuleNode';
import { NodePalette } from '../components/NodePalette';
import { NodeEditPanel } from '../components/NodeEditPanel';

// --- Custom Node: Trigger ---

type TriggerNodeData = {
  label: string;
  config?: Record<string, unknown>;
};

type TriggerNodeType = Node<TriggerNodeData, 'trigger'>;

const TRIGGER_TYPE_STYLES: Record<string, { bg: string; border: string; text: string; icon: string; handle: string }> = {
  CRON: { bg: 'bg-green-50', border: 'border-green-100', text: 'text-green-700', icon: 'text-green-600', handle: '!bg-green-500' },
  MANUAL: { bg: 'bg-blue-50', border: 'border-blue-100', text: 'text-blue-700', icon: 'text-blue-600', handle: '!bg-blue-500' },
  WEBHOOK: { bg: 'bg-purple-50', border: 'border-purple-100', text: 'text-purple-700', icon: 'text-purple-600', handle: '!bg-purple-500' },
  EVENT: { bg: 'bg-orange-50', border: 'border-orange-100', text: 'text-orange-700', icon: 'text-orange-600', handle: '!bg-orange-500' },
};

function getTriggerIcon(triggerType?: string) {
  switch (triggerType) {
    case 'MANUAL': return MousePointerClick;
    case 'WEBHOOK': return Webhook;
    case 'EVENT': return Radio;
    default: return Clock;
  }
}

function TriggerNode({ data, selected }: NodeProps<TriggerNodeType>) {
  const triggerType = (data.config?.triggerType as string) ?? 'CRON';
  const style = TRIGGER_TYPE_STYLES[triggerType] ?? TRIGGER_TYPE_STYLES.CRON;
  const Icon = getTriggerIcon(triggerType);

  return (
    <div className={cn(
      "rounded-lg border border-gray-200 bg-white shadow-md min-w-[180px] cursor-pointer hover:shadow-lg transition-shadow",
      selected && "ring-2 ring-indigo-500 ring-offset-2"
    )}>
      <div className={cn(style.bg, `border-b ${style.border}`, 'px-3 py-1.5 rounded-t-lg flex items-center gap-1.5')}>
        <Icon className={cn('h-3.5 w-3.5', style.icon)} />
        <span className={cn('text-xs font-semibold', style.text)}>Trigger</span>
        <span className={cn('ml-auto inline-flex items-center rounded-full px-1.5 py-0.5 text-[10px] font-medium', style.bg, style.text)}>
          {triggerType}
        </span>
      </div>
      <div className="px-3 py-2">
        <p className="text-sm font-medium text-gray-900">{data.label}</p>
        {data.config?.cronExpression != null && (
          <code className="text-[11px] text-gray-500 mt-0.5 block">
            {String(data.config.cronExpression)}
          </code>
        )}
        {data.config?.webhookUrl != null && (
          <code className="text-[11px] text-gray-500 mt-0.5 block truncate max-w-[160px]">
            {String(data.config.webhookUrl)}
          </code>
        )}
        {data.config?.eventTopic != null && (
          <code className="text-[11px] text-gray-500 mt-0.5 block">
            {String(data.config.eventTopic)}
          </code>
        )}
      </div>
      <Handle type="source" position={Position.Bottom} className={style.handle} />
    </div>
  );
}

// --- Custom Node: Output ---

type OutputNodeData = {
  label: string;
  config?: Record<string, unknown>;
};

type OutputNodeType = Node<OutputNodeData, 'output'>;

function OutputNode({ data, selected }: NodeProps<OutputNodeType>) {
  return (
    <div className={cn(
      "rounded-lg border border-gray-200 bg-white shadow-md min-w-[180px] cursor-pointer hover:shadow-lg transition-shadow",
      selected && "ring-2 ring-indigo-500 ring-offset-2"
    )}>
      <Handle type="target" position={Position.Top} className="!bg-purple-500" />
      <div className="bg-purple-50 border-b border-purple-100 px-3 py-1.5 rounded-t-lg flex items-center gap-1.5">
        <ArrowRightCircle className="h-3.5 w-3.5 text-purple-600" />
        <span className="text-xs font-semibold text-purple-700">Output</span>
      </div>
      <div className="px-3 py-2">
        <p className="text-sm font-medium text-gray-900">{data.label}</p>
        {data.config?.outputType != null && (
          <span className="inline-flex items-center rounded-full bg-purple-100 px-2 py-0.5 text-[10px] font-semibold text-purple-700">
            {String(data.config.outputType)}
          </span>
        )}
      </div>
    </div>
  );
}

// --- Custom Node: Condition ---

type ConditionNodeData = {
  label: string;
  config?: Record<string, unknown>;
};

type ConditionNodeType = Node<ConditionNodeData, 'condition'>;

function ConditionNode({ data, selected }: NodeProps<ConditionNodeType>) {
  return (
    <div className={cn(
      "rounded-lg border border-gray-200 bg-white shadow-md min-w-[180px] cursor-pointer hover:shadow-lg transition-shadow",
      selected && "ring-2 ring-indigo-500 ring-offset-2"
    )}>
      <Handle type="target" position={Position.Top} className="!bg-yellow-500" />
      <div className="bg-yellow-50 border-b border-yellow-100 px-3 py-1.5 rounded-t-lg">
        <span className="text-xs font-semibold text-yellow-700">Condition</span>
      </div>
      <div className="px-3 py-2">
        <p className="text-sm font-medium text-gray-900">{data.label}</p>
        {data.config?.expression != null && (
          <code className="text-[11px] text-gray-500 mt-0.5 block truncate max-w-[160px]">
            {String(data.config.expression)}
          </code>
        )}
      </div>
      <Handle type="source" position={Position.Bottom} className="!bg-yellow-500" />
    </div>
  );
}

// --- Node type registry ---

const nodeTypes: NodeTypes = {
  trigger: TriggerNode,
  rule: RuleNode,
  condition: ConditionNode,
  output: OutputNode,
};

// --- Auto-layout helper ---

function autoLayout(wfNodes: WorkflowNode[]): Node[] {
  return wfNodes.map((n, i) => ({
    id: n.id,
    type: n.type,
    position: n.position.x === 0 && n.position.y === 0
      ? { x: 250, y: i * 150 }
      : n.position,
    data: n.data,
  }));
}

// --- Main Page ---

export function WorkflowBuilderPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const addToast = useToastStore((s) => s.addToast);
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const isNew = id === 'new';

  const { data: workflow, isLoading } = useWorkflow(id ?? '');
  const updateWorkflow = useUpdateWorkflow();
  const createWorkflow = useCreateWorkflow();

  // Handle creating a new workflow
  useEffect(() => {
    if (isNew) {
      createWorkflow.mutateAsync({
        name: 'Untitled Workflow',
        description: '',
        definition: {
          nodes: [
            { id: 'trigger-1', type: 'trigger', position: { x: 250, y: 0 }, data: { label: 'Start' } },
            { id: 'output-1', type: 'output', position: { x: 250, y: 300 }, data: { label: 'Output' } },
          ],
          edges: [],
        },
      }).then((created) => {
        navigate(`/workflows/${created.id}`, { replace: true });
      }).catch(() => {
        addToast({ type: 'error', title: 'Failed to create workflow' });
        navigate('/schedules');
      });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isNew]);

  const initialNodes = useMemo(() => {
    if (!workflow?.definition?.nodes) return [];
    return autoLayout(workflow.definition.nodes);
  }, [workflow]);

  const initialEdges = useMemo<Edge[]>(() => {
    if (!workflow?.definition?.edges) return [];
    return workflow.definition.edges.map((e) => ({
      id: e.id,
      source: e.source,
      target: e.target,
      label: e.label,
      animated: true,
    }));
  }, [workflow]);

  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
  const [editingNode, setEditingNode] = useState<Node | null>(null);
  const [isEditingName, setIsEditingName] = useState(false);
  const [editName, setEditName] = useState('');

  // Sync when workflow loads
  useMemo(() => {
    if (initialNodes.length > 0) setNodes(initialNodes);
  }, [initialNodes, setNodes]);

  useMemo(() => {
    if (initialEdges.length > 0) setEdges(initialEdges);
  }, [initialEdges, setEdges]);

  const onConnect: OnConnect = useCallback(
    (params) => setEdges((eds) => addEdge({ ...params, animated: true }, eds)),
    [setEdges],
  );

  const onNodeDoubleClick = useCallback((_event: React.MouseEvent, node: Node) => {
    setEditingNode(node);
  }, []);

  const handleNodeUpdate = useCallback((nodeId: string, newData: Record<string, unknown>) => {
    setNodes((nds) => nds.map((n) =>
      n.id === nodeId ? { ...n, data: { ...n.data, ...newData } } : n,
    ));
    // Update the editing node reference so the panel reflects changes
    setEditingNode((prev) =>
      prev && prev.id === nodeId ? { ...prev, data: { ...prev.data, ...newData } } : prev,
    );
  }, [setNodes]);

  const onDragOver = useCallback((event: DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  const onDrop = useCallback(
    (event: DragEvent) => {
      event.preventDefault();
      const nodeType = event.dataTransfer.getData('application/reactflow');
      if (!nodeType) return;

      const bounds = reactFlowWrapper.current?.getBoundingClientRect();
      if (!bounds) return;

      const position = {
        x: event.clientX - bounds.left,
        y: event.clientY - bounds.top,
      };

      const newNode: Node = {
        id: `${nodeType}-${Date.now()}`,
        type: nodeType,
        position,
        data: { label: `New ${nodeType}` },
      };

      setNodes((nds) => [...nds, newNode]);
    },
    [setNodes],
  );

  const handleStartEditName = () => {
    if (workflow) {
      setEditName(workflow.name);
      setIsEditingName(true);
    }
  };

  const handleSaveName = async () => {
    if (!id || !editName.trim()) {
      setIsEditingName(false);
      return;
    }
    try {
      await updateWorkflow.mutateAsync({ id, data: { name: editName.trim() } });
      setIsEditingName(false);
    } catch {
      addToast({ type: 'error', title: 'Failed to rename workflow' });
    }
  };

  const handleSave = async () => {
    if (!id) return;
    try {
      // Extract trigger config from the trigger node's data
      const triggerNode = nodes.find((n) => n.type === 'trigger');
      const triggerConfig = (triggerNode?.data as Record<string, unknown>)?.config as Record<string, unknown> | undefined;
      const triggerType = (triggerConfig?.triggerType as TriggerType) ?? 'CRON';

      const definition = {
        nodes: nodes.map((n) => ({
          id: n.id,
          type: n.type as WorkflowNode['type'],
          position: n.position,
          data: n.data as WorkflowNode['data'],
        })),
        edges: edges.map((e) => ({
          id: e.id,
          source: e.source,
          target: e.target,
          label: typeof e.label === 'string' ? e.label : undefined,
        })),
      };
      await updateWorkflow.mutateAsync({
        id,
        data: { definition, triggerType, triggerConfig: triggerConfig ?? {} },
      });
      addToast({ type: 'success', title: 'Workflow saved' });
    } catch {
      addToast({ type: 'error', title: 'Failed to save workflow' });
    }
  };

  const handleToggleEnabled = async () => {
    if (!id || !workflow) return;
    try {
      await updateWorkflow.mutateAsync({
        id,
        data: { enabled: !workflow.enabled },
      });
      addToast({
        type: 'success',
        title: workflow.enabled ? 'Workflow disabled' : 'Workflow enabled',
      });
    } catch {
      addToast({ type: 'error', title: 'Failed to toggle workflow' });
    }
  };

  if (isNew || isLoading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!workflow) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-3">
        <p className="text-gray-500">Workflow not found</p>
        <Button variant="outline" onClick={() => navigate('/schedules')}>
          Back to Schedules
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-[calc(100vh-64px)]">
      {/* Toolbar */}
      <div className="flex items-center justify-between border-b border-gray-200 bg-white px-4 py-2">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/schedules')}
            className="rounded p-1 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
          >
            <ArrowLeft className="h-5 w-5" />
          </button>
          <div>
            {isEditingName ? (
              <input
                type="text"
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                onBlur={handleSaveName}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') handleSaveName();
                  if (e.key === 'Escape') setIsEditingName(false);
                }}
                autoFocus
                className="text-lg font-semibold text-gray-900 border-b-2 border-indigo-500 bg-transparent outline-none px-0 py-0"
              />
            ) : (
              <h1
                className="text-lg font-semibold text-gray-900 cursor-pointer hover:text-indigo-600 transition-colors"
                onClick={handleStartEditName}
                title="Click to rename"
              >
                {workflow.name}
              </h1>
            )}
            {workflow.description && (
              <p className="text-xs text-gray-500">{workflow.description}</p>
            )}
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={handleToggleEnabled}
            className={cn(
              'flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium transition-colors',
              workflow.enabled
                ? 'bg-green-50 text-green-700 hover:bg-green-100'
                : 'bg-gray-50 text-gray-500 hover:bg-gray-100',
            )}
          >
            {workflow.enabled ? (
              <ToggleRight className="h-4 w-4" />
            ) : (
              <ToggleLeft className="h-4 w-4" />
            )}
            {workflow.enabled ? 'Enabled' : 'Disabled'}
          </button>
          <Button onClick={handleSave} loading={updateWorkflow.isPending}>
            <Save className="h-4 w-4" />
            Save
          </Button>
        </div>
      </div>

      {/* Canvas */}
      <div className="flex flex-1 overflow-hidden">
        <NodePalette />
        <div ref={reactFlowWrapper} className="flex-1">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeDoubleClick={onNodeDoubleClick}
            onDragOver={onDragOver}
            onDrop={onDrop}
            nodeTypes={nodeTypes}
            fitView
          >
            <Controls />
            <MiniMap
              nodeStrokeWidth={3}
              className="!bg-gray-50"
            />
            <Background gap={16} size={1} />
          </ReactFlow>
        </div>
        {editingNode && (
          <NodeEditPanel
            node={editingNode}
            onClose={() => setEditingNode(null)}
            onNodeUpdate={handleNodeUpdate}
            workflowId={id}
          />
        )}
      </div>
    </div>
  );
}
