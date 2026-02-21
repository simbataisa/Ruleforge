package io.ruleforge.core.memory;

import io.ruleforge.core.model.FactType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkingMemoryTest {

    private WorkingMemory wm;

    @BeforeEach
    void setUp() {
        wm = new WorkingMemory();
    }

    // -- Assert / Get / Has --

    @Test
    void assertAndRetrieveFact() {
        wm.assertFact("name", "John", FactType.STRING);
        assertThat(wm.hasFact("name")).isTrue();
        assertThat((String) wm.get("name")).isEqualTo("John");
    }

    @Test
    void getReturnsNullForMissingFact() {
        Object result = wm.get("nonexistent");
        assertThat(result).isNull();
    }

    @Test
    void hasFact_returnsFalseForMissingPath() {
        assertThat(wm.hasFact("missing")).isFalse();
    }

    @Test
    void getFact_returnsFact() {
        wm.assertFact("age", 25, FactType.INTEGER);
        Fact<?> fact = wm.getFact("age");
        assertThat(fact).isNotNull();
        assertThat(fact.value()).isEqualTo(25);
        assertThat(fact.type()).isEqualTo(FactType.INTEGER);
        assertThat(fact.path()).isEqualTo("age");
    }

    // -- Modify --

    @Test
    void modifyExistingFact() {
        wm.assertFact("score", 10, FactType.INTEGER);
        wm.modifyFact("score", 20);
        assertThat((Integer) wm.get("score")).isEqualTo(20);
    }

    @Test
    void modifyNonexistentFactThrows() {
        assertThatThrownBy(() -> wm.modifyFact("missing", "value"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing");
    }

    // -- Retract --

    @Test
    void retractRemovesFact() {
        wm.assertFact("temp", "val", FactType.STRING);
        wm.retractFact("temp");
        assertThat(wm.hasFact("temp")).isFalse();
    }

    @Test
    void retractNonexistentIsNoOp() {
        wm.retractFact("nonexistent"); // should not throw
    }

    // -- Modification tracking --

    @Test
    void isModifiedAfterAssert() {
        assertThat(wm.isModified()).isFalse();
        wm.assertFact("x", 1, FactType.INTEGER);
        assertThat(wm.isModified()).isTrue();
    }

    @Test
    void clearModificationFlag() {
        wm.assertFact("x", 1, FactType.INTEGER);
        wm.clearModificationFlag();
        assertThat(wm.isModified()).isFalse();
    }

    // -- Changelog --

    @Test
    void changelogTracksAssertModifyRetract() {
        wm.assertFact("val", "a", FactType.STRING);
        wm.modifyFact("val", "b");
        wm.retractFact("val");

        List<FactChange> log = wm.getChangelog();
        assertThat(log).hasSize(3);
        assertThat(log.get(0).type()).isEqualTo(ChangeType.ASSERT);
        assertThat(log.get(1).type()).isEqualTo(ChangeType.MODIFY);
        assertThat(log.get(1).oldValue()).isEqualTo("a");
        assertThat(log.get(1).newValue()).isEqualTo("b");
        assertThat(log.get(2).type()).isEqualTo(ChangeType.RETRACT);
    }

    // -- Listeners --

    @Test
    void listenerNotifiedOnChange() {
        AtomicInteger count = new AtomicInteger(0);
        wm.addListener((fact, changeType) -> count.incrementAndGet());
        wm.assertFact("a", 1, FactType.INTEGER);
        wm.modifyFact("a", 2);
        wm.retractFact("a");
        assertThat(count.get()).isEqualTo(3);
    }

    @Test
    void removedListenerNotNotified() {
        AtomicInteger count = new AtomicInteger(0);
        FactChangeListener listener = (fact, changeType) -> count.incrementAndGet();
        wm.addListener(listener);
        wm.assertFact("a", 1, FactType.INTEGER);
        wm.removeListener(listener);
        wm.assertFact("b", 2, FactType.INTEGER);
        assertThat(count.get()).isEqualTo(1);
    }

    // -- Snapshot --

    @Test
    void snapshotCreatesIndependentCopy() {
        wm.assertFact("x", 10, FactType.INTEGER);
        WorkingMemory snap = wm.snapshot();
        assertThat((Integer) snap.get("x")).isEqualTo(10);
        snap.assertFact("y", 20, FactType.INTEGER);
        assertThat(wm.hasFact("y")).isFalse();
    }

    // -- Fact paths --

    @Test
    void getFactPathsReturnsAllPaths() {
        wm.assertFact("a", 1, FactType.INTEGER);
        wm.assertFact("b", 2, FactType.INTEGER);
        assertThat(wm.getFactPaths()).containsExactlyInAnyOrder("a", "b");
    }

    // -- Bulk operations --

    @Test
    void loadFromMapBulkLoads() {
        wm.loadFromMap(Map.of("name", "Alice", "age", 30));
        assertThat((String) wm.get("name")).isEqualTo("Alice");
        assertThat((Integer) wm.get("age")).isEqualTo(30);
    }

    @Test
    void toMapExportsAll() {
        wm.assertFact("a", "x", FactType.STRING);
        wm.assertFact("b", 1, FactType.INTEGER);
        Map<String, Object> exported = wm.toMap();
        assertThat(exported).containsEntry("a", "x").containsEntry("b", 1);
    }

    // -- Hash --

    @Test
    void factHashChangesOnModification() {
        wm.assertFact("x", 1, FactType.INTEGER);
        long hash1 = wm.factHash();
        wm.modifyFact("x", 2);
        long hash2 = wm.factHash();
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
