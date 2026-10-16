package org.codeberg.zenxarch.perf.parity;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ProtoChunk;
import org.codeberg.zenxarch.perf.mixin.ChunkAccessor;

public final class ChunkStorage implements Serializable {
  public long seed;
  public ChunkStorageSection[] sections;
  public String worldName;
  public int chunkX;
  public int chunkZ;
  public Map<String, long[]> heightmaps;
  public short[][] postProcessingList;

  public ChunkStorage(long seed, String worldName, ProtoChunk chunk) {
    this.seed = seed;
    this.worldName = worldName;
    var pos = chunk.getPos();
    this.chunkX = pos.x();
    this.chunkZ = pos.z();

    var sections = chunk.getSectionArray();
    this.sections = new ChunkStorageSection[sections.length];

    for (int i = 0; i < sections.length; i++) {
      this.sections[i] = new ChunkStorageSection(sections[i]);
    }

    var postProcessingList = ((ChunkAccessor) chunk).zenxarch$postProcessingLists();

    this.postProcessingList = new short[postProcessingList.length][];
    for (int i = 0; i < postProcessingList.length; i++) {
      if (postProcessingList[i] != null)
        this.postProcessingList[i] = postProcessingList[i].toShortArray();
    }

    this.heightmaps = new Object2ObjectArrayMap<>();
    for (var heightmap : chunk.getHeightmaps()) {
      var key = heightmap.getKey();
      var value = heightmap.getValue();
      this.heightmaps.put(key.asString(), value.asLongArray().clone());
    }
    this.heightmaps = Map.copyOf(this.heightmaps);
  }

  @Override
  public String toString() {
    var res = new StringBuilder();
    res.append("ChunkStorage[");

    res.append("seed=").append(seed).append(",");
    res.append("worldName=").append(worldName).append(",");
    res.append("chunkX=").append(chunkX).append(",");
    res.append("chunkZ=").append(chunkZ).append(",");

    // TODO: make the output more reasonable
    // res.append("sections=").append(Arrays.toString(this.sections)).append(",");

    // TODO: make the output more reasonable
    // res.append("heightmaps=").append(this.heightmaps).append("\n");
    // res.append("postProcessingList=").append(Arrays.deepToString(this.postProcessingList));

    res.append("]");
    return res.toString();
  }

  private static final String RED_COLOR = "\033[31m";
  private static final String GREEN_COLOR = "\033[32m";
  private static final String RESET_COLOR = "\033[0m";

  private static StringBuilder append(StringBuilder res, int value, int size) {
    var l = res.length();
    res.append(value);

    var c = res.length() - l;
    while (c < size) {
      res.insert(l, ' ');
      c++;
    }
    return res;
  }

  private static void blockMatch(StringBuilder res, int a, int b, int size) {
    if (a == b) {
      append(res, a, size + size);
      return;
    }
    res.append(GREEN_COLOR);
    append(res, a, size).append(RED_COLOR);
    append(res, b, size).append(RESET_COLOR);
  }

  public String matches(ProtoChunk chunk) {
    var pos = chunk.getPos();
    if (pos.x() != chunkX || pos.z() != chunkZ) return "Chunk Pos don't match";

    var res = new StringBuilder();

    for (var heightmap : chunk.getHeightmaps()) {
      var key = heightmap.getKey();
      var value = heightmap.getValue();
      if (!Arrays.equals(value.asLongArray(), this.heightmaps.get(key.asString()))) {
        final var size =
            Math.max(
                String.valueOf(chunk.getTopYInclusive()).length(),
                String.valueOf(chunk.getBottomY()).length());
        var self = new Heightmap(chunk, key);
        self.setTo(chunk, key, this.heightmaps.get(key.asString()));

        res.append("Heightmap ").append(key.asString()).append(" doesn't match\n");

        for (int z = 0; z < 16; z++) {
          for (int x = 0; x < 16; x++)
            res.append(self.get(x, z) == value.get(x, z) ? "." : RED_COLOR + "+" + RESET_COLOR);
          if (z == 7) res.append("\n");
        }
        res.append("\n");

        for (int z = 0; z < 16; z++) {
          for (int x = 0; x < 16; x++) blockMatch(res, self.get(x, z), value.get(x, z), size);
          res.append("\n");
        }
      }
    }

    var post = ((ChunkAccessor) chunk).zenxarch$postProcessingLists();
    if (post.length != this.postProcessingList.length)
      return "Post processing lists length don't match";

    for (int i = 0; i < post.length; i++) {
      var ps = (post[i] == null) ? null : post[i].toShortArray();
      if (!Arrays.equals(ps, this.postProcessingList[i])) {
        res.append("Post Processing list[").append(i).append("] doesn't match\n");
        res.append(Arrays.toString(ps))
            .append("\n")
            .append(Arrays.toString(this.postProcessingList[i]))
            .append("\n");
      }
    }

    var sections = chunk.getSectionArray();
    if (sections.length != this.sections.length) return "Chunk section's length don't match";
    for (int i = 0; i < sections.length; i++) {
      var len = res.length();
      this.sections[i].matches(res, sections[i]);

      if (len != res.length())
        res.insert(
            len,
            "sections[" + i + "] at y = " + (chunk.getBottomY() + (i * 16)) + " is different\n");
    }

    if (res.isEmpty()) return null;
    return res.toString();
  }

  public static final class ChunkStorageSection implements Serializable {
    public PalettedContainerStorage biomes;
    public PalettedContainerStorage blocks;

    public short nonEmptyBlockCount;
    public short nonEmptyFluidCount;
    public short randomTickableBlockCount;
    public short randomTickableFluidCount;

    public ChunkStorageSection(ChunkSection section) {
      this.nonEmptyBlockCount = section.nonEmptyBlockCount;
      this.nonEmptyFluidCount = section.nonEmptyFluidCount;
      this.randomTickableBlockCount = section.randomTickableBlockCount;
      this.randomTickableFluidCount = section.randomTickableFluidCount;

      var biomes = ((PalettedContainer<RegistryEntry<Biome>>) section.biomeContainer);
      this.biomes = new PalettedContainerStorage(biomes, RegistryEntry::getIdAsString);

      this.blocks = new PalettedContainerStorage(section.blockStateContainer, BlockState::toString);
    }

    public void matches(StringBuilder res, ChunkSection section) {
      matchValue(res, "nonEmptyBlockCount", this.nonEmptyBlockCount, section.nonEmptyBlockCount);
      matchValue(res, "nonEmptyFluidCount", this.nonEmptyFluidCount, section.nonEmptyFluidCount);
      matchValue(
          res,
          "randomTickableBlockCount",
          this.randomTickableBlockCount,
          section.randomTickableBlockCount);
      matchValue(
          res,
          "randomTickableFluidCount",
          this.randomTickableFluidCount,
          section.randomTickableFluidCount);

      this.blocks.matches(res, section.blockStateContainer, BlockState::toString);
      this.biomes.matches(
          res,
          ((PalettedContainer<RegistryEntry<Biome>>) section.biomeContainer),
          RegistryEntry::getIdAsString);
    }

    private void matchValue(StringBuilder builder, String name, int a, int b) {
      if (a != b) builder.append(name).append(": ").append(a).append(" | ").append(b).append("\n");
    }

    @Override
    public String toString() {
      var res = new StringBuilder();
      res.append("ChunkStorageSection[");
      res.append("nonEmptyBlockCount=")
          .append(this.nonEmptyBlockCount)
          .append(",")
          .append("nonEmptyFluidCount=")
          .append(this.nonEmptyFluidCount)
          .append(",")
          .append("randomTickableBlockCount=")
          .append(this.randomTickableBlockCount)
          .append(",")
          .append("randomTickableFluidCount=")
          .append(this.randomTickableFluidCount)
          .append(",");

      res.append("blocks=").append(this.blocks).append(",").append("biomes=").append(this.biomes);

      res.append("]");
      return res.toString();
    }
  }

  public static final class PalettedContainerStorage implements Serializable {
    public String[] keys;
    public int[] data;

    public <T> PalettedContainerStorage(
        PalettedContainer<T> container, Function<T, String> converter) {
      var data = container.data;
      keys = new String[data.palette().getSize()];
      for (int i = 0; i < keys.length; i++) keys[i] = converter.apply(data.palette().get(i));

      this.data = new int[data.storage().getSize()];
      for (int i = 0; i < this.data.length; i++) this.data[i] = data.storage().get(i);
    }

    public <T> void matches(
        StringBuilder builder, PalettedContainer<T> container, Function<T, String> converter) {
      matches(builder, new PalettedContainerStorage(container, converter));
    }

    private void matches(StringBuilder builder, PalettedContainerStorage other) {
      var different = false;
      for (int i = 0; i < other.data.length; i++) {
        var idxA = this.data[i];
        var keyA = this.keys[idxA];
        var idxB = other.data[i];
        var keyB = other.keys[idxB];
        if (!Objects.equals(keyA, keyB)) {
          different = true;
          break;
        }
      }

      if (!different) return;
      final var commonIdx = new Object2IntOpenHashMap<String>(this.keys.length);
      for (var key : this.keys) commonIdx.computeIfAbsent(key, k -> commonIdx.size());
      for (var key : other.keys) commonIdx.computeIfAbsent(key, k -> commonIdx.size());

      commonIdx.forEach(
          (a, b) -> {
            builder.append("[").append(b).append("]=").append(a).append("\n");
          });
      final var size = String.valueOf(commonIdx.size() - 1).length();
      final var width =
          switch (size) {
            case 1 -> 64;
            case 2 -> 32;
            case 3 -> 16;
            default -> 8;
          };

      var selfIdx = new int[width];
      var otherIdx = new int[width];
      for (int i = 0; i < other.data.length; i += width) {
        for (int j = 0; j < width; j++) {
          selfIdx[j] = commonIdx.getOrDefault(this.keys[this.data[i + j]], -1);
          otherIdx[j] = commonIdx.getOrDefault(other.keys[other.data[i + j]], -1);
        }
        if (!Arrays.equals(selfIdx, otherIdx)) {
          builder.append("[");
          append(builder, i, 4).append("]=|");
          for (var k = 0; k < width; k++)
            builder.append(selfIdx[k] == otherIdx[k] ? "." : RED_COLOR + "+" + RESET_COLOR);
          builder.append("| ");
          for (var k = 0; k < width; k++) blockMatch(builder, selfIdx[k], otherIdx[k], size);
          builder.append("\n");
        }
      }
    }

    @Override
    public String toString() {
      var res = new StringBuilder();
      res.append("PalettedContainerStorage[");

      res.append(Arrays.toString(this.keys)).append(",");

      for (int i = 0; i < this.data.length; i++) res.append(Integer.toHexString(data[i]));

      res.append("]");
      return res.toString();
    }
  }
}
