// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.ui.impl.image;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Provide a way to show {@link Image} loaded in background as {@link Icon}
 * Icon size will be taken from placeholder {@code defaultIcon} and should be loaded and scaled to size with {@code imageLoader}
 */
public class AsyncImageIcon implements Icon, DesktopAWTImage {
    private static final Logger LOG = Logger.getInstance(AsyncImageIcon.class);

    private static final DeferredIconRepaintScheduler ourRepaintScheduler = new DeferredIconRepaintScheduler();

    private final Image myDefaultIcon;
    private final ImageRequest myImageRequest;

    private @Nullable Image myImage;
    private @Nullable Icon myScaledImage;

    public AsyncImageIcon(Image defaultIcon, Supplier<? extends CompletableFuture<? extends @Nullable Image>> imageLoader) {
        this(defaultIcon, new ImageRequest(imageLoader));
    }

    private AsyncImageIcon(Image defaultIcon, ImageRequest imageRequest) {
        myDefaultIcon = defaultIcon;
        myImageRequest = imageRequest;
    }

    @Override
    public int getIconHeight() {
        return TargetAWT.to(myDefaultIcon).getIconHeight();
    }

    @Override
    public int getIconWidth() {
        return TargetAWT.to(myDefaultIcon).getIconWidth();
    }

    @Override
    public int getHeight() {
        return getIconHeight();
    }

    @Override
    public int getWidth() {
        return getIconWidth();
    }

    @RequiredUIAccess
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        ImageRequest imageRequest = myImageRequest;
        if (!imageRequest.myCompleted && c != null) {
            imageRequest.requestRepaint(c, x, y, getIconWidth(), getIconHeight());
        }

        Image image = imageRequest.myImage;
        if (image == null) {
            TargetAWT.to(myDefaultIcon).paintIcon(c, g, x, y);
        }
        else {
            scaled(image).paintIcon(c, g, x, y);
        }
    }

    private Icon scaled(Image image) {
        Icon icon = myScaledImage;
        if (icon == null || myImage != image) {
            icon = TargetAWT.to(ImageEffects.resize(image, getIconWidth(), getIconHeight()));
            myImage = image;
            myScaledImage = icon;
        }
        return icon;
    }

    @Override
    public DesktopAWTImage copyWithNewSize(int width, int height) {
        return new AsyncImageIcon(ImageEffects.resize(myDefaultIcon, width, height), myImageRequest);
    }

    @Override
    public DesktopAWTImage copyWithForceLibraryId(String libraryId) {
        return new AsyncImageIcon(DesktopAWTImage.copyWithForceLibraryId(myDefaultIcon, libraryId), myImageRequest);
    }

    private static final class ImageRequest {
        private final Supplier<? extends CompletableFuture<? extends @Nullable Image>> myImageLoader;

        // We collect repaintRequests for the icon to understand which Components should be repainted when icon is loaded
        // We can receive paintIcon few times for the same component but with different x, y
        // Only the last request should be scheduled
        private final Map<Component, RepaintRequest> myRepaintRequests = new LinkedHashMap<>();
        private final Map<Component, Dimension> myRepaintSizes = new LinkedHashMap<>();

        private boolean myStarted;
        private volatile boolean myCompleted;
        private volatile @Nullable Image myImage;

        private ImageRequest(Supplier<? extends CompletableFuture<? extends @Nullable Image>> imageLoader) {
            myImageLoader = imageLoader;
        }

        @RequiredUIAccess
        private void requestRepaint(Component c, int x, int y, int width, int height) {
            myRepaintRequests.put(c, ourRepaintScheduler.createRepaintRequest(c, x, y));
            myRepaintSizes.put(c, new Dimension(width, height));

            if (!myStarted) {
                myStarted = true;
                AppExecutorUtil.getAppExecutorService().execute(() -> {
                    try {
                        myImageLoader.get().whenComplete((image, error) -> {
                            if (error != null) {
                                LOG.debug("Image loading failed", error);
                            }
                            Image loaded = error == null ? image : null;
                            SwingUtilities.invokeLater(() -> complete(loaded));
                        });
                    }
                    catch (Throwable e) {
                        LOG.debug("Image loading failed", e);
                        SwingUtilities.invokeLater(() -> complete(null));
                    }
                });
            }
        }

        @RequiredUIAccess
        private void complete(@Nullable Image image) {
            myImage = image;
            myCompleted = true;

            List<Map.Entry<Component, RepaintRequest>> requests = new ArrayList<>(myRepaintRequests.entrySet());
            myRepaintRequests.clear();
            for (Map.Entry<Component, RepaintRequest> entry : requests) {
                Dimension size = myRepaintSizes.get(entry.getKey());
                int width = size == null ? 0 : size.width;
                int height = size == null ? 0 : size.height;
                ourRepaintScheduler.scheduleRepaint(entry.getValue(), width, height, false);
            }
            myRepaintSizes.clear();
        }
    }
}
