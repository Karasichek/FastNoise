package org.codeberg.zenxarch.fastnoise.parity;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
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
import org.codeberg.zenxarch.fastnoise.FastNoiseConstants;
import org.codeberg.zenxarch.fastnoise.mixin.ChunkAccessor;
import org.codeberg.zenxarch.fastnoise.parity.ChunkStorage.PalettedContainerStorage;

public final class ChunkStorage implements Serializable {
  public String worldName;
  public int chunkX;
  public int chunkZ;
  Map<String, long[]> heightmaps;
  ChunkStorageSection[] sections;
  ShortList[] postProcessingList;

  public ChunkStorage(String worldName, ProtoChunk chunk) {
    this.worldName = worldName;
    var pos = chunk.getPos();
    this.chunkX = pos.x();
    this.chunkZ = pos.z();

    var sections = chunk.getSectionArray();
    this.sections = new ChunkStorageSection[sections.length];

    for (int i = 0; i < sections.length; i++) {
      this.sections[i] = new ChunkStorageSection(sections[i]);
    }

    this.postProcessingList = ((ChunkAccessor) chunk).zenxarch$postProcessingLists();

    this.heightmaps = new Object2ObjectArrayMap<>();
    for (var heightmap : chunk.getHeightmaps()) {
      var key = heightmap.getKey();
      var value = heightmap.getValue();
      this.heightmaps.put(key.asString(), value.asLongArray().clone());
    }
  }

  public String matches(ProtoChunk chunk) {
    var pos = chunk.getPos();
    if (pos.x() != chunkX || pos.z() != chunkZ) return "Chunk Pos don't match";

    FastNoiseConstants.LOGGER.info("matching sections {}", chunk.getPos());
    var sections = chunk.getSectionArray();
    if (sections.length != this.sections.length) return "Chunk section's length don't match";
    for (int i = 0; i < sections.length; i++) {
      var err = this.sections[i].matches(sections[i]);
      if (err != null) return err;
    }

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
      if (!matches(post[i], this.postProcessingList[i])) return "Post processing lists don't match";
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
        if (!Objects.equals(this.keys[this.data[i]], other.keys[other.data[i]]))
          return "Storage entries don't match "
              + this.keys[this.data[i]]
              + " "
              + other.keys[other.data[i]];
      }

      return null;
    }
  }

  private static boolean matches(ShortList a, ShortList b) {
    if (a == null || b == null) return a == b;
    if (a.size() != b.size()) return false;
    for (int i = 0; i < a.size(); i++) {
      if (a.getShort(i) != b.getShort(i)) return false;
    }
    return true;
  }
}
