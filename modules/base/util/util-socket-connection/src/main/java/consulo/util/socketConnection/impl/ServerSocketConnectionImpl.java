/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.util.socketConnection.impl;

import consulo.util.socketConnection.AbstractRequest;
import consulo.util.socketConnection.AbstractResponse;
import consulo.util.socketConnection.ConnectionStatus;
import consulo.util.socketConnection.RequestResponseExternalizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author nik
 */
public class ServerSocketConnectionImpl<Request extends AbstractRequest, Response extends AbstractResponse> extends SocketConnectionBase<Request, Response> {
  private static final Logger LOG = LoggerFactory.getLogger(ServerSocketConnectionImpl.class);
  private ServerSocket myServerSocket;
  private final int myDefaultPort;
  private final int myConnectionAttempts;
  private final Executor myExecutor;

  public ServerSocketConnectionImpl(ScheduledExecutorService executor, int defaultPort, int connectionAttempts, @Nonnull RequestResponseExternalizerFactory<Request, Response> factory) {
    super(executor, factory);
    myExecutor = executor;
    myDefaultPort = defaultPort;
    myConnectionAttempts = connectionAttempts;
  }

  public void open() throws IOException {
    myServerSocket = createSocket();
    setPort(myServerSocket.getLocalPort());
    myExecutor.execute(() -> {
      try {
        waitForConnection();
      }
      catch (IOException e) {
        LOG.info(e.getMessage(), e);
        setStatus(ConnectionStatus.CONNECTION_FAILED, "Connection failed: " + e.getMessage());
      }
    });
  }

  @Nonnull
  private ServerSocket createSocket() throws IOException {
    IOException exc = null;
    for (int i = 0; i < myConnectionAttempts; i++) {
      int port = myDefaultPort + i;
      try {
        return new ServerSocket(port);
      }
      catch (IOException e) {
        exc = e;
        LOG.info(e.getMessage(), e);
      }
    }
    throw exc;
  }

  private void waitForConnection() throws IOException {
    addThreadToInterrupt();
    try {
      setStatus(ConnectionStatus.WAITING_FOR_CONNECTION, null);
      LOG.debug("waiting for connection on port " + getPort());

      try (Socket socket = myServerSocket.accept()) {
        attachToSocket(socket);
      }
    }
    finally {
      myServerSocket.close();
      removeThreadToInterrupt();
    }
  }
}
