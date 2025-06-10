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
package consulo.ui;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * @author VISTALL
 * @since 2017-06-16
 */
public record Size2D(int width, int height) implements Serializable, Cloneable {
    public static final Size2D UNDEFINED = new Size2D(-1, -1);
    public static final Size2D ZERO = new Size2D(0, 0);

    private static final long serialVersionUID = 3195037735722861866L;

    public Size2D(@Nonnull Size2D size) {
        this(size.width(), size.height());
    }

    public Size2D(int widthAndHeight) {
        this(widthAndHeight, widthAndHeight);
    }

    @Override
    @SuppressWarnings("CloneDoesntCallSuperClone")
    public Size2D clone() {
        return new Size2D(width(), height());
    }
}
