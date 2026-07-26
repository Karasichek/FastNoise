package org.codeberg.zenxarch.fastnoise;

import com.mojang.serialization.Lifecycle;
import java.util.List;
import java.util.Optional;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.dedicated.management.listener.BlankManagementListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestServer;
import net.minecraft.util.Util;
import net.minecraft.util.collection.BoundedRegionArray;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.PalettesFactory;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelInfo.DifficultySettings;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.LevelStorage.Session;

public final class TestGlobals {
  private static DynamicRegistryManager manager;
  private static PalettesFactory factory;
  private static MinecraftServer server;
  private static Session session;

  public static DynamicRegistryManager getManager() {
    return manager;
  }

  public static PalettesFactory getFactory() {
    return factory;
  }

  public static void setManager(
      DynamicRegistryManager manager,
      ResourcePackManager resourcePackManager,
      Session session,
      SaveLoader saveLoader) {
    TestGlobals.manager = manager;
    TestGlobals.factory = PalettesFactory.fromRegistryManager(manager);
    TestGlobals.session = session;
    server =
        new TestServer(
            new Thread(() -> {}),
            session,
            resourcePackManager,
            saveLoader,
            Optional.empty(),
            false,
            0);
    server.setPlayerManager(
        new PlayerManager(
            server,
            server.getCombinedDynamicRegistries(),
            session.createSaveHandler(),
            new BlankManagementListener()) {});
  }

  public static ServerWorld create(
      RegistryKey<World> key, RegistryKey<DimensionOptions> dimensionOptions, long seed) {
    var levelData =
        new LevelProperties(
            new LevelInfo(
                "A",
                GameMode.CREATIVE,
                DifficultySettings.DEFAULT,
                true,
                DataConfiguration.SAFE_MODE),
            LevelProperties.SpecialProperty.NONE,
            Lifecycle.stable());
    return new ServerWorld(
        server,
        Util.getMainWorkerExecutor(),
        session,
        levelData,
        key,
        manager.getOrThrow(RegistryKeys.DIMENSION).getOrThrow(dimensionOptions).value(),
        false,
        BiomeAccess.hashSeed(seed),
        List.of(),
        false);
  }

  public static BoundedRegionArray<ProtoChunk> createRegionArray(
      ServerWorld world, int centerX, int centerZ, int radius) {
    return BoundedRegionArray.create(
        centerX,
        centerZ,
        radius,
        (x, z) ->
            new ProtoChunk(new ChunkPos(x, z), UpgradeData.NO_UPGRADE_DATA, world, factory, null));
  }
}
