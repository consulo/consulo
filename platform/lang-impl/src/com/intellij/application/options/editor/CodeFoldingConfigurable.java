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

package com.intellij.application.options.editor;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class CodeFoldingConfigurable implements Configurable {
  private JCheckBox myCbFolding;
  private JPanel myRootPanel;

  @Override
  @Nls
  public String getDisplayName() {
    return ApplicationBundle.message("group.code.folding");
  }

  @Override
  public String getHelpTopic() {
    return "reference.settingsdialog.IDE.editor.code.folding";
  }

  @Override
  public JComponent createComponent() {
    return myRootPanel;
  }

  @Override
  public boolean isModified() {
    return myCbFolding.isSelected() != EditorSettingsExternalizable.getInstance().isFoldingOutlineShown();
  }

  @Override
  public void apply() throws ConfigurationException {
    EditorSettingsExternalizable.getInstance().setFoldingOutlineShown(myCbFolding.isSelected());

    final List<Pair<Editor, Project>> toUpdate = new ArrayList<Pair<Editor, Project>>();
    for (final Editor editor : EditorFactory.getInstance().getAllEditors()) {
      final Project project = editor.getProject();
      if (project != null && !project.isDefault()) {
        toUpdate.add(Pair.create(editor, project));
      }
    }

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
          for (Pair<Editor, Project> each : toUpdate) {
              if (each.second == null || each.second.isDisposed()) {
                  continue;
              }
              final CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(each.second);
              if (foldingManager != null) {
                  foldingManager.buildInitialFoldings(each.first);
              }
          }
          EditorOptionsPanel.reinitAllEditors();
      }
    }, ModalityState.NON_MODAL);
  }

  @Override
  public void reset() {
    myCbFolding.setSelected(EditorSettingsExternalizable.getInstance().isFoldingOutlineShown());
  }

  @Override
  public void disposeUIResources() {

  }
}
