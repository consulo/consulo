/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.find.editorHeaderActions;

import com.intellij.find.EditorSearchSession;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * Created by IntelliJ IDEA.
 * User: zajac
 * Date: 05.03.11
 * Time: 10:40
 * To change this template use File | Settings | File Templates.
 */
public class RestorePreviousSettingsAction extends AnAction implements ShortcutProvider, DumbAware {
  @Override
  public void update(AnActionEvent e) {
    Project project = e.getProject();
    EditorSearchSession search = e.getData(EditorSearchSession.SESSION_KEY);
    e.getPresentation().setEnabled(project != null && search != null && !project.isDisposed() &&
                                   search.getTextInField().isEmpty() &&
                                   FindManager.getInstance(project).getPreviousFindModel() != null);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    FindModel findModel = e.getRequiredData(EditorSearchSession.SESSION_KEY).getFindModel();
    findModel.copyFrom(FindManager.getInstance(e.getProject()).getPreviousFindModel());
  }

  @Nullable
  @Override
  public ShortcutSet getShortcut() {
    return new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
  }
}
