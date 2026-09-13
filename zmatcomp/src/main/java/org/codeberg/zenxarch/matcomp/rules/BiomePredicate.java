package org.codeberg.zenxarch.matcomp.rules;

import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRuleContext;
import org.jspecify.annotations.Nullable;

public final class BiomePredicate {
  public static record ConstantBooleanSupplier(boolean result) implements BooleanSupplier {
    @Override
    public boolean get() {
      return result;
    }
  }

  public static record NullBlockStateRule() implements BlockStateRule {
    @Override
    public @Nullable BlockState tryApply(int x, int y, int z) {
      return null;
    }
  }

  public static final BooleanSupplier ALWAYS_TRUE = new ConstantBooleanSupplier(true);
  public static final BooleanSupplier ALWAYS_FALSE = new ConstantBooleanSupplier(false);
  public static final BlockStateRule ALWAYS_NULL = new NullBlockStateRule();

  public static BooleanSupplier getSupplier(
      RegistryEntryList<Biome> entryList, MaterialRuleContext context) {
    var includedBiomes = context.includedBiomes;
    if (includedBiomes != null) {
      var count = 0;
      RegistryEntry<Biome> lastValid = null;

      for (var entry : includedBiomes) {
        if (entryList.contains(entry)) {
          count++;
          lastValid = entry;
        }
      }

      if (count == 0) return ALWAYS_FALSE;
      if (count == includedBiomes.size()) return ALWAYS_TRUE;
      if (count == 1) return new SingleBiomePredicate(Objects.requireNonNull(lastValid), context);
    }
    return new VanillaBiomePredicate(entryList, context);
  }

  public static record SingleBiomePredicate(RegistryEntry<Biome> biome, MaterialRuleContext context)
      implements BooleanSupplier {
    @Override
    public boolean get() {
      return context.getCurrentBiome() == biome;
    }
  }
}
