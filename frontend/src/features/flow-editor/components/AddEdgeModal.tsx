import { useState } from 'react';
import { Button } from '@/shared/components/Button';
import { Input } from '@/shared/components/Input';
import { Modal } from '@/shared/components/Modal';
import { FlowNodeData } from '../types';

interface AddEdgeModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: { from: string; to: string; condition: string }) => void;
  nodes: FlowNodeData[];
}

export function AddEdgeModal({ open, onClose, onSubmit, nodes }: AddEdgeModalProps) {
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [condition, setCondition] = useState('true');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!from || !to) return;
    onSubmit({ from, to, condition: condition || 'true' });
    handleClose();
  };

  const handleClose = () => {
    setFrom('');
    setTo('');
    setCondition('true');
    onClose();
  };

  return (
    <Modal open={open} onClose={handleClose} title="Add Edge">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label htmlFor="edge-from" className="block text-sm font-medium text-gray-700 mb-1">
            Source Node
          </label>
          <select
            id="edge-from"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            required
          >
            <option value="">Select source node</option>
            {nodes.map((node) => (
              <option key={node.id} value={node.id}>
                {node.label || node.id} ({node.type})
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="edge-to" className="block text-sm font-medium text-gray-700 mb-1">
            Target Node
          </label>
          <select
            id="edge-to"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm shadow-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
            required
          >
            <option value="">Select target node</option>
            {nodes.map((node) => (
              <option key={node.id} value={node.id}>
                {node.label || node.id} ({node.type})
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="edge-condition" className="block text-sm font-medium text-gray-700 mb-1">
            Condition (optional)
          </label>
          <Input
            id="edge-condition"
            value={condition}
            onChange={(e) => setCondition(e.target.value)}
            placeholder="e.g. true, false, input.age >= 18"
          />
        </div>
        <div className="flex justify-end gap-3 pt-2">
          <Button type="button" variant="outline" onClick={handleClose}>
            Cancel
          </Button>
          <Button type="submit" disabled={!from || !to}>
            Add Edge
          </Button>
        </div>
      </form>
    </Modal>
  );
}
