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
package com.intellij.ide.ui.laf.darcula.ui;

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil_New;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class DarculaTextFieldUI extends TextFieldWithPopupHandlerUI_New {
  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static ComponentUI createUI(final JComponent c) {
    return new DarculaTextFieldUI();
  }

  @Override
  protected void paintBackground(Graphics g) {
    if(DarculaUIUtil_New.isComboBoxEditor(getComponent())) {
      super.paintBackground(g);
      return;
    }

    JTextComponent editor = getComponent();

    GraphicsConfig config = new GraphicsConfig(g);
    config.setupAAPainting();

    g.setColor(editor.getBackground());

    int height = editor.getHeight();
    int width = editor.getWidth();

    g.fillRoundRect(JBUI.scale(1), JBUI.scale(1), width - JBUI.scale(2), height - JBUI.scale(2), JBUI.scale(4), JBUI.scale(4));

    config.restore();
  }

  public static void paintBackground(Graphics g, JComponent editor) {
    GraphicsConfig config = new GraphicsConfig(g);
    config.setupAAPainting();

    g.setColor(editor.getBackground());

    int height = editor.getHeight();
    int width = editor.getWidth();

    g.fillRoundRect(JBUI.scale(1), JBUI.scale(1), width - JBUI.scale(2), height - JBUI.scale(2), JBUI.scale(4), JBUI.scale(4));

    config.restore();
  }
}
