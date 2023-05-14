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
package consulo.util.socketConnection;

import consulo.util.socketConnection.impl.ServerSocketConnectionImpl;
import consulo.util.socketConnection.impl.SocketConnectionImpl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.net.InetAddress;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author nik
 */
public final class SocketConnectionFactory {
  private SocketConnectionFactory() {
  }

  public static <Request extends AbstractRequest, Response extends AbstractResponse> SocketConnection<Request, Response> createServerConnection(@Nonnull ScheduledExecutorService executorService,
                                                                                                                                                int defaultPort,
                                                                                                                                                int attempts,
                                                                                                                                                RequestResponseExternalizerFactory<Request, Response> factory) {
    return new ServerSocketConnectionImpl<>(executorService, defaultPort, attempts, factory);
  }


  public static <Request extends AbstractRequest, Response extends AbstractResponse> SocketConnection<Request, Response> createServerConnection(@Nonnull ScheduledExecutorService executorService,
                                                                                                                                                int defaultPort,
                                                                                                                                                RequestResponseExternalizerFactory<Request, Response> factory) {
    return new ServerSocketConnectionImpl<>(executorService, defaultPort, 1, factory);
  }

  public static <Request extends AbstractRequest, Response extends AbstractResponse> ClientSocketConnection<Request, Response> createConnection(@Nonnull ScheduledExecutorService executor,
                                                                                                                                                @Nullable InetAddress host,
                                                                                                                                                int initialPort,
                                                                                                                                                int portsNumberToTry,
                                                                                                                                                RequestResponseExternalizerFactory<Request, Response> factory) {
    return new SocketConnectionImpl<>(executor, host, initialPort, portsNumberToTry, factory);
  }
}
