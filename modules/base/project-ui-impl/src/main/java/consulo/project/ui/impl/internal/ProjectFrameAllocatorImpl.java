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
import consulo.project.ui.impl.internal.wm.ToolWindowManagerBase;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.*;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.coroutine.UIAction;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * @author VISTALL
 * @since 2024-08-04
 */
@ServiceImpl
@Singleton
public class ProjectFrameAllocatorImpl implements ProjectFrameAllocator {
    private static final Key<ToolWindowManagerBase> TOOL_WINDOW_MANAGER = Key.create(ToolWindowManagerBase.class);

    private final WindowManagerEx myWindowManager;
    private final WelcomeFrameManager myWelcomeFrameManager;

    @Inject
    public ProjectFrameAllocatorImpl(WindowManager windowManager, WelcomeFrameManager welcomeFrameManager) {
        myWelcomeFrameManager = welcomeFrameManager;
        myWindowManager = (WindowManagerEx) windowManager;
    }

    @RequiredUIAccess
    @Override
    public Object allocateFrame(@Nonnull Project project, @Nonnull ProjectOpenContext context) {
        IdeFrameState state = context.getUserData(IdeFrameState.KEY);

        IdeFrameEx frame = myWindowManager.allocateFrame(project, state);

        // force close welcome frame after frame allocating, since its project open
        myWelcomeFrameManager.closeFrame();

        frame.initialize();

        return frame;
    }

    @Override
    public <I, O extends Project> Coroutine<I, O> allocateFrame(@Nonnull ProjectOpenContext context, @Nonnull Coroutine<I, O> in) {
        IdeFrameState state = context.getUserData(IdeFrameState.KEY);

        return in
            .then(UIAction.apply((project, continuation) -> {
                IdeFrameEx frame = myWindowManager.allocateFrame(project, state);
                continuation.putUserData(IdeFrame.KEY, frame);
                return project;
            }))
            .then(UIAction.apply((project, continuation) -> {
                IdeFrameEx ideFrame = (IdeFrameEx) Objects.requireNonNull(continuation.getUserData(IdeFrame.KEY));
                ideFrame.initialize();
                return project;
            }))
            .then(UIAction.apply((project, continuation) -> {
                // force close welcome frame after frame allocating, since its project open
                myWelcomeFrameManager.closeFrame();
                return project;
            }))
            .then(UIAction.apply((project, continuation) -> {
                UIAccess uiAccess = project.getUIAccess();

                IdeFrameEx ideFrame = (IdeFrameEx) Objects.requireNonNull(continuation.getUserData(IdeFrame.KEY));

                StatusBarWidgetsManager statusBarWidgetsManager = project.getInstance(StatusBarWidgetsManager.class);
                statusBarWidgetsManager.updateAllWidgets(ideFrame, uiAccess);
                return project;
            }));
    }

    @Override
    public <I, O> Coroutine<I, O> initializeSteps(@Nonnull Project project, @Nonnull Coroutine<I, O> in) {
        return in
            .then(UIAction.apply((o, c) -> {
                ToolWindowManagerBase manager = (ToolWindowManagerBase) ToolWindowManager.getInstance(project);
                manager.initializeUI();

                c.putCopyableUserData(TOOL_WINDOW_MANAGER, manager);
                return o;
            }))
            .then(UIAction.apply((o, c) -> {
                ToolWindowManagerBase manager = c.getCopyableUserData(TOOL_WINDOW_MANAGER);

                manager.initializeEditorComponent();
                return o;
            }))
            .then(CodeExecution.apply((o, c) -> {
                UIAccess.assetIsNotUIThread();

                UIAccess uiAccess = c.getConfiguration(UIAccess.KEY);

                ToolWindowManagerBase manager = c.getCopyableUserData(TOOL_WINDOW_MANAGER);
                try {
                    manager.registerToolWindowsFromBeans(uiAccess).get();
                }
                catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
                return o;
            }))
            .then(UIAction.apply((o, c) -> {
                ToolWindowManagerBase manager = c.getCopyableUserData(TOOL_WINDOW_MANAGER);
                manager.postInitialize();
                return o;
            }))
            .then(UIAction.apply((o, c) -> {
                ToolWindowManagerBase manager = c.getCopyableUserData(TOOL_WINDOW_MANAGER);
                manager.connectModuleExtensionListener();
                return o;
            }))
            .then(UIAction.apply((o, c) -> {
                ToolWindowManagerBase manager = c.getCopyableUserData(TOOL_WINDOW_MANAGER);
                manager.activateOnProjectOpening();
                return o;
            }));
    }

    @Override
    public <I, O> Coroutine<I, O> postSteps(@Nonnull Project project, @Nonnull Coroutine<I, O> in) {
        return in.then(UIAction.apply((o, c) -> {
            UIAccess uiAccess = c.getConfiguration(UIAccess.KEY);

            // we need update widgets again
            StatusBarWidgetsManager statusBarWidgetsManager = project.getInstance(StatusBarWidgetsManager.class);
            statusBarWidgetsManager.updateAllWidgets(uiAccess);
            return o;
        }));
    }
}
