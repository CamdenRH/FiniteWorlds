package dev.lemma.finiteworlds.core.hydrology;

/**
 * One Hydrology Pass 2H corridor sample attached to a 2G centerline point.
 *
 * All widths are half-widths measured outward from the river centerline in
 * block space. The channel corridor reserves room for the physical channel
 * and near-bank terrain; the floodplain corridor marks low-gradient lateral
 * freedom; the valley corridor is the outer envelope that later erosion is
 * allowed to reorganize.
 */
public record RiverValleyCorridorPoint(
        double blockX,
        double blockZ,
        double distanceBlocks,
        double channelHalfWidthBlocks,
        double floodplainHalfWidthBlocks,
        double valleyHalfWidthBlocks,
        double confinement,
        double localGrade
) {
}
