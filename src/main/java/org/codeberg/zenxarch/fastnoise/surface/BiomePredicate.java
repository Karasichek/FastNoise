package org.codeberg.zenxarch.fastnoise.surface;

import java.util.List;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;

public final class BiomePredicate implements BooleanSupplier {
  public static final BooleanSupplier ALWAYS_TRUE = () -> true;
  public static final BooleanSupplier ALWAYS_FALSE = () -> false;

  private final MaterialRuleContext context;
  private final long mask;

  public BiomePredicate(long mask, MaterialRuleContext context) {
    this.context = context;
    this.mask = mask;
  }

  public static BooleanSupplier getSupplier(
      List<RegistryKey<Biome>> predicates, MaterialRuleContext context) {
    long mask = 0L;
    for (int i = 0; i < context.biomes.length; i++) {
      if (predicates.contains(context.biomes[i].getKey().orElseThrow())) mask = setBit(mask, i);
    }

    if (mask == 0L) return ALWAYS_FALSE;
    if (mask == ((0x1L << context.biomes.length) - 1)) return ALWAYS_TRUE;

    return new BiomePredicate(mask, context);
  }

  private static long setBit(long value, long idx) {
    return value | (0x1L << idx);
  }

  @Override
  public boolean get() {
    var idx = context.getCurrentBiomeIdx();
    return (mask & (0x1L << idx)) != 0;
  }
}
