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
package consulo.desktop.awt.wm.impl;

import consulo.application.Application;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.action.ActionMenu;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.desktop.awt.wm.impl.status.ClockPanel;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.dataContext.BaseDataManager;
import consulo.ide.impl.idea.ide.ui.customization.CustomActionsSchemaImpl;
import consulo.ide.impl.idea.openapi.actionSystem.impl.MenuItemPresentationFactory;
import consulo.ide.impl.idea.openapi.actionSystem.impl.WeakTimerListener;
import consulo.platform.Platform;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.Animator;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public class IdeMenuBar extends JMenuBar implements Predicate<AWTEvent> {
  private static final int COLLAPSED_HEIGHT = 2;

  private enum State {
    EXPANDED,
    COLLAPSING,
    COLLAPSED,
    EXPANDING,
    TEMPORARY_EXPANDED;

    boolean isInProgress() {
      return this == COLLAPSING || this == EXPANDING;
    }
  }

  private final MyTimerListener myTimerListener;
  private List<AnAction> myVisibleActions;
  private List<AnAction> myNewVisibleActions;
  private final MenuItemPresentationFactory myPresentationFactory;
  private final DataManager myDataManager;
  private final ActionManager myActionManager;
  private final Disposable myDisposable = Disposable.newDisposable();
  private boolean myDisabled = false;

  @Nullable
  private final ClockPanel myClockPanel;
  @Nullable
  private final MyExitFullScreenButton myButton;
  @Nullable
  private final Animator myAnimator;
  @Nullable
  private final Timer myActivationWatcher;
  @Nonnull
  private State myState = State.EXPANDED;
  private double myProgress = 0;
  private boolean myActivated = false;
  private final boolean myEnableIcons;
  private final DesktopIdeFrameImpl myFrame;

  public IdeMenuBar(@Nullable DesktopIdeFrameImpl frame,  ActionManager actionManager, DataManager dataManager) {
    myActionManager = actionManager;
    myFrame = frame;
    myTimerListener = new MyTimerListener();
    myVisibleActions = new ArrayList<>();
    myNewVisibleActions = new ArrayList<>();
    myPresentationFactory = new MenuItemPresentationFactory();
    myDataManager = dataManager;
    myEnableIcons = !Platform.current().os().isEnabledTopMenu();

    if (WindowManagerEx.getInstanceEx().isFloatingMenuBarSupported()) {
      myAnimator = new MyAnimator();
      myActivationWatcher = new Timer(100, new MyActionListener());
      myClockPanel = new ClockPanel();
      myButton = new MyExitFullScreenButton();
      add(myClockPanel);
      add(myButton);
      addMouseListener(new MyMouseListener());
    }
    else {
      myAnimator = null;
      myActivationWatcher = null;
      myClockPanel = null;
      myButton = null;
    }
  }

  @Override
  public Border getBorder() {
    //avoid moving lines
    if (myState == State.EXPANDING || myState == State.COLLAPSING) {
      return new EmptyBorder(0, 0, 0, 0);
    }

    //save 1px for mouse handler
    if (myState == State.COLLAPSED) {
      return new EmptyBorder(0, 0, 1, 0);
    }

    return UISettings.getInstance().SHOW_MAIN_TOOLBAR || UISettings.getInstance().SHOW_NAVIGATION_BAR ? super.getBorder() : null;
  }

  @Override
  public void paint(Graphics g) {
    //otherwise, there will be 1px line on top
    if (myState == State.COLLAPSED) {
      return;
    }
    super.paint(g);
  }

  @Override
  public void doLayout() {
    super.doLayout();
    if (myClockPanel != null && myButton != null) {
      if (myState != State.EXPANDED) {
        myClockPanel.setVisible(true);
        myButton.setVisible(true);
        Dimension preferredSize = myButton.getPreferredSize();
        myButton.setBounds(getBounds().width - preferredSize.width, 0, preferredSize.width, preferredSize.height);
        preferredSize = myClockPanel.getPreferredSize();
        myClockPanel.setBounds(getBounds().width - preferredSize.width - myButton.getWidth(), 0, preferredSize.width, preferredSize.height);
      }
      else {
        myClockPanel.setVisible(false);
        myButton.setVisible(false);
      }
    }
  }

  @Override
  public void menuSelectionChanged(boolean isIncluded) {
    if (!isIncluded && myState == State.TEMPORARY_EXPANDED) {
      myActivated = false;
      setState(State.COLLAPSING);
      restartAnimator();
      return;
    }
    if (isIncluded && myState == State.COLLAPSED) {
      myActivated = true;
      setState(State.TEMPORARY_EXPANDED);
      revalidate();
      repaint();
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(() -> {
        JMenu menu = getMenu(getSelectionModel().getSelectedIndex());
        if (menu.isPopupMenuVisible()) {
          menu.setPopupMenuVisible(false);
          menu.setPopupMenuVisible(true);
        }
      });
    }
    super.menuSelectionChanged(isIncluded);
  }

  private boolean isActivated() {
    int index = getSelectionModel().getSelectedIndex();
    return index != -1 && getMenu(index).isPopupMenuVisible();
  }

  private void updateState() {
    if (myAnimator == null) {
      return;
    }

    Window awtComponent = SwingUtilities.getWindowAncestor(this);
    consulo.ui.Window uiWindow = TargetAWT.from(awtComponent);

    if (uiWindow == null) {
      return;
    }

    IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);

    if (ideFrame instanceof IdeFrameEx ideFrameEx) {
      boolean fullScreen = ideFrameEx.isInFullScreen();
      if (fullScreen) {
        setState(State.COLLAPSING);
        restartAnimator();
      }
      else {
        myAnimator.suspend();
        setState(State.EXPANDED);
        if (myClockPanel != null) {
          myClockPanel.setVisible(false);
          myButton.setVisible(false);
        }
      }
    }
  }

  private void setState(@Nonnull State state) {
    myState = state;
    if (myState == State.EXPANDING && myActivationWatcher != null && !myActivationWatcher.isRunning()) {
      myActivationWatcher.start();
    }
    else if (myActivationWatcher != null && myActivationWatcher.isRunning()) {
      if (state == State.EXPANDED || state == State.COLLAPSED) {
        myActivationWatcher.stop();
      }
    }
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension dimension = super.getPreferredSize();
    if (myState.isInProgress()) {
      dimension.height = COLLAPSED_HEIGHT + (int)((myState == State.COLLAPSING ? (1 - myProgress) : myProgress) * (dimension.height - COLLAPSED_HEIGHT));
    }
    else if (myState == State.COLLAPSED) {
      dimension.height = COLLAPSED_HEIGHT;
    }
    return dimension;
  }

  private void restartAnimator() {
    if (myAnimator != null) {
      myAnimator.reset();
      myAnimator.resume();
    }
  }

  @Override
  @RequiredUIAccess
  public void addNotify() {
    super.addNotify();

    updateMenuActions();

    if (myFrame != null) {
      myFrame.addFullScreenListener((v) -> updateState(), myDisposable);
    }

    // Add updater for menus
    myActionManager.addTimerListener(1000, new WeakTimerListener(myTimerListener));
    UISettingsListener UISettingsListener = source -> {
      updateMnemonicsVisibility();
      myPresentationFactory.reset();
    };
    UISettings.getInstance().addUISettingsListener(UISettingsListener, myDisposable);
    Disposer.register(Application.get(), myDisposable);
    IdeEventQueue.getInstance().addDispatcher(this, myDisposable);
  }

  @Override
  public void removeNotify() {
    if (ScreenUtil.isStandardAddRemoveNotify(this)) {
      if (myAnimator != null) {
        myAnimator.suspend();
      }
      Disposer.dispose(myDisposable);
    }
    super.removeNotify();
  }

  @Override
  public boolean test(AWTEvent e) {
    if (e instanceof MouseEvent mouseEvent) {
      Component component = findActualComponent(mouseEvent);

      if (myState != State.EXPANDED /*&& !myState.isInProgress()*/) {
        boolean mouseInside = myActivated || UIUtil.isDescendingFrom(component, this);
        if (e.getID() == MouseEvent.MOUSE_EXITED && e.getSource() == SwingUtilities.windowForComponent(this) && !myActivated) mouseInside = false;
        if (mouseInside && myState == State.COLLAPSED) {
          setState(State.EXPANDING);
          restartAnimator();
        }
        else if (!mouseInside && myState != State.COLLAPSING && myState != State.COLLAPSED) {
          setState(State.COLLAPSING);
          restartAnimator();
        }
      }
    }
    return false;
  }

  @Nullable
  private Component findActualComponent(MouseEvent mouseEvent) {
    Component component = mouseEvent.getComponent();
    if (component == null) {
      return null;
    }

    Component deepestComponent;
    if (myState != State.EXPANDED && !myState.isInProgress()
      && contains(SwingUtilities.convertPoint(component, mouseEvent.getPoint(), this))) {
      deepestComponent = this;
    }
    else {
      deepestComponent = SwingUtilities.getDeepestComponentAt(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
    }
    if (deepestComponent != null) {
      component = deepestComponent;
    }
    return component;
  }

  @RequiredUIAccess
  void updateMenuActions() {
    myNewVisibleActions.clear();

    if (!myDisabled) {
      DataContext dataContext = ((BaseDataManager)myDataManager).getDataContextTest(this);
      expandActionGroup(dataContext, myNewVisibleActions, myActionManager);
    }

    if (!myNewVisibleActions.equals(myVisibleActions)) {
      // should rebuild UI
      final boolean changeBarVisibility = myNewVisibleActions.isEmpty() || myVisibleActions.isEmpty();

      final List<AnAction> temp = myVisibleActions;
      myVisibleActions = myNewVisibleActions;
      myNewVisibleActions = temp;

      removeAll();
      final boolean enableMnemonics = !UISettings.getInstance().DISABLE_MNEMONICS;
      for (final AnAction action : myVisibleActions) {
        add(new ActionMenu(null, ActionPlaces.MAIN_MENU, (ActionGroup)action, myPresentationFactory, enableMnemonics, myEnableIcons));
      }

      updateMnemonicsVisibility();
      if (myClockPanel != null) {
        add(myClockPanel);
        add(myButton);
      }
      validate();

      if (changeBarVisibility) {
        invalidate();
        final JFrame frame = (JFrame)SwingUtilities.getAncestorOfClass(JFrame.class, this);
        if (frame != null) {
          frame.validate();
        }
      }
    }
  }

  @Override
  protected void paintChildren(Graphics g) {
    if (myState.isInProgress()) {
      Graphics2D g2 = (Graphics2D)g;
      AffineTransform oldTransform = g2.getTransform();
      AffineTransform newTransform = oldTransform != null ? new AffineTransform(oldTransform) : new AffineTransform();
      newTransform.concatenate(AffineTransform.getTranslateInstance(0, getHeight() - super.getPreferredSize().height));
      g2.setTransform(newTransform);
      super.paintChildren(g2);
      g2.setTransform(oldTransform);
    }
    else if (myState != State.COLLAPSED) {
      super.paintChildren(g);
    }
  }

  @RequiredUIAccess
  private void expandActionGroup(final DataContext context, final List<AnAction> newVisibleActions, ActionManager actionManager) {
    final ActionGroup mainActionGroup = (ActionGroup)CustomActionsSchemaImpl.getInstance().getCorrectedAction(IdeActions.GROUP_MAIN_MENU);
    if (mainActionGroup == null) return;
    final AnAction[] children = mainActionGroup.getChildren(null);
    for (final AnAction action : children) {
      if (!(action instanceof ActionGroup)) {
        continue;
      }
      final Presentation presentation = myPresentationFactory.getPresentation(action);
      final AnActionEvent e = new AnActionEvent(null, context, ActionPlaces.MAIN_MENU, presentation, actionManager, 0);
      e.setInjectedContext(action.isInInjectedContext());
      action.update(e);
      if (presentation.isVisible()) { // add only visible items
        newVisibleActions.add(action);
      }
    }
  }

  @Override
  public int getMenuCount() {
    int menuCount = super.getMenuCount();
    return myClockPanel != null ? menuCount - 2 : menuCount;
  }

  private void updateMnemonicsVisibility() {
    final boolean enabled = !UISettings.getInstance().DISABLE_MNEMONICS;
    for (int i = 0; i < getMenuCount(); i++) {
      ((ActionMenu)getMenu(i)).setMnemonicEnabled(enabled);
    }
  }

  @RequiredUIAccess
  public void disableUpdates() {
    myDisabled = true;
    updateMenuActions();
  }

  @RequiredUIAccess
  public void enableUpdates() {
    myDisabled = false;
    updateMenuActions();
  }

  private final class MyTimerListener implements TimerListener {
    @Override
    public IdeaModalityState getModalityState() {
      return IdeaModalityState.stateForComponent(IdeMenuBar.this);
    }

    @Override
    @RequiredUIAccess
    public void run() {
      if (!isShowing()) {
        return;
      }

      final Window myWindow = SwingUtilities.windowForComponent(IdeMenuBar.this);
      if (myWindow != null && !myWindow.isActive()) return;

      // do not update when a popup menu is shown (if popup menu contains action which is also in the menu bar, it should not be enabled/disabled)
      final MenuSelectionManager menuSelectionManager = MenuSelectionManager.defaultManager();
      final MenuElement[] selectedPath = menuSelectionManager.getSelectedPath();
      if (selectedPath.length > 0) {
        return;
      }

      // don't update toolbar if there is currently active modal dialog
      final Window window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
      if (window instanceof Dialog dialog) {
        if (dialog.isModal()) {
          return;
        }
      }

      updateMenuActions();
    }
  }

  private class MyAnimator extends Animator {
    public MyAnimator() {
      super("MenuBarAnimator", 16, 300, false);
    }

    @Override
    public void paintNow(int frame, int totalFrames, int cycle) {
      myProgress = (1 - Math.cos(Math.PI * ((float)frame / totalFrames))) / 2;
      revalidate();
      repaint();
    }

    @Override
    protected void paintCycleEnd() {
      myProgress = 1;
      switch (myState) {
        case COLLAPSING:
          setState(State.COLLAPSED);
          break;
        case EXPANDING:
          setState(State.TEMPORARY_EXPANDED);
          break;
        default:
      }
      revalidate();
      if (myState == State.COLLAPSED) {
        //we should repaint parent, to clear 1px on top when menu is collapsed
        getParent().repaint();
      }
      else {
        repaint();
      }
    }
  }

  private class MyActionListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      if (myState == State.EXPANDED || myState == State.EXPANDING) {
        return;
      }
      boolean activated = isActivated();
      if (myActivated && !activated && myState == State.TEMPORARY_EXPANDED) {
        myActivated = false;
        setState(State.COLLAPSING);
        restartAnimator();
      }
      if (activated) {
        myActivated = true;
      }
    }
  }

  private static class MyMouseListener extends MouseAdapter {
    @Override
    public void mousePressed(MouseEvent e) {
      Component c = e.getComponent();
      if (c instanceof IdeMenuBar ideMenuBar) {
        Dimension size = c.getSize();
        Insets insets = ideMenuBar.getInsets();
        Point p = e.getPoint();
        if (p.y < insets.top || p.y >= size.height - insets.bottom) {
          Component item = ideMenuBar.findComponentAt(p.x, size.height / 2);
          if (item instanceof JMenuItem) {
            // re-target border clicks as a menu item ones
            item.dispatchEvent(new MouseEvent(
              item,
              e.getID(),
              e.getWhen(),
              e.getModifiers(),
              1,
              1,
              e.getClickCount(),
              e.isPopupTrigger(),
              e.getButton()
            ));
            e.consume();
            return;
          }
        }
      }

      super.mouseClicked(e);
    }
  }

  private static class MyExitFullScreenButton extends JButton {
    private MyExitFullScreenButton() {
      setFocusable(false);
      addActionListener(e -> {
        Window awtWindow = SwingUtilities.getWindowAncestor(MyExitFullScreenButton.this);
        consulo.ui.Window uiWindow = TargetAWT.from(awtWindow);
        if (uiWindow == null) {
          return;
        }

        IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
        if (ideFrame instanceof IdeFrameEx ideFrameEx) {
          ideFrameEx.toggleFullScreen(false);
        }
      });
      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
          model.setRollover(true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
          model.setRollover(false);
        }
      });
    }

    @Override
    public Dimension getPreferredSize() {
      int height;
      Container parent = getParent();
      if (isVisible() && parent != null) {
        height = parent.getSize().height - parent.getInsets().top - parent.getInsets().bottom;
      }
      else {
        height = super.getPreferredSize().height;
      }
      return new Dimension(height, height);
    }

    @Override
    public Dimension getMaximumSize() {
      return getPreferredSize();
    }

    @Override
    public void paint(Graphics g) {
      Graphics2D g2d = (Graphics2D)g.create();
      try {
        g2d.setColor(UIManager.getColor("Label.background"));
        g2d.fillRect(0, 0, getWidth() + 1, getHeight() + 1);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        double s = (double)getHeight() / 13;
        g2d.translate(s, s);
        Shape plate = new RoundRectangle2D.Double(0, 0, s * 11, s * 11, s, s);
        Color color = UIManager.getColor("Label.foreground");
        boolean hover = model.isRollover() || model.isPressed();
        g2d.setColor(ColorUtil.withAlpha(color, hover ? .25 : .18));
        g2d.fill(plate);
        g2d.setColor(ColorUtil.withAlpha(color, hover ? .4 : .33));
        g2d.draw(plate);
        g2d.setColor(ColorUtil.withAlpha(color, hover ? .7 : .66));
        GeneralPath path = new GeneralPath();
        path.moveTo(s * 2, s * 6);
        path.lineTo(s * 5, s * 6);
        path.lineTo(s * 5, s * 9);
        path.lineTo(s * 4, s * 8);
        path.lineTo(s * 2, s * 10);
        path.quadTo(s * 2 - s / Math.sqrt(2), s * 9 + s / Math.sqrt(2), s, s * 9);
        path.lineTo(s * 3, s * 7);
        path.lineTo(s * 2, s * 6);
        path.closePath();
        g2d.fill(path);
        g2d.draw(path);
        path = new GeneralPath();
        path.moveTo(s * 6, s * 2);
        path.lineTo(s * 6, s * 5);
        path.lineTo(s * 9, s * 5);
        path.lineTo(s * 8, s * 4);
        path.lineTo(s * 10, s * 2);
        path.quadTo(s * 9 + s / Math.sqrt(2), s * 2 - s / Math.sqrt(2), s * 9, s);
        path.lineTo(s * 7, s * 3);
        path.lineTo(s * 6, s * 2);
        path.closePath();
        g2d.fill(path);
        g2d.draw(path);
      }
      finally {
        g2d.dispose();
      }
    }
  }
}
