/*
 * Copyright 2013-2026 consulo.io
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
package consulo.platform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * TCP of a platform's machine - connections are made and ports are bound there. For a remote platform the IDE machine
 * reaches them through the platform's own channel, no port of that machine is opened to the network.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public interface PlatformNetwork {
    /**
     * A connection to {@code host:port} as this machine resolves it - usually {@code 127.0.0.1}.
     */
    PlatformSocket connect(String host, int port) throws IOException;

    /**
     * A server socket of this machine - a process of this machine connects to {@link PlatformServerSocket#getPort()}
     * of {@code host}, the IDE accepts the connections.
     *
     * @param port {@code 0} for any free port
     */
    PlatformServerSocket bind(String host, int port) throws IOException;

    /**
     * An address of the IDE machine leading to {@code host:port} of this machine - for a client which can only connect by
     * an address (a debugger interface, an RMI client). Forwarded until closed.
     */
    default ForwardedPort forward(String host, int port) throws IOException {
        return forward(host, port, 0);
    }

    /**
     * Same as {@link #forward(String, int)}, on the given port of the IDE machine - for an address the client takes from
     * the other side as is (the stubs of RMI name the port of their server).
     *
     * @param localPort the port of the IDE machine, {@code 0} for any free one
     */
    ForwardedPort forward(String host, int port, int localPort) throws IOException;

    /**
     * A port of this machine leading to {@code localPort} of the IDE machine - for a process of this machine which
     * connects back to a server of the IDE, a debugger for one. The connections are piped until it is closed.
     */
    default ForwardedPort publish(int localPort) throws IOException {
        PlatformServerSocket serverSocket = bind("127.0.0.1", 0);
        Thread.ofVirtual().name("Published port " + localPort).start(() -> pipeConnections(serverSocket, localPort));
        return new ForwardedPort() {
            @Override
            public String host() {
                return serverSocket.getHost();
            }

            @Override
            public int port() {
                return serverSocket.getPort();
            }

            @Override
            public void close() {
                try {
                    serverSocket.close();
                }
                catch (IOException ignored) {
                }
            }
        };
    }

    private static void pipeConnections(PlatformServerSocket serverSocket, int localPort) {
        while (true) {
            PlatformSocket remote;
            try {
                remote = serverSocket.accept();
            }
            catch (IOException e) {
                // closed
                return;
            }
            Thread.ofVirtual().start(() -> {
                try (remote; Socket local = new Socket(InetAddress.getLoopbackAddress(), localPort)) {
                    InputStream fromRemote = remote.getInputStream();
                    OutputStream toLocal = local.getOutputStream();
                    Thread toIde = Thread.ofVirtual().start(() -> {
                        try {
                            fromRemote.transferTo(toLocal);
                        }
                        catch (IOException ignored) {
                        }
                        finally {
                            try {
                                local.shutdownOutput();
                            }
                            catch (IOException ignored) {
                            }
                        }
                    });
                    try {
                        local.getInputStream().transferTo(remote.getOutputStream());
                    }
                    catch (IOException ignored) {
                    }
                    finally {
                        remote.getOutputStream().close();
                    }
                    toIde.join();
                }
                catch (Exception ignored) {
                    // the connection ends with any of its sides
                }
            });
        }
    }
}
