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
package consulo.execution.profiler.impl.internal.view;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.component.ProcessCanceledException;
import consulo.execution.profiler.BaseCallStackElement;
import consulo.language.psi.NavigatablePsiElement;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;

import java.util.Optional;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerNavigator {
    private static final Logger LOG = Logger.getInstance(ProfilerNavigator.class);

    private final Project myProject;

    public ProfilerNavigator(Project project) {
        myProject = project;
    }

    public Project getProject() {
        return myProject;
    }

    @RequiredUIAccess
    public void navigate(BaseCallStackElement element) {
        if (!element.isNavigatable() || myProject.isDisposed()) {
            return;
        }

        CoroutineScope.launchAsync(
            myProject.coroutineContext(),
            () -> Coroutine
                .first(CodeExecution.<Void, NavigatablePsiElement[]>apply(ignored -> calcNavigatables(element)))
                .then(ReadLock.<NavigatablePsiElement[], Optional<NavigatablePsiElement>>apply(this::findTarget))
                .then(UIAction.<Optional<NavigatablePsiElement>, Optional<NavigatablePsiElement>>apply(target -> {
                    if (!myProject.isDisposed()) {
                        target.ifPresent(it -> it.navigate(true));
                    }
                    return target;
                }))
        );
    }

    private NavigatablePsiElement[] calcNavigatables(BaseCallStackElement element) {
        if (myProject.isDisposed()) {
            return NavigatablePsiElement.EMPTY_ARRAY;
        }

        try {
            return element.calcNavigatables(myProject);
        }
        catch (ProcessCanceledException e) {
            return NavigatablePsiElement.EMPTY_ARRAY;
        }
        catch (Throwable e) {
            LOG.error("Failed to find the source of profiler frame " + element.fullName(), e);
            return NavigatablePsiElement.EMPTY_ARRAY;
        }
    }

    @RequiredReadAction
    private Optional<NavigatablePsiElement> findTarget(NavigatablePsiElement[] targets) {
        if (myProject.isDisposed()) {
            return Optional.empty();
        }

        for (NavigatablePsiElement target : targets) {
            if (target.isValid() && target.canNavigate()) {
                return Optional.of(target);
            }
        }
        return Optional.empty();
    }
}
