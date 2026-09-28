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
package consulo.http.impl.internal.local;

import consulo.application.progress.ProgressIndicator;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * The body of an answer as a stream, which - unlike {@link HttpResponse.BodySubscribers#ofInputStream()} - does not
 * wait for the next bytes forever: a read fails after the read timeout without bytes, as a read of
 * {@link java.net.URLConnection} does, and stops when the progress of the thread is canceled.
 *
 * @author VISTALL
 * @since 2026-09-28
 */
final class LocalHttpClientBodyStream extends InputStream implements HttpResponse.BodySubscriber<InputStream> {
    private static final Object END = new Object();

    private final BlockingQueue<Object> myQueue = new LinkedBlockingQueue<>();
    private final int myReadTimeout;
    private final Supplier<@Nullable ProgressIndicator> myIndicator;

    private volatile Flow.@Nullable Subscription mySubscription;

    private Iterator<ByteBuffer> myBuffers = Collections.emptyIterator();
    private @Nullable ByteBuffer myCurrent;
    private boolean myEnd;
    private volatile boolean myClosed;

    /**
     * @param readTimeout the time to wait for the next bytes, in milliseconds - zero waits forever
     */
    LocalHttpClientBodyStream(int readTimeout, Supplier<@Nullable ProgressIndicator> indicator) {
        myReadTimeout = readTimeout;
        myIndicator = indicator;
    }

    @Override
    public CompletionStage<InputStream> getBody() {
        return CompletableFuture.completedStage(this);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        mySubscription = subscription;
        if (myClosed) {
            subscription.cancel();
            return;
        }
        subscription.request(1);
    }

    @Override
    public void onNext(List<ByteBuffer> item) {
        myQueue.offer(item);
    }

    @Override
    public void onError(Throwable throwable) {
        myQueue.offer(throwable);
    }

    @Override
    public void onComplete() {
        myQueue.offer(END);
    }

    @Override
    public int read() throws IOException {
        ByteBuffer buffer = nextBuffer();
        return buffer == null ? -1 : buffer.get() & 0xFF;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        if (length == 0) {
            return 0;
        }

        ByteBuffer buffer = nextBuffer();
        if (buffer == null) {
            return -1;
        }

        int count = Math.min(length, buffer.remaining());
        buffer.get(bytes, offset, count);
        return count;
    }

    @Override
    public int available() {
        ByteBuffer current = myCurrent;
        return current == null ? 0 : current.remaining();
    }

    private @Nullable ByteBuffer nextBuffer() throws IOException {
        if (myClosed) {
            throw new IOException("Stream closed");
        }

        while (myCurrent == null || !myCurrent.hasRemaining()) {
            if (myBuffers.hasNext()) {
                myCurrent = myBuffers.next();
                continue;
            }

            if (myEnd) {
                return null;
            }

            Object item = take();
            if (item == END) {
                myEnd = true;
                return null;
            }

            if (item instanceof Throwable throwable) {
                myEnd = true;
                throw throwable instanceof IOException e ? e : new IOException(throwable);
            }

            @SuppressWarnings("unchecked")
            List<ByteBuffer> buffers = (List<ByteBuffer>) item;
            myBuffers = buffers.iterator();

            Flow.Subscription subscription = mySubscription;
            if (subscription != null) {
                subscription.request(1);
            }
        }
        return myCurrent;
    }

    /**
     * Waits for the next bytes in slices, so a canceled progress stops the wait.
     */
    private Object take() throws IOException {
        long deadline = myReadTimeout > 0 ? System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(myReadTimeout) : Long.MAX_VALUE;
        while (true) {
            Object item;
            try {
                item = myQueue.poll(LocalHttpClientExecutor.WAIT_SLICE_MS, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                close();
                Thread.currentThread().interrupt();
                throw new InterruptedIOException();
            }

            if (item != null) {
                return item;
            }

            ProgressIndicator indicator = myIndicator.get();
            if (indicator != null && indicator.isCanceled()) {
                close();
                indicator.checkCanceled();
            }

            if (System.nanoTime() >= deadline) {
                close();
                throw new SocketTimeoutException("Read timed out");
            }
        }
    }

    @Override
    public void close() {
        if (myClosed) {
            return;
        }
        myClosed = true;

        Flow.Subscription subscription = mySubscription;
        if (subscription != null && !myEnd) {
            subscription.cancel();
        }
    }
}
