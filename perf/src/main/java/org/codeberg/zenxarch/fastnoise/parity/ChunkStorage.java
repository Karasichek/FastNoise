package org.codeberg.zenxarch.fastnoise.parity;

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
import org.codeberg.zenxarch.fastnoise.mixin.ChunkAccessor;

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
    res.append("ChunkStorage[");

    res.append("seed=").append(seed).append(",");
    res.append("worldName=").append(worldName).append(",");
    res.append("chunkX=").append(chunkX).append(",");
    res.append("chunkZ=").append(chunkZ).append(",");

    res.append("sections=").append(Arrays.toString(this.sections)).append(",");

    res.append("heightmaps=").append(this.heightmaps).append(",");
    res.append("postProcessingList=").append(Arrays.deepToString(this.postProcessingList));

    res.append("]");
    return res.toString();
  }

  public String matches(ProtoChunk chunk) {
    var pos = chunk.getPos();
    if (pos.x() != chunkX || pos.z() != chunkZ) return "Chunk Pos don't match";

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
      var err = this.sections[i].matches(sections[i]);
      if (err != null) return err;
    }

    return null;
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

    public String matches(ChunkSection section) {
      if (this.nonEmptyBlockCount != section.nonEmptyBlockCount)
        return "nonEmptyBlockCount don't match";
      if (this.nonEmptyFluidCount != section.nonEmptyFluidCount)
        return "nonEmptyFluidCount don't match";
      if (this.randomTickableBlockCount != section.randomTickableBlockCount)
        return "randomTickableBlockCount don't match";
      if (this.randomTickableFluidCount != section.randomTickableFluidCount)
        return "randomTickableFluidCount don't match";

      {
        var blocksErr = this.blocks.matches(section.blockStateContainer, BlockState::toString);
        if (blocksErr != null) return "Blocks " + blocksErr;
      }

      {
        var biomesErr =
            this.biomes.matches(
                ((PalettedContainer<RegistryEntry<Biome>>) section.biomeContainer),
                RegistryEntry::getIdAsString);
        if (biomesErr != null) return "Biomes " + biomesErr;
      }

      return null;
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

    public <T> String matches(PalettedContainer<T> container, Function<T, String> converter) {
      return matches(new PalettedContainerStorage(container, converter));
    }

    private String matches(PalettedContainerStorage other) {
      for (int i = 0; i < other.data.length; i++) {
        var idxA = this.data[i];
        var keyA = this.keys[idxA];
        var idxB = other.data[i];
        var keyB = other.keys[idxB];
        if (!Objects.equals(keyA, keyB))
          return "Storage entries don't match "
              + idxA
              + " "
              + keyA
              + " , "
              + idxB
              + " "
              + keyB
              + other.keys[other.data[i]];
      }

      return null;
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
