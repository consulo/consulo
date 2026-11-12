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

import consulo.platform.PlatformServerSocket;
import consulo.platform.PlatformSocket;
import consulo.remoteServer.agent.impl.internal.protocol.ListenEvent;
import consulo.remoteServer.agent.impl.internal.protocol.ListenRequest;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A port bound on the agent's machine by a {@code Listen} stream - an accepted connection is claimed by a tunnel.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
final class RemoteServerSocket implements PlatformServerSocket {
    private static final int BIND_TIMEOUT_SECONDS = 30;

    // the end of the accepted connections
    private static final Object END = new Object();

    private final RemoteAgentConnection myConnection;
    private final String myHost;
    private final CompletableFuture<Integer> myBound = new CompletableFuture<>();
    private final BlockingQueue<Object> myAccepted = new LinkedBlockingQueue<>();
    private volatile @Nullable ClientCallStreamObserver<ListenRequest> myCall;

    private RemoteServerSocket(RemoteAgentConnection connection, String host) {
        myConnection = connection;
        myHost = host;
    }

    static RemoteServerSocket bind(RemoteAgentConnection connection, String host, int port) throws IOException {
        RemoteServerSocket socket = new RemoteServerSocket(connection, host);
        connection.listen(host, port, socket.new Events());
        try {
            socket.myBound.get(BIND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            socket.close();
            throw new InterruptedIOException();
        }
        catch (ExecutionException | TimeoutException e) {
            socket.close();
            Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
            throw new IOException("Cannot bind " + host + ":" + port + " of the agent's machine: " + cause.getMessage(), cause);
        }
        return socket;
    }

    @Override
    public String getHost() {
        return myHost;
    }

    @Override
    public int getPort() {
        return myBound.getNow(-1);
    }

    @Override
    public PlatformSocket accept() throws IOException {
        Object next;
        try {
            next = myAccepted.take();
        }
        catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        if (next == END) {
            myAccepted.add(END);
            throw new IOException("The server socket is closed");
        }
        if (next instanceof Throwable error) {
            myAccepted.add(END);
            throw new IOException("The server socket failed: " + error.getMessage(), error);
        }
        return myConnection.openAcceptedTunnel((String) next);
    }

    @Override
    public void close() {
        ClientCallStreamObserver<ListenRequest> call = myCall;
        if (call != null) {
            call.cancel("closed", null);
        }
        myAccepted.add(END);
    }

    private final class Events implements ClientResponseObserver<ListenRequest, ListenEvent> {
        @Override
        public void beforeStart(ClientCallStreamObserver<ListenRequest> requestStream) {
            myCall = requestStream;
        }

        @Override
        public void onNext(ListenEvent event) {
            switch (event.getEventCase()) {
                case BOUND -> myBound.complete(event.getBound());
                case ACCEPTED -> myAccepted.add(event.getAccepted());
                default -> {
                }
            }
        }

        @Override
        public void onError(Throwable error) {
            myBound.completeExceptionally(error);
            myAccepted.add(error);
        }

        @Override
        public void onCompleted() {
            myBound.completeExceptionally(new IOException("The listen stream ended before it bound"));
            myAccepted.add(END);
        }
    }
}
