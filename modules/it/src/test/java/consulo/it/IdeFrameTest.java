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
package consulo.it;

import consulo.application.Application;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.IdeFrameState;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.ToolWindowManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Opens a real project through the headless open flow and exercises the frame that the real
 * {@code ProjectFrameAllocatorImpl} allocates via the headless window manager.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class IdeFrameTest {
    @Test
    public void resolvesIdeFrameAndStatusBarForOpenedProject(Application application, ProjectManager projectManager) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-ideframe");

        Project project = projectManager
            .openProjectAsync(directory, application.getLastUIAccess(), new ProjectOpenContext())
            .get(30, TimeUnit.SECONDS);
        assertThat(project).isNotNull();

        // The real ProjectFrameAllocatorImpl allocated a frame through the headless WindowManager.
        WindowManagerEx windowManager = WindowManagerEx.getInstanceEx();
        assertThat(windowManager).isNotNull();

        IdeFrameEx frame = windowManager.getIdeFrame(project);
        assertThat(frame).as("frame allocated for opened project").isNotNull();
        assertThat(frame.getProject()).isSameAs(project);

        // frame geometry/state accessors are callable and return the headless defaults
        assertThat(frame.getFrameState()).isEqualTo(IdeFrameState.EMPTY);
        assertThat(frame.isInFullScreen()).isFalse();
        assertThat(frame.suggestChildFrameBounds()).isNull();
        assertThat(frame.getBalloonLayout()).isNull();

        // the status bar is reachable both from the frame and from the window manager, and is the same instance
        StatusBar statusBar = frame.getStatusBar();
        assertThat(statusBar).isNotNull();
        assertThat(statusBar.getProject()).isSameAs(project);
        assertThat(windowManager.getStatusBar(project)).isSameAs(statusBar);

        // the per-project ToolWindowManager resolved (our real-but-headless ToolWindowManagerBase subclass)
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        assertThat(toolWindowManager).isNotNull();

        // getActiveToolWindowId() is @RequiredUIAccess: dispatch onto the headless UI thread
        String activeId = application.getLastUIAccess()
            .giveAndWaitIfNeed(toolWindowManager::getActiveToolWindowId);
        assertThat(toolWindowManager.getToolWindowIds()).isNotNull();
        assertThat(activeId).isNull();
    }
}
