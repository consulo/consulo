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
package com.intellij.openapi.roots.ui.configuration.classpath;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryTableImplUtil;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablePresentation;
import com.intellij.openapi.roots.ui.configuration.LibraryTableModifiableModelProvider;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.EditExistingLibraryDialog;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.xml.util.XmlStringUtil;
import consulo.roots.types.BinariesOrderRootType;
import consulo.roots.ui.configuration.LibrariesConfigurator;
import consulo.roots.ui.configuration.ModulesConfigurator;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author nik
 */
public class LibraryClasspathTableItem<T extends LibraryOrderEntry> extends ClasspathTableItem<T> {
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final LibrariesConfigurator myLibrariesConfigurator;

  public LibraryClasspathTableItem(T orderEntry, @Nonnull Project project, @Nonnull ModulesConfigurator modulesConfigurator, @Nonnull LibrariesConfigurator librariesConfigurator) {
    super(orderEntry);
    myProject = project;
    myLibrariesConfigurator = librariesConfigurator;
  }

  @Override
  public boolean isEditable() {
    return myEntry.isValid();
  }

  @Override
  public void doEdit(ClasspathPanelImpl panel) {
    final Library library = getEntry().getLibrary();
    if (library == null) {
      return;
    }
    final LibraryTable table = library.getTable();
    final String tableLevel = table != null ? table.getTableLevel() : LibraryTableImplUtil.MODULE_LEVEL;
    final LibraryTablePresentation presentation = LibraryEditingUtil.getLibraryTablePresentation(myProject, tableLevel);
    final LibraryTableModifiableModelProvider provider = panel.getModifiableModelProvider(tableLevel);
    EditExistingLibraryDialog dialog = EditExistingLibraryDialog.createDialog(panel, provider, library, myProject, presentation, myLibrariesConfigurator);
    dialog.setContextModule(getEntry().getOwnerModule());
    dialog.show();
  }

  @Override
  public String getTooltipText() {
    final Library library = myEntry.getLibrary();
    if (library == null) return null;

    final String name = library.getName();
    if (name != null) {
      final List<String> invalidUrls = ((LibraryEx)library).getInvalidRootUrls(BinariesOrderRootType.getInstance());
      if (!invalidUrls.isEmpty()) {
        return ProjectBundle.message("project.roots.tooltip.library.has.broken.paths", name, invalidUrls.size());
      }
    }

    final List<String> descriptions = LibraryPresentationManager.getInstance().getDescriptions(library, myLibrariesConfigurator);
    if (descriptions.isEmpty()) return null;

    return XmlStringUtil.wrapInHtml(StringUtil.join(descriptions, "<br>"));
  }
}
