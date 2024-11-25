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
package consulo.desktop.awt.welcomeScreen;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ui.ApplicationWindowStateService;
import consulo.desktop.awt.ui.impl.window.JFrameAsUIWindow;
import consulo.desktop.awt.ui.util.AppIconUtil;
import consulo.desktop.awt.uiOld.DesktopBalloonLayoutImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.application.FrameTitleUtil;
import consulo.ide.impl.idea.openapi.wm.impl.IdeGlassPaneImpl;
import consulo.ide.impl.idea.util.ui.accessibility.AccessibleContextAccessor;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.ui.wm.BalloonLayout;
import consulo.project.ui.wm.IdeRootPaneNorthExtension;
import consulo.project.ui.wm.WelcomeFrameManager;
import consulo.ui.Coordinate2D;
import consulo.ui.Rectangle2D;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.MnemonicHelper;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nullable;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Konstantin Bulenkov
 */
public class FlatWelcomeFrame extends JFrameAsUIWindow implements Disposable, AccessibleContextAccessor {
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
    AppIconUtil.updateWindowIcon(this);
    setSize(TargetAWT.to(WelcomeFrameManager.getDefaultWindowSize()));
    setResizable(false);
    Point location = TargetAWT.to(ApplicationWindowStateService.getInstance().getLocation(WelcomeFrameManager.DIMENSION_KEY));
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
    Coordinate2D middle = new Coordinate2D(location.x + location.width / 2, location.y = location.height / 2);
    ApplicationWindowStateService.getInstance().putLocation(WelcomeFrameManager.DIMENSION_KEY, middle);
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
}