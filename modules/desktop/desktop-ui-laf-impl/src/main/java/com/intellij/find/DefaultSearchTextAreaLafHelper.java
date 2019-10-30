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

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil;
import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.Gray;
import com.intellij.ui.paint.RectanglePainter;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class DefaultSearchTextAreaLafHelper extends SearchTextAreaLafHelper {
  private SearchTextArea mySearchTextArea;

  DefaultSearchTextAreaLafHelper(SearchTextArea searchTextArea) {
    mySearchTextArea = searchTextArea;
  }

  @Override
  public Border getBorder() {
    return JBUI.Borders.empty(2);
  }

  @Override
  public String getLayoutConstraints() {
    return "flowx, ins 2 " + JBUI.scale(4) + " 2 " + (3 + JBUI.scale(1)) + ", gapx " + JBUI.scale(4);
  }

  @Override
  public String getHistoryButtonConstraints() {
    return "ay top, gaptop " + JBUI.scale(getIconTopGap());//Double scaling inside but it looks not bad
  }

  private int getIconTopGap() {
    return Math
            .max(2, (UIUtil.getLineHeight(mySearchTextArea.getTextArea()) + mySearchTextArea.getTextArea().getInsets().top + mySearchTextArea.getTextArea().getInsets().bottom - JBUI.scale(16)) / 2);
  }

  @Override
  public String getIconsPanelConstraints() {
    return "gaptop " + getIconTopGap() + ",ay top";
  }

  @Override
  public Border getIconsPanelBorder(int rows) {
    return JBUI.Borders.empty();
  }

  @Override
  public Icon getShowHistoryIcon() {
    Icon searchIcon = UIManager.getIcon("TextField.darcula.searchWithHistory.icon");
    if (searchIcon == null) {
      searchIcon = IconLoader.findIcon("/com/intellij/ide/ui/laf/icons/searchWithHistory.png", DefaultSearchTextAreaLafHelper.class, true);
    }
    return searchIcon;
  }

  @Override
  public Icon getClearIcon() {
    Icon clearIcon = UIManager.getIcon("TextField.darcula.clear.icon");
    if (clearIcon == null) {
      clearIcon = IconLoader.findIcon("/com/intellij/ide/ui/laf/icons/clear.png", DefaultSearchTextAreaLafHelper.class, true);
    }
    return clearIcon;
  }

  @Override
  public void paint(Graphics2D g) {
    Rectangle r = new Rectangle(mySearchTextArea.getSize());
    JBInsets.removeFrom(r, mySearchTextArea.getInsets());
    if (r.height % 2 == 1) r.height++;
    int arcSize = JBUI.scale(26);

    JBInsets.removeFrom(r, new JBInsets(1, 1, 1, 1));
    if (mySearchTextArea.getTextArea().hasFocus()) {
      g.setColor(mySearchTextArea.getTextArea().getBackground());
      RectanglePainter.FILL.paint(g, r.x, r.y, r.width, r.height, arcSize);
      DarculaUIUtil.paintSearchFocusRing(g, r, mySearchTextArea.getTextArea(), arcSize);
    }
    else {
      arcSize -= JBUI.scale(5);
      RectanglePainter.paint(g, r.x, r.y, r.width, r.height, arcSize, mySearchTextArea.getTextArea().getBackground(), mySearchTextArea.getTextArea().isEnabled() ? Gray._100 : Gray._83);
    }
  }
}
