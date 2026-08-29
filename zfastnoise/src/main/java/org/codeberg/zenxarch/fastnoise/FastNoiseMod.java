package org.codeberg.zenxarch.fastnoise;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

public class FastNoiseMod implements ModInitializer {

  @Override
  public void onInitialize() {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    // However, some things (like resources) may still be uninitialized.
    // Proceed with mild caution.

    FastNoiseConstants.LOGGER.info("Hello Fabric world!");
  }

  public static Identifier id(String path) {
    return Identifier.of(FastNoiseConstants.MOD_ID, path);
  }
}
