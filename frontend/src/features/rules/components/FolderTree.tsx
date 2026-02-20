import { useState } from 'react';
import { ChevronRight, ChevronDown, Folder, FolderPlus } from 'lucide-react';
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
        className={`group flex items-center gap-1 px-2 py-1.5 cursor-pointer rounded-md text-sm transition-colors ${
          isSelected
            ? 'bg-indigo-50 text-indigo-700 font-medium'
            : 'text-gray-700 hover:bg-gray-100'
        }`}
        style={{ paddingLeft: `${depth * 16 + 8}px` }}
        onClick={() => onSelect(folder.id)}
      >
        <button
          className="flex-shrink-0 p-0.5 rounded hover:bg-gray-200"
          onClick={(e) => {
            e.stopPropagation();
            setExpanded(!expanded);
          }}
        >
          {hasChildren ? (
            expanded ? (
              <ChevronDown className="h-3.5 w-3.5 text-gray-500" />
            ) : (
              <ChevronRight className="h-3.5 w-3.5 text-gray-500" />
            )
          ) : (
            <span className="inline-block w-3.5" />
          )}
        </button>
        <Folder className="h-4 w-4 flex-shrink-0 text-gray-400" />
        <span className="truncate flex-1">{folder.name}</span>
        <span className="text-xs text-gray-400">{folder.ruleCount}</span>
        <button
          className="hidden group-hover:block p-0.5 rounded hover:bg-gray-200"
          onClick={(e) => {
            e.stopPropagation();
            onCreateFolder(folder.id);
          }}
          title="Create subfolder"
        >
          <FolderPlus className="h-3.5 w-3.5 text-gray-400" />
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
      <div className="flex items-center justify-between px-3 py-2 border-b border-gray-200">
        <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">
          Folders
        </span>
        <button
          onClick={() => onCreateFolder()}
          className="p-1 rounded hover:bg-gray-100 transition-colors"
          title="Create folder"
        >
          <FolderPlus className="h-4 w-4 text-gray-500" />
        </button>
      </div>
      <div className="flex-1 overflow-y-auto py-1">
        <div
          className={`flex items-center gap-2 px-3 py-1.5 cursor-pointer rounded-md text-sm mx-1 transition-colors ${
            selectedFolderId === null
              ? 'bg-indigo-50 text-indigo-700 font-medium'
              : 'text-gray-700 hover:bg-gray-100'
          }`}
          onClick={() => onSelect(null)}
        >
          <Folder className="h-4 w-4 text-gray-400" />
          <span>All Rules</span>
        </div>
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
