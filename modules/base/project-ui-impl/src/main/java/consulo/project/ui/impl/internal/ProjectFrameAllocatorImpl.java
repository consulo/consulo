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
package consulo.project.ui.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.project.Project;
import consulo.project.ProjectOpenContext;
import consulo.project.internal.ProjectFrameAllocator;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.IdeFrameState;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2024-08-04
 */
@ServiceImpl
@Singleton
public class ProjectFrameAllocatorImpl implements ProjectFrameAllocator {
    private final WindowManagerEx myWindowManager;
    private final WelcomeFrameManager myWelcomeFrameManager;
    private final Project myProject;

    @Inject
    public ProjectFrameAllocatorImpl(WindowManager windowManager, WelcomeFrameManager welcomeFrameManager, Project project) {
        myWelcomeFrameManager = welcomeFrameManager;
        myWindowManager = (WindowManagerEx) windowManager;
        myProject = project;
    }

    @RequiredUIAccess
    @Override
    public void allocateFrame(@Nonnull ProjectOpenContext context) {
        IdeFrameState state = context.getUserData(IdeFrameState.KEY);

        myWindowManager.allocateFrame(myProject, state);
        
        // force close welcome frame after frame allocating, since its project open
        myWelcomeFrameManager.closeFrame();
    }

    @RequiredUIAccess
    @Override
    public void initializeFrame() {
        IdeFrameEx frame = myWindowManager.getIdeFrame(myProject);

        frame.initialize();
    }
}
