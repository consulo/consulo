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
package consulo.ui;

import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

/**
 * A frontend answers a {@link Length}, so that a size counted in fonts is resolved against the text it draws - the
 * same window is wider in the browser than it is on the desktop. The pixel calls are the same thing with no font part.
 *
 * @author VISTALL
 * @since 2026-08-13
 */
public interface HasSize {
    @RequiredUIAccess
    default void setWidth(Length width) {
        throw new AbstractMethodError("not supported");
    }

    @RequiredUIAccess
    default void setHeight(Length height) {
        throw new AbstractMethodError("not supported");
    }

    @RequiredUIAccess
    default void setMinWidth(Length width) {
        throw new AbstractMethodError("not supported");
    }

    @RequiredUIAccess
    default void setMinHeight(Length height) {
        throw new AbstractMethodError("not supported");
    }

    @RequiredUIAccess
    default void setWidth(int widthInPixels) {
        setWidth(Length.ofPixel(widthInPixels));
    }

    @RequiredUIAccess
    default void setHeight(int heightInPixels) {
        setHeight(Length.ofPixel(heightInPixels));
    }

    @RequiredUIAccess
    default void setMinWidth(int widthInPixels) {
        setMinWidth(Length.ofPixel(widthInPixels));
    }

    @RequiredUIAccess
    default void setMinHeight(int heightInPixels) {
        setMinHeight(Length.ofPixel(heightInPixels));
    }

    /**
     * A dimension of {@code -1} is unspecified and left untouched, keeping whatever default or earlier
     * value the component already carries.
     */
    @RequiredUIAccess
    default void setSize(Size2D size) {
        if (size.width() != -1) {
            setWidth(size.width());
        }

        if (size.height() != -1) {
            setHeight(size.height());
        }
    }

    /**
     * @param width  {@code null} is unspecified and left untouched
     * @param height {@code null} is unspecified and left untouched
     */
    @RequiredUIAccess
    default void setSize(@Nullable Length width, @Nullable Length height) {
        if (width != null) {
            setWidth(width);
        }

        if (height != null) {
            setHeight(height);
        }
    }
}
