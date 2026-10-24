/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors.
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

import consulo.application.util.registry.Registry;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.ui.image.Image;
import consulo.versionControlSystem.log.VcsLogDataKeys;
import consulo.versionControlSystem.log.VcsLogFilterCollection;
import consulo.versionControlSystem.log.VcsLogUi;
import consulo.versionControlSystem.log.impl.internal.VcsLogFilterCollectionImpl;
import consulo.versionControlSystem.log.impl.internal.VcsLogManager;
import consulo.versionControlSystem.log.impl.internal.VcsLogTabLocation;
import consulo.versionControlSystem.log.impl.internal.VcsProjectLogImpl;
import consulo.versionControlSystem.log.impl.internal.ui.VcsLogInternalDataKeys;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import org.jspecify.annotations.Nullable;

public abstract class OpenAnotherLogTabAction extends LegacyDumbAwareAction {
    protected OpenAnotherLogTabAction(Image icon) {
        super(LocalizeValue.empty(), LocalizeValue.empty(), icon);
        getTemplatePresentation().setText(getText(VersionControlSystemLogLocalize.vcs()));
        getTemplatePresentation().setDescription(getDescription(VersionControlSystemLogLocalize.vcs()));
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        VcsLogManager logManager = getMainLogManager(e);
        if (logManager == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        LocalizeValue vcsName = VcsLogUtil.getVcsDisplayName(project, logManager.getDataManager().getLogProviders().values());
        e.getPresentation().setText(getText(vcsName));
        e.getPresentation().setDescription(getDescription(vcsName));
    }

    private static @Nullable VcsLogManager getMainLogManager(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return null;
        }
        VcsLogManager mainLogManager = VcsProjectLogImpl.getInstance(project).getLogManager();
        if (mainLogManager == null) {
            return null;
        }
        VcsLogManager dataLogManager = e.getData(VcsLogInternalDataKeys.LOG_MANAGER);
        if (dataLogManager != null && dataLogManager != mainLogManager) {
            return null;
        }
        return mainLogManager;
    }

    protected LocalizeValue getDescription(LocalizeValue vcsName) {
        return VersionControlSystemLogLocalize.actionOpenAnotherTabDescription(vcsName);
    }

    protected LocalizeValue getText(LocalizeValue vcsName) {
        return VersionControlSystemLogLocalize.actionOpenAnotherTabText(vcsName);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        VcsLogManager logManager = getMainLogManager(e);
        if (logManager == null) {
            return;
        }

        VcsLogFilterCollection filters;
        if (Registry.is("vcs.log.copy.filters.to.new.tab", true)) {
            filters = getFilters(project, e);
        }
        else {
            filters = VcsLogFilterCollectionImpl.EMPTY;
        }
        VcsProjectLogImpl.getInstance(project).openLogTab(filters, getLocation(e));
    }

    protected VcsLogFilterCollection getFilters(Project project, AnActionEvent e) {
        VcsLogUi logUi = e.getData(VcsLogDataKeys.VCS_LOG_UI);
        if (logUi == null) {
            return VcsLogFilterCollectionImpl.EMPTY;
        }
        return logUi.getFilterUi().getFilters();
    }

    protected abstract VcsLogTabLocation getLocation(AnActionEvent e);
}
