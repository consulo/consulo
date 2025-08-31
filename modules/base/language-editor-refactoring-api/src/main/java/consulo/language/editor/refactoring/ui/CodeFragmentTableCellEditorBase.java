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
package consulo.language.editor.refactoring.ui;

import consulo.document.Document;
import consulo.document.event.DocumentListener;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.psi.PsiCodeFragment;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.Project;
import consulo.util.collection.Lists;
import consulo.virtualFileSystem.fileType.FileType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.util.List;

/**
 * @author dsl
 */
public class CodeFragmentTableCellEditorBase extends AbstractCellEditor implements TableCellEditor {
  private Document myDocument;
  protected PsiCodeFragment myCodeFragment;
  private final Project myProject;
  private final FileType myFileType;
  protected EditorTextField myEditorTextField;
  private final List<DocumentListener> myListeners = Lists.newLockFreeCopyOnWriteList();

  public CodeFragmentTableCellEditorBase(Project project, FileType fileType) {
    myProject = project;
    myFileType = fileType;
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
    myCodeFragment = (PsiCodeFragment)value;

    myDocument = PsiDocumentManager.getInstance(myProject).getDocument(myCodeFragment);
    myEditorTextField = createEditorField(myDocument);
    if (myEditorTextField != null) {
      for (DocumentListener listener : myListeners) {
        myEditorTextField.addDocumentListener(listener);
      }
      myEditorTextField.setDocument(myDocument);
      myEditorTextField.setBorder(new LineBorder(table.getSelectionBackground()));
    }

    return myEditorTextField;
  }

  protected EditorTextField createEditorField(Document document) {
    EditorTextField field = new EditorTextField(document, myProject, myFileType) {
      @Override
      protected boolean shouldHaveBorder() {
        return false;
      }
    };
    field.setBorder(new EmptyBorder(1, 1, 1, 1));
    return field;
  }

  @Override
  public PsiCodeFragment getCellEditorValue() {
    return myCodeFragment;
  }

  @Override
  public boolean stopCellEditing() {
    super.stopCellEditing();
    PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
    return true;
  }

  public void addDocumentListener(DocumentListener listener) {
    myListeners.add(listener);
  }

  public void clearListeners() {
    myListeners.clear();
  }
}
