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
import consulo.continuousIntegration.CIBuildMonitor;
import consulo.continuousIntegration.CICapability;
import consulo.continuousIntegration.CIDataKeys;
import consulo.continuousIntegration.CIJob;
import consulo.continuousIntegration.CIJobKind;
import consulo.continuousIntegration.CIParameterDefinition;
import consulo.continuousIntegration.CIRunRequest;
import consulo.continuousIntegration.CIServer;
import consulo.continuousIntegration.impl.internal.CIUtil;
import consulo.continuousIntegration.impl.internal.run.CIRunBuildDialogDescriptor;
import consulo.continuousIntegration.impl.localize.ContinuousIntegrationImplLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Starts the selected job: asks for parameters (and the branch, when the server builds any ref) when there are any,
 * then follows the build in the Run tool window.
 *
 * @since 2026-11-09
 */
@ActionImpl(id = "ContinuousIntegration.Run")
public class CIRunAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    private record Parameters(List<CIParameterDefinition> definitions, @Nullable String error) {
    }

    public CIRunAction() {
        super(
            ContinuousIntegrationImplLocalize.actionRunText(),
            ContinuousIntegrationImplLocalize.actionRunDescription(),
            PlatformIconGroup.actionsExecute()
        );
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        CIServer server = e.getData(CIDataKeys.SERVER);
        CIJob job = e.getData(CIDataKeys.JOB);
        if (project == null || server == null || job == null || !canRun(server, job)) {
            return;
        }
        Component component = e.getData(Component.KEY);

        CoroutineScope.launchAsync(project.coroutineContext(), () -> Coroutine
            .first(CompletableFutureStep.<Object, Parameters>await(ignored -> loadParameters(server, job)))
            .then(UIAction.<Parameters, CompletableFuture<Optional<CIRunRequest>>>apply(
                parameters -> askRequest(project, server, job, parameters, component)
            ))
            .then(CompletableFutureStep.<CompletableFuture<Optional<CIRunRequest>>, Optional<CIRunRequest>>await(request -> request))
            .then(UIAction.<Optional<CIRunRequest>, Boolean>apply(request -> {
                request.ifPresent(value -> CIBuildMonitor.getInstance(project).runBuild(server, job, value));
                return request.isPresent();
            }))
        );
    }

    @Override
    public void update(AnActionEvent e) {
        CIServer server = e.getData(CIDataKeys.SERVER);
        CIJob job = e.getData(CIDataKeys.JOB);
        e.getPresentation().setEnabled(e.getData(Project.KEY) != null && server != null && job != null && canRun(server, job));
    }

    private static boolean canRun(CIServer server, CIJob job) {
        return job.kind() == CIJobKind.JOB && job.buildable() && server.hasCapability(CICapability.RUN);
    }

    /**
     * Never fails: an error is part of the result, so the next step can show it.
     */
    private static CompletableFuture<Parameters> loadParameters(CIServer server, CIJob job) {
        CompletableFuture<List<CIParameterDefinition>> future;
        try {
            future = server.loadParameters(job);
        }
        catch (RuntimeException e) {
            future = CompletableFuture.failedFuture(e);
        }
        return future.handle((definitions, error) -> {
            if (error != null || definitions == null) {
                return new Parameters(List.of(), error == null ? "" : CIUtil.errorMessage(error));
            }
            return new Parameters(List.copyOf(definitions), null);
        });
    }

    @RequiredUIAccess
    private static CompletableFuture<Optional<CIRunRequest>> askRequest(
        Project project,
        CIServer server,
        CIJob job,
        Parameters parameters,
        @Nullable Component component
    ) {
        String error = parameters.error();
        if (error != null) {
            MessageBoxes.okError(ContinuousIntegrationImplLocalize.runLoadParametersFailed(job.displayName(), error)).showAsync(component);
            return CompletableFuture.completedFuture(Optional.empty());
        }
        if (parameters.definitions().isEmpty() && !server.hasCapability(CICapability.RUN_WITH_REF)) {
            return CompletableFuture.completedFuture(Optional.of(CIRunRequest.DEFAULT));
        }
        return CIRunBuildDialogDescriptor.showAsync(project, server, job, parameters.definitions());
    }
}
