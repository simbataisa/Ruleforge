export type FlowNodeType = 'CONDITION' | 'ACTION' | 'GATE' | 'FORK' | 'JOIN' | 'SUB_RULE';

export interface FlowNodeData {
  id: string;
  type: FlowNodeType;
  label?: string;
  expression?: string;       // for CONDITION nodes
  outputs?: Record<string, unknown>;  // for ACTION nodes
  gateType?: 'AND' | 'OR';  // for GATE nodes
  ruleId?: string;           // for SUB_RULE nodes
}

export interface FlowEdgeData {
  from: string;
  to: string;
  condition?: string;
  label?: string;
}

export interface FlowDefinition {
  nodes: FlowNodeData[];
  edges: FlowEdgeData[];
  startNodeId: string;
  maxIterations?: number;
}
