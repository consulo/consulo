/*
 * Copyright 2013-2014 must-be.org
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
package consulo.ide.ui.laf.modern;

import com.intellij.openapi.ui.GraphicsConfig;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicPasswordFieldUI;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * @author VISTALL
 * @since 05.08.14
 * <p>
 * Based on {@link com.intellij.ide.ui.laf.darcula.ui.DarculaPasswordFieldUI}
 */
public class ModernPasswordFieldUI extends BasicPasswordFieldUI implements ModernTextBorder.ModernTextUI {

  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "UnusedDeclaration"})
  public static ComponentUI createUI(final JComponent c) {
    ModernPasswordFieldUI ui = new ModernPasswordFieldUI(c);
    c.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        ui.myFocused = true;
        c.repaint();
      }

      @Override
      public void focusLost(FocusEvent e) {
        ui.myFocused = false;
        c.repaint();
      }
    });

    return ui;
  }

  private boolean myFocused;
  private MouseEnterHandler myMouseEnterHandler;

  public ModernPasswordFieldUI(JComponent c) {
    myMouseEnterHandler = new MouseEnterHandler(c);
  }

  @Override
  protected void paintBackground(Graphics graphics) {
    Graphics2D g = (Graphics2D)graphics;
    final JTextComponent c = getComponent();
    final Container parent = c.getParent();
    if (parent != null) {
      g.setColor(parent.getBackground());
      g.fillRect(0, 0, c.getWidth(), c.getHeight());
    }
    final Border border = c.getBorder();
    if (border instanceof ModernTextBorder) {
      g.setColor(c.getBackground());
      final int width = c.getWidth();
      final int height = c.getHeight();
      final Insets i = border.getBorderInsets(c);
      if (c.hasFocus()) {
        final GraphicsConfig config = new GraphicsConfig(g);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

        g.fillRect(i.left - 5, i.top - 2, width - i.left - i.right + 10, height - i.top - i.bottom + 6);
        config.restore();
      }
      else {
        g.fillRect(i.left - 5, i.top - 2, width - i.left - i.right + 12, height - i.top - i.bottom + 6);
      }
    }
    else {
      super.paintBackground(g);
    }
  }

  @Override
  public boolean isFocused() {
    return myFocused;
  }

  @NotNull
  @Override
  public MouseEnterHandler getMouseEnterHandler() {
    return myMouseEnterHandler;
  }
}