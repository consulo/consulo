/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.actionSystem.impl;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.util.IconLoader;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ActionButton extends JComponent implements ActionButtonComponent, AnActionHolder {
  private static final String uiClassID = "ActionButtonUI";

  @Deprecated
  private static final Icon ourEmptyIcon = EmptyIcon.ICON_18;
  private static final int ourEmptyIconSize = 18;

  private Dimension myMinimumButtonSize;
  private PropertyChangeListener myActionButtonSynchronizer;
  private Icon myDisabledIcon;
  private Icon myIcon;
  protected final Presentation myPresentation;
  protected final AnAction myAction;
  protected final String myPlace;
  private boolean myMouseDown;
  private boolean myRollover;
  private static boolean ourGlobalMouseDown;

  private boolean myNoIconsInPopup;
  private Insets myInsets;

  private boolean myMinimalMode;
  private boolean myDecorateButtons;

  public ActionButton(final AnAction action, final Presentation presentation, final String place, @NotNull final Dimension minimumSize) {
    setMinimumButtonSize(minimumSize);
    setIconInsets(null);
    myRollover = false;
    myMouseDown = false;
    myAction = action;
    myPresentation = presentation;
    myPlace = place;
    setFocusable(false);
    enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    myMinimumButtonSize = minimumSize;

    putClientProperty(UIUtil.CENTER_TOOLTIP_DEFAULT, Boolean.TRUE);

    updateUI();
  }

  public void setMinimalMode(boolean minimalMode) {
    myMinimalMode = minimalMode;
  }

  public void setDecorateButtons(boolean decorateButtons) {
    myDecorateButtons = decorateButtons;
  }

  public boolean isMinimalMode() {
    return myMinimalMode;
  }

  public boolean isDecorateButtons() {
    return myDecorateButtons;
  }

  public void setNoIconsInPopup(boolean noIconsInPopup) {
    myNoIconsInPopup = noIconsInPopup;
  }

  public void setMinimumButtonSize(@NotNull Dimension size) {
    myMinimumButtonSize = size;
  }

  @Override
  public int getPopState() {
    if (myAction instanceof Toggleable) {
      Boolean selected = (Boolean)myPresentation.getClientProperty(Toggleable.SELECTED_PROPERTY);
      boolean flag1 = selected != null && selected;
      return getPopState(flag1);
    }
    else {
      return getPopState(false);
    }
  }

  public boolean isButtonEnabled() {
    return isEnabled() && myPresentation.isEnabled();
  }

  private void onMousePresenceChanged(boolean setInfo) {
    ActionMenu.showDescriptionInStatusBar(setInfo, this, myPresentation.getDescription());
  }

  public void click() {
    performAction(new MouseEvent(this, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false));
  }

  private void performAction(MouseEvent e) {
    AnActionEvent event = new AnActionEvent(e, getDataContext(), myPlace, myPresentation, ActionManager.getInstance(), e.getModifiers());
    if (!ActionUtil.lastUpdateAndCheckDumb(myAction, event, false)) {
      return;
    }

    if (isButtonEnabled()) {
      final ActionManagerEx manager = ActionManagerEx.getInstanceEx();
      final DataContext dataContext = event.getDataContext();
      manager.fireBeforeActionPerformed(myAction, dataContext, event);
      Component component = PlatformDataKeys.CONTEXT_COMPONENT.getData(dataContext);
      if (component != null && !component.isShowing()) {
        return;
      }
      actionPerformed(event);
      manager.queueActionPerformedEvent(myAction, dataContext, event);
    }
  }

  protected DataContext getDataContext() {
    ActionToolbar actionToolbar = UIUtil.getParentOfType(ActionToolbar.class, this);
    return actionToolbar != null ? actionToolbar.getToolbarDataContext() : DataManager.getInstance().getDataContext();
  }

  private void actionPerformed(final AnActionEvent event) {
    if (myAction instanceof ActionGroup && !(myAction instanceof CustomComponentAction) && ((ActionGroup)myAction).isPopup()) {
      final ActionManagerImpl am = (ActionManagerImpl)ActionManager.getInstance();
      ActionPopupMenuImpl popupMenu = (ActionPopupMenuImpl)am.createActionPopupMenu(event.getPlace(), (ActionGroup)myAction, new MenuItemPresentationFactory() {
        @Override
        protected void processPresentation(Presentation presentation) {
          if (myNoIconsInPopup) {
            presentation.setIcon(null);
            presentation.setHoveredIcon(null);
          }
        }
      });
      popupMenu.setDataContextProvider(ActionButton.this::getDataContext);
      if (ActionPlaces.isToolbarPlace(event.getPlace())) {
        popupMenu.getComponent().show(this, 0, getHeight());
      }
      else {
        popupMenu.getComponent().show(this, getWidth(), 0);
      }

    }
    else {
      ActionUtil.performActionDumbAware(myAction, event);
    }
  }

  @Override
  public void removeNotify() {
    if (myActionButtonSynchronizer != null) {
      myPresentation.removePropertyChangeListener(myActionButtonSynchronizer);
      myActionButtonSynchronizer = null;
    }
    super.removeNotify();
  }

  @Override
  public void addNotify() {
    super.addNotify();
    if (myActionButtonSynchronizer == null) {
      myActionButtonSynchronizer = new ActionButtonSynchronizer();
      myPresentation.addPropertyChangeListener(myActionButtonSynchronizer);
    }
    updateToolTipText();
    updateIcon();
  }

  @Override
  public void setToolTipText(String s) {
    String tooltipText = KeymapUtil.createTooltipText(s, myAction);
    super.setToolTipText(tooltipText.length() > 0 ? tooltipText : null);
  }

  @Override
  public Dimension getPreferredSize() {
    Icon icon = getIcon();
    if (icon.getIconWidth() < myMinimumButtonSize.width && icon.getIconHeight() < myMinimumButtonSize.height) {
      return myMinimumButtonSize;
    }
    else {
      return new Dimension(icon.getIconWidth() + myInsets.left + myInsets.right, icon.getIconHeight() + myInsets.top + myInsets.bottom);
    }
  }

  public void setIconInsets(@Nullable Insets insets) {
    myInsets = insets != null ? insets : JBUI.emptyInsets();
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  /**
   * @return button's icon. Icon depends on action's state. It means that the method returns
   * disabled icon if action is disabled. If the action's icon is <code>null</code> then it returns
   * an empty icon.
   */
  public Icon getIcon() {
    Icon icon = isButtonEnabled() ? myIcon : myDisabledIcon;
    if (icon == null) {
      icon = JBUI.emptyIcon(ourEmptyIconSize);
    }
    return icon;
  }

  public void updateIcon() {
    myIcon = myPresentation.getIcon();
    if (myPresentation.getDisabledIcon() != null) { // set disabled icon if it is specified
      myDisabledIcon = myPresentation.getDisabledIcon();
    }
    else {
      myDisabledIcon = IconLoader.getDisabledIcon(myIcon);
    }
  }

  private void setDisabledIcon(Icon icon) {
    myDisabledIcon = icon;
  }

  void updateToolTipText() {
    String text = myPresentation.getText();
    setToolTipText(text == null ? myPresentation.getDescription() : text);
  }

  @Override
  public String getUIClassID() {
    return uiClassID;
  }

  @Override
  public void updateUI() {
    setUI(UIManager.getUI(this));
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    super.processMouseEvent(e);
    if (e.isConsumed()) return;
    boolean skipPress = e.isMetaDown() || e.getButton() != MouseEvent.BUTTON1;
    switch (e.getID()) {
      case MouseEvent.MOUSE_PRESSED:
        if (skipPress || !isButtonEnabled()) return;
        myMouseDown = true;
        ourGlobalMouseDown = true;
        repaint();
        break;

      case MouseEvent.MOUSE_RELEASED:
        if (skipPress || !isButtonEnabled()) return;
        myMouseDown = false;
        ourGlobalMouseDown = false;
        if (myRollover) {
          performAction(e);
        }
        repaint();
        break;

      case MouseEvent.MOUSE_ENTERED:
        if (!myMouseDown && ourGlobalMouseDown) break;
        myRollover = true;
        repaint();
        onMousePresenceChanged(true);
        break;

      case MouseEvent.MOUSE_EXITED:
        myRollover = false;
        if (!myMouseDown && ourGlobalMouseDown) break;
        repaint();
        onMousePresenceChanged(false);
        break;
    }
  }

  private int getPopState(boolean isPushed) {
    if (isPushed || myRollover && myMouseDown && isButtonEnabled()) {
      return PUSHED;
    }
    else {
      return !myRollover || !isButtonEnabled() ? NORMAL : POPPED;
    }
  }

  public Presentation getPresentation() {
    return myPresentation;
  }

  @Override
  public AnAction getAction() {
    return myAction;
  }

  private class ActionButtonSynchronizer implements PropertyChangeListener {
    @NonNls protected static final String SELECTED_PROPERTY_NAME = "selected";

    @Override
    public void propertyChange(PropertyChangeEvent e) {
      String propertyName = e.getPropertyName();
      switch (propertyName) {
        case Presentation.PROP_TEXT:
          updateToolTipText();
          break;
        case Presentation.PROP_ENABLED:
          repaint();
          break;
        case Presentation.PROP_ICON:
          updateIcon();
          repaint();
          break;
        case Presentation.PROP_DISABLED_ICON:
          setDisabledIcon(myPresentation.getDisabledIcon());
          repaint();
          break;
        case Presentation.PROP_VISIBLE:
          break;
        case SELECTED_PROPERTY_NAME:
          repaint();
          break;
      }
    }
  }
}
