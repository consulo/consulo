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
import com.intellij.ide.GeneralSettings;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManagerListener;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.ScreenUtil;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.sun.jna.platform.WindowUtils;
import consulo.awt.TargetAWT;
import consulo.awt.hacking.AWTAccessorHacking;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.start.WelcomeFrameManager;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
@Singleton
@State(name = WindowManagerEx.ID, storages = @Storage(value = "window.manager.xml", roamingType = RoamingType.DISABLED), defaultStateFilePath = "/defaultState/WindowManager.xml")
public final class DesktopWindowManagerImpl extends WindowManagerEx implements PersistentStateComponent<Element> {
  private static final Logger LOG = Logger.getInstance(DesktopWindowManagerImpl.class);

  @NonNls
  private static final String FOCUSED_WINDOW_PROPERTY_NAME = "focusedWindow";
  @NonNls
  private static final String X_ATTR = "x";
  @NonNls
  private static final String FRAME_ELEMENT = "frame";
  @NonNls
  private static final String Y_ATTR = "y";
  @NonNls
  private static final String WIDTH_ATTR = "width";
  @NonNls
  private static final String HEIGHT_ATTR = "height";
  @NonNls
  private static final String EXTENDED_STATE_ATTR = "extended-state";

  static {
    try {
      System.loadLibrary("jawt");
    }
    catch (Throwable t) {
      LOG.info("jawt failed to load", t);
    }
  }

  private Boolean myAlphaModeSupported = null;

  private final EventDispatcher<WindowManagerListener> myEventDispatcher = EventDispatcher.create(WindowManagerListener.class);

  private final DesktopWindowWatcher myWindowWatcher;
  /**
   * That is the default layout.
   */
  private final ToolWindowLayout myLayout;

  private final HashMap<Project, DesktopIdeFrameImpl> myProject2Frame;

  private final HashMap<Project, Set<JDialog>> myDialogsToDispose;

  /**
   * This members is needed to read frame's bounds from XML.
   * <code>myFrameBounds</code> can be <code>null</code>.
   */
  private Rectangle myFrameBounds;
  private int myFrameExtendedState;
  private final WindowAdapter myActivationListener;
  private final DataManager myDataManager;
  private final ActionManager myActionManager;

  @Inject
  public DesktopWindowManagerImpl(Application application, DataManager dataManager, ActionManager actionManager) {
    myDataManager = dataManager;
    myActionManager = actionManager;

    if (!application.isUnitTestMode()) {
      Disposer.register(application, () -> disposeRootFrame());
    }

    myWindowWatcher = new DesktopWindowWatcher(application);
    final KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    keyboardFocusManager.addPropertyChangeListener(FOCUSED_WINDOW_PROPERTY_NAME, myWindowWatcher);
    myLayout = new ToolWindowLayout();
    myProject2Frame = new HashMap<>();
    myDialogsToDispose = new HashMap<>();
    myFrameExtendedState = Frame.NORMAL;

    myActivationListener = new WindowAdapter() {
      @Override
      public void windowActivated(WindowEvent e) {
        consulo.ui.Window activeWindow = TargetAWT.from(e.getWindow());

        IdeFrame ideFrame = activeWindow.getUserData(IdeFrame.KEY);
        if (ideFrame != null) {
          proceedDialogDisposalQueue(ideFrame.getProject());
        }
      }
    };

    application.getMessageBus().connect().subscribe(AppLifecycleListener.TOPIC, new AppLifecycleListener() {
      @Override
      public void appClosing() {
        // save full screen window states
        if (isFullScreenSupportedInCurrentOS() && GeneralSettings.getInstance().isReopenLastProject()) {
          Project[] openProjects = ProjectManager.getInstance().getOpenProjects();

          if (openProjects.length > 0) {
            WindowManagerEx wm = WindowManagerEx.getInstanceEx();
            for (Project project : openProjects) {
              IdeFrameEx frame = wm.getIdeFrame(project);
              if (frame != null) {
                frame.storeFullScreenStateIfNeeded();
              }
            }
          }
        }
      }
    });

    if (UIUtil.hasLeakingAppleListeners()) {
      UIUtil.addAwtListener(new AWTEventListener() {
        @Override
        public void eventDispatched(AWTEvent event) {
          if (event.getID() == ContainerEvent.COMPONENT_ADDED) {
            if (((ContainerEvent)event).getChild() instanceof JViewport) {
              UIUtil.removeLeakingAppleListeners();
            }
          }
        }
      }, AWTEvent.CONTAINER_EVENT_MASK, application);
    }
  }

  @Override
  @Nonnull
  public DesktopIdeFrameImpl[] getAllProjectFrames() {
    final Collection<DesktopIdeFrameImpl> ideFrames = myProject2Frame.values();
    return ideFrames.toArray(new DesktopIdeFrameImpl[ideFrames.size()]);
  }

  @Nullable
  @Override
  public consulo.ui.Window findVisibleWindow() {
    IdeFrame[] frames = getAllProjectFrames();
    if (frames.length > 0) {
      return frames[0].getWindow();
    }
    IdeFrame desktopIdeFrame = WelcomeFrameManager.getInstance().getCurrentFrame();
    if (desktopIdeFrame == null) {
      throw new UnsupportedOperationException("Welcome frame not showing. Possible bug?");
    }
    return desktopIdeFrame.getWindow();
  }

  @Override
  public void addListener(final WindowManagerListener listener) {
    myEventDispatcher.addListener(listener);
  }

  @Override
  public void removeListener(final WindowManagerListener listener) {
    myEventDispatcher.removeListener(listener);
  }

  @Override
  public final Rectangle getScreenBounds() {
    return ScreenUtil.getAllScreensRectangle();
  }

  @Override
  public Rectangle getScreenBounds(@Nonnull Project project) {
    final GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
    final Point onScreen = getFrame(project).getLocationOnScreen();
    final GraphicsDevice[] devices = environment.getScreenDevices();
    for (final GraphicsDevice device : devices) {
      final Rectangle bounds = device.getDefaultConfiguration().getBounds();
      if (bounds.contains(onScreen)) {
        return bounds;
      }
    }

    return null;
  }

  @Override
  public final boolean isInsideScreenBounds(final int x, final int y, final int width) {
    return ScreenUtil.getAllScreensShape().contains(x, y, width, 1);
  }

  @Override
  public final boolean isInsideScreenBounds(final int x, final int y) {
    return ScreenUtil.getAllScreensShape().contains(x, y);
  }

  @Override
  public final boolean isAlphaModeSupported() {
    if (myAlphaModeSupported == null) {
      myAlphaModeSupported = calcAlphaModelSupported();
    }
    return myAlphaModeSupported.booleanValue();
  }

  private static boolean calcAlphaModelSupported() {
    GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)) {
      return true;
    }
    try {
      return WindowUtils.isWindowAlphaSupported();
    }
    catch (Throwable e) {
      return false;
    }
  }

  static boolean isTranslucencySupported() {
    GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    return device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT);
  }

  static boolean isPerPixelTransparencySupported() {
    GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    return device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT);
  }

  @Override
  public final void setAlphaModeRatio(final Window window, final float ratio) {
    if (!window.isDisplayable() || !window.isShowing()) {
      throw new IllegalArgumentException("window must be displayable and showing. window=" + window);
    }
    if (ratio < 0.0f || ratio > 1.0f) {
      throw new IllegalArgumentException("ratio must be in [0..1] range. ratio=" + ratio);
    }
    if (!isAlphaModeSupported() || !isAlphaModeEnabled(window)) {
      return;
    }

    setAlphaMode(window, ratio);
  }

  private static void setAlphaMode(Window window, float ratio) {
    try {
      if (SystemInfo.isMacOSLeopard) {
        if (window instanceof JWindow) {
          ((JWindow)window).getRootPane().putClientProperty("Window.alpha", 1.0f - ratio);
        }
        else if (window instanceof JDialog) {
          ((JDialog)window).getRootPane().putClientProperty("Window.alpha", 1.0f - ratio);
        }
        else if (window instanceof JFrame) {
          ((JFrame)window).getRootPane().putClientProperty("Window.alpha", 1.0f - ratio);
        }
      }
      else if (isTranslucencySupported()) {
        window.setOpacity(1.0f - ratio);
      }
      else {
        WindowUtils.setWindowAlpha(window, 1.0f - ratio);
      }
    }
    catch (Throwable e) {
      LOG.debug(e);
    }
  }

  @Override
  public void setWindowMask(final Window window, @Nullable final Shape mask) {
    try {
      if (isPerPixelTransparencySupported()) {
        window.setShape(mask);
      }
      else {
        WindowUtils.setWindowMask(window, mask);
      }
    }
    catch (Throwable e) {
      LOG.debug(e);
    }
  }

  @Override
  public void setWindowShadow(Window window, WindowShadowMode mode) {
    if (window instanceof JWindow) {
      JRootPane root = ((JWindow)window).getRootPane();
      root.putClientProperty("Window.shadow", mode == WindowShadowMode.DISABLED ? Boolean.FALSE : Boolean.TRUE);
      root.putClientProperty("Window.style", mode == WindowShadowMode.SMALL ? "small" : null);
    }
  }

  @Override
  public void resetWindow(final Window window) {
    try {
      if (!isAlphaModeSupported()) return;

      setWindowMask(window, null);
      setAlphaMode(window, 0f);
      setWindowShadow(window, WindowShadowMode.NORMAL);
    }
    catch (Throwable e) {
      LOG.debug(e);
    }
  }

  @Override
  public final boolean isAlphaModeEnabled(final Window window) {
    if (!window.isDisplayable() || !window.isShowing()) {
      throw new IllegalArgumentException("window must be displayable and showing. window=" + window);
    }
    return isAlphaModeSupported();
  }

  @Override
  public final void setAlphaModeEnabled(final Window window, final boolean state) {
    if (!window.isDisplayable() || !window.isShowing()) {
      throw new IllegalArgumentException("window must be displayable and showing. window=" + window);
    }
  }

  @Override
  public void hideDialog(JDialog dialog, Project project) {
    if (project == null) {
      dialog.dispose();
    }
    else {
      IdeFrameEx frame = getIdeFrame(project);
      if (frame.isActive()) {
        dialog.dispose();
      }
      else {
        queueForDisposal(dialog, project);
        dialog.setVisible(false);
      }
    }
  }

  @Override
  public void adjustContainerWindow(Component c, Dimension oldSize, Dimension newSize) {
    if (c == null) return;

    Window wnd = SwingUtilities.getWindowAncestor(c);

    if (wnd instanceof JWindow) {
      JBPopup popup = (JBPopup)((JWindow)wnd).getRootPane().getClientProperty(JBPopup.KEY);
      if (popup != null) {
        if (oldSize.height < newSize.height) {
          Dimension size = popup.getSize();
          size.height += newSize.height - oldSize.height;
          popup.setSize(size);
          popup.moveToFitScreen();
        }
      }
    }
  }

  @Override
  public final void doNotSuggestAsParent(final consulo.ui.Window window) {
    myWindowWatcher.doNotSuggestAsParent(window);
  }

  @Override
  public final void dispatchComponentEvent(final ComponentEvent e) {
    myWindowWatcher.dispatchComponentEvent(e);
  }

  @Override
  @Nullable
  public consulo.ui.Window suggestParentWindow(@Nullable final Project project) {
    return myWindowWatcher.suggestParentWindow(project);
  }

  @Override
  public final StatusBar getStatusBar(final Project project) {
    if (!myProject2Frame.containsKey(project)) {
      return null;
    }
    final IdeFrameEx frame = getIdeFrame(project);
    LOG.assertTrue(frame != null);
    return frame.getStatusBar();
  }

  @Override
  public StatusBar getStatusBar(@Nonnull Component c) {
    return getStatusBar(c, null);
  }

  @Override
  public StatusBar getStatusBar(@Nonnull consulo.ui.Component c, @Nullable Project project) {
    return getStatusBar(TargetAWT.to(c), project);
  }

  @Override
  public StatusBar getStatusBar(@Nonnull Component c, @Nullable Project project) {
    Component parent = UIUtil.findUltimateParent(c);
    if (parent instanceof Window) {
      consulo.ui.Window uiWindow = TargetAWT.from((Window)parent);

      IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
      if (ideFrame != null) {
        StatusBar statusBar = ideFrame.getStatusBar();
        if (statusBar != null) return statusBar.findChild(c);
      }
    }

    IdeFrame frame = findFrameFor(project);
    if (frame != null) {
      StatusBar statusBar = frame.getStatusBar();
      if (statusBar != null) {
        return statusBar.findChild(c);
      }
    }

    assert false : "Cannot find status bar for " + c;

    return null;
  }

  @RequiredUIAccess
  @Override
  public IdeFrame findFrameFor(@Nullable final Project project) {
    IdeFrame frame = null;
    if (project != null) {
      frame = project.isDefault() ? WelcomeFrameManager.getInstance().getCurrentFrame() : getIdeFrame(project);
      if (frame == null) {
        frame = myProject2Frame.get(null);
      }
    }
    else {
      Container eachParent = TargetAWT.to(getMostRecentFocusedWindow());
      while (eachParent != null) {
        if (eachParent instanceof Window) {
          consulo.ui.Window uiWIndow = TargetAWT.from((Window)eachParent);
          IdeFrame ideFrame = uiWIndow.getUserData(IdeFrame.KEY);
          if (ideFrame != null) {
            frame = ideFrame;
            break;
          }
        }
        eachParent = eachParent.getParent();
      }

      if (frame == null) {
        frame = tryToFindTheOnlyFrame();
      }
    }

    return frame;
  }

  private static IdeFrame tryToFindTheOnlyFrame() {
    IdeFrame candidate = null;
    final Frame[] all = Frame.getFrames();
    for (Frame each : all) {
      consulo.ui.Window uiWindow = TargetAWT.from(each);
      if (uiWindow == null) {
        continue;
      }

      IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
      if (ideFrame != null) {
        if (candidate == null) {
          candidate = ideFrame;
        }
        else {
          candidate = null;
          break;
        }
      }
    }
    return candidate;
  }

  @Nullable
  @Override
  public consulo.ui.Window getWindow(@Nullable Project project) {
    // no assert! otherwise WindowWatcher.suggestParentWindow fails for default project
    //LOG.assertTrue(myProject2Frame.containsKey(project));
    DesktopIdeFrameImpl frame = myProject2Frame.get(project);
    if (frame != null) {
      return frame.getWindow();
    }
    return null;
  }

  @Override
  public IdeFrameEx getIdeFrame(@Nullable final Project project) {
    if (project != null) {
      return myProject2Frame.get(project);
    }
    final Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    final Component parentMaybeWindow = UIUtil.findUltimateParent(window);
    if (parentMaybeWindow instanceof Window) {
      consulo.ui.Window uiWindow = TargetAWT.from((Window)parentMaybeWindow);

      IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
      if (ideFrame instanceof IdeFrameEx) {
        return (IdeFrameEx)ideFrame;
      }
    }

    final Frame[] frames = Frame.getFrames();
    for (Frame each : frames) {
      consulo.ui.Window uiWindow = TargetAWT.from(each);
      if (uiWindow == null) {
        continue;
      }

      IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
      if (ideFrame instanceof IdeFrameEx) {
        return (IdeFrameEx)ideFrame;
      }
    }

    return null;
  }

  public void showFrame() {
    final DesktopIdeFrameImpl frame = new DesktopIdeFrameImpl(myActionManager, myDataManager, ApplicationManager.getApplication());
    myProject2Frame.put(null, frame);

    if (myFrameBounds == null || !ScreenUtil.isVisible(myFrameBounds)) { //avoid situations when IdeFrame is out of all screens
      myFrameBounds = ScreenUtil.getMainScreenBounds();
      int xOff = myFrameBounds.width / 8;
      int yOff = myFrameBounds.height / 8;
      JBInsets.removeFrom(myFrameBounds, new Insets(yOff, xOff, yOff, xOff));
    }

    JFrame jWindow = (JFrame)TargetAWT.to(frame.getWindow());

    jWindow.setBounds(myFrameBounds);
    jWindow.setExtendedState(myFrameExtendedState);
    jWindow.setVisible(true);
  }

  private DesktopIdeFrameImpl getDefaultEmptyIdeFrame() {
    return myProject2Frame.get(null);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public final IdeFrameEx allocateFrame(@Nonnull final Project project) {
    LOG.assertTrue(!myProject2Frame.containsKey(project));

    JFrame jFrame;
    final DesktopIdeFrameImpl ideFrame;
    if (myProject2Frame.containsKey(null)) {
      ideFrame = getDefaultEmptyIdeFrame();
      myProject2Frame.remove(null);
      myProject2Frame.put(project, ideFrame);
      ideFrame.setProject(project);
      jFrame = (JFrame)TargetAWT.to(ideFrame.getWindow());
    }
    else {
      ideFrame = new DesktopIdeFrameImpl(myActionManager, myDataManager, ApplicationManager.getApplication());

      jFrame = (JFrame)TargetAWT.to(ideFrame.getWindow());

      final Rectangle bounds = ProjectFrameBounds.getInstance(project).getBounds();

      if (bounds != null) {
        myFrameBounds = bounds;
      }

      if (myFrameBounds != null) {
        jFrame.setBounds(myFrameBounds);
      }
      myProject2Frame.put(project, ideFrame);
      ideFrame.setProject(project);
      jFrame.setExtendedState(myFrameExtendedState);
      jFrame.setVisible(true);
    }

    jFrame.addWindowListener(myActivationListener);
    jFrame.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentMoved(@Nonnull ComponentEvent e) {
        updateFrameBounds(jFrame, ideFrame);
      }
    });
    myEventDispatcher.getMulticaster().frameCreated(ideFrame);

    return ideFrame;
  }

  private void proceedDialogDisposalQueue(Project project) {
    Set<JDialog> dialogs = myDialogsToDispose.get(project);
    if (dialogs == null) return;
    for (JDialog dialog : dialogs) {
      dialog.dispose();
    }
    myDialogsToDispose.put(project, null);
  }

  private void queueForDisposal(JDialog dialog, Project project) {
    Set<JDialog> dialogs = myDialogsToDispose.get(project);
    if (dialogs == null) {
      dialogs = new HashSet<>();
      myDialogsToDispose.put(project, dialogs);
    }
    dialogs.add(dialog);
  }

  @Override
  public final void releaseFrame(final IdeFrameEx frame) {
    DesktopIdeFrameImpl implFrame = (DesktopIdeFrameImpl)frame;

    myEventDispatcher.getMulticaster().beforeFrameReleased(implFrame);

    final Project project = implFrame.getProject();
    LOG.assertTrue(project != null);

    JFrame jFrame = (JFrame)TargetAWT.to(implFrame.getWindow());

    jFrame.removeWindowListener(myActivationListener);
    proceedDialogDisposalQueue(project);

    implFrame.setProject(null);
    jFrame.setTitle(null);
    implFrame.setFileTitle(null, null);

    myProject2Frame.remove(project);
    if (myProject2Frame.isEmpty()) {
      myProject2Frame.put(null, implFrame);
    }
    else {
      Disposer.dispose(implFrame.getStatusBar());
      jFrame.dispose();
    }
  }

  @Override
  public final void disposeRootFrame() {
    if (myProject2Frame.size() == 1) {
      final DesktopIdeFrameImpl rootFrame = myProject2Frame.remove(null);
      if (rootFrame != null) {
        // disposing last frame if quitting
        rootFrame.getWindow().dispose();
      }
    }
  }

  @Override
  public final consulo.ui.Window getMostRecentFocusedWindow() {
    return myWindowWatcher.getFocusedWindow();
  }

  @Override
  public final Component getFocusedComponent(@Nonnull final Window window) {
    return myWindowWatcher.getFocusedComponent(window);
  }

  @Override
  @Nullable
  public final Component getFocusedComponent(@Nullable final Project project) {
    return myWindowWatcher.getFocusedComponent(project);
  }

  @Override
  public void loadState(Element state) {
    final Element frameElement = state.getChild(FRAME_ELEMENT);
    if (frameElement != null) {
      myFrameBounds = loadFrameBounds(frameElement);
      try {
        myFrameExtendedState = Integer.parseInt(frameElement.getAttributeValue(EXTENDED_STATE_ATTR));
        if ((myFrameExtendedState & Frame.ICONIFIED) > 0) {
          myFrameExtendedState = Frame.NORMAL;
        }
      }
      catch (NumberFormatException ignored) {
        myFrameExtendedState = Frame.NORMAL;
      }
    }

    final Element desktopElement = state.getChild(ToolWindowLayout.TAG);
    if (desktopElement != null) {
      myLayout.readExternal(desktopElement);
    }
  }

  private static Rectangle loadFrameBounds(final Element frameElement) {
    Rectangle bounds = new Rectangle();
    try {
      bounds.x = Integer.parseInt(frameElement.getAttributeValue(X_ATTR));
    }
    catch (NumberFormatException ignored) {
      return null;
    }
    try {
      bounds.y = Integer.parseInt(frameElement.getAttributeValue(Y_ATTR));
    }
    catch (NumberFormatException ignored) {
      return null;
    }
    try {
      bounds.width = Integer.parseInt(frameElement.getAttributeValue(WIDTH_ATTR));
    }
    catch (NumberFormatException ignored) {
      return null;
    }
    try {
      bounds.height = Integer.parseInt(frameElement.getAttributeValue(HEIGHT_ATTR));
    }
    catch (NumberFormatException ignored) {
      return null;
    }
    return FrameBoundsConverter.convertFromDeviceSpace(bounds);
  }

  @Nullable
  @Override
  public Element getState() {
    Element frameState = getFrameState();
    if (frameState == null) {
      return null;
    }

    Element state = new Element("state");
    state.addContent(frameState);

    // Save default layout
    Element layoutElement = myLayout.writeExternal(ToolWindowLayout.TAG);
    if (layoutElement != null) {
      state.addContent(layoutElement);
    }
    return state;
  }

  private Element getFrameState() {
    // Save frame bounds
    final Project[] projects = ProjectManager.getInstance().getOpenProjects();
    if (projects.length == 0) {
      return null;
    }

    Project project = projects[0];
    final IdeFrameEx ideFrame = getIdeFrame(project);
    if (ideFrame == null) {
      return null;
    }

    JFrame jWindow = (JFrame)TargetAWT.to(ideFrame.getWindow());

    int extendedState = updateFrameBounds(jWindow, ideFrame);

    Rectangle rectangle = FrameBoundsConverter.convertToDeviceSpace(jWindow.getGraphicsConfiguration(), myFrameBounds);

    final Element frameElement = new Element(FRAME_ELEMENT);
    frameElement.setAttribute(X_ATTR, Integer.toString(rectangle.x));
    frameElement.setAttribute(Y_ATTR, Integer.toString(rectangle.y));
    frameElement.setAttribute(WIDTH_ATTR, Integer.toString(rectangle.width));
    frameElement.setAttribute(HEIGHT_ATTR, Integer.toString(rectangle.height));

    if (!(ideFrame.isInFullScreen() && SystemInfo.isAppleJvm)) {
      frameElement.setAttribute(EXTENDED_STATE_ATTR, Integer.toString(extendedState));
    }
    return frameElement;
  }

  private int updateFrameBounds(JFrame frame, IdeFrameEx ideFrame) {
    int extendedState = frame.getExtendedState();
    if (SystemInfo.isMacOSLion) {
      extendedState = AWTAccessorHacking.getExtendedStateFromPeer(frame);
    }
    boolean isMaximized = extendedState == Frame.MAXIMIZED_BOTH || isFullScreenSupportedInCurrentOS() && ideFrame.isInFullScreen();
    boolean usePreviousBounds = isMaximized && myFrameBounds != null && frame.getBounds().contains(new Point((int)myFrameBounds.getCenterX(), (int)myFrameBounds.getCenterY()));
    if (!usePreviousBounds) {
      myFrameBounds = frame.getBounds();
    }
    return extendedState;
  }

  @Override
  public final ToolWindowLayout getLayout() {
    return myLayout;
  }

  @Override
  public final void setLayout(final ToolWindowLayout layout) {
    myLayout.copyFrom(layout);
  }

  public DesktopWindowWatcher getWindowWatcher() {
    return myWindowWatcher;
  }

  @Override
  public boolean isFullScreenSupportedInCurrentOS() {
    return SystemInfo.isMacOSLion || SystemInfo.isWindows || SystemInfo.isXWindow && X11UiUtil.isFullScreenSupported();
  }

  @Override
  public boolean isFloatingMenuBarSupported() {
    return !SystemInfo.isMac && getInstance().isFullScreenSupportedInCurrentOS();
  }

  /**
   * Converts the frame bounds b/w the user space (JRE-managed HiDPI mode) and the device space (IDE-managed HiDPI mode).
   * See {@link UIUtil#isJreHiDPIEnabled()}
   */
  private static class FrameBoundsConverter {
    /**
     * @param bounds the bounds in the device space
     * @return the bounds in the user space
     */
    public static Rectangle convertFromDeviceSpace(@Nonnull Rectangle bounds) {
      Rectangle b = bounds.getBounds();
      if (!shouldConvert()) return b;

      try {
        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
          Rectangle devBounds = gd.getDefaultConfiguration().getBounds(); // in user space
          scaleUp(devBounds, gd.getDefaultConfiguration()); // to device space
          Rectangle2D.Float devBounds2D = new Rectangle2D.Float(devBounds.x, devBounds.y, devBounds.width, devBounds.height);
          Point2D.Float center2d = new Point2D.Float(b.x + b.width / 2, b.y + b.height / 2);
          if (devBounds2D.contains(center2d)) {
            scaleDown(b, gd.getDefaultConfiguration());
            break;
          }
        }
      }
      catch (HeadlessException ignore) {
      }
      return b;
    }

    /**
     * @param gc     the graphics config
     * @param bounds the bounds in the user space
     * @return the bounds in the device space
     */
    public static Rectangle convertToDeviceSpace(GraphicsConfiguration gc, @Nonnull Rectangle bounds) {
      Rectangle b = bounds.getBounds();
      if (!shouldConvert()) return b;

      try {
        scaleUp(b, gc);
      }
      catch (HeadlessException ignore) {
      }
      return b;
    }

    private static boolean shouldConvert() {
      if (SystemInfo.isLinux || // JRE-managed HiDPI mode is not yet implemented (pending)
          SystemInfo.isMac)     // JRE-managed HiDPI mode is permanent
      {
        return false;
      }
      if (!UIUtil.isJreHiDPIEnabled()) return false; // device space equals user space
      return true;
    }

    private static void scaleUp(@Nonnull Rectangle bounds, @Nonnull GraphicsConfiguration gc) {
      scale(bounds, gc.getBounds(), JBUI.sysScale(gc));
    }

    private static void scaleDown(@Nonnull Rectangle bounds, @Nonnull GraphicsConfiguration gc) {
      float scale = JBUI.sysScale(gc);
      assert scale != 0;
      scale(bounds, gc.getBounds(), 1 / scale);
    }

    private static void scale(@Nonnull Rectangle bounds, @Nonnull Rectangle deviceBounds, float scale) {
      // On Windows, JB SDK transforms the screen bounds to the user space as follows:
      // [x, y, width, height] -> [x, y, width / scale, height / scale]
      // xy are not transformed in order to avoid overlapping of the screen bounds in multi-dpi env.

      // scale the delta b/w xy and deviceBounds.xy
      int x = (int)Math.floor(deviceBounds.x + (bounds.x - deviceBounds.x) * scale);
      int y = (int)Math.floor(deviceBounds.y + (bounds.y - deviceBounds.y) * scale);

      bounds.setBounds(x, y, (int)Math.ceil(bounds.width * scale), (int)Math.ceil(bounds.height * scale));
    }
  }
}
