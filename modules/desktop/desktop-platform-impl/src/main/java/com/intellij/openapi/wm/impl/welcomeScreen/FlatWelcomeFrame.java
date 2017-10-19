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
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.util.DimensionService;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
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
import consulo.awt.TargetAWT;
import consulo.ide.welcomeScreen.FlatWelcomeScreen;
import consulo.start.WelcomeFrameManager;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.*;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.style.ColorKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.EventListener;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Konstantin Bulenkov
 */
public class FlatWelcomeFrame extends JFrame implements IdeFrameEx, Disposable, AccessibleContextAccessor, UISettingsListener, consulo.ui.Window {
  private BalloonLayout myBalloonLayout;
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
    setSize(TargetAWT.to(WelcomeFrameManager.getDefaultWindowSize()));
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

    myBalloonLayout = new WelcomeDesktopBalloonLayoutImpl(rootPane, JBUI.insets(8), myScreen.getMainWelcomePanel().myEventListener, myScreen.getMainWelcomePanel().myEventLocation);

    setupCloseAction(this);
    MnemonicHelper.init(this);
    Disposer.register(ApplicationManager.getApplication(), this);
  }

  static void setupCloseAction(final JFrame frame) {
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        saveLocation(frame.getBounds());

        frame.dispose();

        if (ProjectManager.getInstance().getOpenProjects().length == 0) {
          ApplicationManagerEx.getApplicationEx().exit();
        }
      }
    });
  }

  public static void saveLocation(Rectangle location) {
    Point middle = new Point(location.x + location.width / 2, location.y = location.height / 2);
    DimensionService.getInstance().setLocationNoRealKey(WelcomeFrame.DIMENSION_KEY, middle);
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
      ((DesktopBalloonLayoutImpl)myBalloonLayout).dispose();
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

  @Override
  public AccessibleContext getCurrentAccessibleContext() {
    return accessibleContext;
  }

  @Override
  public BalloonLayout getBalloonLayout() {
    return myBalloonLayout;
  }

  @Override
  public Rectangle2D suggestChildFrameBounds() {
    return TargetAWT.from(getBounds());
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

  // region Migration staff
  @RequiredUIAccess
  @Override
  public void setContent(@NotNull Component content) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void setMenuBar(@Nullable MenuBar menuBar) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setClosable(boolean value) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void close() {
    setVisible(false);
  }

  @RequiredUIAccess
  @Override
  public void addBorder(@NotNull BorderPosition borderPosition, BorderStyle borderStyle, ColorKey colorKey, int width) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void removeBorder(@NotNull BorderPosition borderPosition) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public <T> Runnable addUserDataProvider(@NotNull Key<T> key, @NotNull Supplier<T> supplier) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public Runnable addUserDataProvider(@NotNull Function<Key<?>, Object> function) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public <T extends EventListener> T getListenerDispatcher(@NotNull Class<T> eventClass) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public <T extends EventListener> Runnable addListener(@NotNull Class<T> eventClass, @NotNull T listener) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public <T> T getUserData(@NotNull Key<T> key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
    throw new UnsupportedOperationException();
  }
  // endregion
}
