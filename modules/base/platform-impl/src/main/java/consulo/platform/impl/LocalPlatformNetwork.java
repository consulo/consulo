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
package consulo.platform.impl;

import consulo.platform.ForwardedPort;
import consulo.platform.PlatformNetwork;
import consulo.platform.PlatformServerSocket;
import consulo.platform.PlatformSocket;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * TCP of the IDE machine itself.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public class LocalPlatformNetwork implements PlatformNetwork {
    @Override
    public PlatformSocket connect(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        return new LocalSocket(socket);
    }

    @Override
    public PlatformServerSocket bind(String host, int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(host));
        return new PlatformServerSocket() {
            @Override
            public String getHost() {
                return serverSocket.getInetAddress().getHostAddress();
            }

            @Override
            public int getPort() {
                return serverSocket.getLocalPort();
            }

            @Override
            public PlatformSocket accept() throws IOException {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                return new LocalSocket(socket);
            }

            @Override
            public void close() throws IOException {
                serverSocket.close();
            }
        };
    }

    @Override
    public ForwardedPort forward(String host, int port) {
        // the address is an address of the IDE machine already
        return new ForwardedPort() {
            @Override
            public String host() {
                return host;
            }

            @Override
            public int port() {
                return port;
            }

            @Override
            public void close() {
            }
        };
    }

    private record LocalSocket(Socket socket) implements PlatformSocket {
        @Override
        public InputStream getInputStream() throws IOException {
            return socket.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return new FilterOutputStream(socket.getOutputStream()) {
                @Override
                public void write(byte[] buffer, int offset, int length) throws IOException {
                    out.write(buffer, offset, length);
                }

                @Override
                public void close() throws IOException {
                    // the end of the data - the socket stays open for reading
                    socket.shutdownOutput();
                }
            };
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
