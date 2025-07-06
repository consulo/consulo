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
package consulo.externalSystem.impl.internal.service.action;

import consulo.application.dumb.DumbAware;
import consulo.externalSystem.impl.internal.util.ExternalSystemUtil;
import consulo.externalSystem.internal.ui.ExternalSystemRecentTasksList;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.model.ExternalSystemDataKeys;
import consulo.externalSystem.ui.awt.ExternalSystemTasksTreeModel;
import consulo.externalSystem.util.ExternalSystemApiUtil;
import consulo.externalSystem.util.ExternalSystemConstants;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Denis Zhdanov
 * @since 2013-06-13
 */
public class DetachExternalProjectAction extends AnAction implements DumbAware {

  public DetachExternalProjectAction() {
    super(
        ExternalSystemLocalize.actionDetachExternalProjectText("external"),
        ExternalSystemLocalize.actionDetachExternalProjectDescription(),
        PlatformIconGroup.generalRemove()
    );
  }

  @Override
  public void update(@Nonnull AnActionEvent e) {
    ExternalActionUtil.MyInfo info = ExternalActionUtil.getProcessingInfo(e.getDataContext());
    e.getPresentation().setEnabled(info.externalProject != null);
  }

  @Override
  @RequiredUIAccess
  public void actionPerformed(@Nonnull AnActionEvent e) {
    ExternalActionUtil.MyInfo info = ExternalActionUtil.getProcessingInfo(e.getDataContext());
    if (info.settings == null || info.localSettings == null || info.externalProject == null || info.ideProject == null
        || info.externalSystemId == null)
    {
      return;
    }
    
    e.getPresentation().setTextValue(
        ExternalSystemLocalize.actionDetachExternalProjectText(info.externalSystemId.getReadableName())
    );

    ExternalSystemTasksTreeModel allTasksModel = e.getDataContext().getData(ExternalSystemDataKeys.ALL_TASKS_MODEL);
    if (allTasksModel != null) {
      allTasksModel.pruneNodes(info.externalProject);
    }

    ExternalSystemRecentTasksList recentTasksList = e.getDataContext().getData(ExternalSystemDataKeys.RECENT_TASKS_LIST);
    if (recentTasksList != null) {
      recentTasksList.getModel().forgetTasksFrom(info.externalProject.getPath());
    }
    
    info.localSettings.forgetExternalProjects(Collections.singleton(info.externalProject.getPath()));
    info.settings.unlinkExternalProject(info.externalProject.getPath());

    // Process orphan modules.
    String externalSystemIdAsString = info.externalSystemId.toString();
    List<Module> orphanModules = new ArrayList<>();
    for (Module module : ModuleManager.getInstance(info.ideProject).getModules()) {
      String systemId = ExternalSystemApiUtil.getExtensionSystemOption(module, ExternalSystemConstants.EXTERNAL_SYSTEM_ID_KEY);
      if (!externalSystemIdAsString.equals(systemId)) {
        continue;
      }
      String path = ExternalSystemApiUtil.getExtensionSystemOption(module, ExternalSystemConstants.LINKED_PROJECT_PATH_KEY);
      if (info.externalProject.getPath().equals(path)) {
        orphanModules.add(module);
      }
    }

    if (!orphanModules.isEmpty()) {
      ExternalSystemUtil.ruleOrphanModules(orphanModules, info.ideProject, info.externalSystemId);
    }
  }
}
