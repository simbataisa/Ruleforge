package io.ruleforge.core.resolution;

import io.ruleforge.core.model.PriorityVector;
import io.ruleforge.core.model.RuleMetadata;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConflictResolutionTest {

    private static ActivatedRule makeRule(String id, int salience, int conditionCount, int declarationOrder) {
        RuleMetadata meta = new RuleMetadata(
                salience, 0, 1.0, null, null, null, null, false, false, Map.of(), null, null);
        PriorityVector pv = new PriorityVector(salience, conditionCount, 1.0, Instant.now(), declarationOrder);
        return new ActivatedRule(id, id, null, meta, pv, conditionCount, Map.of());
    }

    // -- PriorityResolver --

    @Test
    void priorityResolver_selectsHighestSalience() {
        PriorityResolver resolver = new PriorityResolver();
        List<ActivatedRule> rules = List.of(
                makeRule("low", 1, 1, 0),
                makeRule("high", 10, 1, 1),
                makeRule("mid", 5, 1, 2)
        );
        List<ActivatedRule> result = resolver.resolve(rules, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("high");
    }

    @Test
    void priorityResolver_returnsAllWithSameSalience() {
        PriorityResolver resolver = new PriorityResolver();
        List<ActivatedRule> rules = List.of(
                makeRule("a", 10, 1, 0),
                makeRule("b", 10, 2, 1)
        );
        List<ActivatedRule> result = resolver.resolve(rules, null);
        assertThat(result).hasSize(2);
    }

    @Test
    void priorityResolver_singleRuleReturnedUnchanged() {
        PriorityResolver resolver = new PriorityResolver();
        List<ActivatedRule> rules = List.of(makeRule("only", 5, 1, 0));
        assertThat(resolver.resolve(rules, null)).hasSize(1);
    }

    // -- SpecificityResolver --

    @Test
    void specificityResolver_selectsMostConditions() {
        SpecificityResolver resolver = new SpecificityResolver();
        List<ActivatedRule> rules = List.of(
                makeRule("few", 0, 1, 0),
                makeRule("many", 0, 5, 1),
                makeRule("mid", 0, 3, 2)
        );
        List<ActivatedRule> result = resolver.resolve(rules, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("many");
    }

    // -- DeclarationOrderResolver --

    @Test
    void declarationOrderResolver_selectsLowestOrder() {
        DeclarationOrderResolver resolver = new DeclarationOrderResolver();
        List<ActivatedRule> rules = List.of(
                makeRule("third", 0, 1, 3),
                makeRule("first", 0, 1, 1),
                makeRule("second", 0, 1, 2)
        );
        List<ActivatedRule> result = resolver.resolve(rules, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("first");
    }

    // -- CompositeConflictResolver --

    @Test
    void compositeResolver_chainsStrategies() {
        // Two rules with same salience but different specificity
        CompositeConflictResolver resolver = CompositeConflictResolver.defaultChain();
        List<ActivatedRule> rules = List.of(
                makeRule("general", 10, 1, 0),
                makeRule("specific", 10, 5, 1)
        );
        List<ActivatedRule> result = resolver.resolve(rules, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("specific");
    }

    @Test
    void compositeResolver_name() {
        CompositeConflictResolver resolver = CompositeConflictResolver.defaultChain();
        assertThat(resolver.name()).startsWith("COMPOSITE[");
    }

    // -- PriorityVector ordering --

    @Test
    void priorityVector_higherSalienceFirst() {
        PriorityVector high = new PriorityVector(10, 0, 1.0, Instant.now(), 0);
        PriorityVector low = new PriorityVector(1, 0, 1.0, Instant.now(), 0);
        assertThat(high.compareTo(low)).isNegative();
    }

    @Test
    void priorityVector_higherSpecificityFirst() {
        Instant now = Instant.now();
        PriorityVector specific = new PriorityVector(0, 5, 1.0, now, 0);
        PriorityVector general = new PriorityVector(0, 1, 1.0, now, 0);
        assertThat(specific.compareTo(general)).isNegative();
    }

    @Test
    void priorityVector_lowerDeclarationOrderFirst() {
        Instant now = Instant.now();
        PriorityVector first = new PriorityVector(0, 0, 1.0, now, 1);
        PriorityVector second = new PriorityVector(0, 0, 1.0, now, 5);
        assertThat(first.compareTo(second)).isNegative();
    }
}
