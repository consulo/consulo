/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.ui.impl.image;

import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 26.05.2024
 */
public interface DesktopAWTImage extends Image {
    @Nonnull
    DesktopAWTImage copyWithNewSize(int width, int height);

    default DesktopAWTImage copyWithNewScale(float scale) {
        int width = (int) Math.ceil(getWidth() * scale);
        int height = (int) Math.ceil(getHeight() * scale);
        return copyWithNewSize(width, height);
    }

    @Nonnull
    DesktopAWTImage copyWithForceLibraryId(String libraryId);

    static Image copyWithForceLibraryId(@Nonnull Image image, String forceLibraryId) {
        if (image instanceof DesktopAWTImage awt) {
            return awt.copyWithForceLibraryId(forceLibraryId);
        }
        return image;
    }
}
