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
package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.ide.impl.idea.ide.projectView.actions.MoveModulesOutsideGroupAction;
import consulo.ide.impl.idea.ide.projectView.actions.MoveModulesToSubGroupAction;
import consulo.dataContext.DataContext;
import consulo.language.editor.LangDataKeys;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

/**
 * @author cdr
 */
public class MoveModuleToGroupTopLevel extends ActionGroup {
  @Override
  @RequiredUIAccess
  public void update(AnActionEvent e){
    final DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(Project.KEY);
    final Module[] modules = dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
    boolean active = project != null && modules != null && modules.length != 0;
    e.getPresentation().setVisible(active);
  }

  @Override
  @Nonnull
  @RequiredReadAction
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    if (e == null) {
      return EMPTY_ARRAY;
    }
    List<String> topLevelGroupNames = new ArrayList<>(getTopLevelGroupNames(e.getDataContext()));
    Collections.sort ( topLevelGroupNames );

    List<AnAction> result = new ArrayList<>();
    result.add(new MoveModulesOutsideGroupAction());
    result.add(new MoveModulesToSubGroupAction(null));
    result.add(AnSeparator.getInstance());
    for (String name : topLevelGroupNames) {
      result.add(new MoveModuleToGroup(new ModuleGroup(new String[]{name})));
    }
    return result.toArray(new AnAction[result.size()]);
  }

  @RequiredReadAction
  private static Collection<String> getTopLevelGroupNames(final DataContext dataContext) {
    final Project project = dataContext.getData(Project.KEY);

    final ModifiableModuleModel model = dataContext.getData(LangDataKeys.MODIFIABLE_MODULE_MODEL);

    Module[] allModules;
    if ( model != null ) {
      allModules = model.getModules();
    } else {
      allModules = ModuleManager.getInstance(project).getModules();
    }

    Set<String> topLevelGroupNames = new HashSet<>();
    for (final Module child : allModules) {
      String[] group;
      if ( model != null ) {
        group = model.getModuleGroupPath(child);
      } else {
        group = ModuleManager.getInstance(project).getModuleGroupPath(child);
      }
      if (group != null) {
        topLevelGroupNames.add(group[0]);
      }
    }
    return topLevelGroupNames;
  }
}
