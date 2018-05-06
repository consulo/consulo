/*
 * Copyright 2013-2018 consulo.io
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

import consulo.ui.UIInternal;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2018-05-06
 */
public final class ImageEffects {
  @Nonnull
  public static Image folded(@Nonnull Image... images) {
    if (images.length == 0) {
      throw new IllegalArgumentException("empty array");
    }
    return UIInternal.get()._ImageEffects_folded(images);
  }

  @Nonnull
  public static Image transparent(@Nonnull Image original) {
    return transparent(original, .5f);
  }

  @Nonnull
  public static Image transparent(@Nonnull Image original, float alpha) {
    return UIInternal.get()._ImageEffects_transparent(original, alpha);
  }
}
