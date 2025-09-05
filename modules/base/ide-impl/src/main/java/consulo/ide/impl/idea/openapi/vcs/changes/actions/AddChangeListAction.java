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
package consulo.ide.impl.idea.openapi.vcs.changes.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.impl.internal.change.ChangeListManagerImpl;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.ide.impl.idea.openapi.vcs.changes.ui.NewChangelistDialog;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 * @since 2006-11-02
 */
@ActionImpl(id = "ChangesView.NewChangeList")
public class AddChangeListAction extends AnAction implements DumbAware {
    public AddChangeListAction() {
        super(
            ActionLocalize.actionChangesviewNewchangelistText(),
            ActionLocalize.actionChangesviewNewchangelistDescription(),
            PlatformIconGroup.generalAdd()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        NewChangelistDialog dlg = new NewChangelistDialog(project);
        dlg.show();
        if (dlg.getExitCode() == DialogWrapper.OK_EXIT_CODE) {
            String name = dlg.getName();
            if (name.length() == 0) {
                name = getUniqueName(project);
            }

            LocalChangeList list = ChangeListManager.getInstance(project).addChangeList(name, dlg.getDescription());
            if (dlg.isNewChangelistActive()) {
                ChangeListManager.getInstance(project).setDefaultChangeList(list);
            }
            dlg.getPanel().changelistCreatedOrChanged(list);
        }
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    private static String getUniqueName(Project project) {
        int unnamedcount = 0;
        for (ChangeList list : ChangeListManagerImpl.getInstanceImpl(project).getChangeListsCopy()) {
            if (list.getName().startsWith("Unnamed")) {
                unnamedcount++;
            }
        }

        return unnamedcount == 0 ? "Unnamed" : "Unnamed (" + unnamedcount + ")";
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        if (e.getPlace().equals(ActionPlaces.CHANGES_VIEW_POPUP)) {
            ChangeList[] lists = e.getData(VcsDataKeys.CHANGE_LISTS);
            e.getPresentation().setVisible(lists != null && lists.length > 0);
        }
    }
}