package org.codeberg.zenxarch.fastnoise.parity;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public final class ParityClient implements AutoCloseable {
  private final Socket clientSocket;
  private final ObjectOutputStream oos;

  public ParityClient() throws UnknownHostException, IOException {
    this.clientSocket = new Socket("127.0.0.1", ParityGlobals.port);
    this.oos = new ObjectOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
    this.oos.flush();
  }

  public void write(ChunkStorage chunkStorage) throws IOException {
    this.oos.writeObject(chunkStorage);
    this.oos.flush();
  }

  @Override
  public void close() throws Exception {
    oos.close();
    clientSocket.close();
  }
}
