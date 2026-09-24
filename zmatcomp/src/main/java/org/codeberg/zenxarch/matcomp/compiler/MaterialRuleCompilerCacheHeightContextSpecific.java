package org.codeberg.zenxarch.matcomp.compiler;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.shorts.ShortArrays;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRule;
import net.minecraft.world.gen.surfacebuilder.MaterialRules.MaterialRuleContext;
import org.codeberg.zenxarch.matcomp.compiler.cw.MaterialRuleClassWriter;
import org.codeberg.zenxarch.matcomp.compiler.ir.FlattenedBlockStateRule;
import org.codeberg.zenxarch.matcomp.compiler.ir.FlattenedMaterialRule;
import org.codeberg.zenxarch.matcomp.config.MaterialRuleCompilerConfig;

public class MaterialRuleCompilerCacheHeightContextSpecific {
  public final FlattenedMaterialRule flattened;

  private final Object2ObjectLinkedOpenCustomHashMap<short[], FlattenedBlockStateRule> cache =
      new Object2ObjectLinkedOpenCustomHashMap<>(
          MaterialRuleCompilerConfig.MAX_CACHE_SIZE,
          Object2ObjectOpenCustomHashMap.FAST_LOAD_FACTOR,
          ShortArrays.HASH_STRATEGY);

  public MaterialRuleCompilerCacheHeightContextSpecific(
      HeightContext context, NoiseConfig config, MaterialRule rule) {
    this.flattened = FlattenedMaterialRule.flatten(rule, context, config);
  }

  public static final int MATERIAL_RULE = 0;
  public static final int STATE_RULE = 1;
  public static final int STATIC_RULE = 2;
  public static final int CONDITION = 3;
  public static final int BIOME_CONDITION = 4;
  public static final int SINGLE_BIOME_CONDITION = 5;
  public static final int STATIC_CONDITION = 6;
  public static final int NOT_CONDITION = 7;
  public static final int NOT_BIOME_CONDITION = 8;
  public static final int NOT_SINGLE_BIOME_CONDITION = 9;
  public static final int NOT_STATIC_CONDITION = 10;

  public synchronized FlattenedBlockStateRule get(MaterialRuleContext ctx) {
    var key = new short[flattened.biomeConditions.length];
    fillBiomeCacheKey(key, flattened, ctx);
    var res = cache.getAndMoveToFirst(key);
    if (res != null) return res;

    res = MaterialRuleCompilerCacheHeightContextSpecific.of(flattened, key, this);

    cache.put(key, res);
    while (cache.size() > MaterialRuleCompilerConfig.MAX_CACHE_SIZE) cache.removeLast();
    return res;
  }

  public static final short BIOME_NONE = 0;
  public static final short BIOME_ALL = 1;
  public static final short BIOME_MANY = 2;
  public static final short BIOME_SINGLE = 3;

  private static void fillBiomeCacheKey(
      short[] arr, FlattenedMaterialRule rule, MaterialRuleContext ctx) {
    var included = ctx.includedBiomes;

    if (included == null) {
      Arrays.fill(arr, BIOME_MANY);
      return;
    }

    final var biomes = (RegistryEntry<Biome>[]) included.toArray(RegistryEntry[]::new);

    for (int i = 0; i < rule.biomeConditions.length; i++) {
      var cond = rule.biomeConditions[i];
      RegistryEntry<Biome> lastBiome = null;
      int count = 0;

      for (int j = 0; j < biomes.length; j++) {
        if (cond.contains(biomes[j])) {
          lastBiome = biomes[j];
          count++;
        }
      }

      if (count == 0) arr[i] = BIOME_NONE;
      else if (count == 1) {
        var biomeIdx = -1;
        for (int k = 0; k < rule.biomes.length; k++) {
          if (lastBiome == rule.biomes[k]) {
            biomeIdx = k;
            break;
          }
        }
        arr[i] = (short) (BIOME_SINGLE + (short) biomeIdx);
        if (biomeIdx >= 65533) throw new IllegalStateException("Too many biomes detected");
      } else if (count == biomes.length) arr[i] = BIOME_ALL;
      else arr[i] = BIOME_MANY;
    }
  }

  private static AtomicInteger counter = new AtomicInteger(0);

  public static FlattenedBlockStateRule of(
      FlattenedMaterialRule rule,
      short[] biomeIdx,
      MaterialRuleCompilerCacheHeightContextSpecific ctx) {
    return new Builder(rule, biomeIdx).build(ctx);
  }

  private static class Builder {
    private final FlattenedMaterialRule flattened;
    private final IntArrayList condIdx;
    private final IntArrayList ruleIdx;
    private final IntArrayList instructions;

    private final short[] biomeIdx;

    public Builder(FlattenedMaterialRule rule, short[] biomeIdx) {
      this.flattened = rule;
      this.biomeIdx = biomeIdx;
      this.condIdx = new IntArrayList();
      this.ruleIdx = new IntArrayList();
      this.instructions = new IntArrayList();

      push(0, this.flattened.program.length);
    }

    private void conditionalPush(int index, int startIndex, int last) {
      var instIndex = instructions.size() - 1;

      push(index, last);

      if (instIndex + 1 == instructions.size()) {
        this.removeInst(3);
        return;
      }

      this.instructions.set(instIndex, instructions.size());
    }

    private void conditionalPushWithInst(int opcode, int data, int index, int last) {
      this.addInst(opcode, data, 0);
      var nextLast = prog(index + 2);
      this.conditionalPush(index + 3, index, nextLast);
      push(nextLast, last);
    }

    private void push(int index, int last) {
      if (index >= last) return;
      switch (prog(index)) {
        case FlattenedMaterialRule.MATERIAL_RULE -> {
          var idx = this.ruleIdx.size();
          this.ruleIdx.add(prog(index + 1));
          addInst(MATERIAL_RULE, idx);
          push(index + 2, last);
        }
        case FlattenedMaterialRule.CONDITION -> {
          var idx = this.condIdx.size();
          this.condIdx.add(prog(index + 1));
          conditionalPushWithInst(CONDITION, idx, index, last);
        }
        case FlattenedMaterialRule.NOT_CONDITION -> {
          var idx = this.condIdx.size();
          this.condIdx.add(prog(index + 1));
          conditionalPushWithInst(NOT_CONDITION, idx, index, last);
        }
        case FlattenedMaterialRule.STATE_RULE -> {
          addInst(STATE_RULE, prog(index + 1));
        }
        case FlattenedMaterialRule.BIOME_CONDITION -> {
          var pgidx = prog(index + 1);
          var biomeState = this.biomeIdx[pgidx];
          var nextLast = prog(index + 2);

          switch (biomeState) {
            case BIOME_NONE -> {
              push(nextLast, last);
            }
            case BIOME_ALL -> {
              push(index + 3, nextLast);
              push(nextLast, last);
            }
            case BIOME_MANY -> {
              conditionalPushWithInst(BIOME_CONDITION, pgidx, index, last);
            }
            default -> {
              conditionalPushWithInst(
                  SINGLE_BIOME_CONDITION,
                  Short.toUnsignedInt(biomeState) - BIOME_SINGLE,
                  index,
                  last);
            }
          }
        }
        case FlattenedMaterialRule.NOT_BIOME_CONDITION -> {
          var pgidx = prog(index + 1);
          var biomeState = this.biomeIdx[pgidx];
          var nextLast = prog(index + 2);

          switch (biomeState) {
            case BIOME_NONE -> {
              push(index + 3, nextLast);
              push(nextLast, last);
            }
            case BIOME_ALL -> {
              push(nextLast, last);
            }
            case BIOME_MANY -> {
              conditionalPushWithInst(NOT_BIOME_CONDITION, pgidx, index, last);
            }
            default -> {
              conditionalPushWithInst(
                  NOT_SINGLE_BIOME_CONDITION,
                  Short.toUnsignedInt(biomeState) - BIOME_SINGLE,
                  index,
                  last);
            }
          }
        }
        case FlattenedMaterialRule.STATIC_CONDITION -> {
          conditionalPushWithInst(STATIC_CONDITION, prog(index + 1), index, last);
        }
        case FlattenedMaterialRule.NOT_STATIC_CONDITION -> {
          conditionalPushWithInst(NOT_STATIC_CONDITION, prog(index + 1), index, last);
        }
        case FlattenedMaterialRule.STATIC_RULE -> {
          addInst(STATIC_RULE, prog(index + 1));
          push(index + 2, last);
        }
      }
    }

    private int prog(int index) {
      return flattened.program[index];
    }

    private void addInst(int opcode, int... data) {
      this.instructions.add(opcode);
      for (int i = 0; i < data.length; i++) this.instructions.add(data[i]);
    }

    private void removeInst(int len) {
      this.instructions.size(this.instructions.size() - len);
    }

    public FlattenedBlockStateRule build(MaterialRuleCompilerCacheHeightContextSpecific ctx) {
      return new FlattenedBlockStateRule(
          MaterialRuleClassWriter.compile(
              this.flattened,
              "FlattenedBlockStateRule$" + counter.incrementAndGet(),
              this.instructions.toIntArray(),
              ctx),
          this.ruleIdx.toIntArray(),
          this.condIdx.toIntArray());
    }
  }
}
