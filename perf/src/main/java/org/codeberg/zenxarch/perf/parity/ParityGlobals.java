package org.codeberg.zenxarch.perf.parity;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParityGlobals {

  public static final Logger LOGGER = LoggerFactory.getLogger(ParityGlobals.class);

  private static boolean getProperty(String property, boolean def) {
    var prop = System.getProperty(property, Boolean.toString(def));
    if (prop == null) return def;
    try {
      return Boolean.parseBoolean(prop);
    } catch (Exception e) {
      return def;
    }
  }

  private static int getIntProperty(String name, int def, int min) {
    var value = System.getProperty(name);
    if (value == null) return def;
    try {
      return Math.max(Integer.parseInt(value), min);
    } catch (Exception e) {
      return def;
    }
  }

  public static final boolean isServer = getProperty("zparityserver", false);
  public static final int port = getIntProperty("zparityport", 5000, 1);

  public static final Object2ObjectOpenHashMap<ChunkStorageKey, ChunkStorage> clientData =
      new Object2ObjectOpenHashMap<>();

  private static final ParityServer server = createServer();
  private static final ParityClient client = createClient();

  private static ParityServer createServer() {
    if (!isServer) return null;
    try {
      return new ParityServer();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static ParityClient createClient() {
    if (isServer) return null;
    try {
      return new ParityClient();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static void startIngestThread() {
    new Thread("Ingest thread") {
      @Override
      public void run() {
        while (true) {
          try {
            var next = server.read();
            synchronized (clientData) {
              clientData.put(
                  new ChunkStorageKey(next.seed, next.worldName, next.chunkX, next.chunkZ), next);
            }
          } catch (Exception e) {
            break;
          }
        }
      }
    }.start();
  }

  static {
    startIngestThread();
  }

  public static ChunkStorage get(ChunkStorageKey key) {
    while (true) {
      synchronized (clientData) {
        var res = clientData.getOrDefault(key, null);
        if (res != null) return res;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public static void send(ChunkStorage storage) {
    try {
      client.write(storage);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static record ChunkStorageKey(long seed, String worldName, int cx, int cz) {}
}
