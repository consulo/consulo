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
package consulo.wm.impl.welcomeScreen;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeRootPaneNorthExtension;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.impl.welcomeScreen.FlatWelcomeFrame;
import com.intellij.ui.BalloonLayout;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Window;
import consulo.ui.shared.Rectangle2D;
import consulo.wm.ex.DesktopIdeFrame;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

/**
 * @author VISTALL
 * @since 2018-12-29
 */
public class DesktopWelcomeIdeFrame implements DesktopIdeFrame {
  private FlatWelcomeFrame myFlatWelcomeFrame;
  private DesktopWelcomeWindow myWindow;

  @RequiredUIAccess
  public DesktopWelcomeIdeFrame(Runnable clearInstance) {
    myFlatWelcomeFrame = new FlatWelcomeFrame(clearInstance);
    myWindow = new DesktopWelcomeWindow(myFlatWelcomeFrame);
  }

  @Nonnull
  @Override
  public Window getWindow() {
    return myWindow;
  }

  @Override
  public StatusBar getStatusBar() {
    return null;
  }

  @Nullable
  @Override
  public Rectangle2D suggestChildFrameBounds() {
    return myFlatWelcomeFrame.suggestChildFrameBounds();
  }

  @Nullable
  @Override
  public Project getProject() {
    return myFlatWelcomeFrame.getProject();
  }

  @Override
  public void setFrameTitle(String title) {
    myFlatWelcomeFrame.setFrameTitle(title);
  }

  @Override
  public void setFileTitle(String fileTitle, File ioFile) {
    myFlatWelcomeFrame.setFrameTitle(fileTitle);
  }

  @Override
  public IdeRootPaneNorthExtension getNorthExtension(String key) {
    return myFlatWelcomeFrame.getNorthExtension(key);
  }

  @Nullable
  @Override
  public BalloonLayout getBalloonLayout() {
    return myFlatWelcomeFrame.getBalloonLayout();
  }

  @Nonnull
  @Override
  public java.awt.Window getJWindow() {
    return myFlatWelcomeFrame;
  }
}
