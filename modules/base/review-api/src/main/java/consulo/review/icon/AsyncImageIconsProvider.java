// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.icon;

import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @param <T> icon key type
 */
public class AsyncImageIconsProvider<T> implements IconsProvider<T> {
    private final AsyncImageLoader<T> myLoader;

    public AsyncImageIconsProvider(AsyncImageLoader<T> loader) {
        myLoader = loader;
    }

    @Override
    public Image getIcon(@Nullable T key, int iconSize) {
        Image baseIcon = myLoader.createBaseIcon(key, iconSize);
        if (key == null) {
            return baseIcon;
        }
        return Image.async(baseIcon, () -> loadAndResizeImage(key, baseIcon.getWidth(), baseIcon.getHeight()));
    }

    private CompletableFuture<@Nullable Image> loadAndResizeImage(T key, int width, int height) {
        return myLoader.load(key).thenCompose(image -> {
            if (image == null) {
                return CompletableFuture.<@Nullable Image>completedFuture(null);
            }
            return myLoader.postProcess(ImageEffects.resize(image, width, height)).thenApply(it -> (@Nullable Image) it);
        });
    }
}
