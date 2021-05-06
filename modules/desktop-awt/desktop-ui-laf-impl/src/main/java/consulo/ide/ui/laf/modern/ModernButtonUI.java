/*
 * Copyright 2013-2016 consulo.io
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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.*;
import consulo.awt.TargetAWT;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import java.awt.*;

/**
 * @author VISTALL
 * @since 02.08.14
 * <p>
 * Based on {@link com.intellij.ide.ui.laf.darcula.ui.DarculaButtonUI}
 */
public class ModernButtonUI extends BasicButtonUI {
  protected static JBValue HELP_BUTTON_DIAMETER = new JBValue.Float(22);

  public static ComponentUI createUI(JComponent c) {
    return new ModernButtonUI(c);
  }

  public static boolean isSquare(Component c) {
    return c instanceof JButton && "square".equals(((JButton)c).getClientProperty("JButton.buttonType"));
  }

  private MouseEnterHandler myMouseEnterHandler;

  public ModernButtonUI(JComponent c) {
    myMouseEnterHandler = new MouseEnterHandler(c);
  }

  public MouseEnterHandler getMouseEnterHandler() {
    return myMouseEnterHandler;
  }

  @Override
  public void installUI(JComponent c) {
    super.installUI(c);
    myMouseEnterHandler.replace(null, c);
  }

  @Override
  public void uninstallUI(JComponent c) {
    super.uninstallUI(c);
    myMouseEnterHandler.replace(c, null);
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    if(UIUtil.isHelpButton(c)) {
      Rectangle r = new Rectangle(c.getSize());
      JBInsets.removeFrom(r, JBUI.insets(1));

      int diam = HELP_BUTTON_DIAMETER.get();
      int x = r.x + (r.width - diam) / 2;
      int y = r.x + (r.height - diam) / 2;

      TargetAWT.to(AllIcons.Actions.Help).paintIcon(c, g, x + JBUIScale.scale(3), y + JBUIScale.scale(3));
      return;
    }

    final Border border = c.getBorder();
    if (border != null) {
      final GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
      final boolean square = isSquare(c);

      final Insets ins = border.getBorderInsets(c);
      final int yOff = (ins.top + ins.bottom) / 4;

      if (c.isEnabled()) {
        if (!square) {
          if (myMouseEnterHandler.isMousePressed()) {
            g.setColor(ColorUtil.toAlpha(ModernUIUtil.getSelectionBackground(), 100));
          }
          else {
            if (ModernButtonBorderPainter.isDefaultButton(c)) {
              g.setColor(myMouseEnterHandler.isMouseEntered() ? ModernUIUtil.getSelectionBackground().brighter() : ModernUIUtil.getSelectionBackground());
            }
            else {
              g.setColor(getButtonColor1());
            }
          }
        }
      }
      else {
        if (ModernButtonBorderPainter.isDefaultButton(c)) {
          g.setColor(ModernUIUtil.getActiveBorderColor());
        }
      }

      g.fillRect(JBUI.scale(square ? 2 : 4), yOff, c.getWidth() - JBUI.scale(2 * 4), c.getHeight() - 2 * yOff);
      config.restore();
    }
    super.paint(g, c);
  }

  @Override
  protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
    AbstractButton button = (AbstractButton)c;
    ButtonModel model = button.getModel();
    Color fg = button.getForeground();
    if (fg instanceof UIResource && ModernButtonBorderPainter.isDefaultButton(button) || myMouseEnterHandler.isMousePressed()) {
      final Color selectedFg = UIManager.getColor("Button.darcula.selectedButtonForeground");
      if (selectedFg != null) {
        fg = selectedFg;
      }
    }
    g.setColor(fg);

    FontMetrics metrics = c.getFontMetrics(c.getFont());
    int mnemonicIndex = button.getDisplayedMnemonicIndex();
    if (model.isEnabled()) {

      BasicGraphicsUtils
              .drawStringUnderlineCharAt(c, (Graphics2D)g, text, mnemonicIndex, textRect.x + getTextShiftOffset(), textRect.y + metrics.getAscent() + getTextShiftOffset());
    }
    else {
      g.setColor(UIManager.getColor("Button.darcula.disabledText.shadow"));
      BasicGraphicsUtils
              .drawStringUnderlineCharAt(c, (Graphics2D)g, text, -1, textRect.x + getTextShiftOffset() + 1, textRect.y + metrics.getAscent() + getTextShiftOffset() + 1);

      if (!ModernButtonBorderPainter.isDefaultButton(button)) {
        g.setColor(UIManager.getColor("Button.disabledText"));
        BasicGraphicsUtils.drawStringUnderlineCharAt(c, (Graphics2D)g, text, -1, textRect.x + getTextShiftOffset(), textRect.y + metrics.getAscent() + getTextShiftOffset());
      }
    }
  }

  @Override
  public void update(Graphics g, JComponent c) {
    super.update(g, c);
    if (ModernButtonBorderPainter.isDefaultButton(c) && !SystemInfo.isMac) {
      if (!c.getFont().isBold()) {
        c.setFont(c.getFont().deriveFont(Font.BOLD));
      }
    }
  }

  protected Color getButtonColor1() {
    return UIManager.getColor("Button.darcula.color1");
  }

  protected Color getSelectedButtonColor1() {
    return UIManager.getColor("Button.darcula.selection.color1");
  }

  protected Color getSelectedButtonColor2() {
    return UIManager.getColor("Button.darcula.selection.color2");
  }
}
