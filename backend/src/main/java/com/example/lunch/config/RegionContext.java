package com.example.lunch.config;

public class RegionContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public static void set(String region) {
        CURRENT.set(region);
    }

    public static String get() {
        String r = CURRENT.get();
        return (r != null && !r.isBlank()) ? r : "taichung";
    }

    public static void clear() {
        CURRENT.remove();
    }
}
