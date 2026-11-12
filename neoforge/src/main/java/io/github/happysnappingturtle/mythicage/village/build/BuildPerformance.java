package io.github.happysnappingturtle.mythicage.village.build;

/** Server-thread inclusive construction timing; nesting prevents double-counting cell placement inside a worker tick. */
public final class BuildPerformance {
    private static int depth;
    private static long started, totalNanos, calls;
    private BuildPerformance() {}
    static void begin() { if (depth++ == 0) started = System.nanoTime(); }
    static void end() { if (--depth == 0) { totalNanos += System.nanoTime() - started; calls++; } }
    public static long totalNanos() { return totalNanos; }
    public static long calls() { return calls; }
}
