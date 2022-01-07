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
package consulo.ui.impl;

import consulo.localize.LocalizeValue;
import consulo.ui.image.IconLibraryDescriptor;
import consulo.ui.image.IconLibraryManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2020-10-08
 */
public class LightIconLibraryDescriptor implements IconLibraryDescriptor {
  @Nonnull
  @Override
  public String getLibraryId() {
    return IconLibraryManager.LIGHT_LIBRARY_ID;
  }

  @Nullable
  @Override
  public String getBaseLibraryId() {
    return null;
  }

  @Nonnull
  @Override
  public LocalizeValue getName() {
    return LocalizeValue.localizeTODO("Light");
  }
}
