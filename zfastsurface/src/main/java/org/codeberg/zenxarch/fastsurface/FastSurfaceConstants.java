package org.codeberg.zenxarch.fastsurface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FastSurfaceConstants {
  private FastSurfaceConstants() {
    throw new IllegalStateException("Utility class");
  }

  // This logger is used to write text to the console and the log file.
  // It is considered best practice to use your mod id as the logger's name.
  // That way, it's clear which mod wrote info, warnings, and errors.
  public static final Logger LOGGER = LoggerFactory.getLogger(FastSurfaceConstants.MOD_ID);

  public static final String MOD_ID = "zfastsurface";
}
