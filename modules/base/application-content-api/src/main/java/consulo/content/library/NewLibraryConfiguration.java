/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.content.library;

import consulo.content.library.ui.LibraryEditor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
* @author nik
*/
public abstract class NewLibraryConfiguration {
  private final String myDefaultLibraryName;
  private final LibraryType<?> myLibraryType;
  private final LibraryProperties myProperties;

  protected NewLibraryConfiguration(@Nonnull String defaultLibraryName) {
    this(defaultLibraryName, null, null);
  }

  protected <P extends LibraryProperties> NewLibraryConfiguration(@Nonnull String defaultLibraryName, @Nullable LibraryType<P> libraryType, @Nullable P properties) {
    myDefaultLibraryName = defaultLibraryName;
    myLibraryType = libraryType;
    myProperties = properties;
  }

  public LibraryType<?> getLibraryType() {
    return myLibraryType;
  }

  public LibraryProperties getProperties() {
    return myProperties;
  }

  public String getDefaultLibraryName() {
    return myDefaultLibraryName;
  }

  public abstract void addRoots(@Nonnull LibraryEditor editor);
}
