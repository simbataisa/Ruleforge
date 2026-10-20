import { useCallback, useEffect, useMemo } from 'react';
import { useLocation, useSearchParams } from 'react-router-dom';

export type RuleSortField = 'updatedAt' | 'createdAt' | 'name';
export type SortDir = 'asc' | 'desc';

export const PAGE_SIZE_OPTIONS = [10, 25, 50, 100];

/** Rules list view state that is kept in the URL so it survives navigation and reloads. */
export interface RuleListState {
  search: string;
  types: string[];
  statuses: string[];
  tags: string[];
  folderId: string | null;
  datePreset: number | null;
  page: number;
  pageSize: number;
  sortBy: RuleSortField;
  sortDir: SortDir;
}

export const DEFAULT_RULE_LIST_STATE: RuleListState = {
  search: '',
  types: [],
  statuses: [],
  tags: [],
  folderId: null,
  datePreset: null,
  page: 0,
  pageSize: 25,
  sortBy: 'updatedAt',
  sortDir: 'desc',
};

const SORT_FIELDS: RuleSortField[] = ['updatedAt', 'createdAt', 'name'];
const LIST_PATH_KEY = 'ruleforge:rules-list-path';

// URL keys owned by the list; anything else (e.g. spaceId) is left untouched
const KEYS = ['q', 'types', 'statuses', 'tags', 'folder', 'updated', 'page', 'size', 'sort'] as const;

function readList(params: URLSearchParams, key: string): string[] {
  const raw = params.get(key);
  return raw ? raw.split(',').filter(Boolean) : [];
}

function readInt(params: URLSearchParams, key: string): number | null {
  const raw = params.get(key);
  if (raw == null || !/^\d+$/.test(raw)) return null;
  return Number(raw);
}

export function parseRuleListState(params: URLSearchParams): RuleListState {
  const page = readInt(params, 'page');
  const size = readInt(params, 'size');
  const [sortField, sortDir] = (params.get('sort') ?? '').split(':');

  return {
    search: params.get('q') ?? '',
    types: readList(params, 'types'),
    statuses: readList(params, 'statuses'),
    tags: readList(params, 'tags'),
    folderId: params.get('folder') || null,
    datePreset: readInt(params, 'updated'),
    // URL page is 1-based for readability
    page: page != null && page > 0 ? page - 1 : 0,
    pageSize: size != null && PAGE_SIZE_OPTIONS.includes(size) ? size : DEFAULT_RULE_LIST_STATE.pageSize,
    sortBy: SORT_FIELDS.includes(sortField as RuleSortField)
      ? (sortField as RuleSortField)
      : DEFAULT_RULE_LIST_STATE.sortBy,
    sortDir: sortDir === 'asc' || sortDir === 'desc' ? sortDir : DEFAULT_RULE_LIST_STATE.sortDir,
  };
}

export function serializeRuleListState(state: RuleListState, base: URLSearchParams): URLSearchParams {
  const next = new URLSearchParams(base);
  KEYS.forEach((k) => next.delete(k));

  const d = DEFAULT_RULE_LIST_STATE;
  if (state.search) next.set('q', state.search);
  if (state.types.length) next.set('types', state.types.join(','));
  if (state.statuses.length) next.set('statuses', state.statuses.join(','));
  if (state.tags.length) next.set('tags', state.tags.join(','));
  if (state.folderId) next.set('folder', state.folderId);
  if (state.datePreset != null) next.set('updated', String(state.datePreset));
  if (state.page > 0) next.set('page', String(state.page + 1));
  if (state.pageSize !== d.pageSize) next.set('size', String(state.pageSize));
  if (state.sortBy !== d.sortBy || state.sortDir !== d.sortDir) next.set('sort', `${state.sortBy}:${state.sortDir}`);
  return next;
}

/** Remembers the last Rules list URL so links back from the editor restore the filters. */
export function rememberRulesListPath(path: string): void {
  if (path !== '/rules' && !path.startsWith('/rules?')) return;
  try {
    sessionStorage.setItem(LIST_PATH_KEY, path);
  } catch {
    // storage unavailable (private mode) — links fall back to /rules
  }
}

export function getRulesListPath(): string {
  try {
    return sessionStorage.getItem(LIST_PATH_KEY) ?? '/rules';
  } catch {
    return '/rules';
  }
}

/**
 * Rules list state backed by URL search params. Filter changes reset to the first page.
 */
export function useRuleListUrlState() {
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();
  const state = useMemo(() => parseRuleListState(searchParams), [searchParams]);

  useEffect(() => {
    rememberRulesListPath(location.pathname + location.search);
  }, [location.pathname, location.search]);

  const update = useCallback(
    (patch: Partial<RuleListState>) => {
      setSearchParams(
        (prev) => {
          const current = parseRuleListState(prev);
          const pageOnly = Object.keys(patch).every((k) => k === 'page');
          const next = { ...current, ...patch };
          if (!pageOnly && patch.page === undefined) next.page = 0;
          return serializeRuleListState(next, prev);
        },
        { replace: true },
      );
    },
    [setSearchParams],
  );

  return { state, update };
}
