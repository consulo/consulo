/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import consulo.start.WelcomeFrameManager;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;

/**
 * @author Dmitry Avdeev
 *         Date: 11/6/12
 */
public class ImportProjectAction extends ImportModuleAction {
  @Override
  public boolean canCreateNewProject() {
    return true;
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    if (WelcomeFrameManager.isFromWelcomeFrame(e)) {
      e.getPresentation().setIcon(AllIcons.Welcome.ImportProject);
    }
  }
}
