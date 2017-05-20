/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.util.Condition;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.annotations.RequiredDispatchThread;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public abstract class ComboBoxAction extends AnAction implements CustomComponentAction {
  private boolean mySmallVariant = true;
  private String myPopupTitle;

  protected ComboBoxAction() {
  }

  @RequiredDispatchThread
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ComboBoxButton button = (ComboBoxButton)e.getPresentation().getClientProperty(CUSTOM_COMPONENT_PROPERTY);
    if (button == null) {
      Component contextComponent = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
      JRootPane rootPane = UIUtil.getParentOfType(JRootPane.class, contextComponent);
      if (rootPane != null) {
        button = (ComboBoxButton)UIUtil.uiTraverser().withRoot(rootPane).bfsTraversal()
                .filter(component -> component instanceof ComboBoxButton && ((ComboBoxButton)component).getComboBoxAction() == ComboBoxAction.this).first();
      }
      if (button == null) return;
    }
    if (!button.isShowing()) return;
    button.showPopup();
  }

  @Override
  public JComponent createCustomComponent(Presentation presentation) {
    JPanel panel = new JPanel(new GridBagLayout());
    ComboBoxButton button = createComboBoxButton(presentation);
    panel.add(button, new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, JBUI.insets(0, 3, 0, 3), 0, 0));
    return panel;
  }

  protected ComboBoxButton createComboBoxButton(Presentation presentation) {
    return new ComboBoxButton(this, presentation);
  }

  public boolean isSmallVariant() {
    return mySmallVariant;
  }

  public void setSmallVariant(boolean smallVariant) {
    mySmallVariant = smallVariant;
  }

  public void setPopupTitle(String popupTitle) {
    myPopupTitle = popupTitle;
  }

  public String getPopupTitle() {
    return myPopupTitle;
  }

  @RequiredDispatchThread
  @Override
  public void update(@NotNull AnActionEvent e) {
  }

  protected boolean shouldShowDisabledActions() {
    return false;
  }

  @NotNull
  protected abstract DefaultActionGroup createPopupActionGroup(JComponent button);

  protected int getMaxRows() {
    return 30;
  }

  protected int getMinHeight() {
    return 1;
  }

  protected int getMinWidth() {
    return 1;
  }

  protected Condition<AnAction> getPreselectCondition() {
    return null;
  }
}
