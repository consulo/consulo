/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.diff.dir;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.ui.style.ComponentColors;

import jakarta.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public enum DirDiffOperation {
    COPY_TO(PlatformIconGroup.vcsArrow_right(), FileStatus.ADDED.getColor()),
    COPY_FROM(PlatformIconGroup.vcsArrow_left(), FileStatus.ADDED.getColor()),
    MERGE(PlatformIconGroup.vcsNot_equal(), FileStatus.MODIFIED.getColor()),
    EQUAL(PlatformIconGroup.vcsEqual(), ComponentColors.TEXT),
    NONE(Image.empty(Image.DEFAULT_ICON_SIZE), ComponentColors.TEXT),
    DELETE(PlatformIconGroup.generalRemove(), FileStatus.DELETED.getColor());

    private final Image myIcon;
    private final ColorValue myTextColor;

    DirDiffOperation(Image icon, ColorValue textColor) {
        myIcon = icon;
        myTextColor = textColor;
    }

    public Image getIcon() {
        return myIcon;
    }

    @Nullable
    public ColorValue getTextColor() {
        return myTextColor;
    }
}
