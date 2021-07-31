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

/*
 * @author max
 */
package com.intellij.ide.bookmarks.actions;

import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.ui.ClickListener;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightColors;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import consulo.awt.TargetAWT;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class MnemonicChooser extends JPanel {
  private static final Color OCCUPIED_CELL_COLOR = new JBColor(new Color(250, 250, 139), new Color(103, 81, 51));
  private static final Color FREE_CELL_COLOR = new JBColor(LightColors.SLIGHTLY_GRAY, Gray._80);

  public MnemonicChooser() {
    super(new HorizontalLayout(20));
    setBorder(JBUI.Borders.empty(10));
    
    JPanel numbers = new NonOpaquePanel(new GridLayout(2, 5, 10, 10));
    for (char i = '1'; i <= '9'; i++) {
      numbers.add(wrapLabel(i));
    }
    numbers.add(wrapLabel('0'));
    

    JPanel letters = new NonOpaquePanel(new GridLayout(5, 6, 10, 10));
    for (char c = 'A'; c <= 'Z'; c++) {
      letters.add(wrapLabel(c));
    }

    // just ignore vertical expand
    add(new BorderLayoutPanel().addToTop(numbers));
    // just ignore vertical expand
    add(new BorderLayoutPanel().addToTop(letters));

    addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          onCancelled();
        }
        else if (e.getModifiersEx() == 0) {
          final char typed = Character.toUpperCase(e.getKeyChar());
          if (typed >= '0' && typed <= '9' || typed >= 'A' && typed <= 'Z') {
            onMnemonicChosen(typed);
          }
        }
      }
    });

    setFocusable(true);
  }

  protected boolean isOccupied(char c) {
    return false;
  }

  protected void onMnemonicChosen(char c) {

  }

  protected void onCancelled() {

  }

  private Color backgroundForMnemonic(char c) {
    return isOccupied(c) ? OCCUPIED_CELL_COLOR  : FREE_CELL_COLOR;
  }

  private JComponent wrapLabel(char c) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setPreferredSize(TargetAWT.to(ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE));
    panel.setMinimumSize(TargetAWT.to(ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE));
    panel.setMaximumSize(TargetAWT.to(ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE));

    panel.setBorder(new CustomLineBorder(1, 1, 1, 1));

    panel.add(new MnemonicLabel(c), BorderLayout.CENTER);
    return panel;
  }

  private class MnemonicLabel extends JLabel {
    private MnemonicLabel(final char c) {
      setOpaque(true);
      setText(Character.toString(c));
      setHorizontalAlignment(CENTER);

      setBackground(backgroundForMnemonic(c));

      addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
          setForeground(UIUtil.getListSelectionForeground());
          setBackground(UIUtil.getListSelectionBackground());
        }

        @Override
        public void mouseExited(MouseEvent e) {
          setForeground(UIUtil.getListForeground());
          setBackground(backgroundForMnemonic(c));
        }
      });

      new ClickListener() {
        @Override
        public boolean onClick(MouseEvent e, int clickCount) {
          onMnemonicChosen(c);
          return true;
        }
      }.installOn(this);
    }
  }
}
