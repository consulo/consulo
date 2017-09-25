/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl.welcomeScreen;

import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.MnemonicHelper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.DimensionService;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeRootPaneNorthExtension;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl;
import com.intellij.ui.*;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.AccessibleContextAccessor;
import consulo.annotations.RequiredDispatchThread;
import consulo.application.impl.FrameTitleUtil;
import consulo.awt.ToAWT;
import consulo.ide.welcomeScreen.FlatWelcomeScreen;
import consulo.start.WelcomeFrameManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * @author Konstantin Bulenkov
 */
public class FlatWelcomeFrame extends JFrame implements IdeFrameEx, Disposable, AccessibleContextAccessor, UISettingsListener {
  @NotNull
  public static Dimension getDefaultWindowSize() {
    return ToAWT.from(WelcomeFrameManager.getDefaultWindowSize());
  }

  BalloonLayout myBalloonLayout;
  private final FlatWelcomeScreen myScreen;
  private boolean myDisposed;

  @RequiredDispatchThread
  public FlatWelcomeFrame() {
    final JRootPane rootPane = getRootPane();
    myScreen = new FlatWelcomeScreen(this);

    final IdeGlassPaneImpl glassPane = new IdeGlassPaneImpl(rootPane);

    setGlassPane(glassPane);
    glassPane.setVisible(false);
    //setUndecorated(true);
    setContentPane(myScreen);
    setDefaultTitle();
    AppUIUtil.updateWindowIcon(this);
    UIUtil.resetRootPaneAppearance(rootPane);
    setSize(getDefaultWindowSize());
    setResizable(false);
    Point location = DimensionService.getInstance().getLocationNoRealKey(WelcomeFrame.DIMENSION_KEY);
    Rectangle screenBounds = ScreenUtil.getScreenRectangle(location != null ? location : new Point(0, 0));
    setLocation(new Point(screenBounds.x + (screenBounds.width - getWidth()) / 2, screenBounds.y + (screenBounds.height - getHeight()) / 3));

    ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter() {
      @Override
      public void projectOpened(Project project) {
        Disposer.dispose(FlatWelcomeFrame.this);
      }
    }, this);

    myBalloonLayout = new WelcomeBalloonLayoutImpl(rootPane, JBUI.insets(8), myScreen.getMainWelcomePanel().myEventListener, myScreen.getMainWelcomePanel().myEventLocation);

    WelcomeFrame.setupCloseAction(this);
    MnemonicHelper.init(this);
    Disposer.register(ApplicationManager.getApplication(), this);
  }

  public void setDefaultTitle() {
    setTitle(FrameTitleUtil.buildTitle());
  }

  @Override
  public void dispose() {
    if (myDisposed) {
      return;
    }

    myDisposed = true;
    super.dispose();

    if (myBalloonLayout != null) {
      ((BalloonLayoutImpl)myBalloonLayout).dispose();
      myBalloonLayout = null;
    }

    WelcomeFrame.resetInstance();

    // open project from welcome screen show progress dialog and call FocusTrackback.register()
    FocusTrackback.release(this);
  }

  @Override
  public StatusBar getStatusBar() {
    return null;
  }

  public static Color getProjectsBackground() {
    return new JBColor(Gray.xFF, Gray.x39);
  }

  public static Color getLinkNormalColor() {
    return new JBColor(Gray._0, Gray.xBB);
  }

  public static Color getActionLinkSelectionColor() {
    return new JBColor(0xdbe5f5, 0x485875);
  }

  public static Color getSeparatorColor() {
    return UIUtil.getBorderColor();
  }

  @Override
  public AccessibleContext getCurrentAccessibleContext() {
    return accessibleContext;
  }

  public static boolean isUseProjectGroups() {
    return true;
  }

  @Override
  public BalloonLayout getBalloonLayout() {
    return myBalloonLayout;
  }

  @Override
  public Rectangle suggestChildFrameBounds() {
    return getBounds();
  }

  @Nullable
  @Override
  public Project getProject() {
    return ProjectManager.getInstance().getDefaultProject();
  }

  @Override
  public void setFrameTitle(String title) {
    setTitle(title);
  }

  @Override
  public void setFileTitle(String fileTitle, File ioFile) {
    setTitle(fileTitle);
  }

  @Override
  public IdeRootPaneNorthExtension getNorthExtension(String key) {
    return null;
  }

  @Override
  public JComponent getComponent() {
    return getRootPane();
  }

  @Override
  public void uiSettingsChanged(UISettings source) {
    UIUtil.resetRootPaneAppearance(getRootPane());
  }

  @Override
  public boolean isInFullScreen() {
    return false;
  }

  @NotNull
  @Override
  public ActionCallback toggleFullScreen(boolean state) {
    return ActionCallback.REJECTED;
  }
}
