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
package consulo.wm.impl;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeRootPaneNorthExtension;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.ui.BalloonLayout;
import com.vaadin.shared.ui.window.WindowMode;
import consulo.ui.Components;
import consulo.ui.Windows;
import consulo.web.application.WebApplication;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * @author VISTALL
 * @since 24-Sep-17
 */
public class WebIdeFrameImpl implements IdeFrameEx {
  private final WebStatusBarImpl myStatusBar = new WebStatusBarImpl(this);
  private final Project myProject;

  private consulo.ui.Window myWindow;

  public WebIdeFrameImpl(Project project) {
    myProject = project;
  }

  public void show() {
    WebApplication.invokeOnCurrentSession(() -> {
      myWindow = Windows.modalWindow(myProject.getName());
      ((com.vaadin.ui.Window)myWindow).setWindowMode(WindowMode.MAXIMIZED);

      myWindow.setResizable(false);
      myWindow.setClosable(false);
      myWindow.setContent(Components.label("Hello world"));

      myWindow.show();
    });
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
  public Rectangle suggestChildFrameBounds() {
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

  @Override
  public JComponent getComponent() {
    return null;
  }

  @Nullable
  @Override
  public BalloonLayout getBalloonLayout() {
    return null;
  }
}
