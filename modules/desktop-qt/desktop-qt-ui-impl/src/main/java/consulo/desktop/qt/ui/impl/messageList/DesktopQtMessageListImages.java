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
package consulo.desktop.qt.ui.impl.messageList;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.desktop.qt.ui.impl.image.DesktopQtImage;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.impl.messageList.MessageListImages;
import consulo.ui.messageList.AsyncHtmlImageLoader;
import io.qt.core.QBuffer;
import io.qt.core.QByteArray;
import io.qt.core.QIODevice;
import io.qt.gui.QPixmap;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * @author VISTALL
 * @since 2026-10-25
 */
final class DesktopQtMessageListImages {
    private static final Logger LOG = Logger.getInstance(DesktopQtMessageListImages.class);

    private final Executor myUIExecutor;
    private final Runnable myRefresh;
    private final Map<String, CompletableFuture<@Nullable Image>> myImages = new HashMap<>();
    private final Map<String, String> myDataUris = new HashMap<>();

    private @Nullable AsyncHtmlImageLoader myLoader;
    private boolean myRefreshPending;

    DesktopQtMessageListImages(Executor uiExecutor, Runnable refresh) {
        myUIExecutor = uiExecutor;
        myRefresh = refresh;
    }

    @Nullable AsyncHtmlImageLoader getLoader() {
        return myLoader;
    }

    void setLoader(@Nullable AsyncHtmlImageLoader loader) {
        myLoader = loader;
        myImages.clear();
        myDataUris.clear();
    }

    String resolveImages(String html, int maxWidth) {
        return MessageListImages.replaceHtmlSources(html, src -> resolve(src, maxWidth));
    }

    private String resolve(String src, int maxWidth) {
        if (src.startsWith("data:")) {
            return src;
        }

        CompletableFuture<@Nullable Image> request = myImages.computeIfAbsent(src, this::request);
        if (!request.isDone()) {
            return dataUri(PlatformIconGroup.processStep_passive(), "loading");
        }

        Image image = request.isCompletedExceptionally() ? null : request.getNow(null);
        if (image == null) {
            return dataUri(PlatformIconGroup.filetypesUnknown(), "notLoaded");
        }

        int width = Math.max(image.getWidth(), 1);
        int height = Math.max(image.getHeight(), 1);
        float scale = Math.min(1f, (float) MessageListImages.MAX_IMAGE_HEIGHT / height);
        if (maxWidth > 0) {
            scale = Math.min(scale, (float) maxWidth / width);
        }
        int scaledWidth = Math.max(1, Math.round(width * scale));
        int scaledHeight = Math.max(1, Math.round(height * scale));
        return dataUri(scale < 1f ? ImageEffects.resize(image, scaledWidth, scaledHeight) : image, src + "@" + scaledWidth + "x" + scaledHeight);
    }

    private String dataUri(Image image, String cacheKey) {
        return myDataUris.computeIfAbsent(cacheKey, key -> {
            QPixmap pixmap = DesktopQtImage.toQPixmap(image);
            QByteArray bytes = new QByteArray();
            QBuffer buffer = new QBuffer(bytes);
            try {
                buffer.open(QIODevice.OpenModeFlag.WriteOnly);
                pixmap.save(buffer, "PNG");
            }
            finally {
                buffer.close();
                buffer.dispose();
            }
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes.toByteArray());
        });
    }

    private CompletableFuture<@Nullable Image> request(String src) {
        CompletableFuture<@Nullable Image> result = new CompletableFuture<>();
        AsyncHtmlImageLoader loader = myLoader;
        AppExecutorUtil.getAppExecutorService().execute(() -> {
            try {
                if (loader != null) {
                    loader.load(null, src).whenComplete((image, error) -> {
                        if (error != null) {
                            result.completeExceptionally(error);
                        }
                        else {
                            result.complete(image);
                        }
                    });
                }
                else {
                    result.complete(Image.fromUrl(new URL(src)));
                }
            }
            catch (IOException | RuntimeException e) {
                result.completeExceptionally(e);
            }
        });
        result.whenComplete((image, error) -> {
            if (error != null) {
                LOG.debug("Image loading failed: " + src, error);
            }
            myUIExecutor.execute(() -> {
                if (myImages.get(src) == result) {
                    scheduleRefresh();
                }
            });
        });
        return result;
    }

    private void scheduleRefresh() {
        if (myRefreshPending) {
            return;
        }
        myRefreshPending = true;
        myUIExecutor.execute(() -> {
            myRefreshPending = false;
            myRefresh.run();
        });
    }
}
