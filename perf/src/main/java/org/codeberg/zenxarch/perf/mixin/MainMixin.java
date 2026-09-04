package org.codeberg.zenxarch.perf.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.Main;
import net.minecraft.server.SaveLoader;
import net.minecraft.world.level.storage.LevelStorage.Session;
import org.codeberg.zenxarch.perf.BenchmarkMain;
import org.codeberg.zenxarch.perf.TestGlobals;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Main.class)
public abstract class MainMixin {
  @Inject(method = "main", at = @At("HEAD"), cancellable = true)
  private static void zenxarch$main(String[] args, CallbackInfo ci) {
    if (BenchmarkMain.isForked()) return;
    if (BenchmarkMain.isParityTest()) return;
    ci.cancel();
    BenchmarkMain.runTest(args);
  }

  @Inject(
      method = "main",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/world/level/storage/LevelStorage$Session;backupLevelDataFile(Lnet/minecraft/world/SaveProperties;)V"),
      cancellable = true)
  private static void zenxarch$forked(
      String[] args,
      CallbackInfo ci,
      @Local DynamicRegistryManager.Immutable manager,
      @Local ResourcePackManager resourcePackManager,
      @Local Session session,
      @Local SaveLoader saveLoader) {
    ci.cancel();
    TestGlobals.setManager(manager, resourcePackManager, session, saveLoader);
    if (BenchmarkMain.isForked()) BenchmarkMain.runForked(args);
    if (BenchmarkMain.isParityTest()) BenchmarkMain.runParityTest();
  }
}
