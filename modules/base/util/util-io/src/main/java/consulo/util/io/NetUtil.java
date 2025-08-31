/*
 * Copyright 2013-2019 consulo.io
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
package consulo.util.io;

import consulo.util.io.internal.OSInfo;
import consulo.util.lang.SystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.net.*;

public class NetUtil {
  private static final Logger LOG = LoggerFactory.getLogger(NetUtil.class);

  public static InetSocketAddress loopbackSocketAddress() throws IOException {
    return loopbackSocketAddress(-1);
  }

  public static InetSocketAddress loopbackSocketAddress(int port) throws IOException {
    return new InetSocketAddress(InetAddress.getLoopbackAddress(), port == -1 ? findAvailableSocketPort() : port);
  }

  public static boolean canConnectToSocket(String host, int port) {
    if (isLocalhost(host)) {
      return !canBindToLocalSocket(host, port);
    }
    else {
      return canConnectToRemoteSocket(host, port);
    }
  }

  /**
   * @deprecated use {@link InetAddress#getLoopbackAddress()}
   */
  @Deprecated(forRemoval = true)
  public static InetAddress getLoopbackAddress() {
    return InetAddress.getLoopbackAddress();
  }

  public static boolean isLocalhost(@Nonnull String host) {
    return host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1");
  }

  private static boolean canBindToLocalSocket(String host, int port) {
    try {
      ServerSocket socket = new ServerSocket();
      try {
        //it looks like this flag should be set but it leads to incorrect results for NodeJS under Windows
        //socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(host, port));
      }
      finally {
        try {
          socket.close();
        }
        catch (IOException ignored) {
        }
      }
      return true;
    }
    catch (IOException e) {
      LOG.debug(e.getMessage(), e);
      return false;
    }
  }

  public static boolean canConnectToRemoteSocket(String host, int port) {
    try {
      Socket socket = new Socket(host, port);
      socket.close();
      return true;
    }
    catch (IOException ignored) {
      return false;
    }
  }

  public static int findAvailableSocketPort() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(0)) {
      int port = serverSocket.getLocalPort();
      // workaround for linux : calling close() immediately after opening socket
      // may result that socket is not closed
      //noinspection SynchronizationOnLocalVariableOrMethodParameter
      synchronized (serverSocket) {
        try {
          //noinspection WaitNotInLoop
          serverSocket.wait(1);
        }
        catch (InterruptedException e) {
          LOG.error(e.getMessage(), e);
        }
      }
      return port;
    }
  }

  public static int tryToFindAvailableSocketPort(int defaultPort) {
    try {
      return findAvailableSocketPort();
    }
    catch (IOException ignored) {
      return defaultPort;
    }
  }

  public static int tryToFindAvailableSocketPort() {
    return tryToFindAvailableSocketPort(-1);
  }

  public static int[] findAvailableSocketPorts(int capacity) throws IOException {
    int[] ports = new int[capacity];
    ServerSocket[] sockets = new ServerSocket[capacity];

    for (int i = 0; i < capacity; i++) {
      //noinspection SocketOpenedButNotSafelyClosed
      ServerSocket serverSocket = new ServerSocket(0);
      sockets[i] = serverSocket;
      ports[i] = serverSocket.getLocalPort();
    }
    //workaround for linux : calling close() immediately after opening socket
    //may result that socket is not closed
    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized (sockets) {
      try {
        //noinspection WaitNotInLoop
        sockets.wait(1);
      }
      catch (InterruptedException e) {
        LOG.error(e.getMessage(), e);
      }
    }

    for (ServerSocket socket : sockets) {
      socket.close();
    }
    return ports;
  }

  public static String getLocalHostString() {
    // HACK for Windows with ipv6
    String localHostString = "localhost";
    try {
      InetAddress localHost = InetAddress.getByName(localHostString);
      if ((localHost.getAddress().length != 4 && OSInfo.isWindows) || (localHost.getAddress().length == 4 && OSInfo.isMac)) {
        localHostString = "127.0.0.1";
      }
    }
    catch (UnknownHostException ignored) {
    }
    return localHostString;
  }

  public static boolean isSniEnabled() {
    return SystemProperties.getBooleanProperty("jsse.enableSNIExtension", true);
  }
}
