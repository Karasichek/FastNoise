package org.codeberg.zenxarch.fastnoise.surface;

import java.util.Set;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;

public final class BiomeConditionModifier {
  private static final BooleanSupplier ALWAYS_TRUE = () -> true;
  private static final BooleanSupplier ALWAYS_FALSE = () -> false;

  public static BooleanSupplier getSupplier(
      MaterialRuleContext context, Set<RegistryKey<Biome>> predicate) {
    if (containsAll(context.includedBiomeKeys, predicate)) return ALWAYS_TRUE;
    if (containsNone(predicate, context.includedBiomeKeys)) return ALWAYS_FALSE;
    return null;
  }

  private static boolean containsAll(
      Set<RegistryKey<Biome>> biomes, Set<RegistryKey<Biome>> predicate) {
    for (var biome : biomes) {
      if (!predicate.contains(biome)) return false;
    }
    return true;
  }

  private static boolean containsNone(
      Set<RegistryKey<Biome>> predicates, Set<RegistryKey<Biome>> biomes) {
    for (var predicate : predicates) {
      if (biomes.contains(predicate)) return false;
    }
    return true;
  }
}
