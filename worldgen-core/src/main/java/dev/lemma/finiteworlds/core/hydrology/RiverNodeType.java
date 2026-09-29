package dev.lemma.finiteworlds.core.hydrology;

/**
 * Primary diagnostic role for a river-graph node produced by Hydrology 2C.
 */
public enum RiverNodeType {
    SOURCE,
    CONFLUENCE,
    LAKE_INLET,
    LAKE_OUTLET,
    MOUTH
}
