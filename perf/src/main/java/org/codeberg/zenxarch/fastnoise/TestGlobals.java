package org.codeberg.zenxarch.fastnoise;

import com.mojang.serialization.Lifecycle;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;
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
import net.minecraft.world.chunk.AbstractChunkHolder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkGenerationContext;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.PalettesFactory;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelInfo.DifficultySettings;
import net.minecraft.world.level.LevelProperties;
import net.minecraft.world.level.storage.LevelStorage.Session;
import org.jspecify.annotations.Nullable;

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
    try {
      Files.delete(FabricLoader.getInstance().getGameDir().resolve("world/session.lock"));
    } catch (IOException e) {
    }
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

  public static BoundedRegionArray<FakeAbstractChunkHolder> createRegionArray(
      ServerWorld world, int centerX, int centerZ, int radius) {
    return BoundedRegionArray.create(
        centerX, centerZ, radius, (x, z) -> createAbstractChunkHolder(world, x, z));
  }

  private static FakeAbstractChunkHolder createAbstractChunkHolder(
      ServerWorld world, int x, int z) {
    return new FakeAbstractChunkHolder(
        new ProtoChunk(new ChunkPos(x, z), UpgradeData.NO_UPGRADE_DATA, world, factory, null));
  }

  public static class FakeAbstractChunkHolder extends AbstractChunkHolder {
    private final ProtoChunk actualChunk;

    public FakeAbstractChunkHolder(ProtoChunk actualChunk) {
      super(actualChunk.getPos());
      this.actualChunk = actualChunk;
    }

    @Override
    protected void combineSavingFuture(CompletableFuture<?> savingFuture) {
      throw new UnsupportedOperationException("Unimplemented method 'combineSavingFuture'");
    }

    @Override
    public int getCompletedLevel() {
      throw new UnsupportedOperationException("Unimplemented method 'getCompletedLevel'");
    }

    @Override
    public int getLevel() {
      throw new UnsupportedOperationException("Unimplemented method 'getLevel'");
    }

    @Override
    public @Nullable Chunk getUncheckedOrNull(ChunkStatus requestedStatus) {
      return actualChunk;
    }

    public ProtoChunk getChunk() {
      return actualChunk;
    }
  }

  public static ChunkGenerationContext createContext(ServerWorld world) {
    return new ChunkGenerationContext(
        world,
        world.getChunkManager().getChunkGenerator(),
        server.getStructureTemplateManager(),
        null,
        null,
        null);
  }
}
