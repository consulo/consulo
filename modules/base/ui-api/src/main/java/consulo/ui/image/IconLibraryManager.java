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

import consulo.ui.UIInternal;
import consulo.ui.style.Style;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-09-26
 */
public interface IconLibraryManager {
  String LIGHT_LIBRARY_ID = "Default";
  String DARK_LIBRARY_ID = "Dark";

  @Nonnull
  public static IconLibraryManager get() {
    return UIInternal.get()._IconLibraryManager_get();
  }

  /**
   * @return libraries id
   */
  @Nonnull
  Set<String> getLibrariesId();

  @Nonnull
  String getActiveLibraryId();

  @Nonnull
  default Image forceChangeLibrary(@Nonnull String libraryId, @Nonnull Image image) {
    return image;
  }

  boolean isFromStyle();

  void setActiveLibrary(@Nullable String iconLibraryId);

  void setActiveLibraryFromStyle(@Nonnull Style style);

  long getModificationCount();
}
