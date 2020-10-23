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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import consulo.moduleImport.ModuleImportContext;
import consulo.moduleImport.ModuleImportProvider;
import consulo.moduleImport.ModuleImportProviders;
import consulo.moduleImport.ui.ModuleImportProcessor;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.Pair;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Dmitry Avdeev
 * Date: 10/31/12
 */
public class ImportModuleAction extends AnAction {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Project project = e.getProject();
    assert project != null;
    executeImportAction(project, null);
  }

  @RequiredUIAccess
  public static void executeImportAction(@Nonnull Project project, @Nullable FileChooserDescriptor descriptor) {
    AsyncResult<Pair<ModuleImportContext, ModuleImportProvider<ModuleImportContext>>> chooser = ModuleImportProcessor.showFileChooser(project, descriptor);

    chooser.doWhenDone(pair -> {
      ModuleImportContext context = pair.getFirst();

      ModuleImportProvider<ModuleImportContext> provider = pair.getSecond();

      ModifiableModuleModel modifiableModel = ModuleManager.getInstance(project).getModifiableModel();
      provider.process(context, project, modifiableModel, module -> {
      });
      WriteAction.runAndWait(modifiableModel::commit);
    });
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    Presentation presentation = e.getPresentation();

    if (e.getProject() == null) {
      presentation.setEnabledAndVisible(false);
      return;
    }

    presentation.setEnabledAndVisible(!ModuleImportProviders.getExtensions(true).isEmpty());
  }

  @Override
  public boolean isDumbAware() {
    return true;
  }
}
