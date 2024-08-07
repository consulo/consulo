// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.plaf.darcula;

import consulo.application.AllIcons;
import consulo.desktop.awt.ui.AltPressListener;
import consulo.platform.Platform;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ui.ex.awt.util.MacUIUtil;
import consulo.ui.ex.awt.util.UISettingsUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

import static consulo.desktop.awt.ui.plaf.darcula.DarculaUIUtil.BW;
import static consulo.desktop.awt.ui.plaf.darcula.DarculaUIUtil.MINIMUM_HEIGHT;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("UnregisteredNamedColor")
public class DarculaButtonUI extends BasicButtonUI {
  private final Rectangle viewRect = new Rectangle();
  private final Rectangle textRect = new Rectangle();
  private final Rectangle iconRect = new Rectangle();

  protected static JBValue HELP_BUTTON_DIAMETER = new JBValue.Float(22);
  protected static JBValue MINIMUM_BUTTON_WIDTH = new JBValue.Float(72);
  protected static JBValue HORIZONTAL_PADDING = new JBValue.Float(14);

  @SuppressWarnings({"MethodOverridesStaticMethodOfSuperclass", "unused"})
  public static ComponentUI createUI(JComponent c) {
    return new DarculaButtonUI();
  }

  public static boolean isSquare(Component c) {
    return c instanceof AbstractButton && "square".equals(((AbstractButton)c).getClientProperty("JButton.buttonType"));
  }

  public static boolean isDefaultButton(JComponent c) {
    return c instanceof JButton && ((JButton)c).isDefaultButton();
  }

  public static boolean isSmallComboButton(Component c) {
    ComboBoxAction a = getComboAction(c);
    return a != null && a.isSmallVariant();
  }

  public static ComboBoxAction getComboAction(Component c) {
    return c instanceof AbstractButton ? (ComboBoxAction)((JComponent)c).getClientProperty("styleCombo") : null;
  }

  @Override
  public void installDefaults(AbstractButton b) {
    super.installDefaults(b);
    b.setIconTextGap(textIconGap());
    b.setMargin(JBUI.emptyInsets());
  }

  protected int textIconGap() {
    return JBUIScale.scale(4);
  }

  /**
   * Paints additional buttons decorations
   *
   * @param g Graphics
   * @param c button component
   * @return {@code true} if it is allowed to continue painting,
   * {@code false} if painting should be stopped
   */
  @SuppressWarnings("UseJBColor")
  protected boolean paintDecorations(Graphics2D g, JComponent c) {
    Rectangle r = new Rectangle(c.getSize());
    JBInsets.removeFrom(r, JBUI.insets(1));

    if (UIUtil.isHelpButton(c)) {
      g.setPaint(UIUtil.getGradientPaint(0, 0, getButtonColorStart(), 0, r.height, getButtonColorEnd()));
      int diam = HELP_BUTTON_DIAMETER.get();
      int x = r.x + (r.width - diam) / 2;
      int y = r.x + (r.height - diam) / 2;

      g.fill(new Ellipse2D.Float(x, y, diam, diam));
      TargetAWT.to(AllIcons.Actions.Help).paintIcon(c, g, x + JBUIScale.scale(3), y + JBUIScale.scale(3));
      return false;
    }
    else {
      Graphics2D g2 = (Graphics2D)g.create();
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, MacUIUtil.USE_QUARTZ ? RenderingHints.VALUE_STROKE_PURE : RenderingHints.VALUE_STROKE_NORMALIZE);

        g2.translate(r.x, r.y);

        float arc = DarculaUIUtil.BUTTON_ARC.getFloat();
        float bw = isSmallComboButton(c) ? 0 : BW.getFloat();

        if (!c.hasFocus() && !isSmallComboButton(c) && c.isEnabled() && UIManager.getBoolean("Button.paintShadow")) {
          Color shadowColor = JBColor.namedColor("Button.shadowColor", JBColor.namedColor("Button.darcula.shadowColor", new JBColor(new Color(0xa6a6a633, true), new Color(0x36363680, true))));

          int shadowWidth = JBUIScale.scale(JBUI.getInt("Button.shadowWidth", 2));
          g2.setColor(isDefaultButton(c) ? JBColor.namedColor("Button.default.shadowColor", shadowColor) : shadowColor);
          g2.fill(new RoundRectangle2D.Float(bw, bw + shadowWidth, r.width - bw * 2, r.height - bw * 2, arc, arc));
        }

        if (c.isEnabled()) {
          g2.setPaint(getBackground(c, r));
          g2.fill(new RoundRectangle2D.Float(bw, bw, r.width - bw * 2, r.height - bw * 2, arc, arc));
        }
      }
      finally {
        g2.dispose();
      }
      return true;
    }
  }

  private Paint getBackground(JComponent c, Rectangle r) {
    Color backgroundColor = (Color)c.getClientProperty("JButton.backgroundColor");

    return backgroundColor != null
           ? backgroundColor
           : isSmallComboButton(c)
             ? JBColor.namedColor("ComboBoxButton.background", JBColor.namedColor("Button.darcula.smallComboButtonBackground", UIUtil.getPanelBackground()))
             : isDefaultButton(c)
               ? UIUtil.getGradientPaint(0, 0, getDefaultButtonColorStart(), 0, r.height, getDefaultButtonColorEnd())
               : UIUtil.getGradientPaint(0, 0, getButtonColorStart(), 0, r.height, getButtonColorEnd());
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    if (paintDecorations((Graphics2D)g, c)) {
      paintContents(g, (AbstractButton)c);
    }
  }

  @Override
  protected void paintText(Graphics g, JComponent c, Rectangle textRect, String text) {
    if (UIUtil.isHelpButton(c)) {
      return;
    }

    AbstractButton button = (AbstractButton)c;
    ButtonModel model = button.getModel();
    g.setColor(getButtonTextColor(button));

    FontMetrics metrics = c.getFontMetrics(c.getFont());
    if (model.isEnabled()) {

      BasicGraphicsUtils.drawStringUnderlineCharAt(c, (Graphics2D)g, text, getMnemonicIndex(button), textRect.x + getTextShiftOffset(), textRect.y + metrics.getAscent() + getTextShiftOffset());
    }
    else {
      paintDisabledText(g, text, c, textRect, metrics);
    }
  }

  protected int getMnemonicIndex(AbstractButton b) {
    return AltPressListener.isAltPressed() ? b.getDisplayedMnemonicIndex() : -1;
  }

  protected Color getButtonTextColor(AbstractButton button) {
    Color textColor = (Color)button.getClientProperty("JButton.textColor");
    return textColor != null ? textColor : DarculaUIUtil.getButtonTextColor(button);
  }

  public static Color getDisabledTextColor() {
    return UIManager.getColor("Button.disabledText");
  }

  protected void paintDisabledText(Graphics g, String text, JComponent c, Rectangle textRect, FontMetrics metrics) {
    g.setColor(UIManager.getColor("Button.disabledText"));
    BasicGraphicsUtils.drawStringUnderlineCharAt(c, (Graphics2D)g, text, -1, textRect.x + getTextShiftOffset(), textRect.y + metrics.getAscent() + getTextShiftOffset());
  }

  protected void paintContents(Graphics g, AbstractButton b) {
    FontMetrics fm = b.getFontMetrics(b.getFont());
    boolean isDotButton = isSquare(b) && b.getIcon() == AllIcons.General.Ellipsis;
    String text = isDotButton ? "..." : b.getText();
    Icon icon = isDotButton ? null : b.getIcon();
    text = layout(b, text, icon, fm, b.getWidth(), b.getHeight());

    if (isSquare(b)) {
      if (b.getIcon() == AllIcons.General.Ellipsis) {
        UISettingsUtil.setupAntialiasing(g);
        paintText(g, b, textRect, text);
      }
      else if (b.getIcon() != null) {
        paintIcon(g, b, iconRect);
      }
    }
    else {
      // Paint the Icon
      if (b.getIcon() != null) {
        paintIcon(g, b, iconRect);
      }

      if (text != null && !text.isEmpty()) {
        View v = (View)b.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
          v.paint(g, textRect);
        }
        else {
          UISettingsUtil.setupAntialiasing(g);
          paintText(g, b, textRect, text);
        }
      }
    }
  }

  protected Dimension getDarculaButtonSize(JComponent c, Dimension prefSize) {
    Insets i = c.getInsets();
    prefSize = ObjectUtil.notNull(prefSize, JBUI.emptySize());

    if (UIUtil.isHelpButton(c) || isSquare(c)) {
      int helpDiam = HELP_BUTTON_DIAMETER.get();
      return new Dimension(Math.max(prefSize.width, helpDiam + i.left + i.right), Math.max(prefSize.height, helpDiam + i.top + i.bottom));
    }
    else {
      int width = getComboAction(c) != null ? prefSize.width : Math.max(HORIZONTAL_PADDING.get() * 2 + prefSize.width, MINIMUM_BUTTON_WIDTH.get() + i.left + i.right);
      int height = Math.max(prefSize.height, getMinimumHeight() + i.top + i.bottom);

      return new Dimension(width, height);
    }
  }

  protected int getMinimumHeight() {
    return MINIMUM_HEIGHT.get();
  }

  @Override
  public final Dimension getPreferredSize(JComponent c) {
    AbstractButton b = (AbstractButton)c;
    int textIconGap = StringUtil.isEmpty(b.getText()) || b.getIcon() == null ? 0 : b.getIconTextGap();
    Dimension size = BasicGraphicsUtils.getPreferredButtonSize(b, textIconGap);
    return getDarculaButtonSize(c, size);
  }

  @Override
  public void update(Graphics g, JComponent c) {
    setupDefaultButton(c, g);
    super.update(g, c);
  }

  protected void setupDefaultButton(JComponent button, Graphics g) {
    Font f = button.getFont();
    if (!Platform.current().os().isMac() && f instanceof FontUIResource && isDefaultButton(button)) {
      g.setFont(f.deriveFont(Font.BOLD));
    }
  }

  protected Color getButtonColorStart() {
    return JBColor.namedColor("Button.startBackground", JBColor.namedColor("Button.darcula.startColor", 0x555a5c));
  }

  protected Color getButtonColorEnd() {
    return JBColor.namedColor("Button.endBackground", JBColor.namedColor("Button.darcula.endColor", 0x414648));
  }

  protected Color getDefaultButtonColorStart() {
    return JBColor.namedColor("Button.default.startBackground", JBColor.namedColor("Button.darcula.defaultStartColor", 0x384f6b));
  }

  protected Color getDefaultButtonColorEnd() {
    return JBColor.namedColor("Button.default.endBackground", JBColor.namedColor("Button.darcula.defaultEndColor", 0x233143));
  }

  protected String layout(AbstractButton b, String text, Icon icon, FontMetrics fm, int width, int height) {
    textRect.setBounds(0, 0, 0, 0);
    iconRect.setBounds(0, 0, 0, 0);

    viewRect.setBounds(0, 0, width, height);
    modifyViewRect(b, viewRect);

    // layout the text and icon
    return SwingUtilities
            .layoutCompoundLabel(b, fm, text, icon, b.getVerticalAlignment(), b.getHorizontalAlignment(), b.getVerticalTextPosition(), b.getHorizontalTextPosition(), viewRect, iconRect, textRect,
                                 StringUtil.isEmpty(text) || icon == null ? 0 : b.getIconTextGap());
  }

  protected void modifyViewRect(AbstractButton b, Rectangle rect) {
    JBInsets.removeFrom(rect, b.getInsets());
    JBInsets.removeFrom(rect, b.getMargin());
  }
}
