/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot;

import consulo.project.Project;
import consulo.content.OrderRootType;
import consulo.content.library.Library;
import consulo.content.library.OrderRoot;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor.ExistingLibraryEditor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.annotation.DeprecationInfo;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
public interface LibrariesContainer {

  @Nullable
  Project getProject();

  enum LibraryLevel {@Deprecated(forRemoval = true) @DeprecationInfo("Unused") GLOBAL, PROJECT, MODULE;
    public String toString() {
      return StringUtil.capitalize(name().toLowerCase());
    }
  }

  @Nonnull
  Library[] getLibraries(@Nonnull LibraryLevel libraryLevel);

  @Nonnull
  Library[] getAllLibraries();

  @Nonnull
  VirtualFile[] getLibraryFiles(@Nonnull Library library, @Nonnull OrderRootType rootType);

  boolean canCreateLibrary(@Nonnull LibraryLevel level);

  @Nonnull
  List<LibraryLevel> getAvailableLevels();

  Library createLibrary(@Nonnull @NonNls String name, @Nonnull LibraryLevel level,
                        @Nonnull VirtualFile[] classRoots, @Nonnull VirtualFile[] sourceRoots);

  Library createLibrary(@Nonnull @NonNls String name, @Nonnull LibraryLevel level,
                        @Nonnull Collection<? extends OrderRoot> roots);

  Library createLibrary(@Nonnull NewLibraryEditor libraryEditor, @Nonnull LibraryLevel level);

  @Nonnull
  String suggestUniqueLibraryName(@Nonnull String baseName);

  @Nullable
  ExistingLibraryEditor getLibraryEditor(@Nonnull Library library);
}
