import { useState, useRef, useCallback } from 'react';
import { FolderPlus, ChevronDown, ChevronRight, Layers } from 'lucide-react';
import { FolderTree } from './FolderTree';
import { FilterPanel } from './FilterPanel';
import { cn } from '@/shared/utils/cn';
import type { Folder } from '@/api/types/folder.types';
import type { TagCount } from '@/api/types/rule.types';

interface RulesExplorerProps {
  folders: Folder[];
  selectedFolderId: string | null;
  onFolderSelect: (id: string | null) => void;
  onCreateFolder: (parentId?: string) => void;
  selectedTypes: string[];
  onTypeChange: (types: string[]) => void;
  selectedStatuses: string[];
  onStatusChange: (statuses: string[]) => void;
  selectedTags: string[];
  onTagChange: (tags: string[]) => void;
  tags: TagCount[];
  datePreset: number | null;
  onDatePresetChange: (days: number | null) => void;
  activeFilterCount: number;
}

const MIN_WIDTH = 180;
const MAX_WIDTH = 460;
const DEFAULT_WIDTH = 260;

export function RulesExplorer({
  folders,
  selectedFolderId,
  onFolderSelect,
  onCreateFolder,
  selectedTypes,
  onTypeChange,
  selectedStatuses,
  onStatusChange,
  selectedTags,
  onTagChange,
  tags,
  datePreset,
  onDatePresetChange,
  activeFilterCount,
}: RulesExplorerProps) {
  const [width, setWidth] = useState(() => {
    const saved = localStorage.getItem('ruleforge:explorer-width');
    return saved ? Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, parseInt(saved))) : DEFAULT_WIDTH;
  });
  const [collapsed, setCollapsed] = useState(() =>
    localStorage.getItem('ruleforge:explorer-collapsed') === 'true'
  );
  const [foldersOpen, setFoldersOpen] = useState(() =>
    localStorage.getItem('ruleforge:explorer-folders-open') !== 'false'
  );
  const [filtersOpen, setFiltersOpen] = useState(() =>
    localStorage.getItem('ruleforge:explorer-filters-open') !== 'false'
  );
  const [splitRatio, setSplitRatio] = useState(() => {
    const saved = localStorage.getItem('ruleforge:explorer-split');
    return saved ? parseFloat(saved) : 0.5;
  });

  const containerRef = useRef<HTMLDivElement>(null);
  const widthDragRef = useRef<{ startX: number; startWidth: number } | null>(null);
  const splitDragRef = useRef<{ startY: number; startRatio: number; containerH: number } | null>(null);

  const handleWidthDragStart = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    widthDragRef.current = { startX: e.clientX, startWidth: width };
    const onMove = (ev: MouseEvent) => {
      if (!widthDragRef.current) return;
      const newW = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH,
        widthDragRef.current.startWidth + (ev.clientX - widthDragRef.current.startX)));
      setWidth(newW);
      localStorage.setItem('ruleforge:explorer-width', String(newW));
    };
    const onUp = () => {
      widthDragRef.current = null;
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
    };
    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
  }, [width]);

  const handleSplitDragStart = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    const containerH = containerRef.current?.clientHeight ?? 400;
    splitDragRef.current = { startY: e.clientY, startRatio: splitRatio, containerH };
    const onMove = (ev: MouseEvent) => {
      if (!splitDragRef.current) return;
      const delta = (ev.clientY - splitDragRef.current.startY) / splitDragRef.current.containerH;
      const newRatio = Math.max(0.2, Math.min(0.8, splitDragRef.current.startRatio + delta));
      setSplitRatio(newRatio);
      localStorage.setItem('ruleforge:explorer-split', String(newRatio));
    };
    const onUp = () => {
      splitDragRef.current = null;
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
    };
    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
  }, [splitRatio]);

  const toggleFolders = () => {
    const next = !foldersOpen;
    setFoldersOpen(next);
    localStorage.setItem('ruleforge:explorer-folders-open', String(next));
  };
  const toggleFilters = () => {
    const next = !filtersOpen;
    setFiltersOpen(next);
    localStorage.setItem('ruleforge:explorer-filters-open', String(next));
  };
  const toggleCollapsed = () => {
    const next = !collapsed;
    setCollapsed(next);
    localStorage.setItem('ruleforge:explorer-collapsed', String(next));
  };

  return (
    <div
      ref={containerRef}
      className="relative flex flex-col h-full overflow-hidden bg-[#f8f8f8] border-r border-gray-200 flex-shrink-0"
      style={{ width: collapsed ? 0 : width }}
    >
      {!collapsed && (
        <>
          {/* Panel header */}
          <div className="flex items-center h-8 px-3 shrink-0 border-b border-gray-200 bg-[#f3f3f3]">
            <span className="text-[11px] font-semibold uppercase tracking-[0.08em] text-gray-500 select-none">
              Explorer
            </span>
            <div className="ml-auto flex items-center gap-0.5">
              <button
                title="New Folder"
                onClick={() => onCreateFolder()}
                className="p-1 rounded hover:bg-gray-200 transition-colors"
              >
                <FolderPlus className="h-3.5 w-3.5 text-gray-400 hover:text-gray-600" />
              </button>
            </div>
          </div>

          {/* Sections container */}
          <div className="flex flex-col flex-1 overflow-hidden">

            {/* FOLDERS section */}
            <div
              className="flex flex-col overflow-hidden"
              style={
                foldersOpen && filtersOpen
                  ? { flex: splitRatio }
                  : foldersOpen && !filtersOpen
                  ? { flex: 1 }
                  : { flex: 'none' }
              }
            >
              <button
                onClick={toggleFolders}
                className="flex items-center gap-1 h-6 w-full px-2 shrink-0 hover:bg-gray-200/60 transition-colors group"
              >
                {foldersOpen
                  ? <ChevronDown className="h-3 w-3 text-gray-500 shrink-0" />
                  : <ChevronRight className="h-3 w-3 text-gray-500 shrink-0" />
                }
                <span className="text-[11px] font-semibold uppercase tracking-[0.08em] text-gray-500 select-none">
                  Folders
                </span>
              </button>

              {foldersOpen && (
                <div className="flex-1 overflow-y-auto min-h-0">
                  <div
                    className={cn(
                      'flex items-center h-[22px] px-2 cursor-pointer text-[13px] gap-1.5 mx-0.5 rounded-sm',
                      selectedFolderId === null
                        ? 'bg-indigo-100 text-indigo-800 font-medium'
                        : 'text-gray-700 hover:bg-gray-200/60'
                    )}
                    onClick={() => onFolderSelect(null)}
                  >
                    <Layers className="h-3.5 w-3.5 shrink-0 opacity-70" />
                    <span className="truncate">All Rules</span>
                  </div>
                  <FolderTree
                    folders={folders}
                    selectedFolderId={selectedFolderId}
                    onSelect={onFolderSelect}
                    onCreateFolder={onCreateFolder}
                  />
                </div>
              )}
            </div>

            {/* Section resize handle */}
            {foldersOpen && filtersOpen && (
              <div
                className="h-[3px] flex-shrink-0 cursor-row-resize bg-gray-200 hover:bg-indigo-300 transition-colors flex items-center justify-center"
                onMouseDown={handleSplitDragStart}
                title="Drag to resize sections"
              />
            )}

            {/* FILTERS section */}
            <div
              className="flex flex-col overflow-hidden"
              style={
                filtersOpen && foldersOpen
                  ? { flex: 1 - splitRatio }
                  : filtersOpen && !foldersOpen
                  ? { flex: 1 }
                  : { flex: 'none' }
              }
            >
              <button
                onClick={toggleFilters}
                className="flex items-center gap-1 h-6 w-full px-2 shrink-0 hover:bg-gray-200/60 transition-colors"
              >
                {filtersOpen
                  ? <ChevronDown className="h-3 w-3 text-gray-500 shrink-0" />
                  : <ChevronRight className="h-3 w-3 text-gray-500 shrink-0" />
                }
                <span className="text-[11px] font-semibold uppercase tracking-[0.08em] text-gray-500 select-none">
                  Filters
                </span>
                {activeFilterCount > 0 && (
                  <span className="ml-1.5 inline-flex items-center justify-center h-4 min-w-[16px] px-1 rounded-full bg-indigo-100 text-[10px] font-semibold text-indigo-700">
                    {activeFilterCount}
                  </span>
                )}
              </button>

              {filtersOpen && (
                <div className="flex-1 overflow-y-auto min-h-0">
                  <FilterPanel
                    selectedStatuses={selectedStatuses}
                    onStatusChange={onStatusChange}
                    selectedTypes={selectedTypes}
                    onTypeChange={onTypeChange}
                    selectedTags={selectedTags}
                    onTagChange={onTagChange}
                    tags={tags}
                    datePreset={datePreset}
                    onDatePresetChange={onDatePresetChange}
                  />
                </div>
              )}
            </div>
          </div>
        </>
      )}

      {/* Width resize handle */}
      <div
        className="absolute top-0 right-0 w-[4px] h-full cursor-col-resize hover:bg-indigo-300 hover:opacity-100 opacity-0 transition-opacity z-10"
        onMouseDown={handleWidthDragStart}
        title="Drag to resize sidebar"
      />

      {/* Collapse toggle tab */}
      <button
        onClick={toggleCollapsed}
        className="absolute top-1/2 -right-3 -translate-y-1/2 flex items-center justify-center w-3 h-12 bg-gray-200 hover:bg-gray-300 rounded-r border border-l-0 border-gray-300 z-20 transition-colors"
        title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
      >
        {collapsed
          ? <ChevronRight className="h-3 w-3 text-gray-500" />
          : <ChevronRight className="h-3 w-3 text-gray-500 rotate-180" />
        }
      </button>
    </div>
  );
}
