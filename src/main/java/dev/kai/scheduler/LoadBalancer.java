package dev.kai.scheduler;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Spreads expensive, deferrable work across ticks under a fixed per-tick time budget.
 *
 * <p>Kai never does a full world/chunk/inventory scan in a single tick. Heavy work is broken into
 * small units and queued here; {@link #drain(long)} then runs as many units as fit into the
 * configured budget and leaves the rest for the next tick. The result is a flat, predictable tick
 * cost instead of periodic spikes.
 *
 * <p>Runs entirely on the main thread, so queued units may freely touch the Bukkit/Paper API.
 */
public final class LoadBalancer {

    private final Deque<Runnable> queue = new ArrayDeque<>();
    private final long budgetNanos;

    public LoadBalancer(long budgetMicros) {
        this.budgetNanos = Math.max(50L, budgetMicros) * 1_000L;
    }

    /** Queues a unit of work to be executed on a future tick within budget. */
    public void submit(Runnable unit) {
        queue.addLast(unit);
    }

    /** Number of units still waiting. */
    public int pending() {
        return queue.size();
    }

    /**
     * Runs queued units until the per-tick budget is exhausted or the queue is empty.
     *
     * @return how many units ran this tick
     */
    public int drain(long nowNanos) {
        int executed = 0;
        long deadline = nowNanos + budgetNanos;
        while (!queue.isEmpty()) {
            Runnable unit = queue.pollFirst();
            unit.run();
            executed++;
            if (System.nanoTime() >= deadline) {
                break;
            }
        }
        return executed;
    }

    public void clear() {
        queue.clear();
    }
}
