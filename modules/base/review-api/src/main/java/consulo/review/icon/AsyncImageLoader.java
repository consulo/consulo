// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.review.icon;

import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @param <T> icon key type
 */
public interface AsyncImageLoader<T> {
    CompletableFuture<? extends @Nullable Image> load(T key);

    default Image createBaseIcon(@Nullable T key, int iconSize) {
        return Image.empty(iconSize);
    }

    default CompletableFuture<Image> postProcess(Image image) {
        return CompletableFuture.completedFuture(image);
    }
}
