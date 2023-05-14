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
package consulo.module.content.library;

import consulo.content.library.LibraryProperties;
import consulo.content.library.LibraryType;
import consulo.content.library.PersistentLibraryKind;
import consulo.module.content.layer.ModuleRootLayer;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 24/01/2022
 */
public abstract class ModuleAwareLibraryType<P extends LibraryProperties> extends LibraryType<P> {
  public ModuleAwareLibraryType(@Nonnull PersistentLibraryKind<P> libraryKind) {
    super(libraryKind);
  }

  public abstract boolean isAvailable(@Nonnull ModuleRootLayer model);

  @Override
  public boolean isAvailable() {
    throw new UnsupportedOperationException();
  }
}
