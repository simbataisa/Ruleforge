import { X, SendHorizontal, Download, CheckSquare, Layers, FolderInput, Trash2 } from 'lucide-react';

interface BatchActionBarProps {
  selectedCount: number;
  allDraft: boolean;
  onRequestApproval: () => void;
  onExport: () => void;
  onClearSelection: () => void;
  onAssignToSpace?: () => void;
  onMoveToFolder?: () => void;
  /** Shown only when every selected rule can be deleted. */
  onDelete?: () => void;
  exporting?: boolean;
}

export function BatchActionBar({
  selectedCount,
  allDraft,
  onRequestApproval,
  onExport,
  onClearSelection,
  onAssignToSpace,
  onMoveToFolder,
  onDelete,
  exporting = false,
}: BatchActionBarProps) {
  if (selectedCount === 0) return null;

  return (
    <div
      className="fixed bottom-6 left-1/2 z-50 -translate-x-1/2 animate-in slide-in-from-bottom-4 fade-in duration-200"
      role="toolbar"
      aria-label={`Batch actions for ${selectedCount} selected rules`}
    >
      <div className="flex items-center gap-3 rounded-xl bg-gray-900 px-4 py-2.5 shadow-2xl">
        <div className="flex items-center gap-2 text-white">
          <CheckSquare className="h-4 w-4" />
          <span className="text-sm font-medium">
            {selectedCount} rule{selectedCount !== 1 ? 's' : ''} selected
          </span>
        </div>

        <div className="h-6 w-px bg-gray-700" />

        <div className="flex items-center gap-1.5">
          {allDraft && (
            <button
              onClick={onRequestApproval}
              className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium text-white hover:bg-gray-800 transition-colors"
            >
              <SendHorizontal className="h-3.5 w-3.5" />
              Request Approval
            </button>
          )}
          {onAssignToSpace && (
            <button
              onClick={onAssignToSpace}
              className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium text-white hover:bg-gray-800 transition-colors"
            >
              <Layers className="h-3.5 w-3.5" />
              Assign to Space
            </button>
          )}
          {onMoveToFolder && (
            <button
              onClick={onMoveToFolder}
              className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium text-white hover:bg-gray-800 transition-colors"
            >
              <FolderInput className="h-3.5 w-3.5" />
              Move to Folder
            </button>
          )}
          <button
            onClick={onExport}
            disabled={exporting}
            className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium text-white hover:bg-gray-800 transition-colors disabled:opacity-50"
          >
            <Download className="h-3.5 w-3.5" />
            {exporting ? 'Exporting…' : 'Export'}
          </button>
          {onDelete && (
            <button
              onClick={onDelete}
              className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm font-medium text-red-300 hover:bg-gray-800 hover:text-red-200 transition-colors"
            >
              <Trash2 className="h-3.5 w-3.5" />
              Delete
            </button>
          )}
        </div>

        <button
          onClick={onClearSelection}
          className="ml-1 rounded-md p-1 text-gray-400 hover:text-white transition-colors"
          aria-label="Clear selection"
        >
          <X className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
