package org.codeberg.zenxarch.fastnoise;

import net.minecraft.util.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(FastNoiseConstants.MOD_ID)
public class FastNoiseMod {

  public FastNoiseMod(IEventBus modEventBus, ModContainer modContainer) {
    modEventBus.addListener(this::commonSetup);
  }

  private void commonSetup(FMLCommonSetupEvent event) {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    // However, some things (like resources) may still be uninitialized.
    // Proceed with mild caution.

    FastNoiseConstants.LOGGER.info("Hello Fabric world!");
  }

  public static Identifier id(String path) {
    return Identifier.of(FastNoiseConstants.MOD_ID, path);
  }
}
