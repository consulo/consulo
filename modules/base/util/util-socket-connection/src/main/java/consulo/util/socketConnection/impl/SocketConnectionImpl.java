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

import consulo.util.socketConnection.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author nik
 */
public class SocketConnectionImpl<Request extends AbstractRequest, Response extends AbstractResponse> extends SocketConnectionBase<Request, Response>
        implements ClientSocketConnection<Request, Response> {
  private static final Logger LOG = LoggerFactory.getLogger(SocketConnectionImpl.class);
  private static final int MAX_CONNECTION_ATTEMPTS = 60;
  private static final int CONNECTION_ATTEMPT_DELAY = 500;
  private final InetAddress myHost;
  private final int myInitialPort;
  private final int myPortsNumberToTry;
  private final Executor myExecutor;

  public SocketConnectionImpl(@Nonnull ScheduledExecutorService executor, InetAddress host, int initialPort, int portsNumberToTry, @Nonnull RequestResponseExternalizerFactory<Request, Response> factory) {
    super(executor, factory);
    myExecutor = executor;
    myHost = host;
    myInitialPort = initialPort;
    myPortsNumberToTry = portsNumberToTry;
  }

  @Override
  public void open() throws IOException {
    final Socket socket = createSocket();
    setPort(socket.getPort());
    myExecutor.execute(new Runnable() {
      public void run() {
        try {
          attachToSocket(socket);
        }
        catch (IOException e) {
          LOG.info(e.getMessage(), e);
          setStatus(ConnectionStatus.CONNECTION_FAILED, "Connection failed: " + e.getMessage());
        }
      }
    });
  }

  @Nonnull
  private Socket createSocket() throws IOException {
    final InetAddress host = myHost != null ? myHost : InetAddress.getLocalHost();
    IOException exc = null;
    for (int i = 0; i < myPortsNumberToTry; i++) {
      int port = myInitialPort + i;
      try {
        return new Socket(host, port);
      }
      catch (IOException e) {
        exc = e;
        LOG.debug(e.getMessage(), e);
      }
    }
    throw exc;
  }

  @Override
  public void startPolling() {
    setStatus(ConnectionStatus.WAITING_FOR_CONNECTION, null);
    myExecutor.execute(new Runnable() {
      @Override
      public void run() {
        addThreadToInterrupt();
        try {
          for (int attempt = 0; attempt < MAX_CONNECTION_ATTEMPTS; attempt++) {
            try {
              open();
              return;
            }
            catch (IOException e) {
              LOG.debug(e.getMessage(), e);
            }

            //noinspection BusyWait
            Thread.sleep(CONNECTION_ATTEMPT_DELAY);
          }
          setStatus(ConnectionStatus.CONNECTION_FAILED, "Cannot connect to " + myHost + ", the maximum number of connection attempts exceeded");
        }
        catch (InterruptedException ignored) {
        }
        finally {
          removeThreadToInterrupt();
        }
      }
    });
  }
}
