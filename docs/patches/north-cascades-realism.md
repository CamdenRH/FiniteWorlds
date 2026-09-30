# North Cascades realism branch

Working branch: `codex/north-cascades-realism`. The editable IntelliJ project is `C:/Dev/FiniteWorlds`.
Released 1.0.1 is preserved on main at `73a6e51`; staged terrain work was checkpointed at `acc0eb4`.
Changes below are under visual review, not yet a finished release.

## Reference terrain

[USGS North Cascades geology](https://pubs.usgs.gov/sim/2940/) describes resistant bedrock, uplift and glacial dissection.
[NPS glacier comparison photographs](https://www.nps.gov/noca/learn/nature/glacial-mass-balance8.htm) show sharp divides,
cirques, alpine lakes and deep connected valleys. [NPS lakes and ponds](https://www.nps.gov/noca/learn/nature/lakesandponds.htm)
records more than 500 lakes and ponds. These guide appearance; this is a procedural interpretation, not a DEM reconstruction.

## Phase 0 — baseline diagnosis

Inspected the stream-network, river-relief and landmark-elevation PNGs. Near-planar lowlands produced dense parallel D8 lines;
ordinary uplifts read as shallow plateaus. Seed 12345 had 1,560 headwaters, 2,495 channel segments and mean centerline sinuosity 1.002.
The water materializer also retained low coarse valley corners outside channels, allowing dry banks below nearby water.

## Phase 1 — relief and height budget

Ordinary Cascade uplift now has sharp domain-warped ridges and stronger foothill shoulders, separate from volcanic uplift.
The landmark summit target is Y1432, approximately 103 blocks below the dimension ceiling Y1535.
Lowland undulations give catchments varied drainage directions; an additional short-wavelength coastline layer roughens inlets.

## Phase 2 — drainage and erosion (ongoing)

Larger rainfall-sensitive channel initiation areas consolidate the permanent network. Wider, reach-length-limited meanders keep
shared graph endpoints. Depression lakes are more common, with smooth coordinate distortion around their shores.
Climate and catchment runoff are computed before channel planning. A stream-power proxy uses effective discharge and gradient
for mountain incision, tapering cuts at lake connections. Reach budgets allow deeper mountain valleys while bounding modification.
Physical channel envelopes replace coarse carved beds/banks and blend back to the surrounding terrain.

First relief pass: 144 headwaters, 520 channel segments, mean sinuosity 1.030, maximum lateral offset 143 blocks.
After refining lake selection: 708 lakes, 151 headwaters, 511 channel segments. These counts are diagnostic, not acceptance targets.

## Visual review

`worldgen-tools/preview/<seed>/review-continent.png`, `review-mountains.png` and `review-cross-range.png` sample the actual
block-column terrain after local detail and physical water. The companion `review-legend.md` defines elevation hues, northwest
slope illumination, water types, coordinates and pixel scale. River strokes are not enlarged. The first crop showed greater
prominence but exposed river bank seams and lake lattice shapes; those are being corrected before release.
