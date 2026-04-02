// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.icon;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.ui.ex.awt.EmptyIcon;
import consulo.ui.ex.awt.ImageUtil;
import consulo.ui.ex.awt.JBUI;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

public final class AsyncImageIconsProvider<T> implements IconsProvider<T> {
    private final CoroutineScope myScope;
    private final AsyncImageLoader<T> myLoader;

    private static final kotlinx.coroutines.CoroutineDispatcher RESIZE_DISPATCHER =
        kotlinx.coroutines.ExecutorCoroutineDispatcherKt.from(
            AppExecutorUtil.createBoundedApplicationPoolExecutor("Collaboration Tools images resizing executor", 3)
        );

    public AsyncImageIconsProvider(@Nonnull CoroutineScope scope, @Nonnull AsyncImageLoader<T> loader) {
        myScope = scope;
        myLoader = loader;
    }

    @Override
    public @Nonnull Icon getIcon(@Nullable T key, int iconSize) {
        Icon baseIcon = myLoader.createBaseIcon(key, iconSize);
        if (key == null) {
            return baseIcon;
        }
        else {
            return new AsyncImageIcon(myScope, baseIcon, (scaleCtx, width, height, continuation) ->
                loadAndResizeImage(key, scaleCtx, width, height));
        }
    }

    private @Nullable Image loadAndResizeImage(@Nonnull T key, @Nonnull JBUI.ScaleContext scaleCtx, int width, int height) throws Exception {
        Image image = myLoader.load(key);
        if (image == null) {
            return null;
        }
        Image hidpiImage = ImageUtil.ensureHiDPI(image, scaleCtx);
        Image scaleImage = ImageUtil.scaleImage(hidpiImage, width, height);
        return myLoader.postProcess(scaleImage);
    }

    public interface AsyncImageLoader<T> {
        @Nullable
        Image load(@Nonnull T key) throws Exception;

        default @Nonnull Icon createBaseIcon(@Nullable T key, int iconSize) {
            return EmptyIcon.create(iconSize);
        }

        default @Nonnull Image postProcess(@Nonnull Image image) throws Exception {
            return image;
        }
    }
}
