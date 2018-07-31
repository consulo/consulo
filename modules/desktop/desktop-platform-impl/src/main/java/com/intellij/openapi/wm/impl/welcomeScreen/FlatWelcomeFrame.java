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
import com.intellij.openapi.application.Application;
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
import consulo.ide.updateSettings.UpdateSettings;
import consulo.ide.welcomeScreen.FlatWelcomeScreen;
import consulo.start.WelcomeFrameManager;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Window;
import consulo.ui.shared.Rectangle2D;
import consulo.ui.shared.Size;
import consulo.ui.shared.border.BorderPosition;
import consulo.ui.shared.border.BorderStyle;
import consulo.ui.style.ColorKey;

import javax.accessibility.AccessibleContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
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
public class FlatWelcomeFrame extends JFrame implements IdeFrameEx, Disposable, AccessibleContextAccessor, UISettingsListener, Window {
  private final Application myApplication;
  private final DimensionService myDimensionService;
  private final ProjectManager myProjectManager;
  private final UpdateSettings myUpdateSettings;
  private final WelcomeFrameHelper myWelcomeFrameHelper;

  private BalloonLayout myBalloonLayout;
  private boolean myDisposed;

  @RequiredDispatchThread
  @Inject
  public FlatWelcomeFrame(Application application, DimensionService dimensionService, ProjectManager projectManager, UpdateSettings updateSettings, WelcomeFrameHelper welcomeFrameHelper) {
    myApplication = application;
    myDimensionService = dimensionService;
    myProjectManager = projectManager;
    myUpdateSettings = updateSettings;
    myWelcomeFrameHelper = welcomeFrameHelper;
    final JRootPane rootPane = getRootPane();

    FlatWelcomeScreen screen = new FlatWelcomeScreen(this);

    final IdeGlassPaneImpl glassPane = new IdeGlassPaneImpl(rootPane);

    setGlassPane(glassPane);
    glassPane.setVisible(false);
    //setUndecorated(true);
    setContentPane(screen);
    setDefaultTitle();
    AppUIUtil.updateWindowIcon(this);
    UIUtil.resetRootPaneAppearance(rootPane);
    setSize(TargetAWT.to(WelcomeFrameManager.getDefaultWindowSize()));
    setResizable(false);
    Point location = myDimensionService.getLocationNoRealKey(WelcomeFrameManager.DIMENSION_KEY);
    Rectangle screenBounds = ScreenUtil.getScreenRectangle(location != null ? location : new Point(0, 0));
    setLocation(new Point(screenBounds.x + (screenBounds.width - getWidth()) / 2, screenBounds.y + (screenBounds.height - getHeight()) / 3));

    projectManager.addProjectManagerListener(new ProjectManagerAdapter() {
      @Override
      public void projectOpened(Project project) {
        Disposer.dispose(FlatWelcomeFrame.this);
      }
    }, this);

    myBalloonLayout = new WelcomeDesktopBalloonLayoutImpl(rootPane, JBUI.insets(8), screen.getMainWelcomePanel().myEventListener, screen.getMainWelcomePanel().myEventLocation);

    setupCloseAction(this);
    MnemonicHelper.init(this);
    Disposer.register(application, this);
  }

  void setupCloseAction(final JFrame frame) {
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        saveLocation(frame.getBounds());

        frame.dispose();

        if (myProjectManager.getOpenProjects().length == 0) {
          myApplication.exit();
        }
      }
    });
  }

  public void saveLocation(Rectangle location) {
    Point middle = new Point(location.x + location.width / 2, location.y = location.height / 2);
    myDimensionService.setLocationNoRealKey(WelcomeFrameManager.DIMENSION_KEY, middle);
  }

  public void setDefaultTitle() {
    setTitle(FrameTitleUtil.buildTitle(myUpdateSettings));
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

    myWelcomeFrameHelper.resetInstance();

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
    return myProjectManager.getDefaultProject();
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
  public void setContent(@Nonnull Component content) {
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
  public void addBorder(@Nonnull BorderPosition borderPosition, @Nonnull BorderStyle borderStyle, ColorKey colorKey, int width) {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void removeBorder(@Nonnull BorderPosition borderPosition) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    throw new UnsupportedOperationException();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public <T> Disposable addUserDataProvider(@Nonnull Key<T> key, @Nonnull Supplier<T> supplier) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Disposable addUserDataProvider(@Nonnull Function<Key<?>, Object> function) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public <T extends EventListener> T getListenerDispatcher(@Nonnull Class<T> eventClass) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public <T extends EventListener> Disposable addListener(@Nonnull Class<T> eventClass, @Nonnull T listener) {
    throw new UnsupportedOperationException();
  }

  @Nullable
  @Override
  public <T> T getUserData(@Nonnull Key<T> key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> void putUserData(@Nonnull Key<T> key, @Nullable T value) {
    throw new UnsupportedOperationException();
  }
  // endregion
}
