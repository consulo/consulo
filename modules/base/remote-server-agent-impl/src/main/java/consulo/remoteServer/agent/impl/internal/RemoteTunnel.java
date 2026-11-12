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

import com.google.protobuf.ByteString;
import consulo.platform.PlatformSocket;
import consulo.remoteServer.agent.impl.internal.protocol.Empty;
import consulo.remoteServer.agent.impl.internal.protocol.TunnelConnect;
import consulo.remoteServer.agent.impl.internal.protocol.TunnelEvent;
import consulo.remoteServer.agent.impl.internal.protocol.TunnelInput;
import io.grpc.stub.StreamObserver;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * One TCP connection of the agent's machine, carried by a {@code Tunnel} stream of the agent.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
final class RemoteTunnel implements PlatformSocket {
    private static final int CONNECT_TIMEOUT_SECONDS = 30;

    // the end of the incoming data
    private static final Object END = new Object();

    private final BlockingQueue<Object> myIncoming = new LinkedBlockingQueue<>();
    private final CompletableFuture<Void> myConnected = new CompletableFuture<>();
    private final StreamObserver<TunnelInput> myRequest;
    private final InputStream myInputStream = new TunnelInputStream();
    private final OutputStream myOutputStream = new TunnelOutputStream();
    private boolean myRequestClosed;

    private RemoteTunnel(Function<StreamObserver<TunnelEvent>, StreamObserver<TunnelInput>> open) {
        myRequest = open.apply(new StreamObserver<>() {
            @Override
            public void onNext(TunnelEvent event) {
                switch (event.getEventCase()) {
                    case CONNECTED -> myConnected.complete(null);
                    case DATA -> myIncoming.add(event.getData().toByteArray());
                    default -> {
                    }
                }
            }

            @Override
            public void onError(Throwable error) {
                myConnected.completeExceptionally(error);
                myIncoming.add(error);
            }

            @Override
            public void onCompleted() {
                myConnected.completeExceptionally(new IOException("The tunnel closed before it connected"));
                myIncoming.add(END);
            }
        });
    }

    /**
     * @param open opens the stream - the request observer for the given event observer
     */
    static RemoteTunnel open(Function<StreamObserver<TunnelEvent>, StreamObserver<TunnelInput>> open,
                             TunnelConnect connect) throws IOException {
        RemoteTunnel tunnel = new RemoteTunnel(open);
        tunnel.send(TunnelInput.newBuilder().setConnect(connect).build());
        try {
            tunnel.myConnected.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            tunnel.close();
            throw new InterruptedIOException();
        }
        catch (ExecutionException | TimeoutException e) {
            tunnel.close();
            Throwable cause = e instanceof ExecutionException ? e.getCause() : e;
            String target = connect.getAccepted().isEmpty() ? connect.getHost() + ":" + connect.getPort() : connect.getAccepted();
            throw new IOException("Cannot connect to " + target + " of the agent's machine: " + cause.getMessage(), cause);
        }
        return tunnel;
    }

    @Override
    public InputStream getInputStream() {
        return myInputStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return myOutputStream;
    }

    @Override
    public void close() {
        closeRequest();
        myIncoming.add(END);
    }

    private synchronized void send(TunnelInput input) throws IOException {
        if (myRequestClosed) {
            throw new IOException("The tunnel is closed");
        }
        myRequest.onNext(input);
    }

    private synchronized void closeRequest() {
        if (myRequestClosed) {
            return;
        }
        myRequestClosed = true;
        try {
            myRequest.onNext(TunnelInput.newBuilder().setClose(Empty.getDefaultInstance()).build());
            myRequest.onCompleted();
        }
        catch (RuntimeException ignored) {
            // the stream has failed already
        }
    }

    private final class TunnelInputStream extends InputStream {
        private byte @Nullable [] myChunk;
        private int myOffset;
        private boolean myEnded;

        @Override
        public int read() throws IOException {
            byte[] single = new byte[1];
            int count = read(single, 0, 1);
            return count < 0 ? -1 : single[0] & 0xFF;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (length == 0) {
                return 0;
            }
            byte[] chunk = nextChunk();
            if (chunk == null) {
                return -1;
            }
            int count = Math.min(length, chunk.length - myOffset);
            System.arraycopy(chunk, myOffset, buffer, offset, count);
            myOffset += count;
            return count;
        }

        private byte @Nullable [] nextChunk() throws IOException {
            while (myChunk == null || myOffset >= myChunk.length) {
                if (myEnded) {
                    return null;
                }
                Object next;
                try {
                    next = myIncoming.take();
                }
                catch (InterruptedException e) {
                    throw new InterruptedIOException();
                }
                if (next == END) {
                    myEnded = true;
                    myIncoming.add(END);
                    return null;
                }
                if (next instanceof Throwable error) {
                    myEnded = true;
                    throw new IOException("The tunnel failed: " + error.getMessage(), error);
                }
                myChunk = (byte[]) next;
                myOffset = 0;
            }
            return myChunk;
        }
    }

    private final class TunnelOutputStream extends OutputStream {
        @Override
        public void write(int value) throws IOException {
            write(new byte[]{(byte) value}, 0, 1);
        }

        @Override
        public void write(byte[] buffer, int offset, int length) throws IOException {
            if (length > 0) {
                send(TunnelInput.newBuilder().setData(ByteString.copyFrom(buffer, offset, length)).build());
            }
        }

        @Override
        public void close() {
            closeRequest();
        }
    }
}
