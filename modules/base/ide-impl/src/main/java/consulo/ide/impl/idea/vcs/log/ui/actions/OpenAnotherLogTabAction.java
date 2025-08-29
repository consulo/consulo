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
package consulo.ide.impl.idea.vcs.log.ui.actions;

import consulo.annotation.component.ActionImpl;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.application.util.registry.Registry;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogContentProvider;
import consulo.ide.impl.idea.vcs.log.impl.VcsLogManager;
import consulo.ide.impl.idea.vcs.log.impl.VcsProjectLog;
import consulo.ide.impl.idea.vcs.log.ui.VcsLogInternalDataKeys;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "Vcs.Log.OpenAnotherTab")
public class OpenAnotherLogTabAction extends DumbAwareAction {
    public OpenAnotherLogTabAction() {
        super(
            VersionControlSystemLogLocalize.actionOpenAnotherTabText(),
            VersionControlSystemLogLocalize.actionOpenAnotherTabDescription(),
            PlatformIconGroup.generalAdd()
        );
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null || !Registry.is("vcs.log.open.another.log.visible", false)) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }
        VcsProjectLog projectLog = project.getInstance(VcsProjectLog.class);
        VcsLogManager logManager = e.getData(VcsLogInternalDataKeys.LOG_MANAGER);
        // only for main log (it is a question, how and where we want to open tabs for external logs)
        e.getPresentation().setEnabledAndVisible(logManager != null && projectLog.getLogManager() == logManager);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        VcsLogContentProvider.openAnotherLogTab(e.getRequiredData(VcsLogInternalDataKeys.LOG_MANAGER), e.getRequiredData(Project.KEY));
    }
}
