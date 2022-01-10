/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.HelpTooltip;
import com.intellij.ide.actions.ActivateToolWindowAction;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.KeymapManagerListener;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.WindowInfo;
import com.intellij.ui.MouseDragHelper;
import com.intellij.ui.PopupHandler;
import com.intellij.util.ui.JBImageIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ToolWindowStripeButton;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * @author Eugene Belyaev
 * @author Vladimir Kondratyev
 */
public final class DesktopStripeButton extends AnchoredButton implements ActionListener, Disposable, ToolWindowStripeButton {
  private final Color ourBackgroundColor = new Color(247, 243, 239);

  /**
   * This is analog of Swing mnemomic. We cannot use the standard ones
   * because it causes typing of "funny" characters into the editor.
   */
  private int myMnemonic;
  private final DesktopInternalDecorator myDecorator;
  private boolean myPressedWhenSelected;

  private JLayeredPane myDragPane;
  private final DesktopToolWindowPanelImpl myPane;
  private JLabel myDragButtonImage;
  private Point myPressedPoint;
  private DesktopStripePanelImpl myLastStripe;
  private KeyEventDispatcher myDragKeyEventDispatcher;
  private boolean myDragCancelled = false;
  private final DesktopStripeButton.MyKeymapListener myKeymapListener;

  DesktopStripeButton(@Nonnull final DesktopInternalDecorator decorator, DesktopToolWindowPanelImpl pane) {
    myDecorator = decorator;
    myKeymapListener = new MyKeymapListener();
    myPane = pane;

    setFocusable(false);
    setBackground(ourBackgroundColor);
    setBorder(JBUI.Borders.empty(5, 5, 0, 5));
    updatePresentation();
    apply(myDecorator.getWindowInfo());
    addActionListener(this);
    addMouseListener(new MyPopupHandler());
    setRolloverEnabled(true);
    setOpaque(false);

    enableEvents(AWTEvent.MOUSE_EVENT_MASK);

    addMouseMotionListener(new MouseMotionAdapter() {
      @Override
      public void mouseDragged(final MouseEvent e) {
        processDrag(e);
      }
    });
    KeymapManager.getInstance().addKeymapManagerListener(myKeymapListener, this);

    updateHelpTooltip();
  }

  private void updateHelpTooltip() {
    DesktopToolWindowImpl toolWindow = myDecorator.getToolWindow();

    HelpTooltip.dispose(this);

    HelpTooltip tooltip = new HelpTooltip();
    tooltip.setTitle(toolWindow.getDisplayName().getValue());
    String activateActionId = ActivateToolWindowAction.getActionIdForToolWindow(toolWindow.getId());
    tooltip.setShortcut(ActionManager.getInstance().getKeyboardShortcut(activateActionId));
    tooltip.installOn(this);
  }

  @Nonnull
  @Override
  public consulo.ui.Component getComponent() {
    throw new UnsupportedOperationException();
  }

  /**
   * We are using the trick here: the method does all things that super method does
   * excepting firing of the MNEMONIC_CHANGED_PROPERTY event. After that mnemonic
   * doesn't work via standard Swing rules (processing of Alt keystrokes).
   */
  @Override
  public void setMnemonic(final int mnemonic) {
    throw new UnsupportedOperationException("use setMnemonic2(int)");
  }

  private void setMnemonic2(final int mnemonic) {
    myMnemonic = mnemonic;
    updateHelpTooltip();
    revalidate();
    repaint();
  }

  @Override
  public int getMnemonic2() {
    return myMnemonic;
  }

  @Override
  public ToolWindowAnchor getAnchor() {
    return getWindowInfo().getAnchor();
  }

  @Override
  @Nonnull
  public WindowInfoImpl getWindowInfo() {
    return myDecorator.getWindowInfo();
  }

  public boolean isFirst() {
    return is(true);
  }

  public boolean isLast() {
    return is(false);
  }

  public boolean isOppositeSide() {
    return getWindowInfo().isSplit();
  }

  private boolean is(boolean first) {
    Container parent = getParent();
    if (parent == null) return false;

    int max = first ? Integer.MAX_VALUE : 0;
    ToolWindowAnchor anchor = getAnchor();
    Component c = null;
    int count = parent.getComponentCount();
    for (int i = 0; i < count; i++) {
      Component component = parent.getComponent(i);
      if (!component.isVisible()) continue;
      Rectangle r = component.getBounds();
      if (anchor == ToolWindowAnchor.LEFT || anchor == ToolWindowAnchor.RIGHT) {
        if (first && max > r.y || !first && max < r.y) {
          max = r.y;
          c = component;
        }
      } else {
        if (first && max > r.x || !first && max < r.x) {
          max = r.x;
          c = component;
        }
      }
    }


    return c == this;
  }

  @Nonnull
  public DesktopInternalDecorator getDecorator() {
    return myDecorator;
  }

  private void processDrag(final MouseEvent e) {
    if (myDragCancelled || !MouseDragHelper.checkModifiers(e)) return;
    if (!isDraggingNow()) {
      if (myPressedPoint == null) return;
      if (isWithinDeadZone(e)) return;

      myDragPane = findLayeredPane(e);
      if (myDragPane == null) return;
      int width = getWidth() - 1; // -1 because StripeButtonUI.paint will not paint 1 pixel in case (anchor == ToolWindowAnchor.LEFT)
      int height = getHeight() - 1; // -1 because StripeButtonUI.paint will not paint 1 pixel in case (anchor.isHorizontal())
      BufferedImage image = UIUtil.createImage(e.getComponent(), width, height, BufferedImage.TYPE_INT_RGB);
      Graphics graphics = image.getGraphics();
      graphics.setColor(UIUtil.getBgFillColor(getParent()));
      graphics.fillRect(0, 0, width, height);
      paint(graphics);
      graphics.dispose();
      myDragButtonImage = new JLabel(new JBImageIcon(image)) {

        public String toString() {
          return "Image for: " + DesktopStripeButton.this.toString();
        }
      };

      myDragButtonImage.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent e) {
          finishDragging();
          myPressedPoint = null;
          myDragButtonImage = null;
          super.mouseReleased(e);
        }
      });
      myDragPane.add(myDragButtonImage, JLayeredPane.POPUP_LAYER);
      myDragButtonImage.setSize(myDragButtonImage.getPreferredSize());
      setVisible(false);
      myPane.startDrag();
      myDragKeyEventDispatcher = new DragKeyEventDispatcher();
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(myDragKeyEventDispatcher);
    }
    if (!isDraggingNow()) return;

    Point xy = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), myDragPane);
    if (myPressedPoint != null) {
      xy.x -= myPressedPoint.x;
      xy.y -= myPressedPoint.y;
    }
    myDragButtonImage.setLocation(xy);

    SwingUtilities.convertPointToScreen(xy, myDragPane);

    final DesktopStripePanelImpl stripe = myPane.getStripeFor(new Rectangle(xy, myDragButtonImage.getSize()), (DesktopStripePanelImpl)getParent());
    if (stripe == null) {
      if (myLastStripe != null) {
        myLastStripe.resetDrop();
      }
    } else {
      if (myLastStripe != null && myLastStripe != stripe) {
        myLastStripe.resetDrop();
      }
      stripe.processDropButton(this, myDragButtonImage, xy);
    }

    myLastStripe = stripe;
  }

  private class DragKeyEventDispatcher implements KeyEventDispatcher {
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
      if (isDraggingNow() && e.getKeyCode() == KeyEvent.VK_ESCAPE && e.getID() == KeyEvent.KEY_PRESSED) {
        myDragCancelled = true;
        finishDragging();
        return true;
      }
      return false;
    }
  }

  private boolean isWithinDeadZone(final MouseEvent e) {
    return Math.abs(myPressedPoint.x - e.getPoint().x) < MouseDragHelper.DRAG_START_DEADZONE && Math.abs(myPressedPoint.y - e.getPoint().y) < MouseDragHelper
            .DRAG_START_DEADZONE;
  }

  @Nullable
  private static JLayeredPane findLayeredPane(MouseEvent e) {
    if (!(e.getComponent() instanceof JComponent)) return null;
    final JRootPane root = ((JComponent)e.getComponent()).getRootPane();
    return root.getLayeredPane();
  }

  @Override
  protected void processMouseEvent(final MouseEvent e) {
    if (e.isPopupTrigger() && e.getComponent().isShowing()) {
      super.processMouseEvent(e);
      return;
    }

    if (UIUtil.isCloseClick(e)) {
      myDecorator.fireHiddenSide();
      return;
    }

    if (e.getButton() == MouseEvent.BUTTON1) {
      if (MouseEvent.MOUSE_PRESSED == e.getID()) {
        myPressedPoint = e.getPoint();
        myPressedWhenSelected = isSelected();
        myDragCancelled = false;
      }
      else if (MouseEvent.MOUSE_RELEASED == e.getID()) {
        finishDragging();
        myPressedPoint = null;
        myDragButtonImage = null;
      }
    }

    super.processMouseEvent(e);
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    if (myPressedWhenSelected) {
      myDecorator.fireHidden();
    }
    else {
      myDecorator.fireActivated();
    }
    myPressedWhenSelected = false;
    FeatureUsageTracker.getInstance().triggerFeatureUsed("toolwindow.clickstat." + myDecorator.getToolWindow().getId());
  }

  @Override
  public void apply(@Nonnull WindowInfo info) {
    setSelected(info.isVisible() || info.isActive());
    updateState();
  }

  @Override
  public void dispose() {
  }

  private void showPopup(final Component component, final int x, final int y) {
    final ActionGroup group = myDecorator.createPopupGroup();
    final ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group);
    popupMenu.getComponent().show(component, x, y);
  }

  @Override
  public void updateUI() {
    setUI(DesktopStripeButtonUI.createUI(this));
    Font font = UIUtil.getLabelFont(UIUtil.FontSize.SMALL);
    setFont(font);

    // update text if localize was changed
    updateText();
  }

  @RequiredUIAccess
  @Override
  public void updatePresentation() {
    updateState();
    updateText();
    Image image = myDecorator.getToolWindow().getIcon();
    setIcon(TargetAWT.to(image));
    setDisabledIcon(image == null ? null : TargetAWT.to(ImageEffects.grayed(image)));
  }

  @RequiredUIAccess
  private void updateText() {
    // called from component base initialize updateUI()
    if(myDecorator == null) {
      return;
    }

    String text = myDecorator.getToolWindow().getDisplayName().getValue();
    if (UISettings.getInstance().SHOW_TOOL_WINDOW_NUMBERS) {
      String toolWindowId = myDecorator.getToolWindow().getId();
      int mnemonic = ActivateToolWindowAction.getMnemonicForToolWindow(toolWindowId);
      if (mnemonic != -1) {
        text = (char)mnemonic + ": " + text;
        setMnemonic2(mnemonic);
      }
      else {
        setMnemonic2(0);
      }
    }
    setText(text);
    updateHelpTooltip();
  }

  private void updateState() {
    DesktopToolWindowImpl window = myDecorator.getToolWindow();
    boolean toShow = window.isAvailable() || window.isPlaceholderMode();
    if (UISettings.getInstance().ALWAYS_SHOW_WINDOW_BUTTONS) {
      setVisible(window.isShowStripeButton() || isSelected());
    }
    else {
      setVisible(toShow && (window.isShowStripeButton() || isSelected()));
    }
    setEnabled(toShow && !window.isPlaceholderMode());
  }

  private final class MyPopupHandler extends PopupHandler {
    @Override
    public void invokePopup(final Component component, final int x, final int y) {
      showPopup(component, x, y);
    }
  }

  private final class MyKeymapListener implements KeymapManagerListener {
    @Override
    public void activeKeymapChanged(Keymap keymap) {
      updatePresentation();
    }
  }

  private boolean isDraggingNow() {
    return myDragButtonImage != null;
  }

  private void finishDragging() {
    if (!isDraggingNow()) return;
    myDragPane.remove(myDragButtonImage);
    myDragButtonImage = null;
    myPane.stopDrag();
    myDragPane.repaint();
    setVisible(true);
    if (myLastStripe != null) {
      myLastStripe.finishDrop();
      myLastStripe = null;
    }
    if (myDragKeyEventDispatcher != null) {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(myDragKeyEventDispatcher);
      myDragKeyEventDispatcher = null;
    }
  }

  @Override
  public String toString() {
    return StringUtil.getShortName(getClass().getName()) + " text: " + getText();
  }
}
