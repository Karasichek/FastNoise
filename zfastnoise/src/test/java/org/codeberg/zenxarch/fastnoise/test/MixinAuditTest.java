package org.codeberg.zenxarch.fastnoise.test;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class MixinAuditTest {
  @BeforeAll
  static void beforeAll() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @Test
  void auditMixins() {
    MixinEnvironment.getCurrentEnvironment().audit();
  }
}
