/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import consulo.container.boot.ContainerPathManager;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * @author pegov
 */
public class ShowLogAction extends AnAction implements DumbAware {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final File logFile = new File(ContainerPathManager.get().getLogPath(), "consulo.log");
    ShowFilePathAction.openFile(logFile);
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    presentation.setVisible(ShowFilePathAction.isSupported());
    presentation.setText(getActionName());
  }

  @Nonnull
  public static String getActionName() {
    return "Show Log in " + ShowFilePathAction.getFileManagerName();
  }
}
