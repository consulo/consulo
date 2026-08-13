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

/**
 * @author VISTALL
 * @since 2026-08-13
 */
public interface HasSize {
    @RequiredUIAccess
    default void setWidth(int widthInPixels) {
        throw new AbstractMethodError("not supported");
    }

    @RequiredUIAccess
    default void setHeight(int heightInPixels) {
        throw new AbstractMethodError("not supported");
    }

    @RequiredUIAccess
    default void setMinWidth(int widthInPixels) {
        throw new AbstractMethodError("not supported");
    }

    @RequiredUIAccess
    default void setMinHeight(int heightInPixels) {
        throw new AbstractMethodError("not supported");
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
}
