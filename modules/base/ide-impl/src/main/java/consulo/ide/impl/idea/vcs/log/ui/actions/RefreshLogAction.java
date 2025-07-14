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
import consulo.ui.ex.action.configurable.ConfigurableAction;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import jakarta.annotation.Nonnull;

public class RefreshLogAction extends ConfigurableAction {
    private static final Logger LOG = Logger.getInstance(RefreshLogAction.class);

    private ValidData<VcsLogManager> myLogManager;
    private ValidData<VcsLogUi> myUi;

    @Override
    protected void init(@Nonnull Builder builder) {
        builder.text(LocalizeValue.localizeTODO("Refresh"))
            .description(LocalizeValue.localizeTODO("Check for new commits and refresh Log if necessary"))
            .icon(PlatformIconGroup.actionsRefresh());
        myLogManager = builder.invisibleAndDisabledIfAbsent(VcsLogInternalDataKeys.LOG_MANAGER);
        myUi = builder.invisibleAndDisabledIfAbsent(VcsLogUi.KEY);
    }

    @Override
    @RequiredUIAccess
    protected void performAction(@Nonnull AnActionEvent e) {
        VcsLogUtil.triggerUsage(e);

        // diagnostic for possible refresh problems
        if (myUi.get() instanceof VcsLogUiImpl vcsLogUi) {
            VcsLogFilterer filterer = vcsLogUi.getFilterer();
            if (!filterer.isValid()) {
                String message = "Trying to refresh invalid log tab.";
                if (!myLogManager.get().getDataManager().getProgress().isRunning()) {
                    LOG.error(message);
                }
                else {
                    LOG.warn(message);
                }
                filterer.setValid(true);
            }
        }

        myLogManager.get().getDataManager().refreshSoftly();
    }
}
