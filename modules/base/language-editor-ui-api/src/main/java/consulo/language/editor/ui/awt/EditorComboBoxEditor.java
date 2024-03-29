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
package consulo.language.editor.ui.awt;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.document.Document;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * Combobox items are Documents for this combobox
 *
 * @author max
 */
public class EditorComboBoxEditor implements ComboBoxEditor {
  private final EditorTextField myTextField;
  protected static final String NAME = "ComboBox.textField";

  public EditorComboBoxEditor(Project project, FileType fileType) {
    myTextField = new ComboboxEditorTextField((Document)null, project, fileType) {
      @Override
      protected EditorEx createEditor() {
        EditorEx editor = super.createEditor();
        onEditorCreate(editor);
        return editor;
      }
    };
    myTextField.setName(NAME);
  }

  protected void onEditorCreate(EditorEx editor) {
  }

  @Override
  public void selectAll() {
    myTextField.selectAll();
    IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myTextField);
  }

  @Nullable
  public Editor getEditor() {
    return myTextField.getEditor();
  }

  @Override
  public EditorTextField getEditorComponent() {
    return myTextField;
  }

  @Override
  public void addActionListener(ActionListener l) {

  }

  @Override
  public void removeActionListener(ActionListener l) {

  }

  @Override
  public Object getItem() {
    return getDocument();
  }

  protected Document getDocument() {
    return myTextField.getDocument();
  }

  @Override
  public void setItem(Object anObject) {
    myTextField.setDocument((Document)anObject);
  }
}
