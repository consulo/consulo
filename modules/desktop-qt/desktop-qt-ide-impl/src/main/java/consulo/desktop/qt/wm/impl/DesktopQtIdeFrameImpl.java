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
package consulo.desktop.qt.wm.impl;

import consulo.application.Application;
import consulo.application.internal.AppLifecycleListener;
import consulo.desktop.qt.ui.impl.DesktopQtWindowImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.wm.impl.UnifiedStatusBarImpl;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.wm.BalloonLayout;
import consulo.project.ui.wm.FrameTitleBuilder;
import consulo.project.ui.wm.IdeFrameState;
import consulo.project.ui.wm.IdeRootPaneNorthExtension;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.Rectangle2D;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.TitlelessDecorator;
import consulo.ui.ex.TitlelessDecoratorService;
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtIdeFrameImpl implements IdeFrameEx, Disposable {
    private final Project myProject;
    private final DesktopQtRootView myRootView;

    /**
     * Geometry the frame last closed with, kept by {@code RecentProjectsManagerImpl} against the project path and
     * handed back through {@code WindowManagerEx#allocateFrame}.
     */
    private final @Nullable IdeFrameState myState;

    private UnifiedStatusBarImpl myStatusBar;
    private @Nullable Window myWindow;

    @RequiredUIAccess
    public DesktopQtIdeFrameImpl(Project project, @Nullable IdeFrameState state) {
        myProject = project;
        myState = state;
        myRootView = new DesktopQtRootView(project);
    }

    /**
     * The status bar is built here and not in {@link #show()} because the frame allocator instantiates
     * {@code StatusBarWidgetsManager} right after this call, and every widget it creates is dropped for good
     * when {@code WindowManager#getStatusBar} still answers null.
     */
    @RequiredUIAccess
    @Override
    public void initialize() {
        myStatusBar = new UnifiedStatusBarImpl(myProject.getApplication(), null);
        Disposer.register(this, myStatusBar);
        myStatusBar.install(this);

        myRootView.setStatusBar(myStatusBar);
    }

    @RequiredUIAccess
    public void show() {
        if (myWindow != null) {
            return;
        }

        Window window = Window.create(FrameTitleBuilder.getInstance().getProjectTitle(myProject), WindowOptions.builder().build());
        myWindow = window;

        DesktopQtWindowImpl qtWindow = (DesktopQtWindowImpl) window;
        qtWindow.markAsMainFrame();

        // the header of the frame is a strip of its own and holds the room it needs, so nothing the decorator
        // answers is owed by the content here - what this call is for is the header itself
        TitlelessDecoratorService.getInstance().of(qtWindow, TitlelessDecorator.MAIN_WINDOW);

        applyState(qtWindow);

        window.addCloseListener(event -> onCloseRequested());

        window.setMenuBar(myRootView.getMenuBar());
        window.setContent(myRootView.getRootPanel().getComponent());

        window.show();

        myRootView.update();
    }

    @RequiredUIAccess
    private void onCloseRequested() {
        if (myWindow == null) {
            return;
        }

        ProjectManager projectManager = ProjectManager.getInstance();

        Project[] openProjects = projectManager.getOpenProjects();
        if (openProjects.length > 1 || openProjects.length == 1 && Platform.current().os().isMac()) {
            if (myProject.isOpen()) {
                projectManager.closeAndDisposeAsync(myProject, UIAccess.current())
                    .whenComplete((closed, throwable) -> frameClosed());
            }
            else {
                frameClosed();
            }
        }
        else {
            Application.get().exit();
        }
    }

    private static void frameClosed() {
        Application.get().getMessageBus().syncPublisher(AppLifecycleListener.class).projectFrameClosed();

        WelcomeFrameManager.getInstance().showIfNoProjectOpened();
    }

    @RequiredUIAccess
    private void applyState(DesktopQtWindowImpl window) {
        IdeFrameState state = myState;
        if (state == null || IdeFrameState.EMPTY.equals(state)) {
            return;
        }

        window.setBounds(new Rectangle2D(state.x(), state.y(), state.width(), state.height()));

        if (state.maximized()) {
            window.setMaximized(true);
        }
    }

    @Override
    public IdeFrameState getFrameState() {
        if (!(myWindow instanceof DesktopQtWindowImpl window)) {
            return IdeFrameState.EMPTY;
        }

        Rectangle2D bounds = window.getBounds();

        return new IdeFrameState(
            bounds.minX(),
            bounds.minY(),
            bounds.width(),
            bounds.height(),
            window.isMaximized(),
            window.isFullScreen()
        );
    }

    public DesktopQtRootPaneImpl getRootPanel() {
        return myRootView.getRootPanel();
    }

    @Override
    public Window getWindow() {
        return myWindow;
    }

    public void close() {
        Window window = myWindow;
        if (window == null) {
            return;
        }

        myWindow = null;

        UIAccess uiAccess = window.getUIAccess();
        if (uiAccess != null && uiAccess.isValid()) {
            uiAccess.giveIfNeed(window::close);
        }
    }

    @Override
    public StatusBar getStatusBar() {
        return myStatusBar;
    }

    @Override
    public Rectangle2D suggestChildFrameBounds() {
        return null;
    }

    @Override
    public @Nullable Project getProject() {
        return myProject;
    }

    @Override
    public void setFrameTitle(String title) {
    }

    @Override
    public void setFileTitle(String fileTitle, File ioFile) {
    }

    @Override
    public <E extends IdeRootPaneNorthExtension> @Nullable E getNorthExtension(Class<? extends E> extensionClass) {
        return null;
    }

    @Override
    public @Nullable BalloonLayout getBalloonLayout() {
        return null;
    }

    @Override
    public void dispose() {
    }
}
