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

import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.util.ArrayUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.LangDataKeys;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;

public class MoveModulesToSubGroupAction extends MoveModulesToGroupAction {
    public MoveModulesToSubGroupAction(ModuleGroup moduleGroup) {
        super(
            moduleGroup,
            moduleGroup == null
                ? IdeLocalize.actionMoveModuleNewTopLevelGroup()
                : IdeLocalize.actionMoveModuleToNewSubGroup(),
            IdeLocalize.actionDescriptionCreateNewModuleGroup()
        );
    }

    @Override
    @RequiredUIAccess
    public void update(AnActionEvent e) {
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        Module[] modules = dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
        String[] newGroup;
        if (myModuleGroup != null) {
            LocalizeValue message =
                IdeLocalize.promptSpecifyNameOfModuleSubgroup(myModuleGroup.presentableText(), whatToMove(modules));
            String subgroup = Messages.showInputDialog(message.get(), IdeLocalize.titleModuleSubGroup().get(), UIUtil.getQuestionIcon());
            if (subgroup == null || "".equals(subgroup.trim())) {
                return;
            }
            newGroup = ArrayUtil.append(myModuleGroup.getGroupPath(), subgroup);
        }
        else {
            LocalizeValue message = IdeLocalize.promptSpecifyModuleGroupName(whatToMove(modules));
            String group = Messages.showInputDialog(message.get(), IdeLocalize.titleModuleGroup().get(), UIUtil.getQuestionIcon());
            if (group == null || "".equals(group.trim())) {
                return;
            }
            newGroup = new String[]{group};
        }

        doMove(modules, new ModuleGroup(newGroup), dataContext);
    }
}
