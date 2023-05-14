/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.impl.image;

import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.style.StandardColors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Library implementation, used when catch some problem with initializing default libraries
 *
 * @author VISTALL
 * @since 07-Mar-22
 */
class ErrorBaseIconLibraryImpl extends BaseIconLibraryImpl {
  ErrorBaseIconLibraryImpl(@Nonnull BaseIconLibraryManager baseIconLibraryManager) {
    super("", baseIconLibraryManager);
  }

  @Nullable
  @Override
  protected Image createImage(@Nonnull byte[] _1xData, @Nullable byte[] _2xdata, boolean isSVG, int width, int height, String groupId, String imageId) {
    return ImageEffects.colorFilled(width, height, StandardColors.RED);
  }
}
