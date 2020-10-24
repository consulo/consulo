/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.image;

import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 2020-09-26
 */
public interface ImageKey extends Image {
  @Nullable
  static ImageKey fromString(@Nullable String ids, int width, int height) {
    if(ids != null && ids.contains("@")) {
      String[] split = ids.split("@");
      return of(split[0], split[1].toLowerCase(Locale.ROOT), width, height);
    }

    return null;
  }

  /**
   * ImageID must is case sensitive
   */
  @Nonnull
  static ImageKey of(@Nonnull String groupId, @Nonnull String imageId, int width, int height) {
    return UIInternal.get()._ImageKey_of(groupId, imageId, width, height);
  }

  @Nonnull
  String getGroupId();

  @Nonnull
  String getImageId();
}
