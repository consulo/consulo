/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.util.ui.classpath;

import consulo.application.Application;
import consulo.project.Project;
import consulo.module.impl.internal.layer.library.LibraryTableImplUtil;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablesRegistrar;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class ChooseLibrariesFromTablesDialog extends ChooseLibrariesDialogBase {
  private @Nullable final Project myProject;
  private final boolean myShowCustomLibraryTables;

  protected ChooseLibrariesFromTablesDialog(@Nonnull String title, @Nonnull Project project, final boolean showCustomLibraryTables) {
    super(project, title);
    myShowCustomLibraryTables = showCustomLibraryTables;
    myProject = project;
  }

  protected ChooseLibrariesFromTablesDialog(@Nonnull JComponent parentComponent,
                                            @Nonnull String title,
                                            @Nullable Project project,
                                            final boolean showCustomLibraryTables) {
    super(parentComponent, title);
    myShowCustomLibraryTables = showCustomLibraryTables;
    myProject = project;
  }

  public static ChooseLibrariesFromTablesDialog createDialog(@Nonnull String title,
                                                             @Nonnull Project project,
                                                             final boolean showCustomLibraryTables) {
    final ChooseLibrariesFromTablesDialog dialog = new ChooseLibrariesFromTablesDialog(title, project, showCustomLibraryTables);
    dialog.init();
    return dialog;
  }

  @Nonnull
  @Override
  protected Project getProject() {
    if (myProject != null) {
      return myProject;
    }
    return super.getProject();
  }

  @Override
  protected JComponent createNorthPanel() {
    return null;
  }

  @Override
  protected void collectChildren(Object element, List<Object> result) {
    if (element instanceof Application) {
      for (LibraryTable table : getLibraryTables(myProject)) {
        if (hasLibraries(table)) {
          result.add(table);
        }
      }
    }
    else if (element instanceof LibraryTable) {
      Collections.addAll(result, getLibraries((LibraryTable)element));
    }
  }

  public static List<LibraryTable> getLibraryTables(final Project project) {
    final List<LibraryTable> tables = new ArrayList<LibraryTable>();
    final LibraryTablesRegistrar registrar = LibraryTablesRegistrar.getInstance();
    if (project != null) {
      tables.add(registrar.getLibraryTable(project));
    }
    return tables;
  }

  private boolean hasLibraries(LibraryTable table) {
    final Library[] libraries = getLibraries(table);
    for (Library library : libraries) {
      if (acceptsElement(library)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected int getLibraryTableWeight(@Nonnull LibraryTable libraryTable) {
    if (libraryTable.getTableLevel().equals(LibraryTableImplUtil.MODULE_LEVEL)) return 0;
    if (isProjectLibraryTable(libraryTable)) return 1;
    return 3;
  }

  private boolean isProjectLibraryTable(LibraryTable libraryTable) {
    final LibraryTablesRegistrar registrar = LibraryTablesRegistrar.getInstance();
    return myProject != null && libraryTable.equals(registrar.getLibraryTable(myProject));
  }

  @Override
  protected boolean isAutoExpandLibraryTable(@Nonnull LibraryTable libraryTable) {
    return isProjectLibraryTable(libraryTable);
  }

  @Nonnull
  protected Library[] getLibraries(@Nonnull LibraryTable table) {
    return table.getLibraries();
  }
}
