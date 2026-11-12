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
package consulo.remoteServer.agent.impl.internal;

import consulo.logging.Logger;
import consulo.platform.ForwardedPort;
import consulo.platform.PlatformNetwork;
import consulo.platform.PlatformServerSocket;
import consulo.platform.PlatformSocket;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP of the agent's machine - connections and bound ports are carried by streams of the agent.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public class RemotePlatformNetwork implements PlatformNetwork {
    private static final Logger LOG = Logger.getInstance(RemotePlatformNetwork.class);

    private static final int BUFFER_SIZE = 64 * 1024;

    private final RemoteAgentConnection myConnection;

    public RemotePlatformNetwork(RemoteAgentConnection connection) {
        myConnection = connection;
    }

    @Override
    public PlatformSocket connect(String host, int port) throws IOException {
        return myConnection.openTunnel(host, port);
    }

    @Override
    public PlatformServerSocket bind(String host, int port) throws IOException {
        return RemoteServerSocket.bind(myConnection, host, port);
    }

    /**
     * A loopback server socket of the IDE machine - every accepted connection is piped through a connection of its own.
     */
    @Override
    public ForwardedPort forward(String host, int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
        Forwarding forwarding = new Forwarding(serverSocket, host, port);
        Thread.ofVirtual().name("Port forwarding to " + host + ":" + port).start(forwarding::acceptLoop);
        return forwarding;
    }

    private final class Forwarding implements ForwardedPort {
        private final ServerSocket myServerSocket;
        private final String myRemoteHost;
        private final int myRemotePort;
        private final Set<Closeable> myConnections = ConcurrentHashMap.newKeySet();

        private Forwarding(ServerSocket serverSocket, String remoteHost, int remotePort) {
            myServerSocket = serverSocket;
            myRemoteHost = remoteHost;
            myRemotePort = remotePort;
        }

        @Override
        public String host() {
            return myServerSocket.getInetAddress().getHostAddress();
        }

        @Override
        public int port() {
            return myServerSocket.getLocalPort();
        }

        private void acceptLoop() {
            while (!myServerSocket.isClosed()) {
                Socket socket;
                try {
                    socket = myServerSocket.accept();
                }
                catch (IOException e) {
                    if (!myServerSocket.isClosed()) {
                        LOG.warn("Port forwarding to " + myRemoteHost + ":" + myRemotePort + " stopped", e);
                    }
                    return;
                }
                Thread.ofVirtual().start(() -> serve(socket));
            }
        }

        private void serve(Socket socket) {
            PlatformSocket remote;
            try {
                socket.setTcpNoDelay(true);
                remote = connect(myRemoteHost, myRemotePort);
            }
            catch (IOException e) {
                LOG.info("Cannot connect to " + myRemoteHost + ":" + myRemotePort + ": " + e.getMessage());
                closeQuietly(socket);
                return;
            }
            myConnections.add(socket);
            myConnections.add(remote);
            try {
                // the remote side to the local one on a thread of its own, the local side to the remote one here
                Thread toLocal = Thread.ofVirtual().start(() -> {
                    try {
                        pump(remote.getInputStream(), socket.getOutputStream());
                        socket.shutdownOutput();
                    }
                    catch (IOException e) {
                        closeQuietly(socket);
                    }
                });
                OutputStream remoteOutput = remote.getOutputStream();
                pump(socket.getInputStream(), remoteOutput);
                // the local client is done sending - the remote side gets the end of input
                remoteOutput.close();
                toLocal.join();
            }
            catch (IOException | InterruptedException e) {
                LOG.debug(e);
            }
            finally {
                closeQuietly(remote);
                closeQuietly(socket);
                myConnections.remove(socket);
                myConnections.remove(remote);
            }
        }

        @Override
        public void close() {
            closeQuietly(myServerSocket);
            for (Closeable connection : myConnections) {
                closeQuietly(connection);
            }
            myConnections.clear();
        }
    }

    private static void pump(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int count;
        try {
            while ((count = input.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
                output.flush();
            }
        }
        catch (SocketException e) {
            // the local side went away - the same as the end of its input
        }
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        }
        catch (IOException ignored) {
        }
    }
}
