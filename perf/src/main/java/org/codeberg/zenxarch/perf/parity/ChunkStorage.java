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
      if (postProcessingList[i] == null) this.postProcessingList[i] = null;
      else this.postProcessingList[i] = postProcessingList[i].toShortArray();
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
    res.append("ChunkStorage[\n");

    res.append("seed=").append(seed).append("\n");
    res.append("worldName=").append(worldName).append("\n");
    res.append("chunkX=").append(chunkX).append("\n");
    res.append("chunkZ=").append(chunkZ).append("\n");

    // TODO: make the output more reasonable
    // res.append("sections=").append(Arrays.toString(this.sections)).append(",");

    res.append("heightmaps=").append(this.heightmaps).append("\n");
    res.append("postProcessingList=").append(Arrays.deepToString(this.postProcessingList));

    res.append("\n]");
    return res.toString();
  }

  private static final String RED_COLOR = "\033[31m";
  private static final String GREEN_COLOR = "\033[32m";
  private static final String RESET_COLOR = "\033[0m";

  public String matches(ProtoChunk chunk) {
    var pos = chunk.getPos();
    if (pos.x() != chunkX || pos.z() != chunkZ) return "Chunk Pos don't match";

    var res = new StringBuilder();

    for (var heightmap : chunk.getHeightmaps()) {
      var key = heightmap.getKey();
      var value = heightmap.getValue();
      if (!Arrays.equals(value.asLongArray(), this.heightmaps.get(key.asString())))
        return "Heightmap don't match";
    }

    var post = ((ChunkAccessor) chunk).zenxarch$postProcessingLists();
    if (post.length != this.postProcessingList.length)
      return "Post processing lists length don't match";

    for (int i = 0; i < post.length; i++) {
      if (!matches((post[i] == null) ? null : post[i].toShortArray(), this.postProcessingList[i]))
        return "Post processing lists don't match";
    }

    var sections = chunk.getSectionArray();
    if (sections.length != this.sections.length) return "Chunk section's length don't match";
    for (int i = 0; i < sections.length; i++) {
      var len = res.length();
      this.sections[i].matches(res, sections[i]);

      if (len != res.length()) res.insert(len, "sections[" + i + "] is different\n");
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
      var selfIdx = new int[16];
      var otherIdx = new int[16];
      final var commonIdx = new Object2IntOpenHashMap<String>(this.keys.length);
      for (var key : this.keys) commonIdx.computeIfAbsent(key, k -> commonIdx.size());
      for (var key : other.keys) commonIdx.computeIfAbsent(key, k -> commonIdx.size());

      commonIdx.forEach(
          (a, b) -> {
            builder.append("[").append(b).append("]=").append(a).append("\n");
          });

      for (int i = 0; i < other.data.length; i += 16) {
        for (int j = 0; j < 16; j++) {
          selfIdx[j] = commonIdx.getOrDefault(this.keys[this.data[i + j]], -1);
          otherIdx[j] = commonIdx.getOrDefault(other.keys[other.data[i + j]], -1);
        }
        if (!Arrays.equals(selfIdx, otherIdx)) {
          builder.append("[").append(i).append("]=|");
          for (var k = 0; k < 16; k++)
            builder.append(selfIdx[k] == otherIdx[k] ? "." : RED_COLOR + "+" + RESET_COLOR);
          builder.append("| ");
          for (var k = 0; k < 16; k++) blockMatch(builder, selfIdx[k], otherIdx[k]);
          builder.append("\n");
        }
      }
    }

    private void blockMatch(StringBuilder res, int a, int b) {
      if (a == b) {
        res.append(a).append(" ");
        return;
      }
      res.append(GREEN_COLOR).append(a).append(RED_COLOR).append(b).append(RESET_COLOR).append(" ");
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

  private static boolean matches(short[] a, short[] b) {
    if (a == null || b == null) return a == b;
    if (a.length != b.length) return false;
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) return false;
    }
    return true;
  }
}
