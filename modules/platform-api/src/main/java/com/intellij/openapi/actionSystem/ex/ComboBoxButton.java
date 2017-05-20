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

    addActionListener(e -> {
      if (!myForcePressed) {
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(this::showPopup);
      }
    });

    updateUI();
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

    Insets margins = getMargin();
    setMargin(JBUI.insets(margins == null ? 0 : margins.top, 2, margins == null ? 0 : margins.bottom, 2));
    boolean smallVariant = myComboBoxAction != null && myComboBoxAction.isSmallVariant();
    setFont(SystemInfo.isMac && smallVariant ? UIUtil.getLabelFont(UIUtil.FontSize.SMALL) : UIUtil.getLabelFont());
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

  @Override
  public boolean isOpaque() {
    return !myComboBoxAction.isSmallVariant();
  }

  protected void updateButtonSize() {
    invalidate();
    repaint();
    setSize(getPreferredSize());
  }
}
