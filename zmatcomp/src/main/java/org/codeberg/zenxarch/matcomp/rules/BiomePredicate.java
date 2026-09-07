package org.codeberg.zenxarch.matcomp.rules;

import java.util.Objects;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.jspecify.annotations.Nullable;

public final class BiomePredicate {
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

  public static BooleanSupplier getSupplier(
      RegistryEntryList<Biome> predicates, MaterialRuleContext context) {
    var includedBiomes = context.includedBiomes;
    if (includedBiomes != null) {
      if (isDisjoint(includedBiomes, predicates)) return BiomePredicate.ALWAYS_FALSE;
      if (isSuperset(includedBiomes, predicates)) return BiomePredicate.ALWAYS_TRUE;
    }
    return new VanillaBiomePredicate(predicates, context);
  }

  private static boolean isDisjoint(
      final Set<RegistryEntry<Biome>> contextBiomes, RegistryEntryList<Biome> biomes) {
    for (RegistryEntry<Biome> biome : biomes) {
      if (contextBiomes.contains(biome)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isSuperset(
      final Set<RegistryEntry<Biome>> contextBiomes, RegistryEntryList<Biome> biomes) {
    for (RegistryEntry<Biome> possibleBiome : contextBiomes) {
      if (!biomes.contains(possibleBiome)) {
        return false;
      }
    }
    return true;
  }
}
