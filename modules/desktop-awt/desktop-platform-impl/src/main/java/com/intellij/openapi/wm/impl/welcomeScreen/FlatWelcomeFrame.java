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
import com.intellij.openapi.MnemonicHelper;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.WindowStateService;
import com.intellij.openapi.wm.IdeRootPaneNorthExtension;
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl;
import com.intellij.ui.AppUIUtil;
import com.intellij.ui.BalloonLayout;
import com.intellij.ui.DesktopBalloonLayoutImpl;
import com.intellij.ui.ScreenUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.accessibility.AccessibleContextAccessor;
import consulo.application.impl.FrameTitleUtil;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.welcomeScreen.FlatWelcomeScreen;
import consulo.start.WelcomeFrameManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.decorator.SwingUIDecorator;
import consulo.ui.desktop.internal.window.JFrameAsUIWindow;
import consulo.ui.Rectangle2D;

import javax.accessibility.AccessibleContext;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Konstantin Bulenkov
 */
public class FlatWelcomeFrame extends JFrameAsUIWindow implements Disposable, AccessibleContextAccessor, UISettingsListener {
  private final Runnable myClearInstance;
  private BalloonLayout myBalloonLayout;
  private boolean myDisposed;

  @RequiredUIAccess
  public FlatWelcomeFrame(Runnable clearInstance) {
    myClearInstance = clearInstance;
    final JRootPane rootPane = getRootPane();
    FlatWelcomeScreen screen = new FlatWelcomeScreen(this);

    final IdeGlassPaneImpl glassPane = new IdeGlassPaneImpl(rootPane);

    setGlassPane(glassPane);
    glassPane.setVisible(false);
    //setUndecorated(true);
    setContentPane(screen);
    setDefaultTitle();
    AppUIUtil.updateWindowIcon(this);
    SwingUIDecorator.apply(SwingUIDecorator::decorateWindowTitle, rootPane);
    setSize(TargetAWT.to(WelcomeFrameManager.getDefaultWindowSize()));
    setResizable(false);
    Point location = WindowStateService.getInstance().getLocation(WelcomeFrameManager.DIMENSION_KEY);
    Rectangle screenBounds = ScreenUtil.getScreenRectangle(location != null ? location : new Point(0, 0));
    setLocation(new Point(screenBounds.x + (screenBounds.width - getWidth()) / 2, screenBounds.y + (screenBounds.height - getHeight()) / 3));

    myBalloonLayout = new WelcomeDesktopBalloonLayoutImpl(rootPane, JBUI.insets(8), screen.getMainWelcomePanel().myEventListener, screen.getMainWelcomePanel().myEventLocation);

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
          Application.get().exit();
        }
      }
    });
  }

  public static void saveLocation(Rectangle location) {
    Point middle = new Point(location.x + location.width / 2, location.y = location.height / 2);
    WindowStateService.getInstance().putLocation(WelcomeFrameManager.DIMENSION_KEY, middle);
  }

  public void setDefaultTitle() {
    setTitle(FrameTitleUtil.buildTitle());
  }

  @Override
  public void setVisible(boolean value) {
    if (myDisposed) {
      throw new IllegalArgumentException("Already disposed");
    }

    super.setVisible(value);

    if (!value) {
      Disposer.dispose(this);
    }
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

    myClearInstance.run();
  }

  @Override
  public AccessibleContext getCurrentAccessibleContext() {
    return accessibleContext;
  }

  public BalloonLayout getBalloonLayout() {
    return myBalloonLayout;
  }

  public Rectangle2D suggestChildFrameBounds() {
    return TargetAWT.from(getBounds());
  }

  @Nullable
  public Project getProject() {
    return ProjectManager.getInstance().getDefaultProject();
  }

  public void setFrameTitle(String title) {
    setTitle(title);
  }

  public IdeRootPaneNorthExtension getNorthExtension(String key) {
    return null;
  }

  public JComponent getComponent() {
    return getRootPane();
  }

  @Override
  public void uiSettingsChanged(UISettings source) {
    SwingUIDecorator.apply(SwingUIDecorator::decorateWindowTitle, getRootPane());
  }
}