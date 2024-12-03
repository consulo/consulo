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
package consulo.desktop.awt.welcomeScreen;

import consulo.application.Application;
import consulo.project.Project;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.wm.BalloonLayout;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeRootPaneNorthExtension;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.Rectangle2D;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.io.File;

/**
 * @author VISTALL
 * @since 2018-12-29
 */
class DesktopWelcomeIdeFrame implements IdeFrameEx {
    private FlatWelcomeFrame myFrame;

    @RequiredUIAccess
    public DesktopWelcomeIdeFrame(Application application, Runnable clearInstance) {
        myFrame = new FlatWelcomeFrame(application, clearInstance);
        myFrame.toUIWindow().putUserData(IdeFrame.KEY, this);
    }

    @Override
    public JComponent getComponent() {
        return myFrame.getRootPane();
    }

    @Nonnull
    @Override
    public Window getWindow() {
        return myFrame.toUIWindow();
    }

    @Override
    public StatusBar getStatusBar() {
        return null;
    }

    @Nullable
    @Override
    public Rectangle2D suggestChildFrameBounds() {
        return myFrame.suggestChildFrameBounds();
    }

    @Nullable
    @Override
    public Project getProject() {
        return myFrame.getProject();
    }

    @Override
    public void setFrameTitle(String title) {
        myFrame.setTitle(title);
    }

    @Override
    public void setFileTitle(String fileTitle, File ioFile) {
        myFrame.setTitle(fileTitle);
    }

    @Nullable
    @Override
    public <E extends IdeRootPaneNorthExtension> E getNorthExtension(@Nonnull Class<? extends E> extensioClass) {
        return null;
    }

    @Nullable
    @Override
    public BalloonLayout getBalloonLayout() {
        return myFrame.getBalloonLayout();
    }
}
