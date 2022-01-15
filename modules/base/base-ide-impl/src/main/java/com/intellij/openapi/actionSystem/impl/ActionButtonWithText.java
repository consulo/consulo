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

import com.intellij.ide.HelpTooltip;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.annotation.DeprecationInfo;
import consulo.ui.Size;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

public class ActionButtonWithText extends ActionButton {
  private static final int ICON_TEXT_SPACE = 2;

  @Deprecated
  @DeprecationInfo("Use constructor with Size parameter")
  public ActionButtonWithText(final AnAction action, final Presentation presentation, final String place, final Dimension minimumSize) {
    this(action, presentation, place, new Size(minimumSize.width, minimumSize.height));
  }

  public ActionButtonWithText(final AnAction action, final Presentation presentation, final String place, final Size minimumSize) {
    super(action, presentation, place, minimumSize);
    setFont(UIUtil.getLabelFont());
    setForeground(UIUtil.getLabelForeground());
  }

  @Override
  protected void updateToolTipText() {
    String description = myPresentation.getDescription();
    if (Registry.is("ide.helptooltip.enabled")) {
      HelpTooltip.dispose(this);
      if (StringUtil.isNotEmpty(description)) {
        new HelpTooltip().setDescription(description).installOn(this);
      }
    }
    else {
      setToolTipText(description);
    }
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
    return (getIcon() instanceof EmptyIcon || getIcon() == null) ? 0 : JBUI.scale(ICON_TEXT_SPACE);
  }

  @Nonnull
  public String getText() {
    return myLastComputedText;
  }
}
