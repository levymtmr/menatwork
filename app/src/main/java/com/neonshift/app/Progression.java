package com.neonshift.app;

public final class Progression {
    public static final long XP_PER_HOUR=100, XP_PER_LEVEL=1000;
    public static long xp(long workedMs) { return Math.max(0,workedMs)/36000; }
    public static long level(long xp) { return Math.max(0,xp)/XP_PER_LEVEL+1; }
    public static long withinLevel(long xp) { return Math.max(0,xp)%XP_PER_LEVEL; }
}
