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
package consulo.web.ui.impl.internal.image;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
public class WebAsyncImageImpl implements Image {
    private static final Logger LOG = Logger.getInstance(WebAsyncImageImpl.class);

    private static final long LOAD_TIMEOUT_SECONDS = 60;

    private static final ConcurrentMap<String, WebAsyncImageImpl> ourImages = ContainerUtil.createConcurrentWeakValueMap();

    private final String myId = UUID.randomUUID().toString().replace("-", "");
    private final Image myBase;
    private final Supplier<? extends CompletableFuture<? extends @Nullable Image>> myLoader;

    private @Nullable CompletableFuture<@Nullable Image> myRequest;

    public WebAsyncImageImpl(Image base, Supplier<? extends CompletableFuture<? extends @Nullable Image>> loader) {
        myBase = base;
        myLoader = loader;
        ourImages.put(myId, this);
    }

    public static @Nullable WebAsyncImageImpl find(String id) {
        return ourImages.get(id);
    }

    public String getId() {
        return myId;
    }

    public Image getBase() {
        return myBase;
    }

    public @Nullable Image getLoaded() {
        CompletableFuture<@Nullable Image> request;
        synchronized (this) {
            request = myRequest;
        }
        return request != null && request.isDone() && !request.isCompletedExceptionally() ? request.getNow(null) : null;
    }

    public @Nullable Image await() {
        try {
            return request().get(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            LOG.debug("Image loading failed", e);
            return null;
        }
    }

    private synchronized CompletableFuture<@Nullable Image> request() {
        CompletableFuture<@Nullable Image> request = myRequest;
        if (request == null) {
            CompletableFuture<@Nullable Image> result = new CompletableFuture<>();
            AppExecutorUtil.getAppExecutorService().execute(() -> {
                try {
                    myLoader.get().whenComplete((image, error) -> {
                        if (error != null) {
                            result.completeExceptionally(error);
                        }
                        else {
                            result.complete(image);
                        }
                    });
                }
                catch (Throwable e) {
                    result.completeExceptionally(e);
                }
            });
            request = result;
            myRequest = request;
        }
        return request;
    }

    @Override
    public int getHeight() {
        return myBase.getHeight();
    }

    @Override
    public int getWidth() {
        return myBase.getWidth();
    }
}
