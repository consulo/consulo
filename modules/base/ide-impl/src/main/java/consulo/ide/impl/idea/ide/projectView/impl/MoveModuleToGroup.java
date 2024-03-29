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

/**
 * @author cdr
 */
package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.projectView.actions.MoveModulesToGroupAction;
import consulo.ide.impl.idea.ide.projectView.actions.MoveModulesToSubGroupAction;
import consulo.dataContext.DataContext;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.LangDataKeys;
import consulo.module.Module;
import consulo.project.Project;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.ui.ex.action.*;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MoveModuleToGroup extends ActionGroup {
  private final ModuleGroup myModuleGroup;

  public MoveModuleToGroup(ModuleGroup moduleGroup) {
    myModuleGroup = moduleGroup;
    setPopup(true);
  }

  @Override
  public void update(AnActionEvent e){
    final DataContext dataContext = e.getDataContext();
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    final Module[] modules = dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
    boolean active = project != null && modules != null && modules.length != 0;
    final Presentation presentation = e.getPresentation();
    presentation.setVisible(active);
    presentation.setText(myModuleGroup.presentableText());
  }

  @Override
  @Nonnull
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    if (e == null) return EMPTY_ARRAY;

    List<ModuleGroup> children = new ArrayList<ModuleGroup>(myModuleGroup.childGroups(e.getDataContext()));
    Collections.sort ( children, new Comparator<ModuleGroup>() {
      @Override
      public int compare(final ModuleGroup moduleGroup1, final ModuleGroup moduleGroup2) {
        assert moduleGroup1.getGroupPath().length == moduleGroup2.getGroupPath().length;
        return moduleGroup1.toString().compareToIgnoreCase(moduleGroup2.toString());
      }
    });

    List<AnAction> result = new ArrayList<AnAction>();
    result.add(new MoveModulesToGroupAction(myModuleGroup, IdeBundle.message("action.move.module.to.this.group")));
    result.add(new MoveModulesToSubGroupAction(myModuleGroup));
     result.add(AnSeparator.getInstance());
    for (final ModuleGroup child : children) {
      result.add(new MoveModuleToGroup(child));
    }

    return result.toArray(new AnAction[result.size()]);
  }
}
