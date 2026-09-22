package org.codeberg.zenxarch.matcomp.rules;

import java.util.List;
import net.minecraft.util.dynamic.Range;
import net.minecraft.world.gen.surfacebuilder.MaterialCondition;

public final class LithostitchedConditions {
  public static interface SlopeCondition extends MaterialCondition {
    public Range<Integer> threshold();
  }

  public static interface AnyOfCondition extends MaterialCondition {
    public List<MaterialCondition> conditions();
  }

  public static interface AllOfCondition extends MaterialCondition {
    public List<MaterialCondition> conditions();
  }
}
