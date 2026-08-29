package org.codeberg.zenxarch.fastnoise.mixin;

import net.minecraft.world.biome.source.util.MultiNoiseUtil.Entries;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.SearchTree;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entries.class)
public interface EntriesAccessor<T> {
  @Accessor("tree")
  public SearchTree<T> zenxarch$tree();
}
