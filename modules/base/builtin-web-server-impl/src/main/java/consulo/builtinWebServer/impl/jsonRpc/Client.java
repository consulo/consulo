// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.builtinWebServer.impl.jsonRpc;

import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.dataholder.UserDataHolderBase;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoop;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public abstract class Client extends UserDataHolderBase {
    protected final Channel myChannel;

    final ConcurrentIntObjectMap<CompletableFuture<Object>> myMessageCallbackMap = IntMaps.newConcurrentIntObjectHashMap();

    protected Client(Channel channel) {
        myChannel = channel;
    }

    public final EventLoop getEventLoop() {
        return myChannel.eventLoop();
    }

    public final ByteBufAllocator getByteBufAllocator() {
        return myChannel.alloc();
    }

    protected abstract ChannelFuture send(ByteBuf message);

    public abstract void sendHeartbeat();

    @SuppressWarnings("unchecked")
    final <T> @Nullable CompletableFuture<T> send(int messageId, ByteBuf message) {
        ChannelFuture channelFuture = send(message);
        if (messageId == -1) {
            return null;
        }

        CompletableFuture<T> promise = new CompletableFuture<>();
        promise.whenComplete((result, error) -> {
            if (error != null) {
                myMessageCallbackMap.remove(messageId);
            }
        });

        channelFuture.addListener(future -> {
            if (!future.isSuccess()) {
                Throwable cause = future.cause();
                if (cause == null) {
                    promise.completeExceptionally(new IllegalStateException("No success"));
                }
                else {
                    promise.completeExceptionally(cause);
                }
            }
        });
        myMessageCallbackMap.put(messageId, (CompletableFuture<Object>) promise);
        return promise;
    }

    final void rejectAsyncResults(ExceptionHandler exceptionHandler) {
        if (!myMessageCallbackMap.isEmpty()) {
            for (CompletableFuture<Object> promise : myMessageCallbackMap.values()) {
                try {
                    promise.completeExceptionally(new IllegalStateException("rejected"));
                }
                catch (Throwable e) {
                    exceptionHandler.exceptionCaught(e);
                }
            }
        }
    }
}
