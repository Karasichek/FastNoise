package org.codeberg.zenxarch.matcomp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaterialRuleCompilerConstants {
  private MaterialRuleCompilerConstants() {
    throw new IllegalStateException("Utility class");
  }

  // This logger is used to write text to the console and the log file.
  // It is considered best practice to use your mod id as the logger's name.
  // That way, it's clear which mod wrote info, warnings, and errors.
  public static final Logger LOGGER = LoggerFactory.getLogger(MaterialRuleCompilerConstants.MOD_ID);

  public static final String MOD_ID = "zmatcomp";
}
