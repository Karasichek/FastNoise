package org.codeberg.zenxarch.matcomp.rules;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import net.minecraft.world.gen.surfacebuilder.SurfaceBuilder;
import org.jetbrains.annotations.Nullable;

public final class LithostitchedRules {
  public static interface Bandlands {
    public BlockState getBand(SurfaceBuilder system, int x, int y, int z);
  }

  public static interface BandlandsRule extends MaterialRule {
    public RegistryEntry<Bandlands> options();
  }

  public static interface ReferenceRule extends MaterialRule {
    public RegistryEntryList<MaterialRule> options();
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
