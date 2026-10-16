package org.codeberg.zenxarch.fastnoise.mixin;

import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.util.collection.PaletteStorage;
import net.minecraft.world.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Heightmap.class)
public interface HeightmapAccessor {
  @Accessor("storage")
  public PaletteStorage zenxarch$getStorage();

  @Accessor("blockPredicate")
  public Predicate<BlockState> zenxarch$getBlockPredicate();
}
