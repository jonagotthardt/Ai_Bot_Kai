package dev.kai.goal;

/**
 * Kai's mutually exclusive top-level intents, highest priority first.
 */
public enum GoalType {
    /** A valid combat target is acquired; the PvP controller drives behaviour. */
    ENGAGE,
    /** No live target, but a recent threat is remembered; move to its last known position. */
    PURSUE,
    /** Nothing to do. */
    IDLE
}
