package org.codeberg.zenxarch.matcomp.compiler.ir;

import java.lang.invoke.MethodHandle;

// cache value
public final class FlattenedBlockStateRule {
  public final MethodHandle handle;
  public final int[] ruleIdx;
  public final int[] condIdx;

  public FlattenedBlockStateRule(MethodHandle handle, int[] ruleIdx, int[] condIdx) {
    this.handle = handle;
    this.ruleIdx = ruleIdx;
    this.condIdx = condIdx;
  }
}
