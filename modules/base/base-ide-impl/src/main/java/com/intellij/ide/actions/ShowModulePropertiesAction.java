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
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import javax.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 *         Date: Feb 8, 2004
 */
public class ShowModulePropertiesAction extends AnAction{
  private final Provider<ShowSettingsUtil> myShowSettingsUtilProvider;

  @Inject
  public ShowModulePropertiesAction(Provider<ShowSettingsUtil> showSettingsUtilProvider) {
    myShowSettingsUtilProvider = showSettingsUtilProvider;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    final Module module = e.getData(LangDataKeys.MODULE_CONTEXT);
    if (module == null) {
      return;
    }

    myShowSettingsUtilProvider.get().showProjectStructureDialog(project, it -> it.select(module.getName(), null, true));
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    final Module module = e.getData(LangDataKeys.MODULE_CONTEXT);
    e.getPresentation().setVisible(project != null && module != null);
  }
}
