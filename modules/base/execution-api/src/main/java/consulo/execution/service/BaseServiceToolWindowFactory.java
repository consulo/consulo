/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.service;

import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19.05.2024
 */
public abstract class BaseServiceToolWindowFactory implements ToolWindowFactory {
    @Override
    public boolean shouldBeAvailable(@Nonnull Project project) {
        return false;
    }

    @RequiredUIAccess
    @Override
    public void init(Project project, ToolWindow toolWindow) {
        project.getInstance(ServiceViewToolWindowManager.class).initToolWindow(toolWindow);
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
        project.getInstance(ServiceViewToolWindowManager.class).createToolWindowContent(toolWindow);
    }
}
