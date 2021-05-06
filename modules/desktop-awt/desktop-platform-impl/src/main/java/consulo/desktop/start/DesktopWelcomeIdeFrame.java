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
package consulo.desktop.start;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.IdeRootPaneNorthExtension;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame;
import com.intellij.ui.BalloonLayout;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.Window;
import consulo.ui.Rectangle2D;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;

/**
 * @author VISTALL
 * @since 2018-12-29
 */
class DesktopWelcomeIdeFrame implements IdeFrameEx {
  private FlatWelcomeFrame myFrame;

  @RequiredUIAccess
  public DesktopWelcomeIdeFrame(Runnable clearInstance) {
    myFrame = new FlatWelcomeFrame(clearInstance);
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

  @Override
  public IdeRootPaneNorthExtension getNorthExtension(String key) {
    return myFrame.getNorthExtension(key);
  }

  @Nullable
  @Override
  public BalloonLayout getBalloonLayout() {
    return myFrame.getBalloonLayout();
  }
}
