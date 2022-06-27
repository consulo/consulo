/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import consulo.content.library.Library;
import consulo.content.library.LibraryKind;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class LibraryPresentationManager {
  public static LibraryPresentationManager getInstance() {
    return ServiceManager.getService(LibraryPresentationManager.class);
  }

  @Nonnull
  public abstract Image getNamedLibraryIcon(@Nonnull Library library, @Nullable LibrariesConfigurator context);

  @Nullable
  public abstract Image getCustomIcon(@Nonnull Library library, @Nullable LibrariesConfigurator context);

  @Nonnull
  public abstract List<Image> getCustomIcons(@Nonnull Library library, @Nullable LibrariesConfigurator context);

  @Nonnull
  public abstract List<String> getDescriptions(@Nonnull Library library, LibrariesConfigurator context);

  @Nonnull
  public abstract List<String> getDescriptions(@Nonnull VirtualFile[] classRoots, Set<LibraryKind> excludedKinds);

  public abstract List<Library> getLibraries(@Nonnull Set<LibraryKind> kinds, @Nonnull Project project, @Nullable LibrariesConfigurator context);

  public abstract boolean isLibraryOfKind(@Nonnull List<VirtualFile> files, @Nonnull LibraryKind kind);

  public abstract boolean isLibraryOfKind(@Nonnull Library library, @Nonnull LibrariesContainer librariesContainer, @Nonnull Set<? extends LibraryKind> acceptedKinds);
}
