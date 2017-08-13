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
package com.intellij.openapi.keymap.impl.ui;

import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.project.Project;

import javax.swing.*;

public class EditKeymapsDialog extends SingleConfigurableEditor {
  private final String myActionToSelect;

  public EditKeymapsDialog(Project project, String actionToSelect) {
    super(project, new KeymapPanel());
    myActionToSelect = actionToSelect;
  }

  @Override
  public void show() {
    if (myActionToSelect != null) {
      SwingUtilities.invokeLater(() -> ((KeymapPanel)getConfigurable()).selectAction(myActionToSelect));
    }
    super.show();
  }

  @Override
  protected String getDimensionServiceKey(){
    return "#com.intellij.openapi.keymap.impl.ui.EditKeymapsDialog";
  }
}
