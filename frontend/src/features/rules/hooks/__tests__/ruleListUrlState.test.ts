import { describe, it, expect, beforeEach } from 'vitest';
import {
  DEFAULT_RULE_LIST_STATE,
  parseRuleListState,
  serializeRuleListState,
  getRulesListPath,
  rememberRulesListPath,
} from '../ruleListUrlState';

describe('parseRuleListState', () => {
  it('returns defaults for an empty query string', () => {
    expect(parseRuleListState(new URLSearchParams())).toEqual(DEFAULT_RULE_LIST_STATE);
  });

  it('reads every filter from the URL', () => {
    const state = parseRuleListState(new URLSearchParams(
      'q=loan&types=TABLE,TREE&statuses=DRAFT&tags=finance,risk&folder=f1&updated=7&page=3&size=50&sort=name:asc',
    ));
    expect(state).toEqual({
      search: 'loan',
      types: ['TABLE', 'TREE'],
      statuses: ['DRAFT'],
      tags: ['finance', 'risk'],
      folderId: 'f1',
      datePreset: 7,
      page: 2,
      pageSize: 50,
      sortBy: 'name',
      sortDir: 'asc',
    });
  });

  it('falls back to defaults for invalid values', () => {
    const state = parseRuleListState(new URLSearchParams('page=-4&size=13&sort=hack:sideways&updated=abc'));
    expect(state.page).toBe(0);
    expect(state.pageSize).toBe(DEFAULT_RULE_LIST_STATE.pageSize);
    expect(state.sortBy).toBe('updatedAt');
    expect(state.sortDir).toBe('desc');
    expect(state.datePreset).toBeNull();
  });
});

describe('serializeRuleListState', () => {
  it('omits defaults so a fresh list has a clean URL', () => {
    expect(serializeRuleListState(DEFAULT_RULE_LIST_STATE, new URLSearchParams()).toString()).toBe('');
  });

  it('round-trips through parse', () => {
    const state = {
      ...DEFAULT_RULE_LIST_STATE,
      search: 'loan',
      types: ['TABLE'],
      tags: ['finance'],
      folderId: 'f1',
      datePreset: 0,
      page: 1,
      sortBy: 'name' as const,
      sortDir: 'asc' as const,
    };
    expect(parseRuleListState(serializeRuleListState(state, new URLSearchParams()))).toEqual(state);
  });

  it('keeps unrelated params such as spaceId', () => {
    const out = serializeRuleListState(
      { ...DEFAULT_RULE_LIST_STATE, search: 'x' },
      new URLSearchParams('spaceId=s1&q=old'),
    );
    expect(out.get('spaceId')).toBe('s1');
    expect(out.get('q')).toBe('x');
  });
});

describe('rules list path memory', () => {
  beforeEach(() => sessionStorage.clear());

  it('defaults to /rules', () => {
    expect(getRulesListPath()).toBe('/rules');
  });

  it('returns the last remembered list URL', () => {
    rememberRulesListPath('/rules?q=loan&spaceId=s1');
    expect(getRulesListPath()).toBe('/rules?q=loan&spaceId=s1');
  });

  it('ignores paths that are not the rules list', () => {
    rememberRulesListPath('/settings');
    expect(getRulesListPath()).toBe('/rules');
  });
});
