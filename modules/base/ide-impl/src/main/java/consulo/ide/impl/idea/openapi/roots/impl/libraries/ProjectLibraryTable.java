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

package consulo.ide.impl.idea.openapi.roots.impl.libraries;

import consulo.component.persist.State;
import consulo.component.persist.StateSplitterEx;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.content.impl.internal.library.LibraryTableBase;
import consulo.ide.ServiceManager;
import consulo.content.impl.internal.library.LibraryImpl;
import consulo.project.Project;
import consulo.project.ProjectBundle;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablePresentation;
import consulo.content.library.LibraryTablesRegistrar;
import consulo.util.lang.Pair;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author dsl
 */
@Singleton
@State(name = "libraryTable", storages = @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/libraries/", stateSplitter = ProjectLibraryTable.LibraryStateSplitter.class))
public class ProjectLibraryTable extends LibraryTableBase {
  private static final LibraryTablePresentation PROJECT_LIBRARY_TABLE_PRESENTATION = new LibraryTablePresentation() {
    @Override
    public String getDisplayName(boolean plural) {
      return ProjectBundle.message("project.library.display.name", plural ? 2 : 1);
    }

    @Override
    public String getDescription() {
      return ProjectBundle.message("libraries.node.text.project");
    }

    @Override
    public String getLibraryTableEditorTitle() {
      return ProjectBundle.message("library.configure.project.title");
    }
  };

  public static LibraryTable getInstance(Project project) {
    return ServiceManager.getService(project, ProjectLibraryTable.class);
  }

  private final Project myProject;

  @Inject
  public ProjectLibraryTable(Project project) {
    myProject = project;
  }

  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Override
  public String getTableLevel() {
    return LibraryTablesRegistrar.PROJECT_LEVEL;
  }

  @Override
  public LibraryTablePresentation getPresentation() {
    return PROJECT_LIBRARY_TABLE_PRESENTATION;
  }

  @Override
  public boolean isEditable() {
    return true;
  }

  public final static class LibraryStateSplitter extends StateSplitterEx {
    @Override
    public List<Pair<Element, String>> splitState(@Nonnull Element state) {
      return splitState(state, LibraryImpl.LIBRARY_NAME_ATTR);
    }
  }
}
