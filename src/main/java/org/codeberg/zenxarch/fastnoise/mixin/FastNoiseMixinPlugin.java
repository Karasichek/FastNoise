package org.codeberg.zenxarch.fastnoise.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class FastNoiseMixinPlugin implements IMixinConfigPlugin {

  private String mixinPackage;

  @Override
  public void onLoad(String mixinPackage) {
    this.mixinPackage = mixinPackage;
  }

  @Override
  public String getRefMapperConfig() {
    return null;
  }

  @Override
  public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
    if (!mixinClassName.startsWith(mixinPackage)) return false;
    var name = mixinClassName.substring(mixinPackage.length() - "mixin".length());
    if (!name.startsWith("mixin.")) return false;

    return true;
  }

  @Override
  public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

  @Override
  public List<String> getMixins() {
    return null;
  }

  @Override
  public void preApply(
      String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

  @Override
  public void postApply(
      String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
