// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.net;

import java.util.ArrayDeque;

/** Ordered bounded inbox. Network callbacks never run or pace the native simulation. */
public final class ClassicStepBuffer {
    public static final int CAPACITY = 128;
    private final ArrayDeque<int[]> messages = new ArrayDeque<>();
    private final int stepType;
    private int steps;
    private boolean closed;
    public ClassicStepBuffer(int stepType) { this.stepType = stepType; }
    public synchronized boolean offer(int[] message) {
        if (closed) return true;
        if (messages.size() >= CAPACITY) return false;
        messages.add(message);
        if (message[0] == stepType) steps++;
        notifyAll();
        return true;
    }
    public synchronized int[] poll(long timeoutMs) throws InterruptedException {
        long end = System.nanoTime() + timeoutMs * 1_000_000L;
        while (messages.isEmpty() && !closed) {
            long left = end - System.nanoTime();
            if (left <= 0) return null;
            wait(Math.max(1, left / 1_000_000L));
        }
        int[] result = messages.poll();
        if (result != null && result[0] == stepType) steps--;
        return result;
    }
    public synchronized int pendingSteps() { return steps; }
    public synchronized void close() { closed = true; messages.clear(); steps = 0; notifyAll(); }
    // Catch up only when multiple authoritative packets have already arrived; never skip ticks.
    public static int tickMicros(int normal, int pending) {
        if (pending <= 1) return normal;
        return normal * Math.max(50, 100 - (pending - 1) * 10) / 100;
    }
    public static int hostWindow(long relayRttMs) {
        return (int)Math.max(8, Math.min(32, (relayRttMs * 2 + 69) / 70 + 4));
    }
    public static int hostDelayMs(int lag, int window) {
        // A real multi-second stall needs backpressure before the bounded inbox fills.
        // Normal network delay never reaches this branch. All simulation ticks remain intact.
        if(lag>=48)return 70;
        return Math.min(20, Math.max(0, lag - window) * 2);
    }
    public static boolean shouldDisconnect(int lag,long ackAgeMs,long progressAgeMs){
        return ackAgeMs>20000 || (lag>=96 && progressAgeMs>20000);
    }
}
