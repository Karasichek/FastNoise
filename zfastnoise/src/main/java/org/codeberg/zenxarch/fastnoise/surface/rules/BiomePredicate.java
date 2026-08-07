package org.codeberg.zenxarch.fastnoise.surface.rules;

import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import org.codeberg.zenxarch.fastnoise.surface.MaterialRuleContext;
import org.jspecify.annotations.Nullable;

public final class BiomePredicate implements BooleanSupplier {
  public static final BooleanSupplier ALWAYS_TRUE =
      new BooleanSupplier() {
        public boolean get() {
          return true;
        }

        public String toString() {
          return Boolean.toString(true);
        }
      };
  public static final BooleanSupplier ALWAYS_FALSE =
      new BooleanSupplier() {
        public boolean get() {
          return false;
        }

        public String toString() {
          return Boolean.toString(false);
        }
      };
  public static final BlockStateRule ALWAYS_NULL =
      new BlockStateRule() {
        @Override
        public @Nullable BlockState tryApply(int x, int y, int z) {
          return null;
        }

        @Override
        public String toString() {
          return Objects.toString(null);
        }
      };

  private final MaterialRuleContext context;
  public final long mask;

  public BiomePredicate(long mask, MaterialRuleContext context) {
    this.context = context;
    this.mask = mask;
  }

  public static BooleanSupplier getSupplier(
      RegistryEntryList<Biome> predicates, MaterialRuleContext context) {
    long mask = 0L;
    for (int i = 0; i < context.biomes.length; i++) {
      if (predicates.contains(context.biomes[i])) mask = setBit(mask, i);
    }

    if (mask == 0L) return ALWAYS_FALSE;
    if (mask == ((0x1L << context.biomes.length) - 1)) return ALWAYS_TRUE;

    return new BiomePredicate(mask, context);
  }

  private static long setBit(long value, long idx) {
    return value | (0x1L << idx);
  }

  @Override
  public String toString() {
    return "BiomePredicate[mask=" + mask + "]";
  }

  @Override
  public boolean get() {
    var idx = context.getCurrentBiomeIdx();
    return (mask & (0x1L << idx)) != 0;
  }
}
