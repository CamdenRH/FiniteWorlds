package dev.lemma.finiteworlds.core.hydrology;

/**
 * Identifies why the final drainage graph differs from the conditioned D8
 * field at a cell.
 *
 * Pass 1J is graph-only: these route types do not imply that authored terrain
 * elevation was changed at the routed cell.
 */
public enum HydrologyRouteType {
    ORIGINAL,
    LAKE_INTERIOR,
    LAKE_OUTLET,
    COMPOUND_ESCAPE,
    CYCLE_ESCAPE,
    RESIDUAL_ESCAPE
}
