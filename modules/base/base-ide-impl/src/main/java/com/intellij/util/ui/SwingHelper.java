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
package com.intellij.util.ui;

import com.intellij.ui.components.panels.NonOpaquePanel;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

public class SwingHelper {
  @Nonnull
  public static JEditorPane createHtmlViewer(boolean lineWrap, @Nullable Font font, @Nullable Color background, @Nullable Color foreground) {
    final JEditorPane textPane;
    if (lineWrap) {
      textPane = new JEditorPane() {
        @Override
        public Dimension getPreferredSize() {
          // This trick makes text component to carry text over to the next line
          // if the text line width exceeds parent's width
          Dimension dimension = super.getPreferredSize();
          dimension.width = 0;
          return dimension;
        }
      };
    }
    else {
      textPane = new JEditorPane();
    }
    textPane.setFont(font != null ? font : UIUtil.getLabelFont());
    textPane.setEditorKit(JBHtmlEditorKit.create());
    textPane.setEditable(false);
    if (background != null) {
      textPane.setBackground(background);
    }
    else {
      NonOpaquePanel.setTransparent(textPane);
    }
    textPane.setForeground(foreground != null ? foreground : UIUtil.getLabelForeground());
    textPane.setFocusable(false);
    return textPane;
  }
}
