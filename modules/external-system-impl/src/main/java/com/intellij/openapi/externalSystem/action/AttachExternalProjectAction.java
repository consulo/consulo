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
package com.intellij.openapi.externalSystem.action;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.ImportModuleAction;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.projectImport.ProjectImportProvider;
import consulo.annotations.RequiredDispatchThread;
import consulo.externalSystem.service.module.wizard.AbstractExternalModuleImportProvider;
import consulo.moduleImport.ModuleImportProvider;
import consulo.moduleImport.ModuleImportProviders;
import consulo.moduleImport.LegacyModuleImportProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author Denis Zhdanov
 * @since 6/14/13 1:28 PM
 */
public class AttachExternalProjectAction extends AnAction implements DumbAware {

  public AttachExternalProjectAction() {
    getTemplatePresentation().setText(ExternalSystemBundle.message("action.attach.external.project.text", "external"));
    getTemplatePresentation().setDescription(ExternalSystemBundle.message("action.attach.external.project.description", "external"));
  }

  @RequiredDispatchThread
  @Override
  public void update(@NotNull AnActionEvent e) {
    ProjectSystemId externalSystemId = ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID.getData(e.getDataContext());
    if (externalSystemId != null) {
      String name = externalSystemId.getReadableName();
      e.getPresentation().setText(ExternalSystemBundle.message("action.attach.external.project.text", name));
      e.getPresentation().setDescription(ExternalSystemBundle.message("action.attach.external.project.description", name));
    }

    e.getPresentation().setIcon(SystemInfoRt.isMac ? AllIcons.ToolbarDecorator.Mac.Add : AllIcons.ToolbarDecorator.Add);
  }

  @RequiredDispatchThread
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ProjectSystemId externalSystemId = ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID.getData(e.getDataContext());
    if (externalSystemId == null) {
      return;
    }

    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    if (manager == null) {
      return;
    }

    Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
    if (project == null) {
      return;
    }

    Ref<ModuleImportProvider<?>> providerRef = Ref.create();
    for (ModuleImportProvider<?> provider : ModuleImportProviders.getExtensions()) {
      if (provider instanceof AbstractExternalModuleImportProvider &&
          externalSystemId.equals(((AbstractExternalModuleImportProvider)provider).getExternalSystemId())) {
        providerRef.set(provider);
        break;
      }
      else if (provider instanceof LegacyModuleImportProvider) {
        ProjectImportProvider projectImportProvider = ((LegacyModuleImportProvider)provider).getProvider();
        if (projectImportProvider instanceof AbstractExternalProjectImportProvider &&
            externalSystemId.equals(((AbstractExternalProjectImportProvider)projectImportProvider).getExternalSystemId())) {

          providerRef.set(provider);
          break;
        }
      }
    }
    if (providerRef.get() == null) {
      return;
    }

    AddModuleWizard wizard =
            ImportModuleAction.selectFileAndCreateWizard(project, null, manager.getExternalProjectDescriptor(), Arrays.asList(providerRef.get()));
    if (wizard != null && (wizard.getStepCount() <= 0 || wizard.showAndGet())) {
      ImportModuleAction.createFromWizard(project, wizard);
    }
  }
}
