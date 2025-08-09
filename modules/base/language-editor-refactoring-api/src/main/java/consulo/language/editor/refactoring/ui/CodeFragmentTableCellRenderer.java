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
import consulo.virtualFileSystem.fileType.FileType;
import consulo.language.internal.InternalStdFileTypes;
import consulo.project.Project;
import consulo.language.psi.PsiCodeFragment;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.ui.ex.awt.IdeBorderFactory;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * @author dsl
 */
public class CodeFragmentTableCellRenderer implements TableCellRenderer {
  private final Project myProject;
  private final FileType myFileType;

  @Deprecated(forRemoval = true)
  public CodeFragmentTableCellRenderer(Project project) {
    this(project, InternalStdFileTypes.JAVA);
  }

  public CodeFragmentTableCellRenderer(Project project, FileType fileType) {
    myProject = project;
    myFileType = fileType;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, final boolean hasFocus, int row, int column) {
    PsiCodeFragment codeFragment = (PsiCodeFragment)value;

    final EditorTextField editorTextField;
    Document document = null;
    if (codeFragment != null) {
      document = PsiDocumentManager.getInstance(myProject).getDocument(codeFragment);
      editorTextField = new EditorTextField(document, myProject, myFileType) {
        @Override
        protected boolean shouldHaveBorder() {
          return false;
        }
      };
    }
    else {
      editorTextField = new EditorTextField("", myProject, myFileType) {
        @Override
        protected boolean shouldHaveBorder() {
          return false;
        }
      };
    }

    if (!table.isShowing()) {
      editorTextField.ensureWillComputePreferredSize();
    }

    editorTextField.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
    editorTextField.setBorder((hasFocus || isSelected) ? BorderFactory.createLineBorder(table.getSelectionBackground()) : IdeBorderFactory.createEmptyBorder(1));
    if (isSelected && document != null) {
      final Color bg = table.getSelectionBackground();
      final Color fg = table.getSelectionForeground();
      editorTextField.setBackground(bg);
      editorTextField.setForeground(fg);
      editorTextField.setAsRendererWithSelection(bg, fg);
    }
    return editorTextField;
  }
}
