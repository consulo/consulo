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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.classpath;

import consulo.content.base.BinariesOrderRootType;
import consulo.content.internal.LibraryEx;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablePresentation;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor.EditExistingLibraryDialog;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.ide.setting.module.*;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.impl.internal.layer.library.LibraryTableImplUtil;
import consulo.project.Project;
import consulo.project.ProjectBundle;

import jakarta.annotation.Nonnull;
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
  public void doEdit(ClasspathPanel panel) {
    final Library library = getEntry().getLibrary();
    if (library == null) {
      return;
    }
    final LibraryTable table = library.getTable();
    final String tableLevel = table != null ? table.getTableLevel() : LibraryTableImplUtil.MODULE_LEVEL;
    final LibraryTablePresentation presentation = LibraryEditingUtil.getLibraryTablePresentation(myProject, tableLevel);
    final LibraryTableModifiableModelProvider provider = panel.getModifiableModelProvider(tableLevel);
    EditExistingLibraryDialog dialog = EditExistingLibraryDialog.createDialog(panel.getComponent(), provider, library, myProject, presentation, myLibrariesConfigurator);
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
