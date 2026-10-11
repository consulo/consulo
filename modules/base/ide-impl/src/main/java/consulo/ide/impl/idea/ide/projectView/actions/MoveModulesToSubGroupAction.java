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
package consulo.ide.impl.idea.ide.projectView.actions;

import consulo.dataContext.DataContext;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.LangDataKeys;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.ui.Component;
import consulo.ui.InputBoxBuilder;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ArrayUtil;

/**
 * @author cdr
 */
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
    public void update(AnActionEvent e) {
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Module[] modules = e.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
        if (modules == null || modules.length == 0) {
            return;
        }

        ModuleGroup parentGroup = myModuleGroup;
        LocalizeValue message = parentGroup != null
            ? IdeLocalize.promptSpecifyNameOfModuleSubgroup(parentGroup.presentableText(), whatToMove(modules))
            : IdeLocalize.promptSpecifyModuleGroupName(whatToMove(modules));
        LocalizeValue title = parentGroup != null ? IdeLocalize.titleModuleSubGroup() : IdeLocalize.titleModuleGroup();

        DataContext dataContext = e.getDataContext();
        UIAccess uiAccess = UIAccess.current();
        InputBoxBuilder.text()
            .title(title)
            .text(message)
            .asQuestion()
            .showAsync(e.getData(Component.KEY))
            .whenCompleteAsync((name, error) -> {
                if (error != null || name == null || name.isBlank()) {
                    return;
                }

                String[] newGroup = parentGroup != null
                    ? ArrayUtil.append(parentGroup.getGroupPath(), name)
                    : new String[]{name};
                doMove(modules, new ModuleGroup(newGroup), dataContext);
            }, uiAccess);
    }
}
