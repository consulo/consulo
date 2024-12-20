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

import consulo.ui.AntialiasingType;
import consulo.ui.internal.UIInternal;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 22-Jun-16
 */
public interface StyleManager {
    @Nonnull
    static StyleManager get() {
        return UIInternal.get()._StyleManager_get();
    }

    @Nonnull
    List<Style> getStyles();

    @Nullable
    default Style getStyle(@Nonnull String styleId) {
        for (Style style : getStyles()) {
            if (Objects.equals(style.getId(), styleId)) {
                return style;
            }
        }
        return null;
    }

    @Nonnull
    Style getCurrentStyle();

    void setCurrentStyle(@Nonnull Style newStyle);

    @Nonnull
    Runnable addChangeListener(@Nonnull StyleChangeListener listener);

    default void refreshAntialiasingType(@Nonnull AntialiasingType antialiasingType) {
    }

    default void refreshUI() {
    }
}
