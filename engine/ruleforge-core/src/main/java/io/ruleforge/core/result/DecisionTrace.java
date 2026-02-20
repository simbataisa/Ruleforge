package io.ruleforge.core.result;

import io.ruleforge.core.memory.FactChange;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public record DecisionTrace(
    String ruleId,
    TraceNode rootNode,
    List<FactChange> factChanges,
    Duration totalTime
) {
    public DecisionTrace {
        factChanges = factChanges == null ? List.of() : List.copyOf(factChanges);
    }

    public static Builder builder(String ruleId) {
        return new Builder(ruleId);
    }

    public static class Builder {
        private final String ruleId;
        private TraceNode rootNode;
        private final List<FactChange> factChanges = new ArrayList<>();
        private final long startTime = System.nanoTime();

        private Builder(String ruleId) {
            this.ruleId = ruleId;
        }

        public Builder rootNode(TraceNode node) {
            this.rootNode = node;
            return this;
        }

        public Builder addFactChange(FactChange change) {
            this.factChanges.add(change);
            return this;
        }

        public Builder addFactChanges(List<FactChange> changes) {
            this.factChanges.addAll(changes);
            return this;
        }

        public DecisionTrace build() {
            Duration elapsed = Duration.ofNanos(System.nanoTime() - startTime);
            return new DecisionTrace(ruleId, rootNode, List.copyOf(factChanges), elapsed);
        }
    }
}
