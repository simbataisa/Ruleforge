import { useCallback, useRef } from 'react';
import type { Node, Edge } from '@xyflow/react';

interface Snapshot {
  nodes: Node[];
  edges: Edge[];
}

const MAX_HISTORY = 50;

export function useUndoRedo() {
  const historyRef = useRef<Snapshot[]>([]);
  const pointerRef = useRef(-1);

  const pushState = useCallback((nodes: Node[], edges: Edge[]) => {
    const snapshot: Snapshot = {
      nodes: JSON.parse(JSON.stringify(nodes)),
      edges: JSON.parse(JSON.stringify(edges)),
    };

    // Discard any redo states ahead of current pointer
    historyRef.current = historyRef.current.slice(0, pointerRef.current + 1);
    historyRef.current.push(snapshot);

    // Enforce max history
    if (historyRef.current.length > MAX_HISTORY) {
      historyRef.current = historyRef.current.slice(historyRef.current.length - MAX_HISTORY);
    }

    pointerRef.current = historyRef.current.length - 1;
  }, []);

  const undo = useCallback((): Snapshot | null => {
    if (pointerRef.current <= 0) return null;
    pointerRef.current -= 1;
    return historyRef.current[pointerRef.current];
  }, []);

  const redo = useCallback((): Snapshot | null => {
    if (pointerRef.current >= historyRef.current.length - 1) return null;
    pointerRef.current += 1;
    return historyRef.current[pointerRef.current];
  }, []);

  const canUndo = useCallback(() => pointerRef.current > 0, []);
  const canRedo = useCallback(() => pointerRef.current < historyRef.current.length - 1, []);

  return { pushState, undo, redo, canUndo, canRedo };
}
