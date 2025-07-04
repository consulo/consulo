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
package consulo.ide.impl.idea.vcs.log.ui.actions;

import consulo.ide.impl.idea.vcs.log.data.VcsLogFilterer;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogManager;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogInternalDataKeys;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogUiImpl;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.RefreshAction;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import jakarta.annotation.Nonnull;

public class RefreshLogAction extends RefreshAction {
    private static final Logger LOG = Logger.getInstance(RefreshLogAction.class);

    public RefreshLogAction() {
        super(
            LocalizeValue.localizeTODO("Refresh"),
            LocalizeValue.localizeTODO("Check for new commits and refresh Log if necessary"),
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
        if (ui instanceof VcsLogUiImpl) {
            VcsLogFilterer filterer = ((VcsLogUiImpl) ui).getFilterer();
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
        VcsLogManager logManager = e.getData(VcsLogInternalDataKeys.LOG_MANAGER);
        e.getPresentation().setEnabledAndVisible(logManager != null && e.getData(VcsLogUi.KEY) != null);
    }
}
