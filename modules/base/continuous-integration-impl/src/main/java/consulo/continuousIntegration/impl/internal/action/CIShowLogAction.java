/*
 * Copyright 2013-2026 consulo.io
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
package consulo.continuousIntegration.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.continuousIntegration.CIBuild;
import consulo.continuousIntegration.CIBuildMonitor;
import consulo.continuousIntegration.CICapability;
import consulo.continuousIntegration.CIDataKeys;
import consulo.continuousIntegration.CIJob;
import consulo.continuousIntegration.CIServer;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import org.jspecify.annotations.Nullable;

/**
 * Shows the selected build, or the last build of the selected job, in the Run tool window.
 *
 * @since 2026-11-09
 */
@ActionImpl(id = "ContinuousIntegration.ShowLog")
public class CIShowLogAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public CIShowLogAction() {
        super(
            ContinuousIntegrationImplLocalize.actionShowLogText(),
            ContinuousIntegrationImplLocalize.actionShowLogDescription(),
            PlatformIconGroup.filetypesText()
        );
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        CIServer server = e.getData(CIDataKeys.SERVER);
        CIJob job = e.getData(CIDataKeys.JOB);
        CIBuild build = getBuild(e);
        if (project != null && server != null && job != null && build != null) {
            CIBuildMonitor.getInstance(project).showBuild(server, job, build);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        CIServer server = e.getData(CIDataKeys.SERVER);
        e.getPresentation().setEnabled(server != null
            && server.hasCapability(CICapability.LOG)
            && e.getData(CIDataKeys.JOB) != null
            && getBuild(e) != null);
    }

    private static @Nullable CIBuild getBuild(AnActionEvent e) {
        CIBuild build = e.getData(CIDataKeys.BUILD);
        if (build != null) {
            return build;
        }
        CIJob job = e.getData(CIDataKeys.JOB);
        return job == null ? null : job.lastBuild();
    }
}
