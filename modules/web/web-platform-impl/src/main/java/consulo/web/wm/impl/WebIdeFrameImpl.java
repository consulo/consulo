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
package consulo.web.wm.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.IdeRootPaneNorthExtension;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.ui.BalloonLayout;
import com.vaadin.shared.ui.window.WindowMode;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.shared.Rectangle2D;
import consulo.ui.web.internal.TargetVaddin;
import consulo.ui.web.internal.WebRootPaneImpl;
import consulo.web.application.WebApplication;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * @author VISTALL
 * @since 24-Sep-17
 */
public class WebIdeFrameImpl implements IdeFrameEx {
  private final WebStatusBarImpl myStatusBar = new WebStatusBarImpl(this);
  private final Project myProject;

  private Window myWindow;
  private final WebIdeRootView myRootView;

  public WebIdeFrameImpl(Project project) {
    myProject = project;
    myRootView = new WebIdeRootView(project);
  }

  @RequiredUIAccess
  public void show() {
    myWindow = Window.createModal(myProject.getName());

    com.vaadin.ui.Window vaadinWindow = (com.vaadin.ui.Window)TargetVaddin.to(myWindow);
    vaadinWindow.setWindowMode(WindowMode.MAXIMIZED);

    myWindow.setResizable(false);
    myWindow.addListener(Window.CloseListener.class, () -> {
      myWindow.close();

      ProjectManager.getInstance().closeAndDisposeAsync(myProject, UIAccess.current());
    });

    myWindow.setContent(myRootView.getComponent());

    myRootView.update();

    myWindow.show();
  }

  public WebRootPaneImpl getRootPanel() {
    return myRootView.getComponent();
  }

  @Nonnull
  @Override
  public Window getWindow() {
    return myWindow;
  }

  public void close() {
    WebApplication.invokeOnCurrentSession(() -> {
      myWindow.close();
    });
  }

  @Override
  public StatusBar getStatusBar() {
    return myStatusBar;
  }

  @Override
  public Rectangle2D suggestChildFrameBounds() {
    return null;
  }

  @Nullable
  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public void setFrameTitle(String title) {

  }

  @Override
  public void setFileTitle(String fileTitle, File ioFile) {

  }

  @Override
  public IdeRootPaneNorthExtension getNorthExtension(String key) {
    return null;
  }

  @Nullable
  @Override
  public BalloonLayout getBalloonLayout() {
    return null;
  }
}
