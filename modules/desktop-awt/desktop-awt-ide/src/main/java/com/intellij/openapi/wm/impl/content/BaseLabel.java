/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl.content;

import com.intellij.ide.DataManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowDataKeys;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.Content;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.ui.image.ImageEffects;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;

public class BaseLabel extends JLabel {
  protected DesktopToolWindowContentUi myUi;

  private Color myActiveFg;
  private Color myPassiveFg;
  private boolean myBold;

  public BaseLabel(DesktopToolWindowContentUi ui, boolean bold) {
    myUi = ui;
    setOpaque(false);
    myBold = bold;

    DataManager.registerDataProvider(this, dataId -> {
      if (dataId == ToolWindowDataKeys.CONTENT) {
        return getContent();
      }

      return null;
    });
  }

  @Override
  public void updateUI() {
    setActiveFg(JBColor.foreground());
    setPassiveFg(JBColor.foreground());
    super.updateUI();
  }

  @Override
  public Font getFont() {
    Font f = UIUtil.getLabelFont();
    if (myBold) {
      f = f.deriveFont(Font.BOLD);
    }

    return f;
  }

  public void setActiveFg(final Color fg) {
    myActiveFg = fg;
  }

  public void setPassiveFg(final Color passiveFg) {
    myPassiveFg = passiveFg;
  }

  @Override
  protected void paintComponent(final Graphics g) {
    final Color fore = myUi.myWindow.isActive() ? myActiveFg : myPassiveFg;
    setForeground(fore);
    super.paintComponent(_getGraphics((Graphics2D)g));
  }

  protected Graphics _getGraphics(Graphics2D g) {
    return g;
  }

  protected Color getActiveFg(boolean selected) {
    return myActiveFg;
  }

  protected Color getPassiveFg(boolean selected) {
    return myPassiveFg;
  }

  protected void updateTextAndIcon(Content content, boolean isSelected) {
    if (content == null) {
      setText(null);
      setIcon(null);
    }
    else {
      setText(content.getDisplayName());
      setActiveFg(getActiveFg(isSelected));
      setPassiveFg(getPassiveFg(isSelected));

      setToolTipText(content.getDescription());

      final boolean show = Boolean.TRUE.equals(content.getUserData(ToolWindow.SHOW_CONTENT_ICON));
      if (show) {
        if (isSelected) {
          setIcon(TargetAWT.to(content.getIcon()));
        }
        else {
          setIcon(content.getIcon() != null ? TargetAWT.to(ImageEffects.transparent(content.getIcon(), .5f)) : null);
        }
      }
      else {
        setIcon(null);
      }

      myBold = false; //isSelected;
    }
  }

  public Content getContent() {
    return null;
  }

  @Override
  public AccessibleContext getAccessibleContext() {
    if (accessibleContext == null) {
      accessibleContext = new AccessibleBaseLabel();
    }
    return accessibleContext;
  }

  protected class AccessibleBaseLabel extends AccessibleJLabel {
  }
}
