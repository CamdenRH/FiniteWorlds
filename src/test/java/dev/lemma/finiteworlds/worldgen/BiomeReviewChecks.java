package dev.lemma.finiteworlds.worldgen;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;

/** Optional visual audit of the exact Minecraft biome source, at surface quart coordinates. */
final class BiomeReviewChecks {
  static void write(FiniteBiomeSource source) throws Exception {
    if (!Boolean.getBoolean("finiteworlds.biomeReview")) return;
    source = source.withSeed(12345L);
    var ids =
        source.getBiomes().stream()
            .map(b -> b.getKey().orElseThrow().getValue().toString())
            .sorted()
            .toList();
    Map<String, Color> colors = new HashMap<>();
    for (int i = 0; i < ids.size(); i++)
      colors.put(ids.get(i), Color.getHSBColor((float) ((i * .61803398875) % 1), .55f, .85f));
    Path directory = Path.of(System.getProperty("finiteworlds.projectDir"), "build/terrain-review");
    Files.createDirectories(directory);
    Map<String, Integer> counts = new TreeMap<>();
    render(
        source,
        colors,
        directory.resolve("biomes-continent.png"),
        -32768,
        -32768,
        65536,
        768,
        counts);
    render(
        source,
        colors,
        directory.resolve("biomes-mountains.png"),
        -1630,
        -6178,
        8192,
        1024,
        new TreeMap<>());
    BufferedImage legend = new BufferedImage(780, 40 + ids.size() * 23, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = legend.createGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, legend.getWidth(), legend.getHeight());
    g.setColor(Color.DARK_GRAY);
    g.drawString(
        "Exact Minecraft biome IDs: stable categorical colors, brightness carries no elevation",
        12,
        20);
    StringBuilder table =
        new StringBuilder(
            "# Exact biome source review — seed 12345\n\n"
                + "Color identifies a biome registry ID; brightness is constant. Overview: 65536"
                + " blocks at 85.33 blocks/pixel. Mountain crop: 8192 blocks at 8 blocks/pixel,"
                + " centered (2466,-2082).\n\n"
                + "| Biome | Overview samples |\n"
                + "|---|---:|\n");
    for (int i = 0; i < ids.size(); i++) {
      String id = ids.get(i);
      g.setColor(colors.get(id));
      g.fillRect(12, 32 + i * 23, 25, 17);
      g.setColor(Color.DARK_GRAY);
      g.drawString(id + " (" + counts.getOrDefault(id, 0) + ")", 45, 46 + i * 23);
      table.append("| ").append(id).append(" | ").append(counts.getOrDefault(id, 0)).append(" |\n");
    }
    g.dispose();
    ImageIO.write(legend, "png", directory.resolve("biome-legend.png").toFile());
    Files.writeString(directory.resolve("biome-legend.md"), table);
    if (counts.size() < 18)
      throw new AssertionError("Insufficient actual biome diversity: " + counts.size());
  }

  private static void render(
      FiniteBiomeSource source,
      Map<String, Color> colors,
      Path path,
      int left,
      int top,
      int span,
      int size,
      Map<String, Integer> counts)
      throws Exception {
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    for (int z = 0; z < size; z++)
      for (int x = 0; x < size; x++) {
        int bx = left + (int) ((x + .5) * span / size), bz = top + (int) ((z + .5) * span / size);
        String id =
            source
                .getBiome(Math.floorDiv(bx, 4), 0, Math.floorDiv(bz, 4), null)
                .getKey()
                .orElseThrow()
                .getValue()
                .toString();
        counts.merge(id, 1, Integer::sum);
        image.setRGB(x, z, colors.get(id).getRGB());
      }
    ImageIO.write(image, "png", path.toFile());
  }
}
