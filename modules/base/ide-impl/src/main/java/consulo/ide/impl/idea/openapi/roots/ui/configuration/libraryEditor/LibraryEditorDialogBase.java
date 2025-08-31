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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor;

import consulo.module.Module;
import consulo.project.ProjectBundle;
import consulo.module.impl.internal.layer.library.ModuleLibraryTable;
import consulo.content.library.LibraryTable;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.libraries.LibraryEditingUtil;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.FormBuilder;
import consulo.disposer.Disposer;

import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
* @author nik
*/
public abstract class LibraryEditorDialogBase extends DialogWrapper {
  protected JTextField myNameField;
  private LibraryRootsComponent myLibraryRootsComponent;

  public LibraryEditorDialogBase(Component parent, LibraryRootsComponent libraryRootsComponent) {
    super(parent, true);
    myLibraryRootsComponent = libraryRootsComponent;
    libraryRootsComponent.resetProperties();
    setTitle(ProjectBundle.message("library.configure.title"));
    Disposer.register(getDisposable(), myLibraryRootsComponent);
  }

  public void setContextModule(Module module) {
    myLibraryRootsComponent.setContextModule(module);
  }

  @Override
  protected String getDimensionServiceKey() {
    return "#consulo.ide.impl.idea.openapi.roots.ui.configuration.libraryEditor.LibraryEditorDialog";
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myNameField;
  }

  @Override
  protected final void doOKAction() {
    if (!validateAndApply()) {
      return;
    }
    super.doOKAction();
  }

  protected boolean validateAndApply() {
    String newName = myNameField.getText().trim();
    if (newName.length() == 0) {
      newName = null;
    }
    if (shouldCheckName(newName)) {
      LibraryTable.ModifiableModel tableModifiableModel = getTableModifiableModel();
      if (tableModifiableModel != null && !(tableModifiableModel instanceof ModuleLibraryTable)) {
        if (newName == null) {
          Messages.showErrorDialog(ProjectBundle.message("library.name.not.specified.error", newName), ProjectBundle.message("library.name.not.specified.title"));
          return false;
        }
        if (LibraryEditingUtil.libraryAlreadyExists(tableModifiableModel, newName)) {
          Messages.showErrorDialog(ProjectBundle.message("library.name.already.exists.error", newName), ProjectBundle.message("library.name.already.exists.title"));
          return false;
        }
      }
      myLibraryRootsComponent.renameLibrary(newName);
    }
    myLibraryRootsComponent.applyProperties();
    return true;
  }

  protected abstract boolean shouldCheckName(String newName);

  @Nullable
  protected LibraryTable.ModifiableModel getTableModifiableModel() {
    return null;
  }

  protected LibraryRootsComponent getLibraryRootsComponent() {
    return myLibraryRootsComponent;
  }

  @Override
  protected JComponent createNorthPanel() {
    String currentName = myLibraryRootsComponent.getLibraryEditor().getName();
    myNameField = new JTextField(currentName);
    myNameField.selectAll();

    FormBuilder formBuilder = FormBuilder.createFormBuilder().addLabeledComponent("&Name:", myNameField);
    addNorthComponents(formBuilder);
    return formBuilder.addVerticalGap(10).getPanel();
  }

  protected void addNorthComponents(FormBuilder formBuilder) {
  }

  @Override
  protected JComponent createCenterPanel() {
    return myLibraryRootsComponent.getComponent();
  }
}
