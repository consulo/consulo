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

package com.intellij.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * @author max
 */
public class CaptionPanel extends JPanel {
  private static final String uiClassUD = "CaptionPanelUI";

  private boolean myActive = false;
  private ActiveComponent myButtonComponent;
  private JComponent mySettingComponent;

  public CaptionPanel() {
    setLayout(new BorderLayout());
  }

  @Override
  public String getUIClassID() {
    return uiClassUD;
  }

  public boolean isActive() {
    return myActive;
  }

  public void setActive(final boolean active) {
    myActive = active;
    if (myButtonComponent != null) {
      myButtonComponent.setActive(active);
    }
    repaint();
  }

  public void setButtonComponent(@Nonnull ActiveComponent component, @Nullable Border border) {
    if (myButtonComponent != null) {
      remove(myButtonComponent.getComponent());
    }
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(border);
    panel.add(new JLabel(" "), BorderLayout.WEST);
    panel.add(component.getComponent(), BorderLayout.CENTER);
    panel.setOpaque(false);
    add(panel, BorderLayout.EAST);
    myButtonComponent = component;
  }

  public void addSettingsComponent(Component component) {
    if (mySettingComponent == null) {
      mySettingComponent = new JPanel();
      mySettingComponent.setOpaque(false);
      mySettingComponent.setLayout(new BoxLayout(mySettingComponent, BoxLayout.X_AXIS));
      add(mySettingComponent, BorderLayout.WEST);
    }
    mySettingComponent.add(component);
  }

  public boolean isWithinPanel(MouseEvent e) {
    final Point p = SwingUtilities.convertPoint(e.getComponent(), e.getX(), e.getY(), this);
    final Component c = findComponentAt(p);
    return c != null && c != myButtonComponent;
  }

  protected boolean containsSettingsControls() {
    return mySettingComponent != null || myButtonComponent != null;
  }
}
