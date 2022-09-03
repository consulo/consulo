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
package consulo.ide.impl.idea.packaging.impl.ui;

import consulo.application.AllIcons;
import consulo.ui.ex.tree.PresentationData;
import consulo.module.Module;
import consulo.project.ProjectBundle;
import consulo.module.impl.internal.layer.library.ModuleLibraryTable;
import consulo.content.impl.internal.library.LibraryImpl;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.content.library.LibraryTablePresentation;
import consulo.content.base.BinariesOrderRootType;
import consulo.virtualFileSystem.VirtualFile;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingElementPresentation;
import consulo.compiler.artifact.ui.PackagingElementWeights;
import consulo.ui.ex.SimpleTextAttributes;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class LibraryElementPresentation extends PackagingElementPresentation {
  private final String myLevel;
  private final String myModuleName;
  private final Library myLibrary;
  private final String myLibraryName;
  private final ArtifactEditorContext myContext;

  public LibraryElementPresentation(String libraryName, String level, @javax.annotation.Nullable String moduleName, Library library, ArtifactEditorContext context) {
    myLevel = level;
    myModuleName = moduleName;
    myLibrary = library;
    myLibraryName = libraryName;
    myContext = context;
  }

  @Override
  public String getPresentableName() {
    return myLibraryName;
  }

  @Override
  public boolean canNavigateToSource() {
    return myLibrary != null;
  }

  @Override
  public void navigateToSource() {
    myContext.selectLibrary(myLibrary);
  }

  @Override
  public void render(@Nonnull PresentationData presentationData, SimpleTextAttributes mainAttributes, SimpleTextAttributes commentAttributes) {
    if (myLibrary != null) {
      presentationData.setIcon(AllIcons.Nodes.PpLib);
      presentationData.addText(myLibraryName, mainAttributes);
      presentationData.addText(getLibraryTableComment(myLibrary), commentAttributes);
    }
    else {
      presentationData.addText(myLibraryName + " (" + (myModuleName != null ? "module '" + myModuleName + "'" : myLevel) + ")", 
                               SimpleTextAttributes.ERROR_ATTRIBUTES);
    }
  }

  @Override
  public int getWeight() {
    return PackagingElementWeights.LIBRARY;
  }

  public static String getLibraryTableDisplayName(final Library library) {
    LibraryTable table = library.getTable();
    LibraryTablePresentation presentation = table != null ? table.getPresentation() : ModuleLibraryTable.MODULE_LIBRARY_TABLE_PRESENTATION;
    return presentation.getDisplayName(false);
  }

  public static String getLibraryTableComment(final Library library) {
    LibraryTable libraryTable = library.getTable();
    String displayName;
    if (libraryTable != null) {
      displayName = libraryTable.getPresentation().getDisplayName(false);
    }
    else {
      Module module = (Module)((LibraryImpl)library).getModule();
      String tableName = getLibraryTableDisplayName(library);
      displayName = module != null ? "'" + module.getName() + "' " + tableName : tableName;
    }
    return " (" + displayName + ")";
  }

  public static String getLibraryItemText(final @Nonnull Library library, final boolean includeTableName) {
    String name = library.getName();
    VirtualFile[] files = library.getFiles(BinariesOrderRootType.getInstance());
    if (name != null) {
      return name + (includeTableName ? getLibraryTableComment(library) : "");
    }
    else if (files.length > 0) {
      return files[0].getName() + (includeTableName ? getLibraryTableComment(library) : "");
    }
    else {
      return ProjectBundle.message("library.empty.item");
    }
  }
}
