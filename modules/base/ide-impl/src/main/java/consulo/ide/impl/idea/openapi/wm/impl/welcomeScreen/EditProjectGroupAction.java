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

import consulo.project.ProjectGroup;
import consulo.ide.impl.idea.ide.PopupProjectGroupActionGroup;
import consulo.project.internal.RecentProjectsManager;
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
public class EditProjectGroupAction extends RecentProjectsWelcomeScreenActionBase {
    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        final ProjectGroup group = ((PopupProjectGroupActionGroup) getSelectedElements(e).get(0)).getGroup();
        JList list = getList(e);
        assert list != null;
        DefaultListModel model = getDataModel(e);
        String name = Messages.showInputDialog(
            list,
            "Enter group name: ",
            "Change Group Name",
            null,
            group.getName(),
            new InputValidatorEx() {
                @Nullable
                @Override
                @RequiredUIAccess
                public String getErrorText(String inputString) {
                    inputString = inputString.trim();
                    if (inputString.length() == 0) {
                        return "Name cannot be empty.";
                    }
                    if (!checkInput(inputString)) {
                        return "Group '" + inputString + "' already exists.";
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
                Object element = model.get(i);
                if (element instanceof PopupProjectGroupActionGroup) {
                    if (((PopupProjectGroupActionGroup) element).getGroup().equals(group)) {
                        ScrollingUtil.selectItem(list, i);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        final List<AnAction> selected = getSelectedElements(e);
        boolean enabled =
            !selected.isEmpty() && selected.get(0) instanceof PopupProjectGroupActionGroup && !((PopupProjectGroupActionGroup) selected.get(
                0)).getGroup().isTutorials();
        e.getPresentation().setEnabledAndVisible(enabled);
    }
}
