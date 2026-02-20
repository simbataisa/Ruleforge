export interface Folder {
  id: string;
  spaceId: string;
  parentId?: string;
  name: string;
  description?: string;
  ruleCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFolderRequest {
  name: string;
  parentId?: string;
  description?: string;
}

export interface UpdateFolderRequest {
  name?: string;
  description?: string;
}
