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
package consulo.execution.profiler.impl.internal.toolwindow;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.execution.icon.ExecutionIconGroup;
import consulo.execution.profiler.ProfilerToolWindowManager;
import consulo.execution.profiler.icon.ExecutionProfilerIconGroup;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl
public final class ProfilerToolWindowFactory implements ToolWindowFactory, DumbAware {
    private static final Logger LOG = Logger.getInstance(ProfilerToolWindowFactory.class);

    @Override
    public String getId() {
        return ProfilerToolWindowManager.TOOLWINDOW_ID;
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        if (ProfilerToolWindowManager.getInstance(project) instanceof ProfilerToolWindowManagerImpl manager) {
            manager.initToolWindow(toolWindow);
        }
        else {
            LOG.error("Unexpected " + ProfilerToolWindowManager.class.getSimpleName() + " implementation");
        }
    }

    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.BOTTOM;
    }

    @Override
    public Image getIcon() {
        return ExecutionProfilerIconGroup.toolwindowprofiler();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Profiler");
    }

    @Override
    public boolean canCloseContents() {
        return true;
    }

    @Override
    public boolean isDoNotActivateOnStart() {
        return true;
    }

    @Override
    public boolean shouldBeAvailable(Project project) {
        return true;
    }
}
