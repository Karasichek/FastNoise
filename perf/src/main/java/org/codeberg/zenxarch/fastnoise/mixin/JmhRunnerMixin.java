package org.codeberg.zenxarch.fastnoise.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.io.IOException;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;
import org.openjdk.jmh.runner.Runner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Runner.class)
public abstract class JmhRunnerMixin {
  @WrapOperation(
      method = "getForkedMainCommand",
      at = @At(value = "INVOKE", target = "Ljava/lang/Class;getName()Ljava/lang/String;"))
  public String zenxarch$wrapName(Class<?> opClass, Operation<String> op) {
    try {
      Files.delete(FabricLoader.getInstance().getGameDir().resolve("world/session.lock"));
    } catch (IOException e) {

    }
    return "net.fabricmc.devlaunchinjector.Main";
  }
}
