package io.ruleforge.cep;

/**
 * Types of CEP patterns that can be detected in event streams.
 */
public enum PatternType {

    /**
     * Detects a specific sequence of events occurring in order within a time window.
     */
    SEQUENCE,

    /**
     * Aggregates event values (sum, avg, min, max, count) over a time window.
     */
    AGGREGATION,

    /**
     * Detects when an event occurs a certain number of times within a time window.
     */
    FREQUENCY,

    /**
     * Detects the absence of an expected event within a time window.
     */
    ABSENCE,

    /**
     * Detects a specific combination of events regardless of order within a time window.
     */
    CONJUNCTION
}
