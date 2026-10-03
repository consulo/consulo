// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.execution.impl.internal.action;

import consulo.application.dumb.DumbAware;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorGroup;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.util.concurrent.coroutine.Coroutine;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public class ExecutorGroupActionGroup extends ActionGroup implements DumbAware, AnActionWithAsyncUpdate {
    protected final ExecutorGroup<?> myExecutorGroup;
    private final Function<? super Executor, ? extends AnAction> myChildConverter;

    public ExecutorGroupActionGroup(ExecutorGroup<?> executorGroup, Function<? super Executor, ? extends AnAction> childConverter) {
        myExecutorGroup = executorGroup;
        myChildConverter = childConverter;
        Presentation presentation = getTemplatePresentation();
        presentation.setText(executorGroup.getStartActionText());
        presentation.setIcon(executorGroup.getIcon());
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        return getChildren();
    }

    public AnAction[] getChildren() {
        // RunExecutorSettings configurations can be modified, so we request current childExecutors on each call
        List<Executor> childExecutors = myExecutorGroup.childExecutors();
        AnAction[] result = new AnAction[childExecutors.size()];
        for (int i = 0; i < childExecutors.size(); i++) {
            result[i] = myChildConverter.apply(childExecutors.get(i));
        }
        return result;
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return ActionSafeReadLock.run(e, presentation -> {
            Project project = e.getData(Project.KEY);
            if (project == null || !project.isInitialized() || project.isDisposed()) {
                presentation.setEnabled(false);
                return;
            }
            presentation.setEnabledAndVisible(myExecutorGroup.isApplicable(project));
        }).toCoroutine();
    }
}
