# North Cascades realism branch

Working branch: `codex/north-cascades-realism`. The editable IntelliJ project is `C:/Dev/FiniteWorlds`.
Released 1.0.1 is preserved on main at `73a6e51`; staged terrain work was checkpointed at `acc0eb4`.
Changes below are under visual review, not yet a finished release.

## Reference terrain

[USGS North Cascades geology](https://pubs.usgs.gov/sim/2940/) describes resistant bedrock, uplift and glacial dissection.
[NPS glacier comparison photographs](https://www.nps.gov/noca/learn/nature/glacial-mass-balance8.htm) show sharp divides,
cirques, alpine lakes and deep connected valleys. [NPS lakes and ponds](https://www.nps.gov/noca/learn/nature/lakesandponds.htm)
records more than 500 lakes and ponds. These guide appearance; this is a procedural interpretation, not a DEM reconstruction.

## Phase 0 â€” baseline diagnosis

Inspected the stream-network, river-relief and landmark-elevation PNGs. Near-planar lowlands produced dense parallel D8 lines;
ordinary uplifts read as shallow plateaus. Seed 12345 had 1,560 headwaters, 2,495 channel segments and mean centerline sinuosity 1.002.
The water materializer also retained low coarse valley corners outside channels, allowing dry banks below nearby water.

## Phase 1 â€” relief and height budget

Ordinary Cascade uplift now has sharp domain-warped ridges and stronger foothill shoulders, separate from volcanic uplift.
The landmark summit target is Y1432, approximately 103 blocks below the dimension ceiling Y1535.
Lowland undulations give catchments varied drainage directions; an additional short-wavelength coastline layer roughens inlets.

## Phase 2 â€” drainage and erosion (ongoing)

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

## Phase 3 — Terralith and mountain surfaces (ongoing)

The actual Terralith 2.5.13 datapack defines `minecraft:overworld`. Minecraft's dimension merge prefers that loaded
definition over a chosen world preset, explaining the reported generator takeover. A scoped compatibility hook preserves
the selected/saved Finite overworld and its tall dimension; unrelated presets still use their selected datapacks normally.

A test loads real vanilla, Terralith and Finite resource packs through Minecraft RegistryLoader, decodes Cascadia, performs
the actual dimension merge, and initializes Minecraft's feature-order graph. Twenty-seven natural Terralith palette entries
are now available, with coherent regional variants and vanilla forest, birch forest, flower forest and meadow alternatives.
Warm registered companions retain Terralith vegetation, spawn settings, effects and biome tags below the authored snow belt.
These companions are built at runtime from the user's loaded biomes; no third-party biome definitions are bundled.

Physical steep slopes and high alpine surfaces now expose stone. Gentle high terrain receives snow blocks above the permanent
snowline, around Y1000 with regional variation. The lower seasonal belt remains around Y624. Physical chunk and column sample
checks verify the actual summit surface and river water. Seed 12345's rendered summit measured Y1431.87, with 3550/3556 sampled
river centers wet; lake interfaces and rounded shallow cells account for the remaining samples and are still being reviewed.

Phase 1–2 checkpoint: `368bb47`, pushed to the working branch.
