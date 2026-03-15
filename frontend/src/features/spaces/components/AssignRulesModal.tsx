import { useState, useMemo } from 'react';
import { Search, BookOpen } from 'lucide-react';
import { Modal } from '@/shared/components/Modal';
import { Button } from '@/shared/components/Button';
import { Spinner } from '@/shared/components/Spinner';
import { useRules, useMoveRule } from '@/api/rules.api';
import { useToastStore } from '@/stores/toast.store';
import type { Rule } from '@/api/types/rule.types';

interface AssignRulesModalProps {
  spaceId: string;
  currentRuleIds: Set<string>;
  onClose: () => void;
  onSuccess: () => void;
}

function getRuleTypeBadgeClass(type: string): string {
  switch (type) {
    case 'TABLE': return 'bg-blue-100 text-blue-700';
    case 'TREE': return 'bg-purple-100 text-purple-700';
    case 'SCORECARD': return 'bg-orange-100 text-orange-700';
    case 'FLOW': return 'bg-teal-100 text-teal-700';
    case 'SCRIPT': return 'bg-yellow-100 text-yellow-700';
    case 'DMN': return 'bg-indigo-100 text-indigo-700';
    case 'CEP': return 'bg-rose-100 text-rose-700';
    case 'ENSEMBLE': return 'bg-cyan-100 text-cyan-700';
    default: return 'bg-gray-100 text-gray-700';
  }
}

export function AssignRulesModal({
  spaceId,
  currentRuleIds,
  onClose,
  onSuccess,
}: AssignRulesModalProps) {
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [isAssigning, setIsAssigning] = useState(false);

  const addToast = useToastStore((s) => s.addToast);
  const moveRule = useMoveRule();

  // Fetch all org rules with a large page to avoid pagination complexity
  const { data: rulesData, isLoading } = useRules({ page: 0, size: 200 });

  const allRules: Rule[] = useMemo(() => rulesData?.content ?? [], [rulesData]);

  // Exclude rules already in this space
  const assignableRules = useMemo(
    () => allRules.filter((r) => !currentRuleIds.has(r.id)),
    [allRules, currentRuleIds],
  );

  const filteredRules = useMemo(() => {
    if (!search.trim()) return assignableRules;
    const q = search.toLowerCase();
    return assignableRules.filter(
      (r) =>
        r.name.toLowerCase().includes(q) ||
        r.type.toLowerCase().includes(q) ||
        (r.description ?? '').toLowerCase().includes(q),
    );
  }, [assignableRules, search]);

  const toggleRule = (id: string) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleAll = () => {
    if (selected.size === filteredRules.length && filteredRules.length > 0) {
      setSelected(new Set());
    } else {
      setSelected(new Set(filteredRules.map((r) => r.id)));
    }
  };

  const handleAssign = async () => {
    if (selected.size === 0) return;
    setIsAssigning(true);

    let successCount = 0;
    let failCount = 0;

    for (const ruleId of selected) {
      try {
        await moveRule.mutateAsync({ ruleId, targetSpaceId: spaceId });
        successCount++;
      } catch {
        failCount++;
      }
    }

    setIsAssigning(false);

    if (successCount > 0) {
      addToast({
        type: 'success',
        title: `${successCount} rule${successCount !== 1 ? 's' : ''} assigned to space`,
      });
    }
    if (failCount > 0) {
      addToast({
        type: 'error',
        title: `${failCount} rule${failCount !== 1 ? 's' : ''} failed to assign`,
      });
    }
    if (successCount > 0) {
      onSuccess();
      onClose();
    }
  };

  return (
    <Modal
      open
      onClose={onClose}
      title="Assign Rules to Space"
      footer={
        <div className="flex items-center justify-between">
          <span className="text-sm text-gray-500">
            {selected.size > 0 ? `${selected.size} rule${selected.size !== 1 ? 's' : ''} selected` : 'Select rules to assign'}
          </span>
          <div className="flex gap-3">
            <Button variant="outline" onClick={onClose} disabled={isAssigning}>
              Cancel
            </Button>
            <Button
              onClick={handleAssign}
              disabled={selected.size === 0 || isAssigning}
            >
              {isAssigning ? 'Assigning...' : selected.size > 0 ? `Assign (${selected.size})` : 'Assign'}
            </Button>
          </div>
        </div>
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-gray-600">
          Select existing rules to assign to this space. Rules will be moved from their current space.
        </p>

        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-gray-400" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search rules by name, type, or description..."
            className="w-full pl-9 pr-3 py-2 text-sm border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            autoFocus
            aria-label="Search rules to assign"
          />
        </div>

        {/* Rules list */}
        <div className="border border-gray-200 rounded-md overflow-hidden">
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <Spinner />
            </div>
          ) : filteredRules.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 gap-2 text-gray-400">
              <BookOpen className="h-8 w-8" />
              <p className="text-sm text-center">
                {assignableRules.length === 0
                  ? 'All available rules are already in this space.'
                  : 'No rules match your search.'}
              </p>
            </div>
          ) : (
            <>
              {/* Select all */}
              <div className="flex items-center gap-3 px-3 py-2 border-b border-gray-100 bg-gray-50">
                <input
                  type="checkbox"
                  id="assign-select-all"
                  checked={selected.size === filteredRules.length && filteredRules.length > 0}
                  onChange={toggleAll}
                  className="h-3.5 w-3.5 rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <label
                  htmlFor="assign-select-all"
                  className="text-xs text-gray-500 select-none cursor-pointer"
                >
                  Select all ({filteredRules.length})
                </label>
              </div>

              {/* Rule rows */}
              <div className="max-h-64 overflow-y-auto divide-y divide-gray-50">
                {filteredRules.map((rule) => (
                  <label
                    key={rule.id}
                    className="flex items-center gap-3 px-3 py-2.5 hover:bg-gray-50 cursor-pointer"
                  >
                    <input
                      type="checkbox"
                      checked={selected.has(rule.id)}
                      onChange={() => toggleRule(rule.id)}
                      className="h-3.5 w-3.5 rounded border-gray-300 text-blue-600 focus:ring-blue-500 shrink-0"
                    />
                    <div className="flex-1 min-w-0">
                      <div className="text-sm font-medium text-gray-900 truncate">{rule.name}</div>
                      {rule.description && (
                        <div className="text-xs text-gray-400 truncate">{rule.description}</div>
                      )}
                    </div>
                    <span
                      className={`inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium shrink-0 ${getRuleTypeBadgeClass(rule.type)}`}
                    >
                      {rule.type}
                    </span>
                  </label>
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </Modal>
  );
}
