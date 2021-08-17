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
package com.intellij.openapi.actionSystem.impl;

import com.intellij.ide.DataManager;
import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.ex.TooltipDescriptionProvider;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.TextWithMnemonic;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.accessibility.ScreenReader;
import consulo.annotation.DeprecationInfo;
import consulo.awt.TargetAWT;
import consulo.localize.LocalizeValue;
import consulo.ui.Size;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.dataholder.Key;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import javax.accessibility.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Set;

import static java.awt.event.KeyEvent.VK_SPACE;

public class ActionButton extends JComponent implements ActionButtonComponent, AnActionHolder, Accessible {
  private static final String uiClassID = "ActionButtonUI";
  /**
   * By default button representing popup action group displays 'dropdown' icon.
   * This key allows to avoid 'dropdown' icon painting, just put it in ActionButton's presentation or template presentation of ActionGroup like this:
   * <code>presentation.putClientProperty(ActionButton.HIDE_DROPDOWN_ICON, Boolean.TRUE)</code>
   */

  public static final Key<Boolean> HIDE_DROPDOWN_ICON = Key.create("HIDE_DROPDOWN_ICON");
  // Contains actions IDs which descriptions are permitted for displaying in the ActionButton tooltip
  private static final Set<String> WHITE_LIST = Set.of();

  private Size myMinimumButtonSize;
  private PropertyChangeListener myPresentationListener;
  private Image myDisabledIcon;
  private Image myIcon;
  protected final Presentation myPresentation;
  protected final AnAction myAction;
  protected final String myPlace;
  private boolean myMouseDown;
  private boolean myRollover;
  private static boolean ourGlobalMouseDown = false;

  private boolean myNoIconsInPopup = false;
  private Insets myInsets;

  private boolean myMinimalMode;
  private boolean myDecorateButtons;

  private boolean myWithoutBorder;

  private int myDisplayedMnemonicIndex = -1;
  protected String myLastComputedText = "";

  @Deprecated
  @DeprecationInfo("Use constructor with Size parameter")
  public ActionButton(AnAction action, Presentation presentation, String place, @Nonnull Dimension minimumSize) {
    this(action, presentation, place, new Size(minimumSize.width, minimumSize.height));
  }

  public ActionButton(AnAction action, Presentation presentation, String place, @Nonnull Size minimumSize) {
    setMinimumButtonSize(minimumSize);
    setIconInsets(null);
    myRollover = false;
    myMouseDown = false;
    myAction = action;
    myPresentation = presentation;
    myPlace = place;
    // Button should be focusable if screen reader is active
    setFocusable(ScreenReader.isActive());
    enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    // Pressing the SPACE key is the same as clicking the button
    addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        if (e.getModifiers() == 0 && e.getKeyCode() == VK_SPACE) {
          click();
        }
      }
    });
    addFocusListener(new FocusListener() {
      @Override
      public void focusGained(FocusEvent e) {
        repaint();
      }

      @Override
      public void focusLost(FocusEvent e) {
        repaint();
      }
    });

    putClientProperty(UIUtil.CENTER_TOOLTIP_DEFAULT, Boolean.TRUE);

    updateTextAndMnemonic(presentation.getTextValue());

    updateUI();
  }

  public int getDisplayedMnemonicIndex() {
    return myDisplayedMnemonicIndex;
  }

  public void setDisplayedMnemonicIndex(int displayedMnemonicIndex) {
    myDisplayedMnemonicIndex = displayedMnemonicIndex;
  }

  public boolean isWithoutBorder() {
    return myWithoutBorder;
  }

  public void setWithoutBorder(boolean withoutBorder) {
    myWithoutBorder = withoutBorder;
  }

  public boolean shallPaintDownArrow() {
    if (!(myAction instanceof ActionGroup && ((ActionGroup)myAction).isPopup())) return false;
    if (Boolean.TRUE == myAction.getTemplatePresentation().getClientProperty(HIDE_DROPDOWN_ICON)) return false;
    if (Boolean.TRUE == myPresentation.getClientProperty(HIDE_DROPDOWN_ICON)) return false;
    return true;
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

  public void setMinimumButtonSize(@Nonnull Size size) {
    myMinimumButtonSize = size;
  }

  @Override
  public int getPopState() {
    if (myAction instanceof Toggleable) {
      return getPopState(Toggleable.isSelected(myPresentation));
    }
    else {
      return getPopState(false);
    }
  }

  public Presentation getPresentation() {
    return myPresentation;
  }

  @Override
  public boolean isEnabled() {
    return super.isEnabled() && myPresentation.isEnabled();
  }

  public boolean isButtonEnabled() {
    return isEnabled();
  }

  private void onMousePresenceChanged(boolean setInfo) {
    ActionMenu.showDescriptionInStatusBar(setInfo, this, myPresentation.getDescription());
  }

  public void click() {
    performAction(new MouseEvent(this, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false));
  }

  private void performAction(MouseEvent e) {
    AnActionEvent event = AnActionEvent.createFromInputEvent(e, myPlace, myPresentation, getDataContext(), false, true);
    if (!ActionUtil.lastUpdateAndCheckDumb(myAction, event, false)) {
      return;
    }

    if (isButtonEnabled()) {
      final ActionManagerEx manager = ActionManagerEx.getInstanceEx();
      final DataContext dataContext = event.getDataContext();
      manager.fireBeforeActionPerformed(myAction, dataContext, event);
      Component component = dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT);
      if (component != null && !component.isShowing()) {
        return;
      }
      actionPerformed(event);
      manager.queueActionPerformedEvent(myAction, dataContext, event);
      if (event.getInputEvent() instanceof MouseEvent) {
        //FIXME [VISTALL] we need that ?ToolbarClicksCollector.record(myAction, myPlace);
      }
    }
  }

  protected DataContext getDataContext() {
    ActionToolbar actionToolbar = UIUtil.getParentOfType(ActionToolbar.class, this);
    return actionToolbar != null ? actionToolbar.getToolbarDataContext() : DataManager.getInstance().getDataContext();
  }

  private void actionPerformed(final AnActionEvent event) {
    if (myAction instanceof ActionGroup && !(myAction instanceof CustomComponentAction) && ((ActionGroup)myAction).isPopup() && !((ActionGroup)myAction).canBePerformed(event.getDataContext())) {
      final ActionManagerImpl am = (ActionManagerImpl)ActionManager.getInstance();
      DesktopActionPopupMenuImpl popupMenu = (DesktopActionPopupMenuImpl)am.createActionPopupMenu(event.getPlace(), (ActionGroup)myAction, new MenuItemPresentationFactory() {
        @Override
        protected void processPresentation(Presentation presentation) {
          if (myNoIconsInPopup) {
            presentation.setIcon(null);
            presentation.setHoveredIcon(null);
          }
        }
      });
      popupMenu.setDataContextProvider(this::getDataContext);
      if (event.isFromActionToolbar()) {
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
    if (myPresentationListener != null) {
      myPresentation.removePropertyChangeListener(myPresentationListener);
      myPresentationListener = null;
    }
    super.removeNotify();
  }

  @Override
  public void addNotify() {
    super.addNotify();
    if (myPresentationListener == null) {
      myPresentation.addPropertyChangeListener(myPresentationListener = this::presentationPropertyChanded);
    }
    AnActionEvent e = AnActionEvent.createFromInputEvent(null, myPlace, myPresentation, getDataContext(), false, true);
    ActionUtil.performDumbAwareUpdate(LaterInvocator.isInModalContext(), myAction, e, false);
    updateToolTipText();
    updateIcon();
  }

  @Override
  public void setToolTipText(String s) {
    String tooltipText = KeymapUtil.createTooltipText(s, myAction);
    super.setToolTipText(tooltipText.length() > 0 ? tooltipText : null);
  }

  protected void updateToolTipText() {
    LocalizeValue textValue = myPresentation.getTextValue();
    LocalizeValue descriptionValue = myPresentation.getDescriptionValue();
    if (Registry.is("ide.helptooltip.enabled")) {
      HelpTooltip.dispose(this);
      if (textValue != LocalizeValue.of() || descriptionValue != LocalizeValue.of()) {
        HelpTooltip ht = new HelpTooltip().setTitle(textValue.map(Presentation.NO_MNEMONIC).getValue()).setShortcut(getShortcutText());

        String id = ActionManager.getInstance().getId(myAction);
        if (!textValue.equals(descriptionValue) && (id != null && WHITE_LIST.contains(id) || myAction instanceof TooltipDescriptionProvider)) {
          ht.setDescription(descriptionValue.getValue());
        }
        ht.installOn(this);
      }
    }
    else {
      setToolTipText(textValue == LocalizeValue.of() ? descriptionValue.get() : textValue.get());
    }
  }

  @Nullable
  protected String getShortcutText() {
    return KeymapUtil.getFirstKeyboardShortcutText(myAction);
  }

  @Override
  public Dimension getPreferredSize() {
    Image icon = getIcon();
    Dimension minSize = TargetAWT.to(myMinimumButtonSize);
    if (icon.getWidth() < minSize.width && icon.getHeight() < minSize.height) {
      return minSize;
    }
    else {
      return new Dimension(Math.max(minSize.width, icon.getWidth() + myInsets.left + myInsets.right),
                           Math.max(minSize.height, icon.getHeight() + myInsets.top + myInsets.bottom));
    }
  }


  public void setIconInsets(@Nullable Insets insets) {
    myInsets = insets != null ? JBUI.insets(insets) : JBUI.emptyInsets();
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  /**
   * @return button's icon. Icon depends on action's state. It means that the method returns
   * disabled icon if action is disabled. If the action's icon is {@code null} then it returns
   * an empty icon.
   */
  public Image getIcon() {
    boolean enabled = isButtonEnabled();
    int popState = getPopState();
    Image hoveredIcon = (popState == POPPED || popState == PUSHED) ? myPresentation.getHoveredIcon() : null;
    Image icon = enabled ? hoveredIcon != null ? hoveredIcon : myIcon : myDisabledIcon;
    return icon == null ? getFallbackIcon(enabled) : icon;
  }

  @Nonnull
  protected Image getFallbackIcon(boolean enabled) {
    return Image.empty(18);
  }

  public void updateIcon() {
    myIcon = myPresentation.getIcon();
    if (myPresentation.getDisabledIcon() != null) { // set disabled icon if it is specified
      myDisabledIcon = myPresentation.getDisabledIcon();
    }
    else {
      myDisabledIcon = myIcon == null ? null : ImageEffects.grayed(myIcon);
    }
  }

  private void setDisabledIcon(Image icon) {
    myDisabledIcon = icon;
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
    else if (myRollover && isButtonEnabled()) {
      return POPPED;
    }
    else if (isFocusOwner()) {
      return SELECTED;
    }
    else {
      return NORMAL;
    }
  }

  public void update() {
    AnActionEvent e = AnActionEvent.createFromInputEvent(null, myPlace, myPresentation, getDataContext(), false, true);
    ActionUtil.performDumbAwareUpdate(LaterInvocator.isInModalContext(), myAction, e, false);
    updateToolTipText();
    updateIcon();
  }

  public final boolean isSelected() {
    return myAction instanceof Toggleable && Toggleable.isSelected(myPresentation);
  }

  @Override
  public AnAction getAction() {
    return myAction;
  }

  protected void updateTextAndMnemonic(@Nonnull LocalizeValue localizeValue) {
    boolean disabledMnemonic = myPresentation.isDisabledMnemonic();

    if(disabledMnemonic) {
      myLastComputedText = localizeValue.getValue();
      setDisplayedMnemonicIndex(-1);
    }
    else {
      TextWithMnemonic textWithMnemonic = TextWithMnemonic.parse(localizeValue.getValue());
      myLastComputedText = textWithMnemonic.getText();
      setDisplayedMnemonicIndex(textWithMnemonic.getMnemonicIndex());
    }
  }

  protected void presentationPropertyChanded(PropertyChangeEvent e) {
    String propertyName = e.getPropertyName();
    if (Presentation.PROP_TEXT.equals(propertyName)) {
      updateTextAndMnemonic((LocalizeValue)e.getNewValue());
      updateToolTipText();
    }
    else if (Presentation.PROP_ENABLED.equals(propertyName)) {
      updateIcon();
      repaint();
    }
    else if (Presentation.PROP_ICON.equals(propertyName)) {
      updateIcon();
      repaint();
    }
    else if (Presentation.PROP_DISABLED_ICON.equals(propertyName)) {
      setDisabledIcon(myPresentation.getDisabledIcon());
      repaint();
    }
    else if (Presentation.PROP_VISIBLE.equals(propertyName)) {
    }
    else if ("selected".equals(propertyName)) {
      repaint();
    }
  }

  // Accessibility

  @Override
  public AccessibleContext getAccessibleContext() {
    if (this.accessibleContext == null) {
      this.accessibleContext = new AccessibleActionButton();
    }

    return this.accessibleContext;
  }


  protected class AccessibleActionButton extends JComponent.AccessibleJComponent implements AccessibleAction {
    public AccessibleActionButton() {
    }

    @Override
    public AccessibleRole getAccessibleRole() {
      return AccessibleRole.PUSH_BUTTON;
    }

    @Override
    public String getAccessibleName() {
      String name = accessibleName;
      if (name == null) {
        name = (String)ActionButton.this.getClientProperty(ACCESSIBLE_NAME_PROPERTY);
        if (name == null) {
          name = ActionButton.this.getToolTipText();
          if (name == null) {
            name = ActionButton.this.myLastComputedText;
            if (name == null) {
              name = super.getAccessibleName();
            }
          }
        }
      }

      return name;
    }

    @Override
    public AccessibleIcon[] getAccessibleIcon() {
      Image image = ActionButton.this.getIcon();
      Icon icon = TargetAWT.to(image);

      if (icon instanceof Accessible) {
        AccessibleContext context = ((Accessible)icon).getAccessibleContext();
        if (context != null && context instanceof AccessibleIcon) {
          return new AccessibleIcon[]{(AccessibleIcon)context};
        }
      }

      return null;
    }

    @Override
    public AccessibleStateSet getAccessibleStateSet() {
      AccessibleStateSet var1 = super.getAccessibleStateSet();
      int state = ActionButton.this.getPopState();

      // TODO: Not sure what the "POPPED" state represents
      //if (state == POPPED) {
      //  var1.add(AccessibleState.?);
      //}

      if (state == ActionButtonComponent.PUSHED) {
        var1.add(AccessibleState.PRESSED);
      }
      if (state == ActionButtonComponent.SELECTED) {
        var1.add(AccessibleState.CHECKED);
      }

      if (ActionButton.this.isFocusOwner()) {
        var1.add(AccessibleState.FOCUSED);
      }

      return var1;
    }

    @Override
    public AccessibleAction getAccessibleAction() {
      return this;
    }

    // Implements AccessibleAction

    @Override
    public int getAccessibleActionCount() {
      return 1;
    }

    @Override
    public String getAccessibleActionDescription(int index) {
      return index == 0 ? UIManager.getString("AbstractButton.clickText") : null;
    }

    @Override
    public boolean doAccessibleAction(int index) {
      if (index == 0) { //
        ActionButton.this.click();
        return true;
      }
      else {
        return false;
      }
    }
  }
}
