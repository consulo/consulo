/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.ui.impl;

import consulo.localize.LocalizeValue;
import consulo.ui.image.IconLibraryDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 24/08/2023
 */
public class DarkMonoIconLibraryDescriptor implements IconLibraryDescriptor {
  @Nonnull
  @Override
  public String getLibraryId() {
    return "dark_mono";
  }

  @Nullable
  @Override
  public String getBaseLibraryId() {
    return LightMonoIconLibraryDescriptor.ID;
  }

  @Nonnull
  @Override
  public LocalizeValue getName() {
    return LocalizeValue.localizeTODO("Dark (colorless)");
  }
}
