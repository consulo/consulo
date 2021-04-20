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
package com.intellij.openapi.roots.ui.configuration.libraryEditor;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryTableBase;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryType;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.ui.FormBuilder;
import consulo.roots.ui.configuration.LibrariesConfigurator;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;

/**
 * @author nik
 */
public class CreateNewLibraryDialog extends LibraryEditorDialogBase {
  private final LibrariesConfigurator myLibrariesConfigurator;
  private final NewLibraryEditor myLibraryEditor;
  private final ComboBox myLibraryLevelCombobox;

  public CreateNewLibraryDialog(@Nonnull Project project,
                                @Nonnull JComponent parent,
                                @Nonnull LibrariesConfigurator librariesConfigurator,
                                @Nonnull NewLibraryEditor libraryEditor,
                                @Nonnull List<LibraryTable> libraryTables,
                                int selectedTable) {
    super(parent, new LibraryRootsComponent(project, libraryEditor));
    myLibrariesConfigurator = librariesConfigurator;
    myLibraryEditor = libraryEditor;
    final DefaultComboBoxModel model = new DefaultComboBoxModel();
    for (LibraryTable table : libraryTables) {
      model.addElement(table);
    }
    myLibraryLevelCombobox = new ComboBox(model);
    myLibraryLevelCombobox.setSelectedIndex(selectedTable);
    myLibraryLevelCombobox.setRenderer(new ListCellRendererWrapper() {
      @Override
      public void customize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
        if (value instanceof LibraryTable) {
          setText(((LibraryTable)value).getPresentation().getDisplayName(false));
        }
      }
    });
    init();
  }

  @Nonnull
  @Override
  protected LibraryTable.ModifiableModel getTableModifiableModel() {
    final LibraryTable selectedTable = (LibraryTable)myLibraryLevelCombobox.getSelectedItem();
    return myLibrariesConfigurator.getModifiableLibraryTable(selectedTable);
  }

  @Nonnull
  public Library createLibrary() {
    final LibraryTableBase.ModifiableModelEx modifiableModel = (LibraryTableBase.ModifiableModelEx)getTableModifiableModel();
    final LibraryType<?> type = myLibraryEditor.getType();
    final Library library = modifiableModel.createLibrary(myLibraryEditor.getName(), type != null ? type.getKind() : null);
    final LibraryEx.ModifiableModelEx model = (LibraryEx.ModifiableModelEx)library.getModifiableModel();
    myLibraryEditor.applyTo(model);
    WriteAction.run(model::commit);
    return library;
  }

  @Override
  protected void addNorthComponents(FormBuilder formBuilder) {
    formBuilder.addLabeledComponent("&Level:", myLibraryLevelCombobox);
  }

  @Override
  protected boolean shouldCheckName(String newName) {
    return true;
  }
}
