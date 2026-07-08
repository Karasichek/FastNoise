package org.codeberg.zenxarch.fastnoise.mixin;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import java.util.List;
import java.util.Set;
import org.codeberg.zenxarch.fastnoise.FastNoiseConfig;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class FastNoiseMixinPlugin implements IMixinConfigPlugin {

  private Object2BooleanMap<String> config = FastNoiseConfig.loadConfig();
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

    for (var key : config.keySet()) {
      if (name.startsWith(key) && !config.getBoolean(key)) return false;
    }

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
