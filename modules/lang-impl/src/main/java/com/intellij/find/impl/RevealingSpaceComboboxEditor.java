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

/*
 * Created by IntelliJ IDEA.
 * User: cdr
 * Date: Sep 7, 2007
 * Time: 1:45:27 PM
 */
package com.intellij.find.impl;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.StringComboboxEditor;

import javax.swing.*;

public class RevealingSpaceComboboxEditor extends StringComboboxEditor {
  public RevealingSpaceComboboxEditor(final Project project, ComboBox comboBox) {
    super(project, PlainTextFileType.INSTANCE, comboBox);

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        Editor editor = getEditor();
        if (editor != null) {
          editor.getSettings().setWhitespacesShown(true);
        }
      }
    });
  }

  @Override
  public void setItem(Object anObject) {
    super.setItem(anObject);
    Editor editor = getEditor();
    if (editor != null) {
      editor.getSettings().setWhitespacesShown(true);
    }
  }
}