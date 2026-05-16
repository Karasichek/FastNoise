package org.codeberg.zenxarch.fastnoise.surface;

import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.chunk.BlockColumn;
import org.codeberg.zenxarch.fastnoise.heightmap.HeightmapUtil;
import org.codeberg.zenxarch.fastnoise.mixin.HeightmapAccessor;

public class FastBlockColumn implements BlockColumn {

  private final Chunk chunk;

  private int lx;
  private int lz;

  private final int minY;
  private final PaletteStorage[] heightmapData;
  private final ChunkSection[] sections;

  private final BlockState VOID_AIR = Blocks.VOID_AIR.getDefaultState();
  private final BlockState AIR = Blocks.AIR.getDefaultState();

  private static final Heightmap.Type[] heightmaps =
      HeightmapUtil.calculateHeightmaps(ChunkStatus.SURFACE);

  @SuppressWarnings("unchecked")
  private final Predicate<BlockState>[] predicates =
      Stream.of(heightmaps).map(type -> type.getBlockPredicate()).toArray(Predicate[]::new);

  private static final long completedMask = getCompletedMask(heightmaps);

  private static long getCompletedMask(Heightmap.Type[] heightmaps) {
    return (0x1L << heightmaps.length) - 1;
  }

  private long mask;

  public FastBlockColumn(final Chunk chunk) {
    this.chunk = chunk;
    this.minY = this.chunk.getBottomY();
    this.heightmapData = new PaletteStorage[heightmaps.length];

    for (int i = 0; i < heightmapData.length; i++) {
      this.heightmapData[i] =
          ((HeightmapAccessor) chunk.getHeightmap(heightmaps[i])).zenxarch$getStorage();
    }

    this.sections = chunk.getSectionArray();
    this.mask = 0x0L;
  }

  public void updateXZ(int x, int z) {
    this.lx = x;
    this.lz = z;
    this.mask = 0x0L;
  }

  public int getSectionIndex(int y) {
    return (y - minY) >> 4;
  }

  public ChunkSection getSection(int y) {
    return this.sections[getSectionIndex(y)];
  }

  private ChunkSection zenxarch$getSection(final int y) {
    var cy = getSectionIndex(y);
    if (cy < 0 || cy >= sections.length) return null;
    return this.sections[cy];
  }

  @Override
  public BlockState getState(int y) {
    var section = zenxarch$getSection(y);
    if (section == null) return VOID_AIR;
    if (section.isEmpty()) return AIR;
    return section.getBlockState(lx, y & 0xF, lz);
  }

  @Override
  public void setState(int y, BlockState state) {
    var cy = getSectionIndex(y);
    if (cy < 0 || cy >= sections.length) return;
    var section = this.sections[cy];

    final int ly = y & 0xF;

    section.setBlockState(lx, ly, lz, state, false);
    this.fastUpdateHeightmap(lx, lz, y, state);

    if (state.getFluidState().isEmpty()) return;

    Chunk.getList(chunk.getPostProcessingLists(), cy).add((short) (lx | ly << 4 | lz << 8));
  }

  public void fastUpdateHeightmap(int lx, int lz, int iy, BlockState state) {
    if (this.mask == completedMask) return;
    final int y = iy - minY;
    for (int i = 0; i < heightmapData.length; i++) {
      if ((mask & (0x1L << i)) != 0) continue;
      if (HeightmapUtil.updateHeightmap(
          lx, lz, heightmapData[i], predicates[i], state, y, sections)) {
        mask |= (0x1L << i);
      }
    }
  }
}
