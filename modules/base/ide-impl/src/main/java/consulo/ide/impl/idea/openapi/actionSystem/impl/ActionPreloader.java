/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.actionSystem.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.internal.PreloadingActivity;
import consulo.application.progress.ProgressIndicator;
import consulo.execution.executor.ExecutorRegistry;
import consulo.execution.internal.ExecutorRegistryEx;
import consulo.ui.ex.action.ActionManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

/**
 * @author yole
 */
@ExtensionImpl(order = "first")
public class ActionPreloader extends PreloadingActivity {
    private final ActionManager myActionManager;
    private final ExecutorRegistry myExecutorRegistry;

    @Inject
    public ActionPreloader(ActionManager actionManager, ExecutorRegistry executorRegistry) {
        myActionManager = actionManager;
        myExecutorRegistry = executorRegistry;
    }

    @Override
    public void preload(@Nonnull ProgressIndicator indicator) {
        ActionManagerImpl actionManager = (ActionManagerImpl) myActionManager;

        actionManager.initialize(() -> {
            actionManager.loadActions();

            // need it due its register actions
            ((ExecutorRegistryEx) myExecutorRegistry).initExecuteActions();

            actionManager.preloadActions(indicator);
        });
    }
}
