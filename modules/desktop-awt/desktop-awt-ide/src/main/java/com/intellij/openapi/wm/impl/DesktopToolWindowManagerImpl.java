/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.ide.FrameStateManager;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.AnActionListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.commands.DesktopRequestFocusInToolWindowCmd;
import com.intellij.ui.BalloonImpl;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Alarm;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.ObjectUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.PositionTracker;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.UiNotifyConnector;
import consulo.annotation.access.RequiredWriteAction;
import consulo.awt.TargetAWT;
import consulo.desktop.util.awt.migration.AWTComponentProviderUtil;
import consulo.disposer.Disposer;
import consulo.fileEditor.impl.EditorWindow;
import consulo.fileEditor.impl.EditorWithProviderComposite;
import consulo.fileEditor.impl.EditorsSplitters;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.Rectangle2D;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ToolWindowInternalDecorator;
import consulo.ui.ex.ToolWindowStripeButton;
import consulo.ui.image.Image;
import consulo.wm.impl.ToolWindowManagerBase;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.intellij.lang.annotations.JdkConstants;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
@State(name = ToolWindowManagerBase.ID, storages = @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED))
@Singleton
public final class DesktopToolWindowManagerImpl extends ToolWindowManagerBase {

  /**
   * Translates events from InternalDecorator into ToolWindowManager method invocations.
   */
  private final class MyInternalDecoratorListener extends MyInternalDecoratorListenerBase {
    /**
     * Handles event from decorator and modify weight/floating bounds of the
     * tool window depending on decoration type.
     */
    @Override
    public void resized(@Nonnull final ToolWindowInternalDecorator s) {
      DesktopInternalDecorator source = (DesktopInternalDecorator)s;

      if (!source.isShowing()) {
        return; // do not recalculate the tool window size if it is not yet shown (and, therefore, has 0,0,0,0 bounds)
      }

      final WindowInfoImpl info = getInfo(source.getToolWindow().getId());
      if (info.isFloating()) {
        final Window owner = SwingUtilities.getWindowAncestor(source);
        if (owner != null) {
          info.setFloatingBounds(TargetAWT.from(owner.getBounds()));
        }
      }
      else if (info.isWindowed()) {
        DesktopWindowedDecorator decorator = getWindowedDecorator(info.getId());
        Window frame = decorator != null ? decorator.getFrame() : null;
        if (frame == null || !frame.isShowing()) return;
        info.setFloatingBounds(getRootBounds((JFrame)frame));
      }
      else { // docked and sliding windows
        ToolWindowAnchor anchor = info.getAnchor();
        DesktopInternalDecorator another = null;
        if (source.getParent() instanceof Splitter) {
          float sizeInSplit = anchor.isSplitVertically() ? source.getHeight() : source.getWidth();
          Splitter splitter = (Splitter)source.getParent();
          if (splitter.getSecondComponent() == source) {
            sizeInSplit += splitter.getDividerWidth();
            another = (DesktopInternalDecorator)splitter.getFirstComponent();
          }
          else {
            another = (DesktopInternalDecorator)splitter.getSecondComponent();
          }
          if (anchor.isSplitVertically()) {
            info.setSideWeight(sizeInSplit / (float)splitter.getHeight());
          }
          else {
            info.setSideWeight(sizeInSplit / (float)splitter.getWidth());
          }
        }

        float paneWeight = anchor.isHorizontal()
                           ? (float)source.getHeight() / (float)getToolWindowPanel().getMyLayeredPane().getHeight()
                           : (float)source.getWidth() / (float)getToolWindowPanel().getMyLayeredPane().getWidth();
        info.setWeight(paneWeight);
        if (another != null && anchor.isSplitVertically()) {
          paneWeight = anchor.isHorizontal()
                       ? (float)another.getHeight() / (float)getToolWindowPanel().getMyLayeredPane().getHeight()
                       : (float)another.getWidth() / (float)getToolWindowPanel().getMyLayeredPane().getWidth();
          another.getWindowInfo().setWeight(paneWeight);
        }
      }
    }
  }

  private static final Logger LOG = Logger.getInstance(DesktopToolWindowManagerImpl.class);

  private final Map<String, FocusWatcher> myId2FocusWatcher = new HashMap<>();

  private DesktopIdeFrameImpl myFrame;

  private final Map<String, Balloon> myWindow2Balloon = new HashMap<>();

  private KeyState myCurrentState = KeyState.waiting;
  private final Alarm myWaiterForSecondPress = new Alarm();
  private final Runnable mySecondPressRunnable = () -> {
    if (myCurrentState != KeyState.hold) {
      resetHoldState();
    }
  };

  private final Alarm myUpdateHeadersAlarm = new Alarm();

  private enum KeyState {
    waiting,
    pressed,
    released,
    hold
  }

  @Inject
  public DesktopToolWindowManagerImpl(Project project, Provider<WindowManager> windowManager) {
    super(project, windowManager);

    if (project.isDefault()) {
      return;
    }

    MessageBusConnection busConnection = project.getMessageBus().connect();
    busConnection.subscribe(AnActionListener.TOPIC, new AnActionListener() {
      @Override
      public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        if (myCurrentState != KeyState.hold) {
          resetHoldState();
        }
      }
    });
    busConnection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        if (project == myProject) {
          uiAccess.giveAndWaitIfNeed(DesktopToolWindowManagerImpl.this::projectOpened);
        }
      }

      @Override
      public void projectClosed(@Nonnull Project project, @Nonnull UIAccess uiAccess) {
        if (project == myProject) {
          uiAccess.giveAndWaitIfNeed(DesktopToolWindowManagerImpl.this::projectClosed);
        }
      }
    });

    myLayout.copyFrom(((WindowManagerEx)windowManager.get()).getLayout());

    busConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void fileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {
        getFocusManagerImpl(myProject).doWhenFocusSettlesDown(new ExpirableRunnable.ForProject(myProject) {
          @Override
          public void run() {
            if (!hasOpenEditorFiles()) {
              focusToolWindowByDefault(null);
            }
          }
        });
      }
    });

    PropertyChangeListener focusListener = it -> {
      if ("focusOwner".equals(it.getPropertyName())) {
        myUpdateHeadersAlarm.cancelAllRequests();
        myUpdateHeadersAlarm.addRequest(this::updateToolWindowHeaders, 50);
      }
    };
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(focusListener);
    Disposer.register(this, () -> KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(focusListener));
  }


  @Nonnull
  @Override
  protected InternalDecoratorListener createInternalDecoratorListener() {
    return new MyInternalDecoratorListener();
  }

  @Nonnull
  @Override
  protected ToolWindowStripeButton createStripeButton(ToolWindowInternalDecorator internalDecorator) {
    return new DesktopStripeButton((DesktopInternalDecorator)internalDecorator, (DesktopToolWindowPanelImpl)myToolWindowPanel);
  }

  @Nonnull
  @Override
  protected ToolWindowEx createToolWindow(String id, LocalizeValue displayName, boolean canCloseContent, @Nullable Object component, boolean shouldBeAvailable) {
    return new DesktopToolWindowImpl(this, id, displayName, canCloseContent, (JComponent)component, shouldBeAvailable);
  }

  @Nonnull
  @Override
  protected ToolWindowInternalDecorator createInternalDecorator(Project project, @Nonnull WindowInfoImpl info, ToolWindowEx toolWindow, boolean dumbAware) {
    return new DesktopInternalDecorator(project, info, (DesktopToolWindowImpl)toolWindow, dumbAware);
  }

  private void updateToolWindowHeaders() {
    getFocusManager().doWhenFocusSettlesDown(new ExpirableRunnable.ForProject(myProject) {
      @Override
      public void run() {
        WindowInfoImpl[] infos = myLayout.getInfos();
        for (WindowInfoImpl each : infos) {
          if (each.isVisible()) {
            ToolWindow tw = getToolWindow(each.getId());
            if (tw instanceof DesktopToolWindowImpl) {
              DesktopInternalDecorator decorator = (DesktopInternalDecorator)((DesktopToolWindowImpl)tw).getDecorator();
              if (decorator != null) {
                decorator.repaint();
              }
            }
          }
        }
      }
    });
  }

  public boolean dispatchKeyEvent(KeyEvent e) {
    if (e.getKeyCode() != KeyEvent.VK_CONTROL && e.getKeyCode() != KeyEvent.VK_ALT && e.getKeyCode() != KeyEvent.VK_SHIFT && e.getKeyCode() != KeyEvent.VK_META) {
      if (e.getModifiers() == 0) {
        resetHoldState();
      }
      return false;
    }
    if (e.getID() != KeyEvent.KEY_PRESSED && e.getID() != KeyEvent.KEY_RELEASED) return false;

    Component parent = UIUtil.findUltimateParent(e.getComponent());
    if(parent instanceof Window) {
      consulo.ui.Window uiWindow = TargetAWT.from((Window)parent);

      IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
      if (ideFrame != null && ideFrame.getProject() != myProject) {
        resetHoldState();
        return false;
      }
    }

    Set<Integer> vks = getActivateToolWindowVKs();

    if (vks.isEmpty()) {
      resetHoldState();
      return false;
    }

    if (vks.contains(e.getKeyCode())) {
      boolean pressed = e.getID() == KeyEvent.KEY_PRESSED;
      int modifiers = e.getModifiers();

      int mouseMask = InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK;
      if ((e.getModifiersEx() & mouseMask) == 0) {
        if (areAllModifiersPressed(modifiers, vks) || !pressed) {
          processState(pressed);
        }
        else {
          resetHoldState();
        }
      }
    }


    return false;
  }

  private static boolean areAllModifiersPressed(@JdkConstants.InputEventMask int modifiers, Set<Integer> modifierCodes) {
    int mask = 0;
    for (Integer each : modifierCodes) {
      if (each == KeyEvent.VK_SHIFT) {
        mask |= InputEvent.SHIFT_MASK;
      }

      if (each == KeyEvent.VK_CONTROL) {
        mask |= InputEvent.CTRL_MASK;
      }

      if (each == KeyEvent.VK_META) {
        mask |= InputEvent.META_MASK;
      }

      if (each == KeyEvent.VK_ALT) {
        mask |= InputEvent.ALT_MASK;
      }
    }

    return (modifiers ^ mask) == 0;
  }

  public static Set<Integer> getActivateToolWindowVKs() {
    if (ApplicationManager.getApplication() == null) return new HashSet<>();

    Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
    Shortcut[] baseShortcut = keymap.getShortcuts("ActivateProjectToolWindow");
    int baseModifiers = 0;
    for (Shortcut each : baseShortcut) {
      if (each instanceof KeyboardShortcut) {
        KeyStroke keyStroke = ((KeyboardShortcut)each).getFirstKeyStroke();
        baseModifiers = keyStroke.getModifiers();
        if (baseModifiers > 0) {
          break;
        }
      }
    }
    return getModifiersVKs(baseModifiers);
  }

  @Nonnull
  private static Set<Integer> getModifiersVKs(int mask) {
    Set<Integer> codes = new HashSet<>();
    if ((mask & InputEvent.SHIFT_MASK) > 0) {
      codes.add(KeyEvent.VK_SHIFT);
    }
    if ((mask & InputEvent.CTRL_MASK) > 0) {
      codes.add(KeyEvent.VK_CONTROL);
    }

    if ((mask & InputEvent.META_MASK) > 0) {
      codes.add(KeyEvent.VK_META);
    }

    if ((mask & InputEvent.ALT_MASK) > 0) {
      codes.add(KeyEvent.VK_ALT);
    }

    return codes;
  }

  private void resetHoldState() {
    myCurrentState = KeyState.waiting;
    processHoldState();
  }

  private void processState(boolean pressed) {
    if (pressed) {
      if (myCurrentState == KeyState.waiting) {
        myCurrentState = KeyState.pressed;
      }
      else if (myCurrentState == KeyState.released) {
        myCurrentState = KeyState.hold;
        processHoldState();
      }
    }
    else {
      if (myCurrentState == KeyState.pressed) {
        myCurrentState = KeyState.released;
        restartWaitingForSecondPressAlarm();
      }
      else {
        resetHoldState();
      }
    }
  }

  private void processHoldState() {
    if (myToolWindowPanel != null) {
      getToolWindowPanel().setStripesOverlayed(myCurrentState == KeyState.hold);
    }
  }

  private void restartWaitingForSecondPressAlarm() {
    myWaiterForSecondPress.cancelAllRequests();
    myWaiterForSecondPress.addRequest(mySecondPressRunnable, SystemProperties.getIntProperty("actionSystem.keyGestureDblClickTime", 650));
  }

  @Nonnull
  public DesktopToolWindowPanelImpl getToolWindowPanel() {
    return (DesktopToolWindowPanelImpl)myToolWindowPanel;
  }

  private static IdeFocusManager getFocusManagerImpl(Project project) {
    return IdeFocusManager.getInstance(project);
  }

  public void projectOpened() {
    final MyUIManagerPropertyChangeListener uiManagerPropertyListener = new MyUIManagerPropertyChangeListener();
    final MyLafManagerListener lafManagerListener = new MyLafManagerListener();

    UIManager.addPropertyChangeListener(uiManagerPropertyListener);
    LafManager.getInstance().addLafManagerListener(lafManagerListener, this);

    Disposer.register(this, () -> {
      UIManager.removePropertyChangeListener(uiManagerPropertyListener);
    });

    WindowManagerEx windowManager = (WindowManagerEx)myWindowManager.get();

    myFrame = (DesktopIdeFrameImpl)windowManager.allocateFrame(myProject);

    myToolWindowPanel = new DesktopToolWindowPanelImpl(myFrame, this);
    Disposer.register(myProject, getToolWindowPanel());
    JFrame jFrame = (JFrame)TargetAWT.to(myFrame.getWindow());
    ((IdeRootPane)jFrame.getRootPane()).setToolWindowsPane(myToolWindowPanel);
    jFrame.setTitle(FrameTitleBuilder.getInstance().getProjectTitle(myProject));
    ((IdeRootPane)jFrame.getRootPane()).updateToolbar();

    IdeEventQueue.getInstance().addDispatcher(e -> {
      if (e instanceof KeyEvent) {
        dispatchKeyEvent((KeyEvent)e);
      }
      if (e instanceof WindowEvent && e.getID() == WindowEvent.WINDOW_LOST_FOCUS && e.getSource() == myFrame) {
        resetHoldState();
      }
      return false;
    }, myProject);
  }

  @RequiredUIAccess
  @Override
  protected void initializeEditorComponent() {
    JComponent editorComponent = getEditorComponent(myProject);
    editorComponent.setFocusable(false);

    setEditorComponent(editorComponent);
  }

  @Override
  protected void installFocusWatcher(String id, ToolWindow toolWindow) {
    myId2FocusWatcher.put(id, new ToolWindowFocusWatcher((DesktopToolWindowImpl)toolWindow));
  }

  @Override
  protected void uninstallFocusWatcher(String id) {
    ToolWindowFocusWatcher watcher = (ToolWindowFocusWatcher)myId2FocusWatcher.remove(id);
    watcher.deinstall();
  }

  private JComponent getEditorComponent(Project project) {
    return FileEditorManagerEx.getInstanceEx(project).getComponent();
  }

  @Nonnull
  @Override
  protected JLabel createInitializingLabel() {
    JLabel label = new JLabel("Initializing...", SwingConstants.CENTER);
    label.setOpaque(true);
    final Color treeBg = UIManager.getColor("Tree.background");
    label.setBackground(ColorUtil.toAlpha(treeBg, 180));
    final Color treeFg = UIUtil.getTreeForeground();
    label.setForeground(ColorUtil.toAlpha(treeFg, 180));
    return label;
  }

  @RequiredUIAccess
  @Override
  protected void doWhenFirstShown(Object component, Runnable runnable) {
    UiNotifyConnector.doWhenFirstShown((JComponent)component, () -> ApplicationManager.getApplication().invokeLater(runnable));
  }

  public void projectClosed() {
    if (myFrame == null) {
      return;
    }
    final String[] ids = getToolWindowIds();

    WindowManagerEx windowManager = (WindowManagerEx)myWindowManager.get();

    // Remove ToolWindowsPane
    JFrame window = (JFrame)TargetAWT.to(myFrame.getWindow());
    ((IdeRootPane)window.getRootPane()).setToolWindowsPane(null);
    windowManager.releaseFrame(myFrame);
    updateToolWindowsPane();

    // Hide all tool windows

    for (final String id : ids) {
      deactivateToolWindowImpl(id, true);
    }

    // Remove editor component
    setEditorComponent(null);

    myFrame = null;
  }

  @Override
  public void activateEditorComponent() {
    focusDefaultElementInSelectedEditor();
  }

  private void focusDefaultElementInSelectedEditor() {
    EditorsSplitters splittersToFocus = getSplittersToFocus();
    if (splittersToFocus != null) {
      final EditorWindow window = splittersToFocus.getCurrentWindow();
      if (window != null) {
        final EditorWithProviderComposite editor = window.getSelectedEditor();
        if (editor != null) {
          JComponent defaultFocusedComponentInEditor = editor.getPreferredFocusedComponent();
          if (defaultFocusedComponentInEditor != null) {
            defaultFocusedComponentInEditor.requestFocus();
          }
        }
      }
    }
  }

  /**
   * @return floating decorator for the tool window with specified <code>ID</code>.
   */
  @Override
  protected DesktopFloatingDecorator getFloatingDecorator(final String id) {
    return (DesktopFloatingDecorator)super.getFloatingDecorator(id);
  }

  /**
   * @return windowed decorator for the tool window with specified <code>ID</code>.
   */
  @Override
  protected DesktopWindowedDecorator getWindowedDecorator(String id) {
    return (DesktopWindowedDecorator)super.myId2WindowedDecorator.get(id);
  }

  /**
   * @return internal decorator for the tool window with specified <code>ID</code>.
   */
  @Override
  @Nullable
  protected DesktopInternalDecorator getInternalDecorator(final String id) {
    return (DesktopInternalDecorator)super.getInternalDecorator(id);
  }

  /**
   * @return tool button for the window with specified <code>ID</code>.
   */
  @Override
  @Nullable
  protected DesktopStripeButton getStripeButton(final String id) {
    return (DesktopStripeButton)super.getStripeButton(id);
  }

  @Override
  public boolean canShowNotification(@Nonnull final String toolWindowId) {
    if (!Arrays.asList(getToolWindowIds()).contains(toolWindowId)) {
      return false;
    }
    final DesktopStripePanelImpl stripe = getToolWindowPanel().getStripeFor(toolWindowId);
    return stripe != null && stripe.getButtonFor(toolWindowId) != null;
  }

  @Override
  public void notifyByBalloon(@Nonnull final String toolWindowId, @Nonnull final MessageType type, @Nonnull final String htmlBody) {
    notifyByBalloon(toolWindowId, type, htmlBody, null, null);
  }

  @Override
  public void notifyByBalloon(@Nonnull final String toolWindowId, @Nonnull final MessageType type, @Nonnull final String text, @Nullable final Image icon, @Nullable final HyperlinkListener listener) {
    checkId(toolWindowId);


    Balloon existing = myWindow2Balloon.get(toolWindowId);
    if (existing != null) {
      existing.hide();
    }

    final DesktopStripePanelImpl stripe = getToolWindowPanel().getStripeFor(toolWindowId);
    if (stripe == null) {
      return;
    }
    final DesktopToolWindowImpl window = getInternalDecorator(toolWindowId).getToolWindow();
    if (!window.isAvailable()) {
      window.setPlaceholderMode(true);
      stripe.updatePresentation();
      stripe.revalidate();
      stripe.repaint();
    }

    final ToolWindowAnchor anchor = getInfo(toolWindowId).getAnchor();
    final Ref<Balloon.Position> position = Ref.create(Balloon.Position.below);
    if (ToolWindowAnchor.TOP == anchor) {
      position.set(Balloon.Position.below);
    }
    else if (ToolWindowAnchor.BOTTOM == anchor) {
      position.set(Balloon.Position.above);
    }
    else if (ToolWindowAnchor.LEFT == anchor) {
      position.set(Balloon.Position.atRight);
    }
    else if (ToolWindowAnchor.RIGHT == anchor) {
      position.set(Balloon.Position.atLeft);
    }

    final BalloonHyperlinkListener listenerWrapper = new BalloonHyperlinkListener(listener);
    final Balloon balloon = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(text.replace("\n", "<br>"), icon, type.getPopupBackground(), listenerWrapper).setHideOnClickOutside(false)
            .setBorderColor(type.getBorderColor())
            .setHideOnFrameResize(false).createBalloon();
    FrameStateManager.getInstance().getApplicationActive().doWhenDone(() -> {
      final Alarm alarm = new Alarm();
      alarm.addRequest(() -> {
        ((BalloonImpl)balloon).setHideOnClickOutside(true);
        Disposer.dispose(alarm);
      }, 100);
    });
    listenerWrapper.myBalloon = balloon;
    myWindow2Balloon.put(toolWindowId, balloon);
    Disposer.register(balloon, () -> {
      window.setPlaceholderMode(false);
      stripe.updatePresentation();
      stripe.revalidate();
      stripe.repaint();
      myWindow2Balloon.remove(toolWindowId);
    });
    Disposer.register(getProject(), balloon);

    final DesktopStripeButton button = stripe.getButtonFor(toolWindowId);
    LOG.assertTrue(button != null, "Button was not found, popup won't be shown. Toolwindow id: " + toolWindowId + ", message: " + text + ", message type: " + type);
    if (button == null) return;

    final Runnable show = () -> {
      if (button.isShowing()) {
        PositionTracker<Balloon> tracker = new PositionTracker<Balloon>(button) {
          @Override
          @Nullable
          public RelativePoint recalculateLocation(Balloon object) {
            DesktopStripePanelImpl twStripe = getToolWindowPanel().getStripeFor(toolWindowId);
            DesktopStripeButton twButton = twStripe != null ? twStripe.getButtonFor(toolWindowId) : null;

            if (twButton == null) return null;

            if (getToolWindow(toolWindowId).getAnchor() != anchor) {
              object.hide();
              return null;
            }

            final Point point = new Point(twButton.getBounds().width / 2, twButton.getHeight() / 2 - 2);
            return new RelativePoint(twButton, point);
          }
        };
        if (!balloon.isDisposed()) {
          balloon.show(tracker, position.get());
        }
      }
      else {
        final Rectangle bounds = getToolWindowPanel().getBounds();
        final Point target = UIUtil.getCenterPoint(bounds, new Dimension(1, 1));
        if (ToolWindowAnchor.TOP == anchor) {
          target.y = 0;
        }
        else if (ToolWindowAnchor.BOTTOM == anchor) {
          target.y = bounds.height - 3;
        }
        else if (ToolWindowAnchor.LEFT == anchor) {
          target.x = 0;
        }
        else if (ToolWindowAnchor.RIGHT == anchor) {
          target.x = bounds.width;
        }
        if (!balloon.isDisposed()) {
          balloon.show(new RelativePoint(getToolWindowPanel(), target), position.get());
        }
      }
    };

    if (!button.isValid()) {
      SwingUtilities.invokeLater(show::run);
    }
    else {
      show.run();
    }
  }

  @Override
  public Balloon getToolWindowBalloon(String id) {
    return myWindow2Balloon.get(id);
  }

  @Override
  public boolean isEditorComponentActive() {
    UIAccess.assertIsUIThread();

    Component owner = getFocusManager().getFocusOwner();
    EditorsSplitters splitters = AWTComponentProviderUtil.findParent(owner, EditorsSplitters.class);
    return splitters != null;
  }

  @RequiredUIAccess
  @Override
  protected void removeWindowedDecorator(final WindowInfoImpl info) {
    new RemoveWindowedDecoratorCmd(info).run();
  }

  @RequiredUIAccess
  @Override
  protected void addFloatingDecorator(ToolWindowInternalDecorator decorator, WindowInfoImpl toBeShownInfo) {
    new AddFloatingDecoratorCmd((DesktopInternalDecorator)decorator, toBeShownInfo).run();
  }

  @RequiredUIAccess
  @Override
  protected void addWindowedDecorator(ToolWindowInternalDecorator decorator, WindowInfoImpl toBeShownInfo) {
    new AddWindowedDecoratorCmd((DesktopInternalDecorator)decorator, toBeShownInfo).run();
  }

  @RequiredUIAccess
  @Override
  public void requestFocusInToolWindow(final String id, boolean forced) {
    final DesktopToolWindowImpl toolWindow = (DesktopToolWindowImpl)getToolWindow(id);
    final FocusWatcher focusWatcher = myId2FocusWatcher.get(id);
    
    new DesktopRequestFocusInToolWindowCmd(getFocusManager(), toolWindow, focusWatcher, myProject).requestFocus();
  }

  @RequiredUIAccess
  @Override
  protected void updateToolWindowsPane() {
    // frame is not initialized = we don't need update root
    if(myFrame == null) {
      return;
    }
    
    final JRootPane rootPane = ((JFrame)TargetAWT.to(myFrame.getWindow())).getRootPane();
    if (rootPane != null) {
      rootPane.revalidate();
      rootPane.repaint();
    }
  }

  private EditorsSplitters getSplittersToFocus() {
    WindowManagerEx windowManager = (WindowManagerEx)myWindowManager.get();

    Window activeWindow = TargetAWT.to(windowManager.getMostRecentFocusedWindow());

    if (activeWindow instanceof DesktopFloatingDecorator) {
      IdeFocusManager ideFocusManager = IdeFocusManager.findInstanceByComponent(activeWindow);
      IdeFrame lastFocusedFrame = ideFocusManager.getLastFocusedFrame();
      JComponent frameComponent = lastFocusedFrame != null ? lastFocusedFrame.getComponent() : null;
      Window lastFocusedWindow = frameComponent != null ? SwingUtilities.getWindowAncestor(frameComponent) : null;
      activeWindow = ObjectUtil.notNull(lastFocusedWindow, activeWindow);
    }

    FileEditorManagerEx fem = FileEditorManagerEx.getInstanceEx(myProject);
    EditorsSplitters splitters = activeWindow != null ? fem.getSplittersFor(activeWindow) : null;
    return splitters != null ? splitters : fem.getSplitters();
  }

  @RequiredUIAccess
  @Override
  @Nullable
  public String getLastActiveToolWindowId(@Nullable Condition<JComponent> condition) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    String lastActiveToolWindowId = null;
    for (int i = 0; i < myActiveStack.getPersistentSize(); i++) {
      final String id = myActiveStack.peekPersistent(i);
      final ToolWindow toolWindow = getToolWindow(id);
      LOG.assertTrue(toolWindow != null);
      if (toolWindow.isAvailable()) {
        if (condition == null || condition.value(toolWindow.getComponent())) {
          lastActiveToolWindowId = id;
          break;
        }
      }
    }
    return lastActiveToolWindowId;
  }

  @Override
  public Element getStateFromUI() {
    if (myFrame == null) {
      // do nothing if the project was not opened
      return null;
    }

    // Update size of all open floating windows. See SCR #18439
    for (final String id : getToolWindowIds()) {
      final WindowInfoImpl info = getInfo(id);
      if (info.isVisible()) {
        final DesktopInternalDecorator decorator = getInternalDecorator(id);
        LOG.assertTrue(decorator != null);
        decorator.fireResized();
      }
    }

    Element element = new Element("state");

    // Save frame's bounds
    JFrame jFrame = (JFrame)TargetAWT.to(myFrame.getWindow());
    final Rectangle frameBounds = jFrame.getBounds();
    final Element frameElement = new Element(FRAME_ELEMENT);
    element.addContent(frameElement);
    frameElement.setAttribute(X_ATTR, Integer.toString(frameBounds.x));
    frameElement.setAttribute(Y_ATTR, Integer.toString(frameBounds.y));
    frameElement.setAttribute(WIDTH_ATTR, Integer.toString(frameBounds.width));
    frameElement.setAttribute(HEIGHT_ATTR, Integer.toString(frameBounds.height));
    frameElement.setAttribute(EXTENDED_STATE_ATTR, Integer.toString(jFrame.getExtendedState()));

    // Save whether editor is active or not
    if (isEditorComponentActive()) {
      Element editorElement = new Element(EDITOR_ELEMENT);
      editorElement.setAttribute(ACTIVE_ATTR_VALUE, "true");
      element.addContent(editorElement);
    }

    // Save layout of tool windows
    Element layoutElement = myLayout.writeExternal(ToolWindowLayout.TAG);
    if (layoutElement != null) {
      element.addContent(layoutElement);
    }

    Element layoutToRestoreElement = myLayoutToRestoreLater == null ? null : myLayoutToRestoreLater.writeExternal(LAYOUT_TO_RESTORE);
    if (layoutToRestoreElement != null) {
      element.addContent(layoutToRestoreElement);
    }

    return element;
  }

  @RequiredWriteAction
  @Nullable
  @Override
  public Element getState(Element element) {
    return element;
  }

  public void stretchWidth(DesktopToolWindowImpl toolWindow, int value) {
    getToolWindowPanel().stretchWidth(toolWindow, value);
  }

  @Override
  public boolean isMaximized(@Nonnull ToolWindow wnd) {
    return getToolWindowPanel().isMaximized(wnd);
  }

  @Override
  public void setMaximized(@Nonnull ToolWindow wnd, boolean maximized) {
    getToolWindowPanel().setMaximized(wnd, maximized);
  }

  public void stretchHeight(DesktopToolWindowImpl toolWindow, int value) {
    getToolWindowPanel().stretchHeight(toolWindow, value);
  }

  private static class BalloonHyperlinkListener implements HyperlinkListener {
    private Balloon myBalloon;
    private final HyperlinkListener myListener;

    public BalloonHyperlinkListener(HyperlinkListener listener) {
      myListener = listener;
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
      if (myBalloon != null && e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
        myBalloon.hide();
      }
      if (myListener != null) {
        myListener.hyperlinkUpdate(e);
      }
    }
  }


  /**
   * This command creates and shows <code>FloatingDecorator</code>.
   */
  private final class AddFloatingDecoratorCmd implements Runnable {
    private final DesktopFloatingDecorator myFloatingDecorator;

    /**
     * Creates floating decorator for specified floating decorator.
     */
    private AddFloatingDecoratorCmd(final DesktopInternalDecorator decorator, final WindowInfoImpl info) {
      myFloatingDecorator = new DesktopFloatingDecorator(myFrame, info.copy(), decorator);
      myId2FloatingDecorator.put(info.getId(), myFloatingDecorator);
      final Rectangle2D bounds = info.getFloatingBounds();
      if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0 && myWindowManager.get().isInsideScreenBounds(bounds.getX(), bounds.getY(), bounds.getWidth())) {
        myFloatingDecorator.setBounds(TargetAWT.to(bounds));
      }
      else { // place new frame at the center of main frame if there are no floating bounds
        Dimension size = decorator.getSize();
        if (size.width == 0 || size.height == 0) {
          size = decorator.getPreferredSize();
        }
        myFloatingDecorator.setSize(size);
        myFloatingDecorator.setLocationRelativeTo(TargetAWT.to(myFrame.getWindow()));
      }
    }

    @Override
    public void run() {
      myFloatingDecorator.show();
    }
  }

  /**
   * This command creates and shows <code>WindowedDecorator</code>.
   */
  private final class AddWindowedDecoratorCmd implements Runnable {
    private final DesktopWindowedDecorator myWindowedDecorator;

    /**
     * Creates floating decorator for specified floating decorator.
     */
    private AddWindowedDecoratorCmd(@Nonnull DesktopInternalDecorator decorator, @Nonnull WindowInfoImpl info) {
      myWindowedDecorator = new DesktopWindowedDecorator(myProject, info.copy(), decorator);
      Window window = myWindowedDecorator.getFrame();
      final Rectangle2D bounds = info.getFloatingBounds();
      if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0 && myWindowManager.get().isInsideScreenBounds(bounds.getX(), bounds.getY(), bounds.getWidth())) {
        window.setBounds(TargetAWT.to(bounds));
      }
      else { // place new frame at the center of main frame if there are no floating bounds
        Dimension size = decorator.getSize();
        if (size.width == 0 || size.height == 0) {
          size = decorator.getPreferredSize();
        }
        window.setSize(size);
        window.setLocationRelativeTo(TargetAWT.to(myFrame.getWindow()));
      }
      myId2WindowedDecorator.put(info.getId(), myWindowedDecorator);
      myWindowedDecorator.addDisposable(() -> {
        if (myId2WindowedDecorator.get(info.getId()) != null) {
          hideToolWindow(info.getId(), false);
        }
      });
    }

    @Override
    public void run() {
      myWindowedDecorator.show(false);
      Window window = myWindowedDecorator.getFrame();
      JRootPane rootPane = ((RootPaneContainer)window).getRootPane();
      Rectangle rootPaneBounds = rootPane.getBounds();
      Point point = rootPane.getLocationOnScreen();
      Rectangle windowBounds = window.getBounds();
      //Point windowLocation = windowBounds.getLocation();
      //windowLocation.translate(windowLocation.x - point.x, windowLocation.y - point.y);
      window.setLocation(2 * windowBounds.x - point.x, 2 * windowBounds.y - point.y);
      window.setSize(2 * windowBounds.width - rootPaneBounds.width, 2 * windowBounds.height - rootPaneBounds.height);
    }
  }

  /**
   * This command hides and destroys floating decorator for tool window
   * with specified <code>ID</code>.
   */
  private final class RemoveWindowedDecoratorCmd implements Runnable {
    private final DesktopWindowedDecorator myWindowedDecorator;

    private RemoveWindowedDecoratorCmd(final WindowInfoImpl info) {
      myWindowedDecorator = getWindowedDecorator(info.getId());
      myId2WindowedDecorator.remove(info.getId());

      Window frame = myWindowedDecorator.getFrame();
      if (!frame.isShowing()) return;
      Rectangle2D bounds = getRootBounds((JFrame)frame);
      info.setFloatingBounds(bounds);
    }

    @Override
    public void run() {
      Disposer.dispose(myWindowedDecorator);
    }
  }


  /**
   * Notifies window manager about focus traversal in tool window
   */
  private final class ToolWindowFocusWatcher extends FocusWatcher {
    private final String myId;
    private final DesktopToolWindowImpl myToolWindow;

    private ToolWindowFocusWatcher(@Nonnull DesktopToolWindowImpl toolWindow) {
      myId = toolWindow.getId();
      install(toolWindow.getComponent());
      myToolWindow = toolWindow;
    }

    public void deinstall() {
      deinstall(myToolWindow.getComponent());
    }

    @Override
    protected boolean isFocusedComponentChangeValid(final Component comp, final AWTEvent cause) {
      return comp != null;
    }

    @Override
    protected void focusedComponentChanged(final Component component, final AWTEvent cause) {
      if (component == null || !myToolWindow.isActive()) {
        return;
      }
      final WindowInfoImpl info = getInfo(myId);
      //getFocusManagerImpl(myProject)..cancelAllRequests();

      if (!info.isActive()) {
        getFocusManagerImpl(myProject).doWhenFocusSettlesDown(new EdtRunnable() {
          @Override
          public void runEdt() {
            WindowInfoImpl windowInfo = myLayout.getInfo(myId, true);
            if (windowInfo == null || !windowInfo.isVisible()) return;
            activateToolWindow(myId, false, false);
          }
        });
      }
    }
  }

  private void updateComponentTreeUI() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    final WindowInfoImpl[] infos = myLayout.getInfos();
    for (WindowInfoImpl info : infos) {
      // the main goal is to update hidden TW components because they are not in the hierarchy
      // and will not be updated automatically but unfortunately the visibility of a TW may change
      // during the same actionPerformed() so we can't optimize and have to process all of them
      IJSwingUtilities.updateComponentTreeUI(getInternalDecorator(info.getId()));
    }
  }

  private final class MyUIManagerPropertyChangeListener implements PropertyChangeListener {
    @Override
    public void propertyChange(final PropertyChangeEvent e) {
      updateComponentTreeUI();
    }
  }

  private final class MyLafManagerListener implements LafManagerListener {
    @Override
    public void lookAndFeelChanged(final LafManager source) {
      updateComponentTreeUI();
    }
  }

  /**
   * Delegate method for compatibility with older versions of IDEA
   */
  @Nonnull
  public AsyncResult<Void> requestFocus(@Nonnull Component c, boolean forced) {
    return IdeFocusManager.getInstance(myProject).requestFocus(c, forced);
  }

  public void doWhenFocusSettlesDown(@Nonnull Runnable runnable) {
    IdeFocusManager.getInstance(myProject).doWhenFocusSettlesDown(runnable);
  }

  @Nonnull
  private static Rectangle2D getRootBounds(JFrame frame) {
    JRootPane rootPane = frame.getRootPane();
    Rectangle bounds = rootPane.getBounds();
    bounds.setLocation(frame.getX() + rootPane.getX(), frame.getY() + rootPane.getY());
    return TargetAWT.from(bounds);
  }
}
