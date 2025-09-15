/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen;

import consulo.annotation.component.ActionImpl;
import consulo.project.ProjectGroup;
import consulo.ide.impl.idea.ide.PopupProjectGroupActionGroup;
import consulo.project.internal.RecentProjectsManager;
import consulo.project.ui.localize.ProjectUILocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.InputValidatorEx;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.ScrollingUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
@ActionImpl(id = "WelcomeScreen.EditGroup")
public class EditProjectGroupAction extends RecentProjectsWelcomeScreenActionBase {
    public EditProjectGroupAction() {
        super(ProjectUILocalize.actionRecentProjectsEditGroupText());
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final ProjectGroup group = ((PopupProjectGroupActionGroup) getSelectedElements(e).get(0)).getGroup();
        JList list = getList(e);
        assert list != null;
        DefaultListModel model = getDataModel(e);
        String name = Messages.showInputDialog(
            list,
            ProjectUILocalize.labelRecentProjectsEnterGroupName().get(),
            ProjectUILocalize.dialogTitleRecentProjectsChangeGroupName().get(),
            null,
            group.getName(),
            new InputValidatorEx() {
                @Nullable
                @Override
                @RequiredUIAccess
                public String getErrorText(String inputString) {
                    inputString = inputString.trim();
                    if (inputString.length() == 0) {
                        return ProjectUILocalize.errorRecentProjectsNameCannotBeEmpty().get();
                    }
                    if (!checkInput(inputString)) {
                        return ProjectUILocalize.errorRecentProjectsGroupAlreadyExists(inputString).get();
                    }
                    return null;
                }

                @Override
                @RequiredUIAccess
                public boolean checkInput(String inputString) {
                    inputString = inputString.trim();
                    if (inputString.equals(group.getName())) {
                        return true;
                    }
                    for (ProjectGroup projectGroup : RecentProjectsManager.getInstance().getGroups()) {
                        if (projectGroup.getName().equals(inputString)) {
                            return false;
                        }
                    }
                    return true;
                }

                @Override
                @RequiredUIAccess
                public boolean canClose(String inputString) {
                    return true;
                }
            }
        );
        if (name != null && model != null) {
            group.setName(name);
            rebuildRecentProjectDataModel(model);
            for (int i = 0; i < model.getSize(); i++) {
                if (model.get(i) instanceof PopupProjectGroupActionGroup popupGroup && popupGroup.getGroup().equals(group)) {
                    ScrollingUtil.selectItem(list, i);
                    break;
                }
            }
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        List<AnAction> selected = getSelectedElements(e);
        boolean enabled = !selected.isEmpty()
            && selected.get(0) instanceof PopupProjectGroupActionGroup popupGroup
            && !popupGroup.getGroup().isTutorials();
        e.getPresentation().setEnabledAndVisible(enabled);
    }
}
