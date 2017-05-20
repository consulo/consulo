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
package com.intellij.openapi.actionSystem.impl;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

public class ActionButtonWithText extends ActionButton {
  private static final int ICON_TEXT_SPACE = 2;

  public ActionButtonWithText(final AnAction action,
                              final Presentation presentation,
                              final String place,
                              final Dimension minimumSize) {
    super(action, presentation, place, minimumSize);
    setFont(UIUtil.getLabelFont());
    setForeground(UIUtil.getLabelForeground());
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension preferredSize = new Dimension(super.getPreferredSize());
    final String text = getText();
    final FontMetrics fontMetrics = getFontMetrics(getFont());
    preferredSize.width += iconTextSpace();
    preferredSize.width += fontMetrics.stringWidth(text);
    return preferredSize;
  }

  public int horizontalTextAlignment() {
    return SwingConstants.CENTER;
  }

  public int iconTextSpace() {
    return (getIcon() instanceof EmptyIcon || getIcon() == null ) ? 0 : JBUI.scale(ICON_TEXT_SPACE);
  }

  public String getText() {
    final String text = myPresentation.getText();
    return text != null? text : "";
  }
}
