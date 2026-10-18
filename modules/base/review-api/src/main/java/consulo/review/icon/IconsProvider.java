// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.review.icon;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @param <T> icon key type
 */
@FunctionalInterface
public interface IconsProvider<T> {
    /**
     * @param key      icon key
     * @param iconSize required icon size in pixels (unscaled)
     */
    @RequiredUIAccess
    Image getIcon(@Nullable T key, int iconSize);
}
