package dev.lemma.finiteworlds.core.hydrology;

/**
 * Proposed treatment for one measured raw depression.
 *
 * Pass 1E remains diagnostic only.  These actions describe what a later
 * conditioning pass is expected to do; they do not modify elevation or flow.
 */
public enum DepressionResolutionAction {
    FILL,
    BREACH,
    PRESERVE_LAKE,
    MERGE_COMPOUND
}
