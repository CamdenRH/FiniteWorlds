package dev.lemma.finiteworlds.core.hydrology;

/**
 * Provisional channel hierarchy produced by Hydrology Pass 2B.
 *
 * These classes describe the macro drainage network only. They do not yet
 * imply a carved Minecraft channel, a fixed river width, or year-round flow.
 */
public enum StreamClass {
    NONE,
    HEADWATER,
    TRIBUTARY,
    RIVER,
    MAJOR_RIVER,
    TRUNK_RIVER;

    private static final StreamClass[] VALUES =
            values();

    public static StreamClass fromOrdinal(
            int ordinal
    ) {
        if (ordinal < 0 || ordinal >= VALUES.length) {
            return NONE;
        }

        return VALUES[ordinal];
    }

    public boolean isChannel() {
        return this != NONE;
    }
}
