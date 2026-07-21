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
package consulo.it.internal;

import consulo.project.Project;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.BalloonLayout;
import consulo.project.ui.wm.IdeRootPaneNorthExtension;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.Rectangle2D;
import consulo.ui.Window;
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * Headless {@link IdeFrameEx}: carries a project and a headless status bar, but never creates any
 * {@code consulo.ui} window/component.
 *
 * @author VISTALL
 */
public class HeadlessIdeFrame implements IdeFrameEx {
    private final Project myProject;
    private final StatusBarEx myStatusBar;

    public HeadlessIdeFrame(Project project) {
        myProject = project;
        myStatusBar = new HeadlessStatusBar(project);
    }

    @Override
    public @Nullable StatusBar getStatusBar() {
        return myStatusBar;
    }

    @Override
    public @Nullable Rectangle2D suggestChildFrameBounds() {
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
    public Window getWindow() {
        return null;
    }

    @Override
    public void initialize() {
    }
}
