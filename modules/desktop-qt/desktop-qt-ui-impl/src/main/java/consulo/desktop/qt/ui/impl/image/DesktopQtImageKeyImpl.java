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
package consulo.desktop.qt.ui.impl.image;

import consulo.ui.image.IconLibraryManager;
import consulo.ui.image.ImageKey;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.ui.impl.image.ImageReference;
import io.qt.gui.QPixmap;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtImageKeyImpl implements ImageKey, DesktopQtImage {
    private static final BaseIconLibraryManager ourLibraryManager = (BaseIconLibraryManager) IconLibraryManager.get();

    private final String myGroupId;
    private final String myImageId;
    private final int myWidth;
    private final int myHeight;

    public DesktopQtImageKeyImpl(String groupId, String imageId, int width, int height) {
        myGroupId = groupId;
        myImageId = imageId;
        myWidth = width;
        myHeight = height;
    }

    private @Nullable ImageReference resolveImage() {
        return ourLibraryManager.resolveImage(null, this);
    }

    @Override
    public String getGroupId() {
        return myGroupId;
    }

    @Override
    public String getImageId() {
        return myImageId;
    }

    @Override
    public int getHeight() {
        return myHeight;
    }

    @Override
    public int getWidth() {
        return myWidth;
    }

    @Override
    public QPixmap toQPixmap() {
        ImageReference ref = resolveImage();
        if (!(ref instanceof DesktopQtImageReference qtRef)) {
            return DesktopQtEmptyImageImpl.createPixmap(getWidth(), getHeight());
        }
        return qtRef.toQPixmap(getWidth(), getHeight());
    }
}
