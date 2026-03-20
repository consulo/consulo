/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ide.impl.wm.impl;

import consulo.project.Project;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.wm.BalloonLayout;
import consulo.project.ui.wm.IdeRootPaneNorthExtension;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.Rectangle2D;
import consulo.ui.Window;
import org.jspecify.annotations.Nullable;

import java.io.File;

/**
 * @author VISTALL
 * @since 2018-12-29
 */
public class UnifiedWelcomeIdeFrame implements IdeFrameEx {
    private final Window myWindow;
    private final Project myProject;

    public UnifiedWelcomeIdeFrame(Window window, Project project) {
        myWindow = window;
        myProject = project;
    }

    
    @Override
    public Window getWindow() {
        return myWindow;
    }

    @Override
    public StatusBar getStatusBar() {
        return null;
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
        myWindow.setTitle(title);
    }

    @Override
    public void setFileTitle(String fileTitle, File ioFile) {
        myWindow.setTitle(fileTitle);
    }

    @Override
    public <E extends IdeRootPaneNorthExtension> @Nullable E getNorthExtension(Class<? extends E> extensioClass) {
        return null;
    }

    @Override
    public @Nullable BalloonLayout getBalloonLayout() {
        return null;
    }
}
