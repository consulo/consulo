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
package consulo.desktop.qt.ui.impl.image;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.logging.Logger;
import consulo.ui.ex.UIModificationTracker;
import consulo.ui.image.Image;
import io.qt.core.QRect;
import io.qt.gui.QIcon;
import io.qt.gui.QPainter;
import io.qt.gui.QPixmap;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public class DesktopQtDeferredImageImpl implements Image, DesktopQtImage {
    private static final Logger LOG = Logger.getInstance(DesktopQtDeferredImageImpl.class);

    private static final long EVALUATION_POLL_DELAY = 50L;

    private final Image myBase;
    private final Supplier<Image> myImageSupplier;

    private @Nullable Image myImage;
    private boolean myEvaluating;
    private long myGeneration;
    private long myModificationCount = -1;

    private @Nullable QPixmap myPixmap;
    private double myPixmapRatio;

    public DesktopQtDeferredImageImpl(Image base, Supplier<Image> imageSupplier) {
        myBase = base;
        myImageSupplier = imageSupplier;
    }

    private synchronized @Nullable Image evaluated() {
        long modificationCount = UIModificationTracker.getInstance().getModificationCount();
        if (myModificationCount != modificationCount) {
            myModificationCount = modificationCount;
            myGeneration++;
            myImage = null;
            myPixmap = null;
            myEvaluating = false;
        }

        Image image = myImage;
        if (image == null && !myEvaluating) {
            myEvaluating = true;

            long generation = myGeneration;
            AppExecutorUtil.getAppExecutorService().execute(() -> evaluate(generation));
        }
        return image;
    }

    private void evaluate(long generation) {
        Image image;
        try {
            image = myImageSupplier.get();
        }
        catch (Throwable e) {
            LOG.warn(e);
            image = myBase;
        }

        synchronized (this) {
            if (myGeneration == generation) {
                myImage = image;
                myEvaluating = false;
            }
        }
    }

    @Override
    public int getWidth() {
        Image image;
        synchronized (this) {
            image = myImage;
        }
        return (image == null ? myBase : image).getWidth();
    }

    @Override
    public int getHeight() {
        Image image;
        synchronized (this) {
            image = myImage;
        }
        return (image == null ? myBase : image).getHeight();
    }

    @Override
    public boolean isLive() {
        Image image = evaluated();
        return image == null || DesktopQtImage.isLive(image);
    }

    @Override
    public long paintFrame(QPainter painter, QRect rect, QIcon.Mode mode) {
        Image image = evaluated();
        if (image == null) {
            DesktopQtImage.paintFrame(myBase, painter, rect, mode);
            return EVALUATION_POLL_DELAY;
        }

        if (mode == QIcon.Mode.Disabled || DesktopQtImage.isLive(image)) {
            return DesktopQtImage.paintFrame(image, painter, rect, mode);
        }

        QPixmap pixmap = pixmap(image);
        if (!pixmap.isNull()) {
            painter.drawPixmap(rect, pixmap);
        }
        return 0;
    }

    @Override
    public QPixmap toQPixmap() {
        Image image = evaluated();
        return image == null ? DesktopQtImage.toQPixmap(myBase) : pixmap(image);
    }

    private QPixmap pixmap(Image image) {
        double ratio = DesktopQtImage.devicePixelRatio();

        QPixmap pixmap = myPixmap;
        if (pixmap == null || myPixmapRatio != ratio) {
            pixmap = DesktopQtImage.toQPixmap(image);
            myPixmap = pixmap;
            myPixmapRatio = ratio;
        }
        return pixmap;
    }
}
