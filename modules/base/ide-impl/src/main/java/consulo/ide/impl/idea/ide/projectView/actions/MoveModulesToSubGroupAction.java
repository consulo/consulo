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
package consulo.ide.impl.idea.ide.projectView.actions;

import consulo.ide.IdeBundle;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.language.editor.LangDataKeys;
import consulo.ui.ex.action.Presentation;
import consulo.module.Module;
import consulo.ui.ex.awt.Messages;
import consulo.ide.impl.idea.util.ArrayUtil;

public class MoveModulesToSubGroupAction extends MoveModulesToGroupAction {
  public MoveModulesToSubGroupAction(ModuleGroup moduleGroup) {
    super(moduleGroup, moduleGroup == null ? IdeBundle.message("action.move.module.new.top.level.group") : IdeBundle.message("action.move.module.to.new.sub.group"));
  }

  @Override
  public void update(AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    String description = IdeBundle.message("action.description.create.new.module.group");
    presentation.setDescription(description);
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Module[] modules = dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
    final String[] newGroup;
    if (myModuleGroup != null) {
      String message = IdeBundle.message("prompt.specify.name.of.module.subgroup", myModuleGroup.presentableText(), whatToMove(modules));
      String subgroup = Messages.showInputDialog(message, IdeBundle.message("title.module.sub.group"), Messages.getQuestionIcon());
      if (subgroup == null || "".equals(subgroup.trim())) return;
      newGroup = ArrayUtil.append(myModuleGroup.getGroupPath(), subgroup);
    }
    else {
      String message = IdeBundle.message("prompt.specify.module.group.name", whatToMove(modules));
      String group = Messages.showInputDialog(message, IdeBundle.message("title.module.group"), Messages.getQuestionIcon());
      if (group == null || "".equals(group.trim())) return;
      newGroup = new String[]{group};
    }

    doMove(modules, new ModuleGroup(newGroup), dataContext);
  }
}
