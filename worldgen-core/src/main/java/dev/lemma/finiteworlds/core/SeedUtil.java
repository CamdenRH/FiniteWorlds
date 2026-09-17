package dev.lemma.finiteworlds.core;

import java.nio.charset.StandardCharsets;

public final class SeedUtil {

    private SeedUtil() {}

    public static long derive(long worldSeed, String domain) {
        long h = worldSeed;

        for (byte b : domain.getBytes(StandardCharsets.UTF_8)) {
            h ^= b;
            h *= 0x100000001B3L;
        }

        return mix64(h);
    }

    public static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
}