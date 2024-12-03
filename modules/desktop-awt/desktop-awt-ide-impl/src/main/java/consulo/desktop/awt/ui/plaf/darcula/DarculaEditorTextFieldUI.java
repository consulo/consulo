/*
 * Copyright 2013-2019 consulo.io
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
package consulo.desktop.awt.ui.plaf.darcula;

import consulo.codeEditor.Editor;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.editor.ui.internal.BasicEditorTextFieldUI;
import consulo.ui.ex.awt.JBInsets;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBUIScale;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2019-10-30
 */
public class DarculaEditorTextFieldUI extends BasicEditorTextFieldUI {
  public static BasicEditorTextFieldUI createUI(JComponent c) {
    return new DarculaEditorTextFieldUI();
  }

  @Override
  protected void paintBackground(Graphics g, EditorTextField field) {
  }

  @Override
  public Dimension getMinimumSize(JComponent c) {
    EditorTextField editorTextField = (EditorTextField)c;

    Editor editor = editorTextField.getEditor();

    Dimension size = JBUI.size(1, 10);
    if (editor != null) {
      size.height = editor.getLineHeight();

      size.height = Math.max(size.height, JBUIScale.scale(16));

      JBInsets.addTo(size, editorTextField.getInsets());
      JBInsets.addTo(size, editor.getInsets());
    }

    return size;
  }
}
