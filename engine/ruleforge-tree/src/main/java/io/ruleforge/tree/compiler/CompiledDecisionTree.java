package io.ruleforge.tree.compiler;

import io.ruleforge.core.memory.WorkingMemory;
import io.ruleforge.core.model.CompiledRule;
import io.ruleforge.core.model.RuleMetadata;
import io.ruleforge.core.model.RuleType;
import io.ruleforge.core.result.RuleResult;
import io.ruleforge.tree.evaluator.TreeTraverser;

/**
 * Compiled representation of a decision tree rule. Extends {@link CompiledRule} and holds
 * the compiled root node along with tree-level configuration flags.
 * <p>
 * Evaluation is delegated to {@link TreeTraverser} which performs depth-first traversal
 * of the compiled tree structure.
 */
public final class CompiledDecisionTree extends CompiledRule {

    private final CompiledTreeNode root;
    private final boolean scoringMode;
    private final boolean earlyTermination;

    /**
     * Creates a new compiled decision tree.
     *
     * @param ruleId           the unique rule identifier
     * @param metadata         the rule metadata (salience, priority, etc.)
     * @param compiledAt       epoch millis when compilation occurred
     * @param root             the compiled root node of the tree
     * @param scoringMode      if true, all paths are traversed and scores accumulated
     * @param earlyTermination if true, traversal stops at the first action node reached
     */
    public CompiledDecisionTree(String ruleId,
                                RuleMetadata metadata,
                                long compiledAt,
                                CompiledTreeNode root,
                                boolean scoringMode,
                                boolean earlyTermination) {
        super(ruleId, RuleType.DECISION_TREE, metadata, compiledAt);
        this.root = root;
        this.scoringMode = scoringMode;
        this.earlyTermination = earlyTermination;
    }

    @Override
    public RuleResult evaluate(WorkingMemory workingMemory) {
        var traverser = new TreeTraverser(ruleId(), scoringMode, earlyTermination);
        return traverser.traverse(root, workingMemory);
    }

    /**
     * Returns the compiled root node of the decision tree.
     */
    public CompiledTreeNode root() {
        return root;
    }

    /**
     * Returns whether this tree operates in scoring mode (accumulate scores across all matching paths).
     */
    public boolean scoringMode() {
        return scoringMode;
    }

    /**
     * Returns whether this tree uses early termination (stop at first action/score node).
     */
    public boolean earlyTermination() {
        return earlyTermination;
    }
}
