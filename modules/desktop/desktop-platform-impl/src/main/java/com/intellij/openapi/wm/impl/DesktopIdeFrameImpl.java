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
package com.intellij.openapi.wm.impl;

import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.DataManager;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.MnemonicHelper;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.impl.MouseGestureManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.IdeRootPaneNorthExtension;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.ex.LayoutFocusTraversalPolicyExt;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsActionGroup;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomeFrame;
import com.intellij.ui.*;
import com.intellij.ui.mac.MacMainFrameDecorator;
import com.intellij.util.Alarm;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.AccessibleContextAccessor;
import consulo.actionSystem.ex.TopApplicationMenuUtil;
import consulo.application.impl.FrameTitleUtil;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.Rectangle2D;
import consulo.ui.UIAccess;
import consulo.ui.desktop.internal.window.JFrameAsUIWindow;
import consulo.util.dataholder.Key;

import javax.accessibility.AccessibleContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class DesktopIdeFrameImpl implements IdeFrameEx, AccessibleContextAccessor {
  private class MyFrame extends JFrameAsUIWindow {
    protected class AccessibleIdeFrameImpl extends AccessibleJFrame {
      @Override
      public String getAccessibleName() {
        final StringBuilder builder = new StringBuilder();

        if (myProject != null) {
          builder.append(myProject.getName());
          builder.append(" - ");
        }
        builder.append(FrameTitleUtil.buildTitle());
        return builder.toString();
      }
    }

    @Override
    public void setRootPane(JRootPane root) {
      super.setRootPane(root);
    }

    @Nonnull
    @Override
    public Insets getInsets() {
      if (SystemInfo.isMac && isInFullScreen()) {
        return JBUI.emptyInsets();
      }
      return super.getInsets();
    }

    @Override
    public void show() {
      super.show();
      SwingUtilities.invokeLater(() -> setFocusableWindowState(true));
    }

    @SuppressWarnings("SSBasedInspection")
    @Override
    public void setVisible(boolean value) {
      super.setVisible(value);

      if (value && myRestoreFullScreen) {
        SwingUtilities.invokeLater(() -> {
          toggleFullScreen(true);
          if (SystemInfo.isMacOSLion) {
            setBounds(ScreenUtil.getScreenRectangle(getLocationOnScreen()));
          }
          myRestoreFullScreen = false;
        });
      }
    }

    @Override
    public void setTitle(final String title) {
      if (myUpdatingTitle) {
        super.setTitle(title);
      }
      else {
        myTitle = title;
      }

      updateTitle();
    }

    @Override
    public void paint(@Nonnull Graphics g) {
      UISettings.setupAntialiasing(g);
      //noinspection Since15
      super.paint(g);
    }

    @Override
    public Color getBackground() {
      return super.getBackground();
    }

    @Override
    public void doLayout() {
      super.doLayout();
    }

    @Override
    public void dispose() {
      if (SystemInfo.isMac && isInFullScreen()) {
        ((MacMainFrameDecorator)myFrameDecorator).toggleFullScreenNow();
      }
      if (isTemporaryDisposed()) {
        super.dispose();
        return;
      }
      MouseGestureManager.getInstance().remove(DesktopIdeFrameImpl.this);

      if (myBalloonLayout != null) {
        ((DesktopBalloonLayoutImpl)myBalloonLayout).dispose();
        myBalloonLayout = null;
      }

      // clear both our and swing hard refs
      if (myRootPane != null) {
        if (ApplicationManager.getApplication().isUnitTestMode()) {
          myRootPane.removeNotify();
        }
        setRootPane(new JRootPane());
        Disposer.dispose(myRootPane);
        myRootPane = null;
      }

      if (myFrameDecorator != null) {
        Disposer.dispose(myFrameDecorator);
        myFrameDecorator = null;
      }

      if (myWindowsBorderUpdater != null) {
        Toolkit.getDefaultToolkit().removePropertyChangeListener("win.xpstyle.themeActive", myWindowsBorderUpdater);
        myWindowsBorderUpdater = null;
      }

      super.dispose();
    }

    @Override
    public AccessibleContext getAccessibleContext() {
      if (accessibleContext == null) {
        accessibleContext = new AccessibleIdeFrameImpl();
      }
      return accessibleContext;
    }


    public void setTitleWithoutCheck(String title) {
      super.setTitle(title);
    }

    public AccessibleContext getAccessibleContextWithoutInitialization() {
      return accessibleContext;
    }
  }

  private static final class TitleBuilder {
    public StringBuilder sb = new StringBuilder();

    public TitleBuilder append(@Nullable final String s) {
      if (s == null || s.isEmpty()) return this;
      if (sb.length() > 0) sb.append(" - ");
      sb.append(s);
      return this;
    }
  }

  private static final Logger LOG = Logger.getInstance(DesktopIdeFrameImpl.class);

  private static final String FULL_SCREEN = "FullScreen";

  private static boolean myUpdatingTitle;

  private String myTitle;
  private String myFileTitle;
  private File myCurrentFile;

  private Project myProject;

  private IdeRootPane myRootPane;
  private BalloonLayout myBalloonLayout;
  private IdeFrameDecorator myFrameDecorator;
  private PropertyChangeListener myWindowsBorderUpdater;
  private boolean myRestoreFullScreen;

  private MyFrame myJFrame;

  public DesktopIdeFrameImpl(ActionManager actionManager, DataManager dataManager, Application application) {
    myJFrame = new MyFrame();
    myJFrame.toUIWindow().putUserData(IdeFrame.KEY, this);
    myJFrame.toUIWindow().addUserDataProvider(key -> getData(key));

    myJFrame.setTitle(FrameTitleUtil.buildTitle());
    myRootPane = createRootPane(actionManager, dataManager, application);
    myJFrame.setRootPane(myRootPane);
    myJFrame.setBackground(UIUtil.getPanelBackground());
    AppUIUtil.updateWindowIcon(myJFrame);
    final Dimension size = ScreenUtil.getMainScreenBounds().getSize();

    size.width = Math.min(1400, size.width - 20);
    size.height = Math.min(1000, size.height - 40);

    myJFrame.setSize(size);
    myJFrame.setLocationRelativeTo(null);

    LayoutFocusTraversalPolicyExt layoutFocusTraversalPolicy = new LayoutFocusTraversalPolicyExt();
    myJFrame.setFocusTraversalPolicy(layoutFocusTraversalPolicy);

    setupCloseAction();
    MnemonicHelper.init(myJFrame);

    myBalloonLayout = new DesktopBalloonLayoutImpl(myRootPane, new Insets(8, 8, 8, 8));

    // to show window thumbnail under Macs
    // http://lists.apple.com/archives/java-dev/2009/Dec/msg00240.html
    if (SystemInfo.isMac) myJFrame.setIconImage(null);

    MouseGestureManager.getInstance().add(this);

    myFrameDecorator = IdeFrameDecorator.decorate(this);

    myJFrame.addWindowStateListener(new WindowAdapter() {
      @Override
      public void windowStateChanged(WindowEvent e) {
        updateBorder();
      }
    });

    if (SystemInfo.isWindows) {
      myWindowsBorderUpdater = __ -> updateBorder();
      Toolkit.getDefaultToolkit().addPropertyChangeListener("win.xpstyle.themeActive", myWindowsBorderUpdater);
      if (!SystemInfo.isJavaVersionAtLeast(8, 0, 0)) {
        final Ref<Dimension> myDimensionRef = new Ref<>(new Dimension());
        final Alarm alarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD);
        final Runnable runnable = new Runnable() {
          @Override
          public void run() {
            if (myJFrame.isDisplayable() && !myJFrame.getSize().equals(myDimensionRef.get())) {
              Rectangle bounds = myJFrame.getBounds();
              bounds.width--;
              myJFrame.setBounds(bounds);
              bounds.width++;
              myJFrame.setBounds(bounds);
              myDimensionRef.set(myJFrame.getSize());
            }
            alarm.addRequest(this, 50);
          }
        };
        alarm.addRequest(runnable, 50);
      }
    }
    // UIUtil.suppressFocusStealing();
  }

  private void updateBorder() {
    int state = myJFrame.getExtendedState();
    if (!WindowManager.getInstance().isFullScreenSupportedInCurrentOS() || !SystemInfo.isWindows || myRootPane == null) {
      return;
    }

    myRootPane.setBorder(null);
    boolean isNotClassic = Boolean.parseBoolean(String.valueOf(Toolkit.getDefaultToolkit().getDesktopProperty("win.xpstyle.themeActive")));
    if (isNotClassic && (state & JFrame.MAXIMIZED_BOTH) != 0) {
      IdeFrame[] projectFrames = WindowManager.getInstance().getAllProjectFrames();
      GraphicsDevice device = ScreenUtil.getScreenDevice(myJFrame.getBounds());

      for (IdeFrame frame : projectFrames) {
        if (frame == this) continue;

        IdeFrameEx ideFrameEx = (IdeFrameEx)frame;
        if (ideFrameEx.isInFullScreen() && ScreenUtil.getScreenDevice(TargetAWT.to(ideFrameEx.getWindow()).getBounds()) == device) {
          Insets insets = ScreenUtil.getScreenInsets(device.getDefaultConfiguration());
          int mask = SideBorder.NONE;
          if (insets.top != 0) mask |= SideBorder.TOP;
          if (insets.left != 0) mask |= SideBorder.LEFT;
          if (insets.bottom != 0) mask |= SideBorder.BOTTOM;
          if (insets.right != 0) mask |= SideBorder.RIGHT;
          myRootPane.setBorder(new SideBorder(JBColor.BLACK, mask, 3));
          break;
        }
      }
    }
  }

  protected IdeRootPane createRootPane(ActionManager actionManager, DataManager dataManager, Application application) {
    return new IdeRootPane(actionManager, dataManager, application, this);
  }

  @Override
  public JComponent getComponent() {
    return myJFrame.getRootPane();
  }

  @Nonnull
  @Override
  public consulo.ui.Window getWindow() {
    return myJFrame.toUIWindow();
  }

  private void setupCloseAction() {
    myJFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myJFrame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(@Nonnull final WindowEvent e) {
        if (isTemporaryDisposed()) return;

        final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        if (openProjects.length > 1 || openProjects.length == 1 && TopApplicationMenuUtil.isMacSystemMenu) {
          if (myProject != null && myProject.isOpen()) {
            ProjectUtil.closeAndDispose(myProject);
          }
          ApplicationManager.getApplication().getMessageBus().syncPublisher(AppLifecycleListener.TOPIC).projectFrameClosed();
          WelcomeFrame.showIfNoProjectOpened();
        }
        else {
          Application.get().exit();
        }
      }
    });
  }

  @Override
  public StatusBar getStatusBar() {
    return myRootPane == null ? null : myRootPane.getStatusBar();
  }

  @Override
  public void setFrameTitle(final String text) {
    myJFrame.setTitleWithoutCheck(text);
  }

  @Override
  public void setFileTitle(@Nullable final String fileTitle, @Nullable File file) {
    myFileTitle = fileTitle;
    myCurrentFile = file;
    updateTitle();
  }

  @Override
  public IdeRootPaneNorthExtension getNorthExtension(String key) {
    return myRootPane.findByName(key);
  }

  private void updateTitle() {
    updateTitle(myJFrame, myTitle, myFileTitle, myCurrentFile);
  }

  public static void updateTitle(JFrame frame, final String title, final String fileTitle, final File currentFile) {
    if (myUpdatingTitle) return;

    try {
      myUpdatingTitle = true;

      frame.getRootPane().putClientProperty("Window.documentFile", currentFile);

      final String applicationName = FrameTitleUtil.buildTitle();
      final TitleBuilder titleBuilder = new TitleBuilder();
      if (SystemInfo.isMac) {
        boolean addAppName = StringUtil.isEmpty(title) || ProjectManager.getInstance().getOpenProjects().length == 0;
        titleBuilder.append(fileTitle).append(title).append(addAppName ? applicationName : null);
      }
      else {
        titleBuilder.append(title).append(fileTitle).append(applicationName);
      }

      frame.setTitle(titleBuilder.sb.toString());
    }
    finally {
      myUpdatingTitle = false;
    }
  }

  @Override
  public void updateView() {
    ((IdeRootPane)myJFrame.getRootPane()).updateToolbar();
    ((IdeRootPane)myJFrame.getRootPane()).updateMainMenuActions();
    ((IdeRootPane)myJFrame.getRootPane()).updateNorthComponents();
  }

  @Override
  public AccessibleContext getCurrentAccessibleContext() {
    return myJFrame.getAccessibleContextWithoutInitialization();
  }

  private Object getData(@Nonnull Key<?> dataId) {
    if (CommonDataKeys.PROJECT == dataId) {
      if (myProject != null) {
        return myProject.isInitialized() ? myProject : null;
      }
    }

    if (IdeFrame.KEY == dataId) {
      return this;
    }

    return null;
  }

  public void setProject(final Project project) {
    if (WindowManager.getInstance().isFullScreenSupportedInCurrentOS() && myProject != project && project != null) {
      myRestoreFullScreen = myProject == null && shouldRestoreFullScreen(project);

      if (myProject != null) {
        storeFullScreenStateIfNeeded(false); // disable for old project
      }
    }

    myProject = project;
    if (project != null) {
      ProjectFrameBounds.getInstance(project);   // make sure the service is initialized and its state will be saved
      if (myRootPane != null) {
        myRootPane.installNorthComponents(project);
        project.getMessageBus().connect().subscribe(StatusBar.Info.TOPIC, myRootPane.getStatusBar());
      }

      installDefaultProjectStatusBarWidgets(myProject);
    }
    else {
      if (myRootPane != null) { //already disposed
        myRootPane.deinstallNorthComponents();
      }
    }

    if (myJFrame.isVisible() && myRestoreFullScreen) {
      toggleFullScreen(true);
      myRestoreFullScreen = false;
    }
  }

  private void installDefaultProjectStatusBarWidgets(@Nonnull final Project project) {
    project.getInstance(StatusBarWidgetsManager.class).updateAllWidgets(UIAccess.current());
    
    JComponent component = Objects.requireNonNull(getStatusBar()).getComponent();
    PopupHandler.installPopupHandler(component, StatusBarWidgetsActionGroup.GROUP_ID, ActionPlaces.STATUS_BAR_PLACE);
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  private boolean isTemporaryDisposed() {
    return myRootPane != null && myRootPane.getClientProperty(ScreenUtil.DISPOSE_TEMPORARY) != null;
  }

  @Override
  public void storeFullScreenStateIfNeeded() {
    if (myFrameDecorator != null) {
      storeFullScreenStateIfNeeded(myFrameDecorator.isInFullScreen());
    }
  }

  @Override
  public void storeFullScreenStateIfNeeded(boolean state) {
    if (!WindowManager.getInstance().isFullScreenSupportedInCurrentOS()) return;

    if (myProject != null) {
      PropertiesComponent.getInstance(myProject).setValue(FULL_SCREEN, state);
      myJFrame.doLayout();
    }
  }

  private static boolean shouldRestoreFullScreen(@Nullable Project project) {
    return WindowManager.getInstance().isFullScreenSupportedInCurrentOS() &&
           project != null &&
           (SHOULD_OPEN_IN_FULL_SCREEN.get(project) == Boolean.TRUE || PropertiesComponent.getInstance(project).getBoolean(FULL_SCREEN));
  }

  @Override
  public Rectangle2D suggestChildFrameBounds() {
    //todo [kirillk] a dummy implementation
    final Rectangle b = myJFrame.getBounds();
    b.x += 100;
    b.width -= 200;
    b.y += 100;
    b.height -= 200;
    return TargetAWT.from(b);
  }

  @Override
  public final BalloonLayout getBalloonLayout() {
    return myBalloonLayout;
  }

  @Override
  public boolean isInFullScreen() {
    return myFrameDecorator != null && myFrameDecorator.isInFullScreen();
  }

  @Nonnull
  @Override
  public ActionCallback toggleFullScreen(boolean state) {

    if (temporaryFixForIdea156004(state)) return ActionCallback.DONE;

    if (myFrameDecorator != null) {
      return myFrameDecorator.toggleFullScreen(state);
    }
    IdeFrame[] frames = WindowManager.getInstance().getAllProjectFrames();
    for (IdeFrame frame : frames) {
      ((DesktopIdeFrameImpl)frame).updateBorder();
    }

    return ActionCallback.DONE;
  }

  private boolean temporaryFixForIdea156004(final boolean state) {
    if (SystemInfo.isMac) {
      try {
        Field modalBlockerField = Window.class.getDeclaredField("modalBlocker");
        modalBlockerField.setAccessible(true);
        final Window modalBlocker = (Window)modalBlockerField.get(myJFrame);
        if (modalBlocker != null) {
          ApplicationManager.getApplication().invokeLater(() -> toggleFullScreen(state), ModalityState.NON_MODAL);
          return true;
        }
      }
      catch (NoSuchFieldException | IllegalAccessException e) {
        LOG.error(e);
      }
    }
    return false;
  }
}
