import type { Node, Edge } from '@xyflow/react';

export interface ValidationIssue {
  nodeId?: string;
  severity: 'error' | 'warning';
  message: string;
}

export function validateWorkflow(nodes: Node[], edges: Edge[]): ValidationIssue[] {
  const issues: ValidationIssue[] = [];

  // Check for trigger node
  const triggerNodes = nodes.filter(n => n.type === 'trigger');
  if (triggerNodes.length === 0) {
    issues.push({ severity: 'error', message: 'Workflow must have at least one trigger node' });
  }

  // Check for disconnected nodes (except trigger which only needs outgoing)
  for (const node of nodes) {
    const hasIncoming = edges.some(e => e.target === node.id);
    const hasOutgoing = edges.some(e => e.source === node.id);

    if (node.type === 'trigger' && !hasOutgoing) {
      issues.push({ nodeId: node.id, severity: 'warning', message: `Trigger node "${node.data?.label}" has no outgoing connections` });
    } else if (node.type === 'output' && !hasIncoming) {
      issues.push({ nodeId: node.id, severity: 'warning', message: `Output node "${node.data?.label}" has no incoming connections` });
    } else if (node.type !== 'trigger' && !hasIncoming && !hasOutgoing) {
      issues.push({ nodeId: node.id, severity: 'warning', message: `Node "${node.data?.label}" is disconnected` });
    }
  }

  // Check rule nodes without ruleId
  for (const node of nodes) {
    if (node.type === 'rule' && !node.data?.ruleId) {
      issues.push({ nodeId: node.id, severity: 'error', message: `Rule node "${node.data?.label}" has no rule selected` });
    }
  }

  // Check condition nodes without expression
  for (const node of nodes) {
    if (node.type === 'condition') {
      const config = (node.data as Record<string, unknown>)?.config as Record<string, unknown> | undefined;
      if (!config?.expression) {
        issues.push({ nodeId: node.id, severity: 'warning', message: `Condition node "${node.data?.label}" has no expression` });
      }
    }
  }

  return issues;
}
