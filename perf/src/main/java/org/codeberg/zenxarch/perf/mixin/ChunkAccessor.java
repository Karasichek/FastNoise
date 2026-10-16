package org.codeberg.zenxarch.perf.mixin;

import it.unimi.dsi.fastutil.shorts.ShortList;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Chunk.class)
public interface ChunkAccessor {
  @Accessor("postProcessingLists")
  public ShortList[] zenxarch$postProcessingLists();
}
