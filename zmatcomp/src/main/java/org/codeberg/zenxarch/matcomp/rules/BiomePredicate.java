package org.codeberg.zenxarch.matcomp.rules;

import java.util.List;
import java.util.Objects;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BlockStateRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.BooleanSupplier;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.context.MaterialRuleContextExtension;
import org.jetbrains.annotations.Nullable;

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
      List<RegistryKey<Biome>> entryList, MaterialRuleContext context) {
    var includedBiomes = ((MaterialRuleContextExtension) context).zenxarch$getIncludedBiomes();

    if (includedBiomes != null) {
      var count = 0;
      RegistryEntry<Biome> lastValid = null;

      for (var entry : includedBiomes) {
        if (entryList.contains(entry.getKey().get())) {
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
      return ((MaterialRuleContextExtension) context).zenxarch$getCurrentBiome() == biome;
    }
  }
}
