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
package com.intellij.openapi.wm.impl.content;

import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.ui.Gray;
import com.intellij.ui.awt.RelativeRectangle;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

abstract class ContentLayout {

  static final Color TAB_BORDER_ACTIVE_WINDOW = new Color(38, 63, 106);
  static final Color TAB_BORDER_PASSIVE_WINDOW = new Color(130, 120, 111);

  static final Color TAB_BG_ACTIVE_WND_SELECTED_FROM = Gray._111;
  static final Color TAB_BG_ACTIVE_WND_SELECTED_TO = Gray._164;
  static final Color TAB_BG_ACTIVE_WND_UNSELECTED_FROM = Gray._130;
  static final Color TAB_BG_ACTIVE_WND_UNSELECTED_TO = Gray._85;
  static final Color TAB_BG_PASSIVE_WND_FROM = new Color(152, 143, 134);
  static final Color TAB_BG_PASSIVE_WND_TO = new Color(165, 157, 149);

  static final int TAB_ARC = 2;
  static final int TAB_SHIFT = 2;

  DesktopToolWindowContentUi myUi;
  BaseLabel myIdLabel;

  ContentLayout(DesktopToolWindowContentUi ui) {
    myUi = ui;
  }

  public abstract void init();

  public abstract void reset();

  public abstract void layout();

  public abstract void paintComponent(Graphics g);

  public abstract void paintChildren(Graphics g);

  public abstract void update();

  public abstract void rebuild();

  public abstract int getMinimumWidth();

  public abstract void contentAdded(ContentManagerEvent event);

  public abstract void contentRemoved(ContentManagerEvent event);

  protected void updateIdLabel(BaseLabel label) {
    String title = myUi.myWindow.getStripeTitle();

    String suffix = getTitleSuffix();
    if (suffix != null) title += suffix;

    label.setText(title);
    label.setBorder(JBUI.Borders.empty(0, 2, 0, 7));
    label.setVisible(shouldShowId());
  }

  private String getTitleSuffix() {
    switch (myUi.myManager.getContentCount()) {
      case 0:
        return null;
      case 1:
        Content content = myUi.myManager.getContent(0);
        if (content == null) return null;

        final String text = content.getDisplayName();
        if (text != null && text.trim().length() > 0 && myUi.myManager.canCloseContents()) {
          return ":";
        }
        return null;
      default:
        return ":";
    }
  }

  protected void fillTabShape(Graphics2D g2d, Shape shape, boolean isSelected, Rectangle bounds) {
    if (myUi.myWindow.isActive()) {
      if (isSelected) {
        g2d.setPaint(UIUtil.getGradientPaint(bounds.x, bounds.y, TAB_BG_ACTIVE_WND_SELECTED_FROM, bounds.x, (float)bounds.getMaxY(), TAB_BG_ACTIVE_WND_SELECTED_TO));
      }
      else {
        g2d.setPaint(UIUtil.getGradientPaint(bounds.x, bounds.y, TAB_BG_ACTIVE_WND_UNSELECTED_FROM, bounds.x, (float)bounds.getMaxY(), TAB_BG_ACTIVE_WND_UNSELECTED_TO));
      }
    }
    else {
      g2d.setPaint(UIUtil.getGradientPaint(bounds.x, bounds.y, TAB_BG_PASSIVE_WND_FROM, bounds.x, (float)bounds.getMaxY(), TAB_BG_PASSIVE_WND_TO));
    }

    g2d.fill(shape);
  }

  public abstract void showContentPopup(ListPopup listPopup);

  public abstract RelativeRectangle getRectangleFor(Content content);

  public abstract Component getComponentFor(Content content);

  public abstract String getCloseActionName();

  public abstract String getCloseAllButThisActionName();

  public abstract String getPreviousContentActionName();

  public abstract String getNextContentActionName();

  protected boolean shouldShowId() {
    final JComponent component = myUi.myWindow.getComponent();
    return component != null && !"true".equals(component.getClientProperty(DesktopToolWindowContentUi.HIDE_ID_LABEL));
  }

  boolean isIdVisible() {
    return myIdLabel.isVisible();
  }
}
