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
package com.intellij.openapi.roots.ui.configuration.libraryEditor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablePresentation;
import com.intellij.openapi.roots.ui.configuration.LibraryTableModifiableModelProvider;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesModifiableModel;
import com.intellij.openapi.util.Comparing;
import consulo.disposer.Disposer;
import consulo.roots.ui.configuration.LibrariesConfigurator;

import javax.annotation.Nullable;
import java.awt.*;

/**
 * @author nik
 */
public class EditExistingLibraryDialog extends LibraryEditorDialogBase {
  private ExistingLibraryEditor myLibraryEditor;
  private boolean myCommitChanges;
  private LibraryTable.ModifiableModel myTableModifiableModel;

  public static EditExistingLibraryDialog createDialog(Component parent,
                                                       LibraryTableModifiableModelProvider modelProvider,
                                                       Library library,
                                                       @Nullable Project project,
                                                       LibraryTablePresentation presentation,
                                                       LibrariesConfigurator librariesConfigurator) {
    LibraryTable.ModifiableModel modifiableModel = modelProvider.getModifiableModel();
    boolean commitChanges = false;
    ExistingLibraryEditor libraryEditor;
    if (modifiableModel instanceof LibrariesModifiableModel) {
      libraryEditor = ((LibrariesModifiableModel)modifiableModel).getLibraryEditor(library);
    }
    else {
      libraryEditor = new ExistingLibraryEditor(library, librariesConfigurator);
      commitChanges = true;
    }
    return new EditExistingLibraryDialog(parent, modifiableModel, project, libraryEditor, commitChanges, presentation, librariesConfigurator);
  }

  private EditExistingLibraryDialog(Component parent,
                                    LibraryTable.ModifiableModel tableModifiableModel,
                                    @Nullable Project project,
                                    ExistingLibraryEditor libraryEditor,
                                    boolean commitChanges,
                                    LibraryTablePresentation presentation,
                                    LibrariesConfigurator librariesConfigurator) {
    super(parent, new LibraryRootsComponent(project, libraryEditor));
    setTitle("Configure " + presentation.getDisplayName(false));
    myTableModifiableModel = tableModifiableModel;
    myLibraryEditor = libraryEditor;
    myCommitChanges = commitChanges;
    if (commitChanges) {
      Disposer.register(getDisposable(), libraryEditor);
    }
    librariesConfigurator.addLibraryEditorListener((library, oldName, newName) -> {
      if (library.equals(myLibraryEditor.getLibrary())) {
        myNameField.setText(newName);
      }
    }, getDisposable());
    init();
  }

  @Override
  protected boolean validateAndApply() {
    if (!super.validateAndApply()) {
      return false;
    }

    if (myCommitChanges) {
      myLibraryEditor.commit();
    }
    return true;
  }

  @Override
  protected LibraryTable.ModifiableModel getTableModifiableModel() {
    return myTableModifiableModel;
  }

  @Override
  protected boolean shouldCheckName(String newName) {
    return !Comparing.equal(newName, getLibraryRootsComponent().getLibraryEditor().getName());
  }
}
