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
import consulo.content.library.Library;
import consulo.content.library.LibraryKind;
import consulo.ide.ServiceManager;
import consulo.ide.setting.module.LibrariesConfigurator;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;
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

  
  public abstract Image getNamedLibraryIcon(Library library, @Nullable LibrariesConfigurator context);

  public abstract @Nullable Image getCustomIcon(Library library, @Nullable LibrariesConfigurator context);

  
  public abstract List<Image> getCustomIcons(Library library, @Nullable LibrariesConfigurator context);

  
  public abstract List<String> getDescriptions(Library library, LibrariesConfigurator context);

  
  public abstract List<String> getDescriptions(VirtualFile[] classRoots, Set<LibraryKind> excludedKinds);

  public abstract List<Library> getLibraries(Set<LibraryKind> kinds, Project project, @Nullable LibrariesConfigurator context);
}
