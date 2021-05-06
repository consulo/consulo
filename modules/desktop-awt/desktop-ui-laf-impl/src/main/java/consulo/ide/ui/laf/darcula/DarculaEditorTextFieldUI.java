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
package consulo.ide.ui.laf.darcula;

import com.intellij.ide.ui.BasicEditorTextFieldUI;
import consulo.desktop.ui.laf.idea.darcula.DarculaUIUtil;
import com.intellij.ui.EditorTextField;

import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2019-04-27
 */
public class DarculaEditorTextFieldUI extends BasicEditorTextFieldUI {
  public static BasicEditorTextFieldUI createUI(JComponent c) {
    return new DarculaEditorTextFieldUI();
  }

  @Override
  protected void paintBackground(Graphics g, EditorTextField field) {
    if (DarculaUIUtil.isComboBoxEditor(field)) {
      super.paintBackground(g, field);
      return;
    }

   // DarculaTextFieldUI.paintBackground(g, field);
  }
}
