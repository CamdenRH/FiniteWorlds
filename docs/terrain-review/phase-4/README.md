# Physical terrain review — phase 4

Seed 12345. Read [the terrain legend](review-legend.md) before comparing colors. All current terrain views sample the production block-column sampler, including local detail and physical water. The two perspective meshes use equal vertical and horizontal block scales. They are terrain review renders, not Minecraft screenshots; phase 5 retains client screenshots.

| Review | What it establishes |
|---|---|
| [Continent](review-continent.png) | Single continent, coast outline and distribution of relief/water |
| [Ordinary Cascades](review-cascades.png), [perspective](review-cascades-perspective.png) | Separate ridges, summits, valleys and alpine basins outside the volcanic landmark |
| [Volcano](review-mountains.png), [perspective](review-volcano-perspective.png) | High cone and surrounding mountain relief |
| [Cascade profile](review-cascade-profile.png), [cross range](review-cross-range.png) | Actual Y elevations on labeled transects |
| [Biome overview](biomes-continent.png), [mountain crop](biomes-mountains.png) | Exact Minecraft biome-source IDs, curved boundaries and regional diversity |
| [Biome legend](biome-legend.md) | Categorical ID colors and overview counts; 42 distinct IDs actually sampled |

## Fourteen-seed acceptance audit

[Full CSV](seed-audit.csv) covers 0, 1–6, 42, 777, 12345, 20260930, -987654321 and both signed 64-bit extremes. Each summit was Y1432.000, leaving 103 blocks to the Y1535 ceiling. All 951,341 sampled planned centerline points contained physical water. No sampled channel had a downstream water rise over one block; measured maximum rise was zero. All routing graphs had zero routed cycles or internal sinks and no lake-to-lake channel rose downstream.

The near-bank normal probe remains a diagnostic rather than an acceptance assertion: 26 of 232,245 sampled dry-bank probes fell below their associated center water (maximum 9.91 blocks). Some lie beside steep curved channel transitions, where a normal probe can point toward a lower downstream reach. These residual local interfaces deserve visual review; the audit does not prove every bank or every fluid tick is correct. Centerline sampling also does not cover every intervening block. The terrain is a procedural approximation of glacially dissected North Cascades relief and catchment erosion, not a DEM reconstruction or a geologic/fluid simulation.

The [NPS glacier photographs](https://www.nps.gov/noca/learn/nature/glacial-mass-balance8.htm) were viewed directly: Inspiration's sharp divides, Silver's steep cirque walls and lake, and South Cascade's elongated glacial valley guided the review. [USGS geology](https://pubs.usgs.gov/sim/2940/) supplies the regional bedrock and glacial-dissection context. [NPS lakes and ponds](https://www.nps.gov/noca/learn/nature/lakesandponds.htm) supports the use of numerous mountain basins.

## Reproduce

With Java 21, run from the repository root:

```powershell
.\gradlew.bat :worldgen-tools:auditTerrain '-Pseeds=12345,1,42,-987654321,0,2,3,4,5,6,777,20260930,9223372036854775807,-9223372036854775808'
.\gradlew.bat :worldgen-tools:generateWorldPreview -Pseed=12345
.\gradlew.bat test -PbiomeReview=true '-PterralithJar=build/compat-mods/Terralith_1.21.x_v2.5.13.jar'
```

Use the [official Terralith 2.5.13 release](https://modrinth.com/datapack/terralith/version/JKg71Gq0) for the optional biome review. Third-party biome assets are not copied into this repository or the Finite Worlds JAR.

