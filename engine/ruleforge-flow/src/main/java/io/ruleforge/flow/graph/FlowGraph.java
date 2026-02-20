package io.ruleforge.flow.graph;

import java.util.*;

/**
 * Represents a directed acyclic graph of flow nodes and edges.
 * Supports topological ordering and cycle detection via Kahn's algorithm.
 */
public final class FlowGraph {

    private final Map<String, FlowNode> nodeMap;
    private final List<FlowEdge> edges;
    private final String startNodeId;
    private final int maxIterations;

    public FlowGraph(List<FlowNode> nodes, List<FlowEdge> edges, String startNodeId, int maxIterations) {
        this.nodeMap = new LinkedHashMap<>();
        for (FlowNode node : nodes) {
            nodeMap.put(node.id(), node);
        }
        this.edges = List.copyOf(edges);
        this.startNodeId = startNodeId;
        this.maxIterations = maxIterations;
    }

    public FlowNode getNode(String id) {
        return nodeMap.get(id);
    }

    public FlowNode getStartNode() {
        return nodeMap.get(startNodeId);
    }

    public String getStartNodeId() {
        return startNodeId;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public List<FlowNode> getNodes() {
        return List.copyOf(nodeMap.values());
    }

    public List<FlowEdge> getEdges() {
        return edges;
    }

    /**
     * Returns all outgoing edges from the given node.
     */
    public List<FlowEdge> getOutgoingEdges(String nodeId) {
        return edges.stream()
                .filter(e -> e.fromId().equals(nodeId))
                .toList();
    }

    /**
     * Returns all incoming edges to the given node.
     */
    public List<FlowEdge> getIncomingEdges(String nodeId) {
        return edges.stream()
                .filter(e -> e.toId().equals(nodeId))
                .toList();
    }

    /**
     * Detects cycles using DFS. Returns true if the graph contains a cycle.
     */
    public boolean hasCycle() {
        Set<String> visited = new HashSet<>();
        Set<String> inStack = new HashSet<>();
        for (String nodeId : nodeMap.keySet()) {
            if (dfsCycleDetect(nodeId, visited, inStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfsCycleDetect(String nodeId, Set<String> visited, Set<String> inStack) {
        if (inStack.contains(nodeId)) return true;
        if (visited.contains(nodeId)) return false;
        visited.add(nodeId);
        inStack.add(nodeId);
        for (FlowEdge edge : getOutgoingEdges(nodeId)) {
            if (dfsCycleDetect(edge.toId(), visited, inStack)) return true;
        }
        inStack.remove(nodeId);
        return false;
    }

    /**
     * Returns nodes reachable from startNodeId in topological order (Kahn's algorithm).
     * Only includes nodes reachable from the start node.
     */
    public List<FlowNode> topologicalOrder() {
        // Compute in-degrees for reachable nodes
        Set<String> reachable = findReachable(startNodeId);

        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (String id : reachable) {
            inDegree.put(id, 0);
        }
        for (FlowEdge edge : edges) {
            if (reachable.contains(edge.fromId()) && reachable.contains(edge.toId())) {
                inDegree.merge(edge.toId(), 1, Integer::sum);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) queue.add(entry.getKey());
        }

        List<FlowNode> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            FlowNode node = nodeMap.get(nodeId);
            if (node != null) sorted.add(node);
            for (FlowEdge edge : getOutgoingEdges(nodeId)) {
                if (reachable.contains(edge.toId())) {
                    int deg = inDegree.merge(edge.toId(), -1, Integer::sum);
                    if (deg == 0) queue.add(edge.toId());
                }
            }
        }
        return sorted;
    }

    private Set<String> findReachable(String startId) {
        Set<String> reachable = new LinkedHashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.add(startId);
        while (!queue.isEmpty()) {
            String id = queue.poll();
            if (reachable.add(id)) {
                for (FlowEdge edge : getOutgoingEdges(id)) {
                    queue.add(edge.toId());
                }
            }
        }
        return reachable;
    }
}
