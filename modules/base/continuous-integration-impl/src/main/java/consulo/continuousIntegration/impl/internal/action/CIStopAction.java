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
import consulo.continuousIntegration.impl.internal.CIUtil;
import consulo.continuousIntegration.impl.internal.monitor.CIBuildMonitorImpl;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import org.jspecify.annotations.Nullable;

/**
 * Stops the selected running or queued build, or the running last build of the selected job.
 *
 * @since 2026-11-09
 */
@ActionImpl(id = "ContinuousIntegration.Stop")
public class CIStopAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public CIStopAction() {
        super(
            ContinuousIntegrationImplLocalize.actionStopText(),
            ContinuousIntegrationImplLocalize.actionStopDescription(),
            PlatformIconGroup.actionsSuspend()
        );
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        CIServer server = e.getData(CIDataKeys.SERVER);
        CIJob job = e.getData(CIDataKeys.JOB);
        CIBuild build = getBuild(e);
        if (project == null || server == null || job == null || build == null) {
            return;
        }

        UIAccess uiAccess = UIAccess.current();
        Component component = e.getData(Component.KEY);
        CIBuildMonitorImpl monitor = (CIBuildMonitorImpl) CIBuildMonitor.getInstance(project);
        monitor.stopBuild(server, job, build).whenCompleteAsync((result, error) -> {
            if (error != null) {
                LocalizeValue message = ContinuousIntegrationImplLocalize.stopFailed(build.displayName(), CIUtil.errorMessage(error));
                MessageBoxes.okError(message).showAsync(component);
            }
        }, uiAccess);
    }

    @Override
    public void update(AnActionEvent e) {
        CIServer server = e.getData(CIDataKeys.SERVER);
        e.getPresentation().setEnabled(server != null
            && server.hasCapability(CICapability.STOP)
            && e.getData(CIDataKeys.JOB) != null
            && getBuild(e) != null);
    }

    private static @Nullable CIBuild getBuild(AnActionEvent e) {
        CIBuild build = e.getData(CIDataKeys.BUILD);
        if (build == null) {
            CIJob job = e.getData(CIDataKeys.JOB);
            build = job == null ? null : job.lastBuild();
        }
        return build != null && build.status().isActive() ? build : null;
    }
}
