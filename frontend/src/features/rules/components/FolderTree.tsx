import { useState } from 'react';
import { ChevronRight, ChevronDown, Folder, FolderPlus } from 'lucide-react';
import { cn } from '@/shared/utils/cn';
import type { Folder as FolderType } from '@/api/types/folder.types';

interface FolderTreeProps {
  folders: FolderType[];
  selectedFolderId: string | null;
  onSelect: (folderId: string | null) => void;
  onCreateFolder: (parentId?: string) => void;
}

interface FolderNodeProps {
  folder: FolderType;
  folders: FolderType[];
  selectedFolderId: string | null;
  depth: number;
  onSelect: (folderId: string | null) => void;
  onCreateFolder: (parentId?: string) => void;
}

function FolderNode({
  folder,
  folders,
  selectedFolderId,
  depth,
  onSelect,
  onCreateFolder,
}: FolderNodeProps) {
  const [expanded, setExpanded] = useState(true);
  const children = folders.filter((f) => f.parentId === folder.id);
  const hasChildren = children.length > 0;
  const isSelected = selectedFolderId === folder.id;

  return (
    <div>
      <div
        className={cn(
          'flex items-center h-[22px] cursor-pointer select-none group rounded-sm mx-0.5',
          isSelected ? 'bg-indigo-100 text-indigo-800' : 'text-gray-700 hover:bg-gray-200/60'
        )}
        style={{ paddingLeft: `${depth * 16 + 8}px` }}
        onClick={() => onSelect(folder.id)}
      >
        {/* Chevron */}
        <span
          className="w-4 flex items-center justify-center shrink-0 mr-0.5"
          onClick={(e) => {
            e.stopPropagation();
            if (hasChildren) setExpanded(!expanded);
          }}
        >
          {hasChildren ? (
            expanded ? (
              <ChevronDown className="h-3.5 w-3.5 text-gray-500" />
            ) : (
              <ChevronRight className="h-3.5 w-3.5 text-gray-500" />
            )
          ) : (
            <span className="w-3.5" />
          )}
        </span>
        {/* Icon */}
        <Folder className="h-3.5 w-3.5 shrink-0 mr-1.5 text-amber-500" />
        {/* Name */}
        <span className={cn('flex-1 truncate text-[13px] leading-none', isSelected && 'font-medium')}>
          {folder.name}
        </span>
        {/* Count */}
        {folder.ruleCount != null && folder.ruleCount > 0 && (
          <span className="text-[11px] text-gray-400 mr-1">{folder.ruleCount}</span>
        )}
        {/* Inline action */}
        <button
          className="opacity-0 group-hover:opacity-100 p-0.5 mr-1 rounded hover:bg-gray-300 transition-opacity"
          title="New subfolder"
          onClick={(e) => {
            e.stopPropagation();
            onCreateFolder(folder.id);
          }}
        >
          <FolderPlus className="h-3 w-3 text-gray-500" />
        </button>
      </div>
      {expanded &&
        children.map((child) => (
          <FolderNode
            key={child.id}
            folder={child}
            folders={folders}
            selectedFolderId={selectedFolderId}
            depth={depth + 1}
            onSelect={onSelect}
            onCreateFolder={onCreateFolder}
          />
        ))}
    </div>
  );
}

export function FolderTree({
  folders,
  selectedFolderId,
  onSelect,
  onCreateFolder,
}: FolderTreeProps) {
  const rootFolders = folders.filter((f) => !f.parentId);

  return (
    <div className="flex flex-col h-full">
      <div className="flex-1 overflow-y-auto py-0.5">
        {rootFolders.map((folder) => (
          <FolderNode
            key={folder.id}
            folder={folder}
            folders={folders}
            selectedFolderId={selectedFolderId}
            depth={0}
            onSelect={onSelect}
            onCreateFolder={onCreateFolder}
          />
        ))}
      </div>
    </div>
  );
}
