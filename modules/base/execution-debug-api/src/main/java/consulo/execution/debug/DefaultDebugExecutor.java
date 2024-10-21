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
package consulo.execution.debug;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.executor.Executor;
import consulo.execution.executor.ExecutorRegistry;
import consulo.localize.LocalizeValue;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author spleaner
 */
@ExtensionImpl(id = "debug", order = "after run")
public class DefaultDebugExecutor extends Executor {
    public static final String EXECUTOR_ID = ToolWindowId.DEBUG;

    @Override
    public String getToolWindowId() {
        return ToolWindowId.DEBUG;
    }

    @Override
    public Image getToolWindowIcon() {
        return AllIcons.Toolwindows.ToolWindowDebugger;
    }

    @Override
    @Nonnull
    public Image getIcon() {
        return AllIcons.Actions.StartDebugger;
    }

    @Override
    @Nonnull
    public LocalizeValue getActionName() {
        return UILocalize.toolWindowNameDebug();
    }

    @Override
    @Nonnull
    public String getId() {
        return EXECUTOR_ID;
    }

    @Nonnull
    @Override
    public String getContextActionId() {
        return "DebugClass";
    }

    @Nonnull
    @Override
    public LocalizeValue getStartActionText() {
        return XDebuggerLocalize.debuggerRunnerStartActionText();
    }

    @Nonnull
    @Override
    public LocalizeValue getStartActiveText(@Nonnull String configurationName) {
        return XDebuggerLocalize.debuggerRunnerStartActionText0(configurationName);
    }

    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return XDebuggerLocalize.stringDebuggerRunnerDescription();
    }

    @Override
    public String getHelpId() {
        return "debugging.DebugWindow";
    }

    public static Executor getDebugExecutorInstance() {
        return ExecutorRegistry.getInstance().getExecutorById(EXECUTOR_ID);
    }
}
