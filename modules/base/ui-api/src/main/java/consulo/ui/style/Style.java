/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.style;

import consulo.ui.color.ColorValue;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 22-Jun-16
 */
public interface Style {
    static String LIGHT_ID = "light";
    static String SEMI_DARK = "semi_dark";
    static String DARK_ID = "dark";

    @Nonnull
    String getId();

    @Nonnull
    String getName();

    @Nonnull
    default String getIconLibraryId() {
        return IconLibraryManager.LIGHT_LIBRARY_ID;
    }

    @Nonnull
    ColorValue getColorValue(@Nonnull StyleColorValue colorKey);

    /**
     * Try to modify source image to showing in current style. Can return same image. Also step into composite icons
     *
     * @param image source image
     * @return cloned image in current style, or source image
     */
    @Nonnull
    default Image getImage(@Nonnull Image image) {
        return image;
    }

    default boolean isLight() {
        return !isDark();
    }

    default boolean isDark() {
        return false;
    }
}
