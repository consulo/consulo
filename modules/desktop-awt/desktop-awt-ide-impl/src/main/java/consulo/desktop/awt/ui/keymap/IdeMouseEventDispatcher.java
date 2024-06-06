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
package consulo.desktop.awt.ui.keymap;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.registry.Registry;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.desktop.awt.wm.FocusManagerImpl;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionUtil;
import consulo.ide.impl.idea.openapi.actionSystem.impl.BasePresentationFactory;
import consulo.ide.impl.idea.openapi.keymap.impl.KeymapManagerImpl;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.MouseShortcutPanel;
import consulo.ide.impl.idea.openapi.wm.impl.IdeGlassPaneImpl;
import consulo.ide.impl.idea.util.ReflectionUtil;
import consulo.platform.Platform;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.internal.ActionManagerEx;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.intellij.lang.annotations.JdkConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import java.util.*;

import static consulo.ui.ex.awt.JBScrollPane.isScrollEvent;
import static java.awt.event.MouseEvent.*;

/**
 * Current implementation of the dispatcher is intended to filter mouse event addressed to
 * the editor. Also it allows to map middle mouse's button to some action.
 *
 * @author Vladimir Kondratyev
 * @author Konstantin Bulenkov
 */
public final class IdeMouseEventDispatcher {
  private final BasePresentationFactory myPresentationFactory = new BasePresentationFactory();
  private final ArrayList<AnAction> myActions = new ArrayList<>(1);
  private final Map<Container, BlockState> myRootPane2BlockedId = new HashMap<>();
  private int myLastHorScrolledComponentHash;
  private boolean myPressedModifiersStored;
  @JdkConstants.InputEventMask
  private int myModifiers;
  @JdkConstants.InputEventMask
  private int myModifiersEx;

  private static boolean myForceTouchIsAllowed = true;

  public static void forbidForceTouch() {
    myForceTouchIsAllowed = false;
  }

  public static boolean isForceTouchAllowed() {
    return myForceTouchIsAllowed;
  }

  // Don't compare MouseEvent ids. Swing has wrong sequence of events: first is mouse_clicked(500)
  // then mouse_pressed(501), mouse_released(502) etc. Here, mouse events sorted so we can compare
  // theirs ids to properly use method blockNextEvents(MouseEvent)
  private static final List<Integer> SWING_EVENTS_PRIORITY = Arrays.asList(MOUSE_PRESSED, MOUSE_ENTERED, MOUSE_EXITED, MOUSE_MOVED, MOUSE_DRAGGED, MOUSE_WHEEL, MOUSE_RELEASED, MOUSE_CLICKED);

  public IdeMouseEventDispatcher() {
  }

  private void fillActionsList(Component component, MouseShortcut mouseShortcut, boolean isModalContext) {
    myActions.clear();

    // here we try to find "local" shortcuts
    for (; component != null; component = component.getParent()) {
      if (component instanceof JComponent) {
        for (AnAction action : ActionUtil.getActions((JComponent)component)) {
          for (Shortcut shortcut : action.getShortcutSet().getShortcuts()) {
            if (mouseShortcut.equals(shortcut) && !myActions.contains(action)) {
              myActions.add(action);
            }
          }
        }
        // once we've found a proper local shortcut(s), we exit
        if (!myActions.isEmpty()) {
          return;
        }
      }
    }

    // search in main keymap
    if (KeymapManagerImpl.ourKeymapManagerInitialized) {
      final KeymapManager keymapManager = KeymapManager.getInstance();
      if (keymapManager != null) {
        final Keymap keymap = keymapManager.getActiveKeymap();
        final String[] actionIds = keymap.getActionIds(mouseShortcut);

        ActionManager actionManager = ActionManager.getInstance();
        for (String actionId : actionIds) {
          AnAction action = actionManager.getAction(actionId);

          if (action == null) continue;

          if (isModalContext && !action.isEnabledInModalContext()) continue;

          if (!myActions.contains(action)) {
            myActions.add(action);
          }
        }
      }
    }
  }

  /**
   * @return <code>true</code> if and only if the passed event is already dispatched by the
   * <code>IdeMouseEventDispatcher</code> and there is no need for any other processing of the event.
   * If the method returns <code>false</code> then it means that the event should be delivered
   * to normal event dispatching.
   */
  @RequiredUIAccess
  public boolean dispatchMouseEvent(MouseEvent e) {
    Component c = e.getComponent();

    //frame activation by mouse click
    if (e.getID() == MOUSE_PRESSED && !c.hasFocus()) {
      if (c instanceof Window) {
        Window awtWindow = (Window)c;

        consulo.ui.Window uiWindow = TargetAWT.from(awtWindow);

        IdeFrame ideFrame = uiWindow == null ? null : uiWindow.getUserData(IdeFrame.KEY);
        if (ideFrame != null) {
          IdeFocusManager focusManager = IdeFocusManager.getGlobalInstance();
          if (focusManager instanceof FocusManagerImpl) {
            Component at = SwingUtilities.getDeepestComponentAt(c, e.getX(), e.getY());
            if (at != null && at.isFocusable()) {
              ((FocusManagerImpl)focusManager).setLastFocusedAtDeactivation(ideFrame, at);
            }
          }
        }
      }
    }

    if (Platform.current().os().isXWindow() && e.isPopupTrigger() && e.getButton() != 3) {
      // we can do better than silly triggering popup on everything but left click
      resetPopupTrigger(e);
    }

    boolean ignore = false;
    if (!(e.getID() == MouseEvent.MOUSE_PRESSED || e.getID() == MouseEvent.MOUSE_RELEASED || e.getID() == MOUSE_WHEEL && 0 < e.getModifiersEx() || e.getID() == MOUSE_CLICKED)) {
      ignore = true;
    }

    patchClickCount(e);

    int clickCount = e.getClickCount();
    int button = MouseShortcut.getButton(e);
    if (button == MouseShortcut.BUTTON_WHEEL_UP || button == MouseShortcut.BUTTON_WHEEL_DOWN) {
      clickCount = 1;
    }

    if (e.isConsumed() ||
        e.isPopupTrigger() ||
        (button > 3 ? e.getID() != MOUSE_PRESSED && e.getID() != MOUSE_WHEEL : e.getID() != MOUSE_RELEASED) ||
        clickCount < 1 ||
        button == NOBUTTON) { // See #16995. It did happen
      ignore = true;
    }

    @JdkConstants.InputEventMask int modifiers = e.getModifiers();
    @JdkConstants.InputEventMask int modifiersEx = e.getModifiersEx();
    if (e.getID() == MOUSE_PRESSED) {
      myPressedModifiersStored = true;
      myModifiers = modifiers;
      myModifiersEx = modifiersEx;
    }
    else if (e.getID() == MOUSE_RELEASED) {
      myForceTouchIsAllowed = true;
      if (myPressedModifiersStored) {
        myPressedModifiersStored = false;
        modifiers = myModifiers;
        modifiersEx = myModifiersEx;
      }
    }

    final JRootPane root = findRoot(e);
    if (root != null) {
      BlockState blockState = myRootPane2BlockedId.get(root);
      if (blockState != null) {
        if (SWING_EVENTS_PRIORITY.indexOf(blockState.currentEventId) < SWING_EVENTS_PRIORITY.indexOf(e.getID())) {
          blockState.currentEventId = e.getID();
          if (blockState.blockMode == IdeEventQueue.BlockMode.COMPLETE) {
            return true;
          }
          else {
            ignore = true;
          }
        }
        else {
          myRootPane2BlockedId.remove(root);
        }
      }
    }

    if (c == null) {
      throw new IllegalStateException("component cannot be null");
    }
    c = SwingUtilities.getDeepestComponentAt(c, e.getX(), e.getY());

    if (c instanceof IdeGlassPaneImpl) {
      c = ((IdeGlassPaneImpl)c).getTargetComponentFor(e);
    }

    if (c == null) { // do nothing if component doesn't contains specified point
      return false;
    }

    if (c instanceof MouseShortcutPanel || c.getParent() instanceof MouseShortcutPanel) {
      return false; // forward mouse processing to the special shortcut panel
    }

    if (isHorizontalScrolling(c, e)) {
      boolean done = doHorizontalScrolling(c, (MouseWheelEvent)e);
      if (done) return true;
    }

    if (ignore) return false;

    // avoid "cyclic component initialization error" in case of dialogs shown because of component initialization failure
    if (!KeymapManagerImpl.ourKeymapManagerInitialized) {
      return false;
    }

    final MouseShortcut shortcut = new MouseShortcut(button, modifiersEx, clickCount);
    fillActionsList(c, shortcut, IdeKeyEventDispatcher.isModalContext(c));
    ActionManagerEx actionManager = ActionManagerEx.getInstanceEx();
    if (actionManager != null) {
      AnAction[] actions = myActions.toArray(new AnAction[myActions.size()]);
      for (AnAction action : actions) {
        DataContext dataContext = DataManager.getInstance().getDataContext(c);
        Presentation presentation = myPresentationFactory.getPresentation(action);
        AnActionEvent actionEvent = new AnActionEvent(e, dataContext, ActionPlaces.MAIN_MENU, presentation, ActionManager.getInstance(), modifiers);
        action.beforeActionPerformedUpdate(actionEvent);

        if (ActionUtil.lastUpdateAndCheckDumb(action, actionEvent, false)) {
          actionManager.fireBeforeActionPerformed(action, dataContext, actionEvent);
          final Component context = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);

          if (context != null && !context.isShowing()) continue;

          ActionUtil.performActionDumbAware(action, actionEvent);
          actionManager.fireAfterActionPerformed(action, dataContext, actionEvent);
          e.consume();
        }
      }
    }
    return e.getButton() > 3;
  }

  private static void resetPopupTrigger(final MouseEvent e) {
    ReflectionUtil.setField(MouseEvent.class, e, boolean.class, "popupTrigger", false);
  }

  /**
   * This method patches event if it concerns side buttons like 4 (Backward) or 5 (Forward)
   * AND it's not single-click event. We won't support double-click for side buttons.
   * Also some JDK bugs produce zero-click events for side buttons.
   *
   * @return true if event was patched
   */
  public static boolean patchClickCount(final MouseEvent e) {
    if (e.getClickCount() != 1 && e.getButton() > 3) {
      ReflectionUtil.setField(MouseEvent.class, e, int.class, "clickCount", 1);
      return true;
    }
    return false;
  }

  private boolean doHorizontalScrolling(Component c, MouseWheelEvent me) {
    final JScrollBar scrollBar = findHorizontalScrollBar(c);
    if (scrollBar != null) {
      if (scrollBar.hashCode() != myLastHorScrolledComponentHash) {
        FeatureUsageTracker.getInstance().triggerFeatureUsed("ui.horizontal.scrolling");
        myLastHorScrolledComponentHash = scrollBar.hashCode();
      }
      scrollBar.setValue(scrollBar.getValue() + getScrollAmount(c, me, scrollBar));
      return true;
    }
    return false;
  }

  public void resetHorScrollingTracker() {
    myLastHorScrolledComponentHash = 0;
  }

  private static int getScrollAmount(Component c, MouseWheelEvent me, JScrollBar scrollBar) {
    final int scrollBarWidth = scrollBar.getWidth();
    final int ratio = Registry.is("ide.smart.horizontal.scrolling") && scrollBarWidth > 0
                      ? Math.max((int)Math.pow(c.getWidth() / scrollBarWidth, 2), 10)
                      : 10; // do annoying scrolling faster if smart scrolling is on
    return me.getUnitsToScroll() * scrollBar.getUnitIncrement() * ratio;
  }

  private static boolean isHorizontalScrolling(Component c, MouseEvent e) {
    if (c != null && e instanceof MouseWheelEvent && (!Platform.current().os().isMac() || false)) {
      final MouseWheelEvent mwe = (MouseWheelEvent)e;
      return mwe.isShiftDown() && mwe.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL && isScrollEvent(mwe) && findHorizontalScrollBar(c) != null;
    }
    return false;
  }

  @Nullable
  private static JScrollBar findHorizontalScrollBar(Component c) {
    if (c == null) return null;
    if (c instanceof JScrollPane) {
      JScrollBar scrollBar = ((JScrollPane)c).getHorizontalScrollBar();
      return scrollBar != null && scrollBar.isVisible() ? scrollBar : null;
    }

    return findHorizontalScrollBar(c.getParent());
  }

  public void blockNextEvents(final MouseEvent e, IdeEventQueue.BlockMode blockMode) {
    final JRootPane root = findRoot(e);
    if (root == null) return;

    myRootPane2BlockedId.put(root, new BlockState(e.getID(), blockMode));
  }

  @Nullable
  private static JRootPane findRoot(MouseEvent e) {
    final Component parent = UIUtil.findUltimateParent(e.getComponent());
    JRootPane root = null;

    if (parent instanceof JWindow) {
      root = ((JWindow)parent).getRootPane();
    }
    else if (parent instanceof JDialog) {
      root = ((JDialog)parent).getRootPane();
    }
    else if (parent instanceof JFrame) {
      root = ((JFrame)parent).getRootPane();
    }

    return root;
  }

  public static void requestFocusInNonFocusedWindow(@Nonnull MouseEvent event) {
    if (event.getID() == MOUSE_PRESSED) {
      // request focus by mouse pressed before focus settles down
      requestFocusInNonFocusedWindow(event.getComponent());
    }
  }

  private static void requestFocusInNonFocusedWindow(@Nullable Component component) {
    Window window = UIUtil.getWindow(component);
    if (window != null && !UIUtil.isFocusAncestor(window)) {
      Component focusable = UIUtil.isFocusable(component) ? component : findDefaultFocusableComponent(component);
      if (focusable != null) focusable.requestFocus();
    }
  }

  @Nullable
  private static Component findDefaultFocusableComponent(@Nullable Component component) {
    Container provider = findFocusTraversalPolicyProvider(component);
    return provider == null ? null : provider.getFocusTraversalPolicy().getDefaultComponent(provider);
  }

  @Nullable
  private static Container findFocusTraversalPolicyProvider(@Nullable Component component) {
    Container container = component == null || component instanceof Container ? (Container)component : component.getParent();
    while (container != null) {
      // ensure that container is focus cycle root and provides focus traversal policy
      // it means that Container.getFocusTraversalPolicy() returns non-null object
      if (container.isFocusCycleRoot() && container.isFocusTraversalPolicyProvider()) return container;
      container = container.getParent();
    }
    return null;
  }

  private static class BlockState {
    private int currentEventId;
    private final IdeEventQueue.BlockMode blockMode;

    private BlockState(int id, IdeEventQueue.BlockMode mode) {
      currentEventId = id;
      blockMode = mode;
    }
  }
}
