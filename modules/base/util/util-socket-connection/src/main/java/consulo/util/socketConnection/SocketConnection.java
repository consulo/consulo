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

import org.jspecify.annotations.Nullable;
import java.io.IOException;

/**
 * @author nik
 */
public interface SocketConnection<Request extends AbstractRequest, Response extends AbstractResponse> {
  ConnectionState getState();

  void open() throws IOException;

  /**
   * Return runnable for unregister this listener
   */
  Runnable addListener(SocketConnectionListener listener);

  int getPort();

  void sendRequest(Request request);

  void sendRequest(Request request, @Nullable AbstractResponseToRequestHandler<? extends Response> handler);

  <R extends Response> void registerHandler(Class<R> responseClass, AbstractResponseHandler<R> handler);

  void close();

  void sendRequest(Request request, @Nullable AbstractResponseToRequestHandler<? extends Response> handler, int timeout, Runnable onTimeout);

  boolean isStopping();
}
