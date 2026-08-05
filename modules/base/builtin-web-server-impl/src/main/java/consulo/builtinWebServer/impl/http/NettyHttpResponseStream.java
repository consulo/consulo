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
package consulo.builtinWebServer.impl.http;

import consulo.builtinWebServer.http.HttpResponseStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.LastHttpContent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
final class NettyHttpResponseStream implements HttpResponseStream {
    private final Channel myChannel;
    private final AtomicBoolean myClosed = new AtomicBoolean();
    private final List<Runnable> myCloseListeners = new CopyOnWriteArrayList<>();

    NettyHttpResponseStream(Channel channel) {
        myChannel = channel;
        channel.closeFuture().addListener(future -> fireClosed());
    }

    @Override
    public void write(byte[] chunk) {
        if (!isOpen()) {
            return;
        }
        myChannel.writeAndFlush(new DefaultHttpContent(Unpooled.copiedBuffer(chunk)));
    }

    @Override
    public void close() {
        if (myClosed.get()) {
            return;
        }
        if (myChannel.isActive()) {
            myChannel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        }
        fireClosed();
    }

    @Override
    public boolean isOpen() {
        return !myClosed.get() && myChannel.isActive();
    }

    @Override
    public void onClose(Runnable listener) {
        if (myClosed.get()) {
            listener.run();
            return;
        }
        myCloseListeners.add(listener);
    }

    private void fireClosed() {
        if (!myClosed.compareAndSet(false, true)) {
            return;
        }
        for (Runnable listener : myCloseListeners) {
            listener.run();
        }
        myCloseListeners.clear();
    }
}
