/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.log.impl.internal.ui.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.RefreshAction;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.impl.internal.VcsLogManager;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogFilterer;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogInternalDataKeys;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogUiImpl;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "Vcs.Log.Refresh", shortcutFrom = @ActionRef(id = IdeActions.ACTION_REFRESH))
public class RefreshLogAction extends RefreshAction {
    private static final Logger LOG = Logger.getInstance(RefreshLogAction.class);

    public RefreshLogAction() {
        super(
            VersionControlSystemLogLocalize.actionRefreshLogText(),
            VersionControlSystemLogLocalize.actionRefreshLogDescription(),
            PlatformIconGroup.actionsRefresh()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        VcsLogUtil.triggerUsage(e);

        VcsLogManager logManager = e.getRequiredData(VcsLogInternalDataKeys.LOG_MANAGER);

        // diagnostic for possible refresh problems
        VcsLogUi ui = e.getRequiredData(VcsLogUi.KEY);
        if (ui instanceof VcsLogUiImpl vcsLogUi) {
            VcsLogFilterer filterer = vcsLogUi.getFilterer();
            if (!filterer.isValid()) {
                String message = "Trying to refresh invalid log tab.";
                if (!logManager.getDataManager().getProgress().isRunning()) {
                    LOG.error(message);
                }
                else {
                    LOG.warn(message);
                }
                filterer.setValid(true);
            }
        }

        logManager.getDataManager().refreshSoftly();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.hasData(VcsLogInternalDataKeys.LOG_MANAGER) && e.hasData(VcsLogUi.KEY));
    }
}
