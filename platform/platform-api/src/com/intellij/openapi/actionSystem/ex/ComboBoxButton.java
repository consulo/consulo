/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.actionSystem.ex;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.UserActivityProviderComponent;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ComboBoxButton extends JButton implements UserActivityProviderComponent {
  private static final String uiClassID = "ComboBoxButtonUI";

  //public static final Icon ARROW_ICON = UIUtil.isUnderDarcula() ? AllIcons.General.ComboArrow : AllIcons.General.ComboBoxButtonArrow;
  //public static final Icon DISABLED_ARROW_ICON = IconLoader.getDisabledIcon(ARROW_ICON);

  private ComboBoxAction myComboBoxAction;
  private final Presentation myPresentation;
  private boolean myForcePressed = false;
  private PropertyChangeListener myButtonSynchronizer;
  private JBPopup myPopup;
  private boolean myForceTransparent = false;

  public ComboBoxButton(ComboBoxAction comboBoxAction, Presentation presentation) {
    myComboBoxAction = comboBoxAction;
    myPresentation = presentation;
    setEnabled(myPresentation.isEnabled());
    setModel(new MyButtonModel());
    setHorizontalAlignment(LEFT);
    setFocusable(false);
    Insets margins = getMargin();
    setMargin(JBUI.insets(margins.top, 2, margins.bottom, 2));
    if (myComboBoxAction.isSmallVariant()) {
      if (!UIUtil.isUnderGTKLookAndFeel()) {
        setFont(JBUI.Fonts.label(11));
      }
    }

    addActionListener(e -> {
      if (!myForcePressed) {
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(this::showPopup);
      }
    });
  }

  @Override
  public String getUIClassID() {
    return uiClassID;
  }

  public void setForceTransparent(boolean transparent) {
    myForceTransparent = transparent;
  }

  public boolean isForceTransparent() {
    return myForceTransparent;
  }

  @NotNull
  private Runnable setForcePressed() {
    myForcePressed = true;
    repaint();

    return () -> {
      // give the button a chance to handle action listener
      ApplicationManager.getApplication().invokeLater(() -> {
        myForcePressed = false;
        myPopup = null;
      }, ModalityState.any());
      repaint();
      fireStateChanged();
    };
  }

  public JBPopup getPopup() {
    return myPopup;
  }

  @Nullable
  @Override
  public String getToolTipText() {
    return myForcePressed ? null : super.getToolTipText();
  }

  public void showPopup() {
    createPopup(setForcePressed()).showUnderneathOf(this);
  }

  protected JBPopup createPopup(Runnable onDispose) {
    DefaultActionGroup group = myComboBoxAction.createPopupActionGroup(this);

    DataContext context = getDataContext();
    ListPopup popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(myComboBoxAction.getPopupTitle(), group, context, false, myComboBoxAction.shouldShowDisabledActions(), false, onDispose,
                                    myComboBoxAction.getMaxRows(), myComboBoxAction.getPreselectCondition());
    popup.setMinimumSize(new Dimension(myComboBoxAction.getMinWidth(), myComboBoxAction.getMinHeight()));
    return popup;
  }

  public ComboBoxAction getComboBoxAction() {
    return myComboBoxAction;
  }

  protected DataContext getDataContext() {
    return DataManager.getInstance().getDataContext(this);
  }

  @Override
  public void removeNotify() {
    if (myButtonSynchronizer != null) {
      myPresentation.removePropertyChangeListener(myButtonSynchronizer);
      myButtonSynchronizer = null;
    }
    super.removeNotify();
  }

  @Override
  public void addNotify() {
    super.addNotify();
    if (myButtonSynchronizer == null) {
      myButtonSynchronizer = new MyButtonSynchronizer();
      myPresentation.addPropertyChangeListener(myButtonSynchronizer);
    }
    initButton();
  }

  private void initButton() {
    setIcon(myPresentation.getIcon());
    setText(myPresentation.getText());
    updateTooltipText(myPresentation.getDescription());
    updateButtonSize();
  }

  private void updateTooltipText(String description) {
    String tooltip = KeymapUtil.createTooltipText(description, myComboBoxAction);
    setToolTipText(!tooltip.isEmpty() ? tooltip : null);
  }

  @Override
  public void updateUI() {
    setUI(UIManager.getUI(this));
  }

  protected class MyButtonModel extends DefaultButtonModel {
    @Override
    public boolean isPressed() {
      return myForcePressed || super.isPressed();
    }

    @Override
    public boolean isArmed() {
      return myForcePressed || super.isArmed();
    }
  }

  private class MyButtonSynchronizer implements PropertyChangeListener {
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      String propertyName = evt.getPropertyName();
      if (Presentation.PROP_TEXT.equals(propertyName)) {
        setText((String)evt.getNewValue());
        updateButtonSize();
      }
      else if (Presentation.PROP_DESCRIPTION.equals(propertyName)) {
        updateTooltipText((String)evt.getNewValue());
      }
      else if (Presentation.PROP_ICON.equals(propertyName)) {
        setIcon((Icon)evt.getNewValue());
        updateButtonSize();
      }
      else if (Presentation.PROP_ENABLED.equals(propertyName)) {
        setEnabled(((Boolean)evt.getNewValue()).booleanValue());
      }
    }
  }

 /* @Override
  public Insets getInsets() {
    final Insets insets = super.getInsets();
    return new Insets(insets.top, insets.left, insets.bottom, insets.right + ARROW_ICON.getIconWidth());
  } */

 /* @Override
  public Insets getInsets(Insets insets) {
    final Insets result = super.getInsets(insets);
    result.right += ARROW_ICON.getIconWidth();

    return result;
  } */

  @Override
  public boolean isOpaque() {
    return !myComboBoxAction.isSmallVariant();
  }

  /*@Override
  public Dimension getPreferredSize() {
    final boolean isEmpty = getIcon() == null && StringUtil.isEmpty(getText());
    int width = isEmpty ? JBUI.scale(10) + ARROW_ICON.getIconWidth() : super.getPreferredSize().width;
    if (myComboBoxAction.isSmallVariant()) width += JBUI.scale(4);
    return new Dimension(width, myComboBoxAction.isSmallVariant() ? JBUI.scale(19) : super.getPreferredSize().height);
  }*/

  /*@Override
  public Dimension getMinimumSize() {
    return new Dimension(super.getMinimumSize().width, getPreferredSize().height);
  } */

  @Override
  public Font getFont() {
    return SystemInfo.isMac && myComboBoxAction.isSmallVariant() ? UIUtil.getLabelFont(UIUtil.FontSize.SMALL) : UIUtil.getLabelFont();
  }

 /* @Override
  public void paint(Graphics g) {
    UISettings.setupAntialiasing(g);
    final Dimension size = getSize();
    final boolean isEmpty = getIcon() == null && StringUtil.isEmpty(getText());

    final Color textColor = isEnabled() ? UIManager.getColor("Panel.foreground") : UIUtil.getInactiveTextColor();
    if (myForceTransparent) {
      final Icon icon = getIcon();
      int x = 7;
      if (icon != null) {
        icon.paintIcon(this, g, x, (size.height - icon.getIconHeight()) / 2);
        x += icon.getIconWidth() + 3;
      }
      if (!StringUtil.isEmpty(getText())) {
        final Font font = getFont();
        g.setFont(font);
        g.setColor(textColor);
        g.drawString(getText(), x, (size.height + font.getSize()) / 2 - 1);
      }
    }
    else {

      if (myComboBoxAction.isSmallVariant()) {
        final Graphics2D g2 = (Graphics2D)g;
        g2.setColor(UIUtil.getControlColor());
        final int w = getWidth();
        final int h = getHeight();
        if (getModel().isArmed() && getModel().isPressed()) {
          g2.setPaint(UIUtil.getGradientPaint(0, 0, UIUtil.getControlColor(), 0, h, ColorUtil.shift(UIUtil.getControlColor(), 0.8)));
        }
        else {
          if (UIUtil.isUnderDarcula()) {
            g2.setPaint(UIUtil.getGradientPaint(0, 0, ColorUtil.shift(UIUtil.getControlColor(), 1.1), 0, h, ColorUtil.shift(UIUtil.getControlColor(), 0.9)));
          }
          else {
            g2.setPaint(UIUtil.getGradientPaint(0, 0, new JBColor(SystemInfo.isMac ? Gray._226 : Gray._245, Gray._131), 0, h,
                                                new JBColor(SystemInfo.isMac ? Gray._198 : Gray._208, Gray._128)));
          }
        }
        g2.fillRoundRect(2, 0, w - 2, h, 5, 5);

        Color borderColor = myMouseInside ? new JBColor(Gray._111, Gray._118) : new JBColor(Gray._151, Gray._95);
        g2.setPaint(borderColor);
        g2.drawRoundRect(2, 0, w - 3, h - 1, 5, 5);

        final Icon icon = getIcon();
        int x = 7;
        if (icon != null) {
          icon.paintIcon(this, g, x, (size.height - icon.getIconHeight()) / 2);
          x += icon.getIconWidth() + 3;
        }
        if (!StringUtil.isEmpty(getText())) {
          final Font font = getFont();
          g2.setFont(font);
          g2.setColor(textColor);
          g2.drawString(getText(), x, (size.height + font.getSize()) / 2 - 1);
        }
      }
      else {
        super.paint(g);
      }
    }
    final Insets insets = super.getInsets();
    final Icon icon = isEnabled() ? ARROW_ICON : DISABLED_ARROW_ICON;
    final int x;
    if (isEmpty) {
      x = (size.width - icon.getIconWidth()) / 2;
    }
    else {
      if (myComboBoxAction.isSmallVariant()) {
        x = size.width - icon.getIconWidth() - insets.right + 1;
      }
      else {
        x = size.width - icon.getIconWidth() - insets.right + (UIUtil.isUnderNimbusLookAndFeel() ? -3 : 2);
      }
    }

    icon.paintIcon(null, g, x, (size.height - icon.getIconHeight()) / 2);
    g.setPaintMode();
  }  */

  protected void updateButtonSize() {
    invalidate();
    repaint();
    setSize(getPreferredSize());
  }
}
