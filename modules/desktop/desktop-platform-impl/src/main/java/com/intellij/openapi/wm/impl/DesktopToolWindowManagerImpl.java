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
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.DesktopEditorsSplitters;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.ex.IdeFocusTraversalPolicy;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.commands.DesktopRequestFocusInToolWindowCmd;
import com.intellij.openapi.wm.impl.commands.FinalizableCommand;
import com.intellij.openapi.wm.impl.commands.RequestFocusInEditorComponentCmd;
import com.intellij.openapi.wm.impl.commands.UpdateRootPaneCmd;
import com.intellij.ui.BalloonImpl;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Alarm;
import com.intellij.util.IJSwingUtilities;
import com.intellij.util.ObjectUtil;
import com.intellij.util.containers.HashMap;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.PositionTracker;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.UiNotifyConnector;
import consulo.awt.TargetAWT;
import consulo.fileEditor.impl.EditorSplitters;
import consulo.ui.RequiredUIAccess;
import consulo.ui.ex.ToolWindowInternalDecorator;
import consulo.ui.ex.ToolWindowStripeButton;
import consulo.ui.shared.Rectangle2D;
import consulo.wm.impl.DesktopCommandProcessorImpl;
import consulo.wm.impl.ToolWindowManagerBase;
import gnu.trove.THashSet;
import org.intellij.lang.annotations.JdkConstants;
import org.jdom.Element;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
@State(name = ToolWindowManagerBase.ID, storages = @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED))
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

  private final EditorComponentFocusWatcher myEditorComponentFocusWatcher = new EditorComponentFocusWatcher();

  private IdeFrameImpl myFrame;

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
  public DesktopToolWindowManagerImpl(final Project project, final WindowManagerEx windowManagerEx, final FileEditorManager fem, final ActionManager actionManager) {
    super(project, windowManagerEx);

    if (project.isDefault()) {
      return;
    }

    actionManager.addAnActionListener((action, dataContext, event) -> {
      if (myCurrentState != KeyState.hold) {
        resetHoldState();
      }
    }, project);

    MessageBusConnection busConnection = project.getMessageBus().connect();
    busConnection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(Project project) {
        if (project == myProject) {
          DesktopToolWindowManagerImpl.this.projectOpened();
        }
      }

      @Override
      public void projectClosed(Project project) {
        if (project == myProject) {
          DesktopToolWindowManagerImpl.this.projectClosed();
        }
      }
    });

    myLayout.copyFrom(windowManagerEx.getLayout());

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
  protected CommandProcessorBase createCommandProcessor() {
    return new DesktopCommandProcessorImpl();
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
  protected ToolWindowEx createToolWindow(String id, boolean canCloseContent, @Nullable Object component) {
    return new DesktopToolWindowImpl(this, id, canCloseContent, (JComponent)component);
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
    if (parent instanceof IdeFrame) {
      if (((IdeFrame)parent).getProject() != myProject) {
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
    Set<Integer> codes = new THashSet<>();
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
    myWaiterForSecondPress.addRequest(mySecondPressRunnable, Registry.intValue("actionSystem.keyGestureDblClickTime"));
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
    LafManager.getInstance().addLafManagerListener(lafManagerListener);

    Disposer.register(this, () -> {
      UIManager.removePropertyChangeListener(uiManagerPropertyListener);
      LafManager.getInstance().removeLafManagerListener(lafManagerListener);
    });
    myFrame = (IdeFrameImpl)myWindowManager.allocateFrame(myProject);
    LOG.assertTrue(myFrame != null);

    myToolWindowPanel = new DesktopToolWindowPanelImpl(myFrame, this);
    Disposer.register(myProject, getToolWindowPanel());
    ((IdeRootPane)myFrame.getRootPane()).setToolWindowsPane(myToolWindowPanel);
    myFrame.setTitle(FrameTitleBuilder.getInstance().getProjectTitle(myProject));
    ((IdeRootPane)myFrame.getRootPane()).updateToolbar();

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

  @Override
  protected void initAll(List<FinalizableCommand> commandsList) {
    appendUpdateToolWindowsPaneCmd(commandsList);

    JComponent editorComponent = createEditorComponent(myProject);
    myEditorComponentFocusWatcher.install(editorComponent);

    appendSetEditorComponentCmd(editorComponent, commandsList);
    if (myEditorWasActive && editorComponent instanceof DesktopEditorsSplitters) {
      activateEditorComponentImpl(commandsList, true);
    }
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

  private JComponent createEditorComponent(Project project) {
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

    // Remove ToolWindowsPane
    ((IdeRootPane)myFrame.getRootPane()).setToolWindowsPane(null);
    myWindowManager.releaseFrame(myFrame);
    List<FinalizableCommand> commandsList = new ArrayList<>();
    appendUpdateToolWindowsPaneCmd(commandsList);

    // Hide all tool windows

    for (final String id : ids) {
      deactivateToolWindowImpl(id, true, commandsList);
    }

    // Remove editor component

    final JComponent editorComponent = FileEditorManagerEx.getInstanceEx(myProject).getComponent();
    myEditorComponentFocusWatcher.deinstall(editorComponent);
    appendSetEditorComponentCmd(null, commandsList);
    execute(commandsList);
    myFrame = null;
  }

  @Override
  public void activateEditorComponent() {
    activateEditorComponent(true);
  }

  private void activateEditorComponent(final boolean forced) {
    activateEditorComponent(forced, false); //TODO[kirillk]: runnable in activateEditorComponent(boolean, boolean) never runs
  }

  private void activateEditorComponent(final boolean forced, boolean now) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: activateEditorComponent()");
    }
    ApplicationManager.getApplication().assertIsDispatchThread();

    final ExpirableRunnable runnable = new ExpirableRunnable.ForProject(myProject) {
      @Override
      public void run() {
        final ArrayList<FinalizableCommand> commandList = new ArrayList<>();
        activateEditorComponentImpl(commandList, forced);
        execute(commandList);
      }
    };
    if (now) {
      if (!runnable.isExpired()) {
        runnable.run();
      }
    }
    else {
      final FocusRequestor requestor = getFocusManager().getFurtherRequestor();
      getFocusManager().doWhenFocusSettlesDown(new ExpirableRunnable.ForProject(myProject) {
        @Override
        public void run() {
          requestor.requestFocus(new FocusCommand() {
            @Nonnull
            @Override
            public AsyncResult<Void> run() {
              runnable.run();
              return AsyncResult.resolved();
            }
          }.setExpirable(runnable), forced);
        }
      });
    }
  }

  @Override
  protected void activateEditorComponentImpl(List<FinalizableCommand> commandList, final boolean forced) {
    final String active = getActiveToolWindowId();
    // Now we have to request focus into most recent focused editor
    appendRequestFocusInEditorComponentCmd(commandList, forced).doWhenDone(() -> {
      final ArrayList<FinalizableCommand> postExecute = new ArrayList<>();

      if (LOG.isDebugEnabled()) {
        LOG.debug("editor activated");
      }
      deactivateWindows(postExecute, null);
      myActiveStack.clear();

      execute(postExecute);
    }).doWhenRejected(() -> {
      if (forced) {
        getFocusManagerImpl(myProject).requestFocus(new FocusCommand() {
          @Nonnull
          @Override
          public AsyncResult<Void> run() {
            final ArrayList<FinalizableCommand> cmds = new ArrayList<>();

            final WindowInfoImpl toReactivate = getInfo(active);
            final boolean reactivateLastActive = toReactivate != null && !isToHide(toReactivate);
            deactivateWindows(cmds, reactivateLastActive ? active : null);
            execute(cmds);

            if (reactivateLastActive) {
              activateToolWindow(active, false, true);
            }
            else {
              if (active != null) {
                myActiveStack.remove(active, false);
              }

              if (!myActiveStack.isEmpty()) {
                activateToolWindow(myActiveStack.peek(), false, true);
              }
            }
            return AsyncResult.resolved();
          }
        }, false);
      }
    });
  }

  private void deactivateWindows(final ArrayList<FinalizableCommand> postExecute, @Nullable String idToIgnore) {
    final WindowInfoImpl[] infos = myLayout.getInfos();
    for (final WindowInfoImpl info : infos) {
      final boolean shouldHide = isToHide(info);
      if (idToIgnore != null && idToIgnore.equals(info.getId())) {
        continue;
      }
      deactivateToolWindowImpl(info.getId(), shouldHide, postExecute);
    }
  }

  private boolean isToHide(final WindowInfoImpl info) {
    return (info.isAutoHide() || info.isSliding()) && !(info.isFloating() && hasModalChild(info)) && !info.isWindowed();
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
  @javax.annotation.Nullable
  protected DesktopInternalDecorator getInternalDecorator(final String id) {
    return (DesktopInternalDecorator)super.getInternalDecorator(id);
  }

  /**
   * @return tool button for the window with specified <code>ID</code>.
   */
  @Override
  @javax.annotation.Nullable
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
  public void notifyByBalloon(@Nonnull final String toolWindowId, @Nonnull final MessageType type, @Nonnull final String text, @Nullable final Icon icon, @Nullable final HyperlinkListener listener) {
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

    execute(new ArrayList<>(Arrays.<FinalizableCommand>asList(new FinalizableCommand(null) {
      @Override
      public void run() {
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
          SwingUtilities.invokeLater(() -> show.run());
        }
        else {
          show.run();
        }
      }
    })));
  }

  @Override
  public Balloon getToolWindowBalloon(String id) {
    return myWindow2Balloon.get(id);
  }

  @Override
  public boolean isEditorComponentActive() {
    ApplicationManager.getApplication().assertIsDispatchThread();

    Component owner = getFocusManager().getFocusOwner();
    DesktopEditorsSplitters splitters = UIUtil.getParentOfType(DesktopEditorsSplitters.class, owner);
    return splitters != null;
  }

  @Override
  protected void appendRemoveWindowedDecoratorCmd(final WindowInfoImpl info, final List<FinalizableCommand> commandsList) {
    final RemoveWindowedDecoratorCmd command = new RemoveWindowedDecoratorCmd(info);
    commandsList.add(command);
  }

  @Override
  protected void appendAddFloatingDecorator(ToolWindowInternalDecorator decorator, List<FinalizableCommand> commandList, WindowInfoImpl toBeShownInfo) {
    commandList.add(new AddFloatingDecoratorCmd((DesktopInternalDecorator)decorator, toBeShownInfo));
  }

  @Override
  protected void appendAddWindowedDecorator(ToolWindowInternalDecorator decorator, List<FinalizableCommand> commandList, WindowInfoImpl toBeShownInfo) {
    commandList.add(new AddWindowedDecoratorCmd((DesktopInternalDecorator)decorator, toBeShownInfo));
  }

  private ActionCallback appendRequestFocusInEditorComponentCmd(List<FinalizableCommand> commandList, boolean forced) {
    if (myProject.isDisposed()) return new ActionCallback.Done();
    EditorSplitters splitters = getSplittersToFocus();
    CommandProcessorBase commandProcessor = myCommandProcessor;
    RequestFocusInEditorComponentCmd command = new RequestFocusInEditorComponentCmd(splitters, getFocusManager(), commandProcessor, forced);
    commandList.add(command);
    return command.getDoneCallback();
  }

  @Override
  public void appendRequestFocusInToolWindowCmd(final String id, List<FinalizableCommand> commandList, boolean forced) {
    final DesktopToolWindowImpl toolWindow = (DesktopToolWindowImpl)getToolWindow(id);
    final FocusWatcher focusWatcher = myId2FocusWatcher.get(id);
    commandList.add(new DesktopRequestFocusInToolWindowCmd(getFocusManager(), toolWindow, focusWatcher, myCommandProcessor, myProject));
  }

  /**
   * @see DesktopToolWindowPanelImpl#createSetEditorComponentCmd
   */
  public void appendSetEditorComponentCmd(@javax.annotation.Nullable final JComponent component, final List<FinalizableCommand> commandsList) {
    final CommandProcessorBase commandProcessor = myCommandProcessor;
    final FinalizableCommand command = getToolWindowPanel().createSetEditorComponentCmd(component, commandProcessor);
    commandsList.add(command);
  }

  @Override
  protected void appendUpdateToolWindowsPaneCmd(final List<FinalizableCommand> commandsList) {
    final JRootPane rootPane = myFrame.getRootPane();
    if (rootPane != null) {
      final FinalizableCommand command = new UpdateRootPaneCmd(rootPane, myCommandProcessor);
      commandsList.add(command);
    }
  }

  private EditorSplitters getSplittersToFocus() {
    Window activeWindow = myWindowManager.getMostRecentFocusedWindow();

    if (activeWindow instanceof DesktopFloatingDecorator) {
      IdeFocusManager ideFocusManager = IdeFocusManager.findInstanceByComponent(activeWindow);
      IdeFrame lastFocusedFrame = ideFocusManager.getLastFocusedFrame();
      JComponent frameComponent = lastFocusedFrame != null ? lastFocusedFrame.getComponent() : null;
      Window lastFocusedWindow = frameComponent != null ? SwingUtilities.getWindowAncestor(frameComponent) : null;
      activeWindow = ObjectUtil.notNull(lastFocusedWindow, activeWindow);
    }

    FileEditorManagerEx fem = FileEditorManagerEx.getInstanceEx(myProject);
    EditorSplitters splitters = activeWindow != null ? fem.getSplittersFor(activeWindow) : null;
    return splitters != null ? splitters : fem.getSplitters();
  }

  /**
   * @return <code>true</code> if tool window with the specified <code>id</code>
   * is floating and has modal showing child dialog. Such windows should not be closed
   * when auto-hide windows are gone.
   */
  @Override
  protected boolean hasModalChild(final WindowInfoImpl info) {
    if (!info.isVisible() || !info.isFloating()) {
      return false;
    }
    final DesktopFloatingDecorator decorator = getFloatingDecorator(info.getId());
    LOG.assertTrue(decorator != null);
    return isModalOrHasModalChild(decorator);
  }

  private static boolean isModalOrHasModalChild(final Window window) {
    if (window instanceof Dialog) {
      final Dialog dialog = (Dialog)window;
      if (dialog.isModal() && dialog.isShowing()) {
        return true;
      }
      final Window[] ownedWindows = dialog.getOwnedWindows();
      for (int i = ownedWindows.length - 1; i >= 0; i--) {
        if (isModalOrHasModalChild(ownedWindows[i])) {
          return true;
        }
      }
    }
    return false;
  }

  @RequiredUIAccess
  @Override
  @javax.annotation.Nullable
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
  public Element getState() {
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
    final Rectangle frameBounds = myFrame.getBounds();
    final Element frameElement = new Element(FRAME_ELEMENT);
    element.addContent(frameElement);
    frameElement.setAttribute(X_ATTR, Integer.toString(frameBounds.x));
    frameElement.setAttribute(Y_ATTR, Integer.toString(frameBounds.y));
    frameElement.setAttribute(WIDTH_ATTR, Integer.toString(frameBounds.width));
    frameElement.setAttribute(HEIGHT_ATTR, Integer.toString(frameBounds.height));
    frameElement.setAttribute(EXTENDED_STATE_ATTR, Integer.toString(myFrame.getExtendedState()));

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
  private final class AddFloatingDecoratorCmd extends FinalizableCommand {
    private final DesktopFloatingDecorator myFloatingDecorator;

    /**
     * Creates floating decorator for specified floating decorator.
     */
    private AddFloatingDecoratorCmd(final DesktopInternalDecorator decorator, final WindowInfoImpl info) {
      super(myCommandProcessor);
      myFloatingDecorator = new DesktopFloatingDecorator(myFrame, info.copy(), decorator);
      myId2FloatingDecorator.put(info.getId(), myFloatingDecorator);
      final Rectangle2D bounds = info.getFloatingBounds();
      if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0 && myWindowManager.isInsideScreenBounds(bounds.getX(), bounds.getY(), bounds.getWidth())) {
        myFloatingDecorator.setBounds(TargetAWT.to(bounds));
      }
      else { // place new frame at the center of main frame if there are no floating bounds
        Dimension size = decorator.getSize();
        if (size.width == 0 || size.height == 0) {
          size = decorator.getPreferredSize();
        }
        myFloatingDecorator.setSize(size);
        myFloatingDecorator.setLocationRelativeTo(myFrame);
      }
    }

    @Override
    public void run() {
      try {
        myFloatingDecorator.show();
      }
      finally {
        finish();
      }
    }
  }

  /**
   * This command creates and shows <code>WindowedDecorator</code>.
   */
  private final class AddWindowedDecoratorCmd extends FinalizableCommand {
    private final DesktopWindowedDecorator myWindowedDecorator;

    /**
     * Creates floating decorator for specified floating decorator.
     */
    private AddWindowedDecoratorCmd(@Nonnull DesktopInternalDecorator decorator, @Nonnull WindowInfoImpl info) {
      super(myCommandProcessor);
      myWindowedDecorator = new DesktopWindowedDecorator(myProject, info.copy(), decorator);
      Window window = myWindowedDecorator.getFrame();
      final Rectangle2D bounds = info.getFloatingBounds();
      if (bounds != null && bounds.getWidth() > 0 && bounds.getHeight() > 0 && myWindowManager.isInsideScreenBounds(bounds.getX(), bounds.getY(), bounds.getWidth())) {
        window.setBounds(TargetAWT.to(bounds));
      }
      else { // place new frame at the center of main frame if there are no floating bounds
        Dimension size = decorator.getSize();
        if (size.width == 0 || size.height == 0) {
          size = decorator.getPreferredSize();
        }
        window.setSize(size);
        window.setLocationRelativeTo(myFrame);
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
      try {
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
      finally {
        finish();
      }
    }
  }

  /**
   * This command hides and destroys floating decorator for tool window
   * with specified <code>ID</code>.
   */
  private final class RemoveWindowedDecoratorCmd extends FinalizableCommand {
    private final DesktopWindowedDecorator myWindowedDecorator;

    private RemoveWindowedDecoratorCmd(final WindowInfoImpl info) {
      super(myCommandProcessor);
      myWindowedDecorator = getWindowedDecorator(info.getId());
      myId2WindowedDecorator.remove(info.getId());

      Window frame = myWindowedDecorator.getFrame();
      if (!frame.isShowing()) return;
      Rectangle2D bounds = getRootBounds((JFrame)frame);
      info.setFloatingBounds(bounds);
    }

    @Override
    public void run() {
      try {
        Disposer.dispose(myWindowedDecorator);
      }
      finally {
        finish();
      }
    }

    @Override
    @javax.annotation.Nullable
    public Condition getExpireCondition() {
      return ApplicationManager.getApplication().getDisposed();
    }
  }

  private final class EditorComponentFocusWatcher extends FocusWatcher {
    @Override
    protected void focusedComponentChanged(final Component component, final AWTEvent cause) {
      if (myCommandProcessor.getCommandCount() > 0 || component == null) {
        return;
      }
      final KeyboardFocusManager mgr = KeyboardFocusManager.getCurrentKeyboardFocusManager();
      final Component owner = mgr.getFocusOwner();

      if (owner instanceof EditorComponentImpl && cause instanceof FocusEvent) {
        JFrame frame = WindowManager.getInstance().getFrame(myProject);
        Component oppositeComponent = ((FocusEvent)cause).getOppositeComponent();
        if (oppositeComponent != null && UIUtil.getWindow(oppositeComponent) != frame) {
          return;
        }
      }

      IdeFocusManager.getInstance(myProject).doWhenFocusSettlesDown(new ExpirableRunnable.ForProject(myProject) {
        @Override
        public void run() {
          if (mgr.getFocusOwner() == owner) {
            activateEditorComponent(false);
          }
        }
      });
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
      return myCommandProcessor.getCommandCount() == 0 && comp != null;
    }

    @Override
    protected void focusedComponentChanged(final Component component, final AWTEvent cause) {
      if (myCommandProcessor.getCommandCount() > 0 || component == null) {
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

  @Nonnull
  public AsyncResult<Void> requestDefaultFocus(final boolean forced) {
    return getFocusManagerImpl(myProject).requestFocus(new FocusCommand() {
      @Nonnull
      @Override
      public AsyncResult<Void> run() {
        return processDefaultFocusRequest(forced);
      }
    }, forced);
  }


  private void focusToolWindowByDefault(@javax.annotation.Nullable String idToIgnore) {
    String toFocus = null;

    for (String each : myActiveStack.getStack()) {
      if (idToIgnore != null && idToIgnore.equalsIgnoreCase(each)) continue;

      if (getInfo(each).isVisible()) {
        toFocus = each;
        break;
      }
    }

    if (toFocus == null) {
      for (String each : myActiveStack.getPersistentStack()) {
        if (idToIgnore != null && idToIgnore.equalsIgnoreCase(each)) continue;

        if (getInfo(each).isVisible()) {
          toFocus = each;
          break;
        }
      }
    }

    if (toFocus != null && !ApplicationManager.getApplication().isDisposeInProgress() && !ApplicationManager.getApplication().isDisposed()) {
      activateToolWindow(toFocus, false, true);
    }
  }

  private AsyncResult<Void> processDefaultFocusRequest(boolean forced) {
    if (ModalityState.NON_MODAL.equals(ModalityState.current())) {
      final String activeId = getActiveToolWindowId();
      if (isEditorComponentActive() || activeId == null || getToolWindow(activeId) == null) {
        activateEditorComponent(forced, true);
      }
      else {
        activateToolWindow(activeId, forced, true);
      }

      return AsyncResult.resolved();
    }
    Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    if (activeWindow != null) {
      JRootPane root = null;
      if (activeWindow instanceof JDialog) {
        root = ((JDialog)activeWindow).getRootPane();
      }
      else if (activeWindow instanceof JFrame) {
        root = ((JFrame)activeWindow).getRootPane();
      }

      if (root != null) {
        JComponent toFocus = IdeFocusTraversalPolicy.getPreferredFocusedComponent(root);
        if (toFocus != null) {
          if (DialogWrapper.findInstance(toFocus) != null) {
            return AsyncResult.resolved(); //IDEA-80929
          }
          return IdeFocusManager.findInstanceByComponent(toFocus).requestFocus(toFocus, forced);
        }
      }
    }
    return AsyncResult.rejected();
  }


  /**
   * Delegate method for compatibility with older versions of IDEA
   */
  @Nonnull
  public AsyncResult<Void> requestFocus(@Nonnull Component c, boolean forced) {
    return IdeFocusManager.getInstance(myProject).requestFocus(c, forced);
  }

  @Nonnull
  public AsyncResult<Void> requestFocus(@Nonnull FocusCommand command, boolean forced) {
    return IdeFocusManager.getInstance(myProject).requestFocus(command, forced);
  }

  public void doWhenFocusSettlesDown(@Nonnull Runnable runnable) {
    IdeFocusManager.getInstance(myProject).doWhenFocusSettlesDown(runnable);
  }

  public boolean dispatch(@Nonnull KeyEvent e) {
    return IdeFocusManager.getInstance(myProject).dispatch(e);
  }

  public Expirable getTimestamp(boolean trackOnlyForcedCommands) {
    return IdeFocusManager.getInstance(myProject).getTimestamp(trackOnlyForcedCommands);
  }

  @Nonnull
  private static Rectangle2D getRootBounds(JFrame frame) {
    JRootPane rootPane = frame.getRootPane();
    Rectangle bounds = rootPane.getBounds();
    bounds.setLocation(frame.getX() + rootPane.getX(), frame.getY() + rootPane.getY());
    return TargetAWT.from(bounds);
  }
}
