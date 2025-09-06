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
package consulo.versionControlSystem.impl.internal.change.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.impl.internal.change.ui.awt.EditChangelistDialog;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 * @since 2006-11-02
 */
@ActionImpl(id = "ChangesView.Rename")
public class RenameChangeListAction extends AnAction implements DumbAware {
    public RenameChangeListAction() {
        super(ActionLocalize.actionChangesviewRenameText(), ActionLocalize.actionChangesviewRenameDescription());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        ChangeList[] lists = e.getData(VcsDataKeys.CHANGE_LISTS);
        boolean visible =
            lists != null && lists.length == 1 && lists[0] instanceof LocalChangeList localChangeList && !localChangeList.isReadOnly();
        if (e.getPlace().equals(ActionPlaces.CHANGES_VIEW_POPUP)) {
            e.getPresentation().setVisible(visible);
        }
        else {
            e.getPresentation().setEnabled(visible);
        }
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        ChangeList[] lists = e.getRequiredData(VcsDataKeys.CHANGE_LISTS);
        LocalChangeList list = ChangeListManager.getInstance(project).findChangeList(lists[0].getName());
        if (list != null) {
            new EditChangelistDialog(project, list).show();
        }
    }
}