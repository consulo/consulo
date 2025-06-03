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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.dumb.DumbAware;
import consulo.platform.Platform;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.internal.RecentProjectsManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class RecentProjectsGroup extends ActionGroup implements DumbAware {
    private final Provider<RecentProjectsManager> myRecentProjectsManager;

    @Inject
    public RecentProjectsGroup(Provider<RecentProjectsManager> recentProjectsManager) {
        myRecentProjectsManager = recentProjectsManager;

        Presentation templatePresentation = getTemplatePresentation();
        // Let's make title more macish
        templatePresentation.setTextValue(
            Platform.current().os().isMac() ? ActionLocalize.groupReopenMacText() : ActionLocalize.groupReopenWinText()
        );
    }

    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        return myRecentProjectsManager.get().getRecentProjectsActions(
            RecentProjectsManager.RECENT_ACTIONS_LIMIT_ACTION_ITEMS | RecentProjectsManager.RECENT_ACTIONS_USE_GROUPS_CONTEXT_MENU
        );
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(getChildren(event).length > 0);
    }
}
