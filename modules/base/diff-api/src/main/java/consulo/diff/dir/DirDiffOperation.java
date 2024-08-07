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

import consulo.application.AllIcons;
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
  COPY_TO, COPY_FROM, MERGE, EQUAL, NONE, DELETE;

  public Image getIcon() {
    switch (this) {
      case COPY_TO:   return AllIcons.Vcs.Arrow_right;
      case COPY_FROM: return AllIcons.Vcs.Arrow_left;
      case MERGE:     return AllIcons.Vcs.Not_equal;
      case EQUAL:     return AllIcons.Vcs.Equal;
      case DELETE:    return PlatformIconGroup.generalRemove();
      case NONE:
    }
    return Image.empty(Image.DEFAULT_ICON_SIZE);
  }

  @Nullable
  public ColorValue getTextColor() {
    switch (this) {
      case COPY_TO:
      case COPY_FROM:
        return FileStatus.ADDED.getColor();
      case MERGE:
        return FileStatus.MODIFIED.getColor();
      case DELETE:
        return FileStatus.DELETED.getColor();
      case EQUAL:
      case NONE:
    }
    return ComponentColors.TEXT;
  }
}
