/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.internal.wm;

import com.vaadin.flow.component.UI;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.wm.impl.UnifiedStatusBarImpl;
import consulo.project.Project;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.wm.BalloonLayout;
import consulo.project.ui.wm.FrameTitleBuilder;
import consulo.project.ui.wm.IdeRootPaneNorthExtension;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.Rectangle2D;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.web.internal.servlet.VaadinRootLayout;
import consulo.web.ui.impl.internal.WebRootPaneImpl;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2017-09-24
 */
public class WebIdeFrameImpl implements IdeFrameEx, Disposable {
    private final Project myProject;
    private final WebIdeRootView myRootView;

    private UnifiedStatusBarImpl myStatusBar;

    private VaadinRootLayout myRootLayout;

    public WebIdeFrameImpl(Project project) {
        myProject = project;
        myRootView = new WebIdeRootView(project);
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
        UI ui = UI.getCurrent();

        myRootLayout = (VaadinRootLayout) ui.getCurrentView();

        String projectTitle = FrameTitleBuilder.getInstance().getProjectTitle(myProject);

        ui.getPage().setTitle(projectTitle);

        myRootView.update();

        myRootLayout.update(TargetVaadin.to(myRootView.getRootPanel().getComponent()));
    }

    /**
     * Takes the frame's component tree out of the state tree of the ui it is attached to. Vaadin refuses to attach
     * a tree which still belongs to another ui, so this runs under the old ui before {@link #show()} runs under the
     * new one.
     * <p>
     * {@code false} asks for the full reset. The keeping variant leaves every node remembering it was attached
     * once, and a node which remembers fires no attach events when the new ui takes it in - the elements arrive,
     * while everything a component sends from its attach listener does not: the grid never asks for rows, the
     * editor never loads its scripts, and nothing says a word about it.
     */
    public void removeFromTree() {
        TargetVaadin.to(myRootView.getRootPanel().getComponent()).getElement().removeFromTree(false);
    }

    public @Nullable VaadinRootLayout getRootLayout() {
        return myRootLayout;
    }

    public WebRootPaneImpl getRootPanel() {
        return myRootView.getRootPanel();
    }

    @Override
    public Window getWindow() {
        return (Window) Objects.requireNonNull(myRootLayout).toUIComponent();
    }

    /**
     * A browser ignores {@code window.close()} for a tab it did not open itself, so asking for that left the
     * frame of the closed project on screen with the welcome frame drawn over it. The content of the root
     * layout is what the frame is, and taking it out is what closing means here.
     */
    public void close() {
        VaadinRootLayout rootLayout = myRootLayout;
        if (rootLayout == null) {
            return;
        }

        myRootLayout = null;

        UIAccess uiAccess = rootLayout.toUIComponent().getUIAccess();
        if (uiAccess != null && uiAccess.isValid()) {
            uiAccess.giveIfNeed(rootLayout::removeAll);
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
