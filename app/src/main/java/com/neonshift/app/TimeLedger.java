package com.neonshift.app;

public final class TimeLedger {
    /** Monotonic time survives process restarts; wall time is a reboot fallback. */
    public static long segment(long wallStart, long uptimeStart, int bootStart,
            long nowWall, long nowUptime, int bootNow) {
        if (bootNow >= 0 && bootStart == bootNow && uptimeStart >= 0 && nowUptime >= uptimeStart)
            return nowUptime - uptimeStart;
        return Math.max(0, nowWall - wallStart);
    }
}
