# North Cascades realism branch

Working branch: `codex/north-cascades-realism`. The editable IntelliJ project is `C:/Dev/FiniteWorlds`.
Released 1.0.1 is preserved on main at `73a6e51`; staged terrain work was checkpointed at `acc0eb4`.
Release candidate: 1.1.0 for Minecraft 1.21.8 / Fabric. Final JAR validation is recorded below.

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

## Phase 2 — drainage and erosion

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
prominence but exposed river bank seams and lake lattice shapes; phase 4 corrected those physical interfaces.

## Phase 3 — Terralith and mountain surfaces

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
checks verify the actual summit surface and river water. The earlier six dry center samples exposed physical shoreline problems; the final water audit samples every centerline point and is recorded below.

Phase 1–2 checkpoint: `368bb47`, pushed to the working branch.

## Phase 4 — physical flow continuity and visual refinement

The full column audit exposed more than a drawing problem: some receiving lakes sat above their feeder channels, bends entered neighboring lakes, and dry coarse banks could sit below the water. Connected lakes now obey their feeding water ceilings while retaining their basin depths. The shared distorted lake footprint constrains meanders; separated tributaries avoid early crossings. Actual touching junction levels are carried through the graph.

A final physical pass consolidates early contacts between graph nodes: downstream water stays at or below the first merged level. Connected lake surfaces remain coherent. Curved physical edges retain their exact centerline geometry; shortening those bends had introduced small water discontinuities. These are appearance-driven approximations, not a full geologic or fluid erosion simulation.

Reviewed actual solid-surface maps, absolute-height profiles and unexaggerated isometric views against NPS Inspiration, Silver and South Cascade photographs. The resulting ridges have separated summits and deep saddles; steep basin walls and tarns replace the shallow plateau appearance. Three scales of shoreline distortion reduce the visible lake lattice. Narrow rivers are shown at their real sampled width, so overview maps can hide subpixel channels.

The exact Minecraft biome-source maps show curved ecological boundaries and distinct regional biome IDs. Their categorical colors mean biome identity only; terrain maps separately encode altitude and slope illumination. Legends and selected images are retained in `docs/terrain-review` rather than checking in every generated diagnostic.

Phase 3 checkpoint: `1d2d20e`. Phase 4 checkpoint and final validation follow in the release record.

## Phase 5 — client and packaged release validation

The isolated Fabric client test selects Cascadia in the actual world-creation screen, verifies the selected generator and Y1535 dimension ceiling, generates completed chunks including the Y1400+ summit, checks actual forest tree placement, and saves/reopens the world. Terralith 2.5.13 passed creation and reload while preserving Finite terrain; the forest sample contained 868 log blocks. The development test runs under `build/run/clientGameTest`, independently of Prism.

A fourteen-seed audit includes 0, positive and negative seeds, and both signed 64-bit extremes. It checks every planned channel center against physical terrain columns, lake ordering, routing connectivity and the summit reserve. Final metrics and packaged-client results are saved with the release review.
