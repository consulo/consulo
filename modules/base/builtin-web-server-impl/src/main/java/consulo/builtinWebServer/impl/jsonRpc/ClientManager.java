// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.builtinWebServer.impl.jsonRpc;

import consulo.builtinWebServer.impl.webSocket.WebSocketServerOptions;
import consulo.disposer.Disposable;
import consulo.util.lang.Pair;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ClientManager implements Disposable {
    public static final AttributeKey<Client> CLIENT = AttributeKey.valueOf("SocketHandler.client");

    private final @Nullable ClientListener myListener;
    private final ExceptionHandler myExceptionHandler;
    private final ScheduledFuture<?> myHeartbeatTimer;
    private final Set<Client> myClients = new HashSet<>();

    public ClientManager(@Nullable ClientListener listener,
                         ExceptionHandler exceptionHandler,
                         @Nullable WebSocketServerOptions options,
                         ScheduledExecutorService heartbeatScheduler) {
        myListener = listener;
        myExceptionHandler = exceptionHandler;

        long heartbeatDelay = (options == null ? new WebSocketServerOptions() : options).getHeartbeatDelay();
        myHeartbeatTimer = heartbeatScheduler.scheduleWithFixedDelay(
            () -> forEachClient(client -> {
                try {
                    if (client.myChannel.isActive()) {
                        client.sendHeartbeat();
                    }
                }
                catch (Throwable e) {
                    myExceptionHandler.exceptionCaught(e);
                }
            }),
            heartbeatDelay,
            heartbeatDelay,
            TimeUnit.MILLISECONDS
        );
    }

    public ExceptionHandler getExceptionHandler() {
        return myExceptionHandler;
    }

    public void addClient(Client client) {
        synchronized (myClients) {
            myClients.add(client);
        }
    }

    private int getClientCount() {
        synchronized (myClients) {
            return myClients.size();
        }
    }

    public boolean hasClients() {
        return getClientCount() > 0;
    }

    @Override
    public void dispose() {
        try {
            myHeartbeatTimer.cancel(false);
        }
        finally {
            synchronized (myClients) {
                myClients.clear();
            }
        }
    }

    public <T> void send(int messageId, ByteBuf message, @Nullable List<CompletableFuture<Pair<Client, T>>> results) {
        message.retain();
        forEachClient(new Consumer<>() {
            private boolean myFirst = true;

            @Override
            public void accept(Client client) {
                try {
                    CompletableFuture<Pair<Client, T>> result = client.send(messageId, myFirst ? message : message.retainedDuplicate());
                    myFirst = false;
                    if (results != null) {
                        results.add(Objects.requireNonNull(result));
                    }
                }
                catch (Throwable e) {
                    myExceptionHandler.exceptionCaught(e);
                }
            }
        });
        message.release();
    }

    public boolean disconnectClient(Channel channel, Client client, boolean closeChannel) {
        synchronized (myClients) {
            if (!myClients.remove(client)) {
                return false;
            }
        }

        try {
            channel.attr(CLIENT).set(null);

            if (closeChannel) {
                channel.close();
            }

            client.rejectAsyncResults(myExceptionHandler);
        }
        finally {
            if (myListener != null) {
                myListener.disconnected(client);
            }
        }
        return true;
    }

    private void forEachClient(Consumer<Client> procedure) {
        synchronized (myClients) {
            for (Client client : myClients) {
                procedure.accept(client);
            }
        }
    }

    public @Nullable Client findClient(Predicate<? super Client> predicate) {
        synchronized (myClients) {
            for (Client client : myClients) {
                if (predicate.test(client)) {
                    return client;
                }
            }
            return null;
        }
    }
}
