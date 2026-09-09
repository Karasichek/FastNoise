package org.codeberg.zenxarch.fastsurface.surface;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;
import java.util.function.Function;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FastPosToBiomeBindings {
  private static final Logger LOGGER = LoggerFactory.getLogger(FastPosToBiomeBindings.class);

  public static final MethodHandle MH_FastPosToBiome$createPosToBiome;

  static {
    if (FabricLoader.getInstance().isModLoaded("zmatcomp")) {
      MethodHandle mh_FastPosToBiome$createPosToBiome;

      try {
        Class<?> clazz = Class.forName("org.codeberg.zenxarch.matcomp.biome.FastPosToBiome");
        mh_FastPosToBiome$createPosToBiome =
            MethodHandles.lookup()
                .findStatic(
                    clazz,
                    "createPosToBiome",
                    MethodType.methodType(
                        Function.class, BiomeAccess.class, Chunk.class, Set.class));
        LOGGER.info("Binding to FastPosToBiome.createPosToBiome");
      } catch (Throwable t) {
        LOGGER.warn("Unable to find compatible FastPosToBiome.createPosToBiome", t);
        mh_FastPosToBiome$createPosToBiome = null;
      }
      MH_FastPosToBiome$createPosToBiome = mh_FastPosToBiome$createPosToBiome;
    } else {
      MH_FastPosToBiome$createPosToBiome = null;
    }
  }

  public static Function<BlockPos, RegistryEntry<Biome>> call_FastPosToBiome$createPosToBiome(
      final BiomeAccess biomeAccess,
      final Chunk chunk,
      final Set<RegistryEntry<Biome>> includedBiomes) {
    if (MH_FastPosToBiome$createPosToBiome == null) return biomeAccess::getBiome;

    try {
      return (Function<BlockPos, RegistryEntry<Biome>>)
          MH_FastPosToBiome$createPosToBiome.invokeExact(biomeAccess, chunk, includedBiomes);
    } catch (Throwable t) {
      return biomeAccess::getBiome;
    }
  }
}
