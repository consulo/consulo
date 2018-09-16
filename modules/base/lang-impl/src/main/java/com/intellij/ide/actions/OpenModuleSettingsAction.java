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

import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleNavigatable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;

import javax.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class OpenModuleSettingsAction extends EditSourceAction {
  @Override
  public void update(AnActionEvent event) {
    super.update(event);
    if (!isModuleInProjectViewPopup(event)) {
      event.getPresentation().setEnabledAndVisible(false);
    }
  }

  @Nullable
  @Override
  protected Navigatable[] getNavigatables(DataContext dataContext) {
    Module data = dataContext.getData(LangDataKeys.MODULE);
    if(data == null) {
      return NavigationItem.EMPTY_ARRAY;
    }
    return new Navigatable[]{new ModuleNavigatable(data)};
  }

  protected static boolean isModuleInProjectViewPopup(AnActionEvent e) {
    if (ActionPlaces.PROJECT_VIEW_POPUP.equals(e.getPlace())) {
      final Project project = getEventProject(e);
      final Module module = e.getDataContext().getData(LangDataKeys.MODULE);
      if (project != null && module != null) {
        final VirtualFile moduleFolder = e.getDataContext().getData(PlatformDataKeys.VIRTUAL_FILE);
        if (moduleFolder == null) {
          return false;
        }
        if (ProjectRootsUtil.isModuleContentRoot(moduleFolder, project) || ProjectRootsUtil.isSourceOrTestRoot(moduleFolder, project)) {
          return true;
        }
      }
    }
    return false;
  }
}
