package io.ruleforge.table.compiler;

import io.ruleforge.core.memory.Fact;
import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.FactType;
import io.ruleforge.core.model.PriorityVector;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.result.DecisionTrace;
import io.ruleforge.core.result.MatchedRule;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.core.result.RuleResultMetadata;
import io.ruleforge.table.compiler.RowDefinition.CellValue;
import io.ruleforge.table.evaluator.HitPolicy;
import io.ruleforge.table.index.ConditionIndex;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A compiled decision table that extends {@link CompiledRule}.
 * <p>
 * Holds compiled rows, column definitions, hit policy, and per-column indexes
 * for fast condition matching.
 */
public final class CompiledDecisionTable extends CompiledRule {

    private final List<ColumnDefinition> conditionColumns;
    private final List<ColumnDefinition> actionColumns;
    private final List<CompiledRow> rows;
    private final HitPolicy hitPolicy;
    private final Map<Integer, ConditionIndex> columnIndexes; // keyed by condition column index
    private final String ruleName;
    private final int ruleVersion;

    public CompiledDecisionTable(String ruleId,
                                  String ruleName,
                                  int ruleVersion,
                                  RuleMetadata metadata,
                                  long compiledAt,
                                  List<ColumnDefinition> conditionColumns,
                                  List<ColumnDefinition> actionColumns,
                                  List<CompiledRow> rows,
                                  HitPolicy hitPolicy,
                                  Map<Integer, ConditionIndex> columnIndexes) {
        super(ruleId, RuleType.DECISION_TABLE, metadata, compiledAt);
        this.ruleName = ruleName;
        this.ruleVersion = ruleVersion;
        this.conditionColumns = List.copyOf(conditionColumns);
        this.actionColumns = List.copyOf(actionColumns);
        this.rows = List.copyOf(rows);
        this.hitPolicy = hitPolicy;
        this.columnIndexes = Map.copyOf(columnIndexes);
    }

    public List<ColumnDefinition> conditionColumns() {
        return conditionColumns;
    }

    public List<ColumnDefinition> actionColumns() {
        return actionColumns;
    }

    public List<CompiledRow> rows() {
        return rows;
    }

    public HitPolicy hitPolicy() {
        return hitPolicy;
    }

    public Map<Integer, ConditionIndex> columnIndexes() {
        return columnIndexes;
    }

    public String ruleName() {
        return ruleName;
    }

    public int ruleVersion() {
        return ruleVersion;
    }

    /**
     * Default evaluate using FIRST hit policy.
     * The engine's {@code evaluate()} method with context is preferred; this method
     * provides basic standalone evaluation.
     */
    @Override
    public RuleResult evaluate(WorkingMemory workingMemory) {
        long startNanos = System.nanoTime();
        DecisionTrace.Builder traceBuilder = DecisionTrace.builder(ruleId());

        // Step 1: Use indexes to find candidate rows
        Set<Integer> candidateRows = findCandidateRows(workingMemory);

        // Step 2: Verify candidates with full condition evaluation
        List<CompiledRow> matchedRows = new ArrayList<>();
        for (CompiledRow row : rows) {
            if (candidateRows.contains(row.rowNumber()) && evaluateRowConditions(row, workingMemory)) {
                matchedRows.add(row);
            }
        }

        // Step 3: Apply hit policy
        List<MatchedRule> matchedRules = new ArrayList<>();
        Map<String, Object> output = new LinkedHashMap<>();

        if (!matchedRows.isEmpty()) {
            switch (hitPolicy) {
                case FIRST -> {
                    CompiledRow first = matchedRows.getFirst();
                    output.putAll(first.actionMap());
                    matchedRules.add(toMatchedRule(first));
                }
                case PRIORITY -> {
                    CompiledRow best = matchedRows.stream()
                            .max(Comparator.comparingInt(CompiledRow::priority))
                            .orElseThrow();
                    output.putAll(best.actionMap());
                    matchedRules.add(toMatchedRule(best));
                }
                case ALL -> {
                    for (CompiledRow row : matchedRows) {
                        output.putAll(row.actionMap());
                        matchedRules.add(toMatchedRule(row));
                    }
                }
                case AGGREGATE -> {
                    output.putAll(aggregateOutputs(matchedRows));
                    for (CompiledRow row : matchedRows) {
                        matchedRules.add(toMatchedRule(row));
                    }
                }
                case RANKED -> {
                    List<CompiledRow> ranked = matchedRows.stream()
                            .sorted(Comparator.comparingInt(CompiledRow::priority).reversed())
                            .toList();
                    for (CompiledRow row : ranked) {
                        output.putAll(row.actionMap());
                        matchedRules.add(toMatchedRule(row));
                    }
                }
            }
        }

        Duration elapsed = Duration.ofNanos(System.nanoTime() - startNanos);
        traceBuilder.addFactChanges(workingMemory.getChangelog());
        DecisionTrace trace = traceBuilder.build();
        RuleResultMetadata resultMetadata = new RuleResultMetadata(
                elapsed, rows.size(), matchedRows.size(), 0, hitPolicy.name()
        );

        return new RuleResult(output, resultMetadata, matchedRules, trace);
    }

    /**
     * Uses per-column indexes to find candidate rows. Intersects results from all
     * condition columns using smallest-set-first optimization.
     */
    public Set<Integer> findCandidateRows(WorkingMemory workingMemory) {
        List<Set<Integer>> candidateSets = new ArrayList<>();

        for (int i = 0; i < conditionColumns.size(); i++) {
            ColumnDefinition col = conditionColumns.get(i);
            ConditionIndex index = columnIndexes.get(col.index());

            if (index != null) {
                Object factValue = resolveFactValue(workingMemory, col.name(), col.dataType());
                Set<Integer> candidates = index.lookup(factValue);
                candidateSets.add(candidates);
            }
        }

        if (candidateSets.isEmpty()) {
            // No indexes; return all row numbers
            return rows.stream()
                    .map(CompiledRow::rowNumber)
                    .collect(Collectors.toSet());
        }

        return intersect(candidateSets);
    }

    /**
     * Evaluates all conditions in a row against working memory (full verification
     * after index-based candidate selection).
     */
    public boolean evaluateRowConditions(CompiledRow row, WorkingMemory workingMemory) {
        List<CellValue> conditions = row.conditionValues();
        for (int i = 0; i < conditionColumns.size() && i < conditions.size(); i++) {
            CellValue cell = conditions.get(i);
            if (cell.isWildcard()) {
                continue; // wildcard matches everything
            }
            ColumnDefinition col = conditionColumns.get(i);
            Object factValue = resolveFactValue(workingMemory, col.name(), col.dataType());
            if (!evaluateCondition(col.operator(), factValue, cell.value(), cell.secondValue(), col.dataType())) {
                return false;
            }
        }
        return true;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private Object resolveFactValue(WorkingMemory wm, String path, FactType expectedType) {
        // Try direct path first
        Object value = wm.get(path);
        if (value != null) {
            return value;
        }
        // Try getFact
        Fact<?> fact = wm.getFact(path);
        return fact != null ? fact.value() : null;
    }

    private boolean evaluateCondition(String operator, Object factValue, Object cellValue,
                                       Object secondValue, FactType dataType) {
        return switch (operator.toUpperCase()) {
            case "EQ" -> isEqual(factValue, cellValue);
            case "NEQ" -> !isEqual(factValue, cellValue);
            case "GT" -> compareValues(factValue, cellValue) > 0;
            case "GTE" -> compareValues(factValue, cellValue) >= 0;
            case "LT" -> compareValues(factValue, cellValue) < 0;
            case "LTE" -> compareValues(factValue, cellValue) <= 0;
            case "BETWEEN" -> {
                int cmpLow = compareValues(factValue, cellValue);
                int cmpHigh = compareValues(factValue, secondValue);
                yield cmpLow >= 0 && cmpHigh <= 0;
            }
            case "IN" -> {
                if (cellValue instanceof Collection<?> coll) {
                    yield coll.stream().anyMatch(v -> isEqual(factValue, v));
                }
                yield isEqual(factValue, cellValue);
            }
            case "NOT_IN" -> {
                if (cellValue instanceof Collection<?> coll) {
                    yield coll.stream().noneMatch(v -> isEqual(factValue, v));
                }
                yield !isEqual(factValue, cellValue);
            }
            case "CONTAINS" -> {
                if (factValue == null || cellValue == null) yield false;
                yield factValue.toString().toLowerCase()
                        .contains(cellValue.toString().toLowerCase());
            }
            case "STARTS_WITH" -> {
                if (factValue == null || cellValue == null) yield false;
                yield factValue.toString().toLowerCase()
                        .startsWith(cellValue.toString().toLowerCase());
            }
            case "REGEX" -> {
                if (factValue == null || cellValue == null) yield false;
                yield factValue.toString().matches(cellValue.toString());
            }
            case "IS_NULL" -> factValue == null;
            case "IS_NOT_NULL" -> factValue != null;
            default -> isEqual(factValue, cellValue);
        };
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        // Numeric comparison with tolerance
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue()) == 0;
        }
        // Case-insensitive string comparison
        if (a instanceof String sa && b instanceof String sb) {
            return sa.equalsIgnoreCase(sb);
        }
        return a.equals(b);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareValues(Object a, Object b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        if (a instanceof Number na && b instanceof Number nb) {
            return Double.compare(na.doubleValue(), nb.doubleValue());
        }
        if (a instanceof Comparable ca && b instanceof Comparable cb) {
            return ca.compareTo(cb);
        }
        return a.toString().compareToIgnoreCase(b.toString());
    }

    private MatchedRule toMatchedRule(CompiledRow row) {
        Map<String, Object> matchedConditions = new LinkedHashMap<>();
        for (int i = 0; i < conditionColumns.size() && i < row.conditionValues().size(); i++) {
            CellValue cv = row.conditionValues().get(i);
            if (!cv.isWildcard()) {
                ColumnDefinition col = conditionColumns.get(i);
                matchedConditions.put(col.name(), cv.value());
            }
        }
        PriorityVector priority = new PriorityVector(
                row.priority(), conditionColumns.size(), 1.0, Instant.now(), row.rowNumber()
        );
        return new MatchedRule(
                ruleId(), ruleName, ruleVersion, priority,
                matchedConditions, row.actionMap()
        );
    }

    private Map<String, Object> aggregateOutputs(List<CompiledRow> matchedRows) {
        Map<String, List<Number>> numericCollector = new LinkedHashMap<>();
        Map<String, Object> nonNumeric = new LinkedHashMap<>();

        for (CompiledRow row : matchedRows) {
            for (Map.Entry<String, Object> entry : row.actionMap().entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Number n) {
                    numericCollector.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(n);
                } else {
                    nonNumeric.put(entry.getKey(), val);
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>(nonNumeric);
        for (Map.Entry<String, List<Number>> entry : numericCollector.entrySet()) {
            List<Number> values = entry.getValue();
            double sum = values.stream().mapToDouble(Number::doubleValue).sum();
            double avg = sum / values.size();
            double min = values.stream().mapToDouble(Number::doubleValue).min().orElse(0.0);
            double max = values.stream().mapToDouble(Number::doubleValue).max().orElse(0.0);
            // Store aggregated values as a map
            Map<String, Object> aggregated = new LinkedHashMap<>();
            aggregated.put("SUM", sum);
            aggregated.put("AVG", avg);
            aggregated.put("MIN", min);
            aggregated.put("MAX", max);
            aggregated.put("COUNT", values.size());
            result.put(entry.getKey(), aggregated);
        }
        return result;
    }

    /**
     * Intersects multiple candidate sets using smallest-set-first optimization.
     */
    private Set<Integer> intersect(List<Set<Integer>> sets) {
        if (sets.isEmpty()) return Set.of();
        if (sets.size() == 1) return sets.getFirst();

        // Sort by size ascending for smallest-set-first intersection
        List<Set<Integer>> sorted = new ArrayList<>(sets);
        sorted.sort(Comparator.comparingInt(Set::size));

        Set<Integer> result = new HashSet<>(sorted.getFirst());
        for (int i = 1; i < sorted.size() && !result.isEmpty(); i++) {
            result.retainAll(sorted.get(i));
        }
        return result;
    }
}
