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
package consulo.language.inject;

import consulo.language.Language;
import consulo.localize.LocalizeValue;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 * @since 2013-08-01
 */
public abstract class Injectable implements Comparable<Injectable> {
  @Nonnull
  public abstract String getId();

  @Nonnull
  public abstract LocalizeValue getDisplayName();

  @Nullable
  public String getAdditionalDescription() {
    return null;
  }

  @Nonnull
  public Image getIcon() {
    return Image.empty(Image.DEFAULT_ICON_SIZE);
  }

  @Override
  public int compareTo(@Nonnull Injectable o) {
    return getDisplayName().compareTo(o.getDisplayName());
  }

  /**
   * @return null for reference injections
   */
  @Nullable
  public abstract Language getLanguage();

  public Language toLanguage() {
    return getLanguage() == null ? new Language(getId(), false) {
      @Nonnull
      @Override
      public LocalizeValue getDisplayName() {
        return Injectable.this.getDisplayName();
      }
    } : getLanguage();
  }

  public static Injectable fromLanguage(final Language language) {
    return new Injectable() {
      @Nonnull
      @Override
      public String getId() {
        return language.getID();
      }

      @Nonnull
      @Override
      public LocalizeValue getDisplayName() {
        return language.getDisplayName();
      }

      @Nullable
      @Override
      public String getAdditionalDescription() {
        final FileType ft = language.getAssociatedFileType();
        return ft != null ? " (" + ft.getDescription() + ")" : null;
      }

      @Nonnull
      @Override
      public Image getIcon() {
        final FileType ft = language.getAssociatedFileType();
        //noinspection ConstantConditions
        return ft != null && ft.getIcon() != null ? ft.getIcon() : Image.empty(Image.DEFAULT_ICON_SIZE);
      }

      @Override
      public Language getLanguage() {
        return language;
      }
    };
  }
}
