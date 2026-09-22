package org.codeberg.zenxarch.matcomp.rules;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.jspecify.annotations.Nullable;

public final class LithostitchedRules {
  public static interface Bandlands {
    public BlockState getBand(SurfaceBuilder system, int x, int y, int z);
  }

  public static interface BandlandsRule extends MaterialRule {
    public RegistryEntry<Bandlands> options();
  }

  public static BlockStateRule compile(BandlandsRule rule, MaterialRuleContext ctx) {
    return new BandlandsRuleCompiled(rule.options().value(), ctx.surfaceBuilder);
  }

  public static record BandlandsRuleCompiled(Bandlands bandlands, SurfaceBuilder builder)
      implements BlockStateRule {
    @Override
    public @Nullable BlockState tryApply(int x, int y, int z) {
      return bandlands.getBand(builder, x, y, z);
    }
  }
}
