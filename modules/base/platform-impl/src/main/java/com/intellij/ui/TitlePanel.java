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

package com.intellij.ui;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.ui.image.Image;

import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import java.awt.*;

/**
 * @author max
 */
public class TitlePanel extends CaptionPanel {
  private final JLabel myLabel;
  private final Image myRegular;
  private final Image myInactive;

  private boolean myHtml;
  
  public TitlePanel() {
    this(null, null);
  }

  public TitlePanel(Image regular, Image inactive) {
    myRegular = regular;
    myInactive = inactive;

    myLabel = new JBLabel();
    if (UIUtil.isUnderAquaLookAndFeel()) {
      myLabel.setFont(myLabel.getFont().deriveFont(12f));
    }
    myLabel.setForeground(JBColor.foreground());
    myLabel.setHorizontalAlignment(SwingConstants.CENTER);
    myLabel.setVerticalAlignment(SwingConstants.CENTER);
    myLabel.setBorder(JBUI.Borders.empty(1, 10, 2, 10));

    add(myLabel, BorderLayout.CENTER);

    setActive(false);
  }

  @Override
  public void setActive(final boolean active) {
    super.setActive(active);
    myLabel.setIcon(TargetAWT.to(active ? myRegular : myInactive));
    myLabel.setForeground(active ? UIUtil.getLabelForeground() : UIUtil.getLabelDisabledForeground());
  }

  public void setText(String titleText) {
    myHtml = BasicHTML.isHTMLString(titleText);
    myLabel.setText(titleText);
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(10, getPreferredSize().height);
  }

  @Override
  public Dimension getPreferredSize() {
    final String text = myLabel.getText();
    if (text == null || text.trim().isEmpty()) {
      return JBUI.emptySize();
    }

    final Dimension preferredSize = super.getPreferredSize();
    preferredSize.height = JBUI.CurrentTheme.Popup.headerHeight(containsSettingsControls());
    int maxWidth = JBUIScale.scale(350);
    if (!myHtml && preferredSize.width > maxWidth) { // do not allow caption to extend parent container
      return new Dimension(maxWidth, preferredSize.height);
    }

    return preferredSize;
  }
}


