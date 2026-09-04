package org.codeberg.zenxarch.fastnoise.parity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public final class ParityServer implements AutoCloseable {
  private final ServerSocket server;
  private final Socket socket;
  private ObjectInputStream ois;
  private final ObjectOutputStream oos;

  public ParityServer() throws UnknownHostException, IOException {
    this.server = new ServerSocket(ParityGlobals.port);
    ParityGlobals.LOGGER.info("Server started");

    ParityGlobals.LOGGER.info("Waiting for a client ...");
    this.socket = server.accept();
    ParityGlobals.LOGGER.info("Client accepted");

    this.oos = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    this.oos.flush();
  }

  public ChunkStorage read() throws ClassNotFoundException, IOException {
    if (ois == null) {
      this.ois = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
    }
    Object obj;
    while ((obj = ois.readObject()) != null) {
      if (obj instanceof ChunkStorage storage) return storage;
    }
    throw new IllegalStateException();
  }

  @Override
  public void close() throws Exception {
    ois.close();
    oos.close();
    socket.close();
  }
}
