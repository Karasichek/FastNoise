package org.codeberg.zenxarch.fastnoise.parity;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ProtoChunk;
import org.codeberg.zenxarch.fastnoise.mixin.ChunkAccessor;

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

  public boolean matches(ProtoChunk chunk) {
    var pos = chunk.getPos();
    if (pos.x() != chunkX || pos.z() != chunkZ) return false;

    var sections = chunk.getSectionArray();
    if (sections.length != this.sections.length) return false;
    for (int i = 0; i < sections.length; i++) {
      if (!this.sections[i].matches(sections[i])) return false;
    }

    for (var heightmap : chunk.getHeightmaps()) {
      var key = heightmap.getKey();
      var value = heightmap.getValue();
      if (!Arrays.equals(value.asLongArray(), this.heightmaps.get(key.asString()))) return false;
    }

    var post = ((ChunkAccessor) chunk).zenxarch$postProcessingLists();
    if (post.length != this.postProcessingList.length) return false;

    for (int i = 0; i < post.length; i++) {
      if (!matches(post[i], this.postProcessingList[i])) return false;
    }

    return true;
  }

  public static final class ChunkStorageSection implements Serializable {
    public String[] biomeKeys;
    public int[] biomeData;

    public String[] blockStateKeys;
    public int[] blockStateData;

    public short nonEmptyBlockCount;
    public short nonEmptyFluidCount;
    public short randomTickableBlockCount;
    public short randomTickableFluidCount;

    public ChunkStorageSection(ChunkSection section) {
      this.nonEmptyBlockCount = section.nonEmptyBlockCount;
      this.nonEmptyFluidCount = section.nonEmptyFluidCount;
      this.randomTickableBlockCount = section.randomTickableBlockCount;
      this.randomTickableFluidCount = section.randomTickableFluidCount;

      {
        var biomes = ((PalettedContainer<RegistryEntry<Biome>>) section.biomeContainer);
        this.biomeKeys = new String[biomes.data.palette().getSize()];
        for (int i = 0; i < this.biomeKeys.length; i++) {
          this.biomeKeys[i] = biomes.data.palette().get(i).getIdAsString();
        }
        this.biomeData = new int[biomes.data.storage().getSize()];
        for (int i = 0; i < biomeData.length; i++) {
          this.biomeData[i] = biomes.data.storage().get(i);
        }
      }

      var blocks = section.blockStateContainer;
      this.blockStateKeys = new String[blocks.data.palette().getSize()];
      for (int i = 0; i < this.blockStateKeys.length; i++) {
        this.blockStateKeys[i] = blocks.data.palette().get(i).toString();
      }
      this.blockStateData = new int[blocks.data.storage().getSize()];
      for (int i = 0; i < blockStateData.length; i++) {
        this.blockStateData[i] = blocks.data.storage().get(i);
      }
    }

    public boolean matches(ChunkSection section) {
      if (this.nonEmptyBlockCount != section.nonEmptyBlockCount) return false;
      if (this.nonEmptyFluidCount != section.nonEmptyFluidCount) return false;
      if (this.randomTickableBlockCount != section.randomTickableBlockCount) return false;
      if (this.randomTickableFluidCount != section.randomTickableFluidCount) return false;

      var blocks = section.blockStateContainer;
      var blockStateKeys = new String[blocks.data.palette().getSize()];
      for (int i = 0; i < blockStateKeys.length; i++) {
        blockStateKeys[i] = blocks.data.palette().get(i).toString();
      }

      for (int i = 0; i < this.blockStateData.length; i++) {
        if (!Objects.equals(
            this.blockStateKeys[this.blockStateData[i]],
            blockStateKeys[blocks.data.storage().get(i)])) {
          return false;
        }
      }

      var biomes = ((PalettedContainer<RegistryEntry<Biome>>) section.biomeContainer);
      var biomeKeys = new String[biomes.data.palette().getSize()];
      for (int i = 0; i < biomeKeys.length; i++) {
        biomeKeys[i] = biomes.data.palette().get(i).getIdAsString();
      }

      for (int i = 0; i < this.biomeData.length; i++) {
        if (!Objects.equals(
            this.biomeKeys[this.biomeData[i]], biomeKeys[biomes.data.storage().get(i)])) {
          return false;
        }
      }

      return true;
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
