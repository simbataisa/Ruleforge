import { describe, it, expect } from 'vitest';
import { flattenFolders } from '../folders.api';

describe('flattenFolders', () => {
  it('flattens nested children into a parentId-linked list', () => {
    const tree = [
      {
        id: 'root', spaceId: 's1', name: 'Lending', ruleCount: 0, createdAt: '', updatedAt: '',
        children: [
          {
            id: 'child', spaceId: 's1', parentId: 'root', name: 'Mortgages', ruleCount: 0, createdAt: '', updatedAt: '',
            children: [
              { id: 'grandchild', spaceId: 's1', parentId: 'child', name: 'Fixed', ruleCount: 0, createdAt: '', updatedAt: '' },
            ],
          },
        ],
      },
    ];

    const flat = flattenFolders(tree);

    expect(flat.map((f) => [f.id, f.parentId])).toEqual([
      ['root', undefined],
      ['child', 'root'],
      ['grandchild', 'child'],
    ]);
    expect(flat.every((f) => !('children' in f))).toBe(true);
  });

  it('handles an empty or missing response', () => {
    expect(flattenFolders(undefined)).toEqual([]);
    expect(flattenFolders([])).toEqual([]);
  });
});
