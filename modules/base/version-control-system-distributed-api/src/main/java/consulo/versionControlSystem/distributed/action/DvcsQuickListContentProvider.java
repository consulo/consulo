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
package consulo.versionControlSystem.distributed.action;

import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.action.VcsQuickListContentProvider;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnSeparator;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class DvcsQuickListContentProvider implements VcsQuickListContentProvider {
    @Nullable
    @Override
    public List<AnAction> getVcsActions(
        @Nullable Project project,
        @Nullable AbstractVcs activeVcs,
        @Nullable DataContext dataContext
    ) {
        if (activeVcs == null || !getVcsName().equals(activeVcs.getId())) {
            return null;
        }

        ActionManager manager = ActionManager.getInstance();
        List<AnAction> actions = new ArrayList<>();

        actions.add(AnSeparator.create(activeVcs.getDisplayName()));
        add("CheckinProject", manager, actions);
        add("CheckinFiles", manager, actions);
        add(IdeActions.CHANGES_VIEW_REVERT, manager, actions);

        addSeparator(actions);
        add("Vcs.ShowTabbedFileHistory", manager, actions);
        add("Annotate", manager, actions);
        add("Compare.SameVersion", manager, actions);

        addSeparator(actions);
        addVcsSpecificActions(manager, actions);
        return actions;
    }

    @Nonnull
    protected abstract String getVcsName();

    protected abstract void addVcsSpecificActions(@Nonnull ActionManager manager, @Nonnull List<AnAction> actions);

    @Override
    public boolean replaceVcsActionsFor(@Nonnull AbstractVcs activeVcs, @Nullable DataContext dataContext) {
        return getVcsName().equals(activeVcs.getId());
    }

    protected static void addSeparator(@Nonnull List<AnAction> actions) {
        actions.add(AnSeparator.create());
    }

    protected static void add(String actionName, ActionManager manager, List<AnAction> actions) {
        AnAction action = manager.getAction(actionName);
        assert action != null : "Can not find action " + actionName;
        actions.add(action);
    }
}
