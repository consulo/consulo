/*
 * Copyright 2013-2018 consulo.io
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
package com.intellij.find;

import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.laf.intellij.MacIntelliJIconCache;
import com.intellij.ide.ui.laf.intellij.MacIntelliJTextFieldUI;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MacSearchTextAreaLafHelper extends SearchTextAreaLafHelper {
  private SearchTextArea mySearchTextArea;

  MacSearchTextAreaLafHelper(SearchTextArea searchTextArea) {
    mySearchTextArea = searchTextArea;
  }

  @Override
  public Border getBorder() {
    return new EmptyBorder(3 + Math.max(0, JBUI.scale(16) - UIUtil.getLineHeight(mySearchTextArea.getTextArea())) / 2, 6, 4, 4);
  }

  @Override
  public String getLayoutConstraints() {
    return "flowx, ins 0, gapx " + JBUI.scale(4);
  }

  @Override
  public String getHistoryButtonConstraints() {
    int extraGap = getExtraGap();
    return "ay top, gaptop " + extraGap + ", gapleft" + (JBUI.isUsrHiDPI() ? 4 : 0);
  }

  private int getExtraGap() {
    int height = UIUtil.getLineHeight(mySearchTextArea.getTextArea());
    Insets insets = mySearchTextArea.getTextArea().getInsets();
    return Math.max(JBUI.isUsrHiDPI() ? 0 : 1, (height + insets.top + insets.bottom - JBUI.scale(16)) / 2);
  }


  @Override
  public String getIconsPanelConstraints() {
    int extraGap = getExtraGap();
    return "gaptop " + extraGap + ",ay top, gapright " + extraGap / 2;
  }

  @Override
  public Border getIconsPanelBorder(int rows) {
    return JBUI.Borders.emptyBottom(rows == 2 ? 3 : 0);
  }

  @Override
  public Icon getShowHistoryIcon() {
    return MacIntelliJIconCache.getIcon("searchFieldWithHistory");
  }

  @Override
  public Icon getClearIcon() {
    return AllIcons.Actions.Clear;
  }

  @Override
  public void paint(Graphics2D g) {
    Rectangle r = new Rectangle(mySearchTextArea.getSize());
    int h = mySearchTextArea.getIconsPanel().getParent() != null
            ? Math.max(mySearchTextArea.getIconsPanel().getHeight(), mySearchTextArea.getScrollPane().getHeight())
            : mySearchTextArea.getScrollPane().getHeight();

    Insets i = mySearchTextArea.getInsets();
    Insets ei = mySearchTextArea.getTextArea().getInsets();

    int deltaY = i.top - ei.top;
    r.y += deltaY;
    r.height = Math.max(r.height, h + i.top + i.bottom) - (i.bottom - ei.bottom) - deltaY;
    MacIntelliJTextFieldUI.paintAquaSearchFocusRing(g, r, mySearchTextArea.getTextArea());
  }
}
