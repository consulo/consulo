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

package consulo.execution.impl.internal.action;

import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

public class ChooseRunConfigurationPopupAction extends BaseChooseRunConfigurationPopupAction {
    public ChooseRunConfigurationPopupAction() {
        super("Run...", "Choose and run configuration", PlatformIconGroup.actionsExecute());
    }

    @Override
    protected Executor getDefaultExecutor() {
        return DefaultRunExecutor.getRunExecutorInstance();
    }

    @Override
    protected Executor getAlternativeExecutor() {
        return ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.DEBUG);
    }

    @Override
    protected String getAdKey() {
        return "run.configuration.alternate.action.ad";
    }
}
