// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.ui.plaf.darcula;

import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ide.impl.idea.util.ui.table.JBTableRowEditor;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.JBValue;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.MacUIUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

import static javax.swing.SwingConstants.EAST;
import static javax.swing.SwingConstants.WEST;

/**
 * @author Konstantin Bulenkov
 */
@SuppressWarnings("UnregisteredNamedColor")
public class DarculaUIUtil {
  public enum Outline {
    error {
      @Override
      public void setGraphicsColor(Graphics2D g, boolean focused) {
        g.setColor(JBCurrentTheme.Focus.errorColor(focused));
      }
    },

    warning {
      @Override
      public void setGraphicsColor(Graphics2D g, boolean focused) {
        g.setColor(JBCurrentTheme.Focus.warningColor(focused));
      }
    },

    defaultButton {
      @Override
      public void setGraphicsColor(Graphics2D g, boolean focused) {
        if (focused) {
          g.setColor(JBCurrentTheme.Focus.defaultButtonColor());
        }
      }
    },

    focus {
      @Override
      public void setGraphicsColor(Graphics2D g, boolean focused) {
        if (focused) {
          g.setColor(JBCurrentTheme.Focus.focusColor());
        }
      }
    };

    abstract public void setGraphicsColor(Graphics2D g, boolean focused);
  }

  /**
   * Deprecated in favor of {@link #paintFocusBorder(Graphics2D, int, int, float, boolean)}
   */
  @Deprecated
  public static void paintFocusRing(Graphics g, Rectangle r) {
    Graphics2D g2 = (Graphics2D)g.create();
    try {
      g2.translate(r.x, r.y);
      paintFocusBorder(g2, r.width, r.height, COMPONENT_ARC.getFloat(), true);
    }
    finally {
      g2.dispose();
    }
  }

  public static void paintFocusOval(Graphics2D g, float x, float y, float width, float height) {
    Outline.focus.setGraphicsColor(g, true);

    float blw = BW.getFloat() + LW.getFloat();
    Path2D shape = new Path2D.Float(Path2D.WIND_EVEN_ODD);
    shape.append(new Ellipse2D.Float(x - blw, y - blw, width + blw * 2, height + blw * 2), false);
    shape.append(new Ellipse2D.Float(x, y, width, height), false);
    g.fill(shape);
  }

  @Deprecated
  public static void paintErrorBorder(Graphics2D g, int width, int height, int arc, boolean symmetric, boolean hasFocus) {
    paintOutlineBorder(g, width, height, arc, symmetric, hasFocus, Outline.error);
  }

  public static void paintOutlineBorder(Graphics2D g, int width, int height, float arc, boolean symmetric, boolean hasFocus, Outline type) {
    type.setGraphicsColor(g, hasFocus);
    doPaint(g, width, height, arc, symmetric);
  }

  public static void paintFocusBorder(Graphics2D g, int width, int height, float arc, boolean symmetric) {
    Outline.focus.setGraphicsColor(g, true);
    doPaint(g, width, height, arc, symmetric);
  }

  @SuppressWarnings("SuspiciousNameCombination")
  public static void doPaint(Graphics2D g, int width, int height, float arc, boolean symmetric) {
    float bw = UIUtil.isUnderDefaultMacTheme() ? JBUI.scale(3) : BW.getFloat();
    float lw = UIUtil.isUnderDefaultMacTheme() ? JBUI.scale(UIUtil.isRetina(g) ? 0.5f : 1.0f) : LW.getFloat();

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, MacUIUtil.USE_QUARTZ ? RenderingHints.VALUE_STROKE_PURE : RenderingHints.VALUE_STROKE_NORMALIZE);

    float outerArc = arc > 0 ? arc + bw - JBUI.scale(2f) : bw;
    float rightOuterArc = symmetric ? outerArc : JBUI.scale(6f);
    Path2D outerRect = new Path2D.Float(Path2D.WIND_EVEN_ODD);
    outerRect.moveTo(width - rightOuterArc, 0);
    outerRect.quadTo(width, 0, width, rightOuterArc);
    outerRect.lineTo(width, height - rightOuterArc);
    outerRect.quadTo(width, height, width - rightOuterArc, height);
    outerRect.lineTo(outerArc, height);
    outerRect.quadTo(0, height, 0, height - outerArc);
    outerRect.lineTo(0, outerArc);
    outerRect.quadTo(0, 0, outerArc, 0);
    outerRect.closePath();

    bw += lw;
    float rightInnerArc = symmetric ? outerArc : JBUI.scale(7f);
    Path2D innerRect = new Path2D.Float(Path2D.WIND_EVEN_ODD);
    innerRect.moveTo(width - rightInnerArc, bw);
    innerRect.quadTo(width - bw, bw, width - bw, rightInnerArc);
    innerRect.lineTo(width - bw, height - rightInnerArc);
    innerRect.quadTo(width - bw, height - bw, width - rightInnerArc, height - bw);
    innerRect.lineTo(outerArc, height - bw);
    innerRect.quadTo(bw, height - bw, bw, height - outerArc);
    innerRect.lineTo(bw, outerArc);
    innerRect.quadTo(bw, bw, outerArc, bw);
    innerRect.closePath();

    Path2D path = new Path2D.Float(Path2D.WIND_EVEN_ODD);
    path.append(outerRect, false);
    path.append(innerRect, false);
    g.fill(path);
  }

  public static boolean isCurrentEventShiftDownEvent() {
    AWTEvent event = IdeEventQueue.getInstance().getTrueCurrentEvent();
    return (event instanceof KeyEvent && ((KeyEvent)event).isShiftDown());
  }

  /**
   * @return -1 if visual position shouldn't be patched, otherwise selection start or selection end
   * @see javax.swing.plaf.basic.BasicTextUI#getNextVisualPositionFrom(JTextComponent, int, Position.Bias, int, Position.Bias[])
   */
  public static int getPatchedNextVisualPositionFrom(JTextComponent t, int pos, int direction) {
    if (!isCurrentEventShiftDownEvent()) {
      if (direction == WEST && t.getSelectionStart() < t.getSelectionEnd() && t.getSelectionEnd() == pos) {
        return t.getSelectionStart();
      }
      if (direction == EAST && t.getSelectionStart() < t.getSelectionEnd() && t.getSelectionStart() == pos) {
        return t.getSelectionEnd();
      }
    }
    return -1;
  }

  public static void paintCellEditorBorder(Graphics2D g2, Component c, Rectangle r, boolean hasFocus) {
    g2 = (Graphics2D)g2.create();
    try {
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, MacUIUtil.USE_QUARTZ ? RenderingHints.VALUE_STROKE_PURE : RenderingHints.VALUE_STROKE_NORMALIZE);

      float bw = CELL_EDITOR_BW.getFloat();

      Path2D border = new Path2D.Float(Path2D.WIND_EVEN_ODD);
      border.append(new Rectangle2D.Float(0, 0, r.width, r.height), false);
      border.append(new Rectangle2D.Float(bw, bw, r.width - bw * 2, r.height - bw * 2), false);

      Object op = ((JComponent)c).getClientProperty("JComponent.outline");
      if (op != null || hasFocus) {
        Outline outline = op == null ? Outline.focus : Outline.valueOf(op.toString());
        outline.setGraphicsColor(g2, true);
        g2.fill(border);
      }
    }
    finally {
      g2.dispose();
    }
  }

  public static class MouseHoverPropertyTrigger extends MouseAdapter {
    private final JComponent repaintComponent;
    private final String hoverProperty;

    public MouseHoverPropertyTrigger(@Nonnull JComponent repaintComponent, @Nonnull String hoverProperty) {
      this.repaintComponent = repaintComponent;
      this.hoverProperty = hoverProperty;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      setHover((JComponent)e.getComponent(), Boolean.TRUE);
    }

    @Override
    public void mouseExited(MouseEvent e) {
      setHover((JComponent)e.getComponent(), Boolean.FALSE);
    }

    private void setHover(JComponent c, Boolean value) {
      if (c.isEnabled()) {
        c.putClientProperty(hoverProperty, value);
        repaintComponent.repaint();
      }
    }
  }

  public static final String COMPACT_PROPERTY = "JComponent.compactHeight";

  public static boolean isCompact(Component c) {
    return c instanceof JComponent && ((JComponent)c).getClientProperty(COMPACT_PROPERTY) == Boolean.TRUE;
  }

  public static boolean isTableCellEditor(Component c) {
    return Boolean.TRUE.equals(((JComponent)c).getClientProperty("JComboBox.isTableCellEditor")) ||
           UIUtil.findParentByCondition(c, p -> p instanceof JBTableRowEditor) == null && UIUtil.findParentByCondition(c, p -> p instanceof JTable) != null;
  }

  public static final JBValue MINIMUM_WIDTH = new JBValue.Float(64);
  public static final JBValue MINIMUM_HEIGHT = new JBValue.Float(24);
  public static final JBValue COMPACT_HEIGHT = new JBValue.Float(20);
  public static final JBValue ARROW_BUTTON_WIDTH = new JBValue.Float(23);
  public static final JBValue LW = new JBValue.Float(1);
  public static final JBValue BW = new JBValue.UIInteger("Component.focusWidth", 2);
  public static final JBValue CELL_EDITOR_BW = new JBValue.UIInteger("CellEditor.border.width", 2);
  public static final JBValue BUTTON_ARC = new JBValue.UIInteger("Button.arc", 6);
  public static final JBValue COMPONENT_ARC = new JBValue.UIInteger("Component.arc", 5);

  /**
   * @deprecated use LW.get() instead
   */
  @SuppressWarnings("unused")
  @Deprecated
  public static float lw(Graphics2D g2) {
    return JBUI.scale(1.0f);
  }

  /**
   * @deprecated use BW.get() instead
   */
  @Deprecated
  public static float bw() {
    return BW.getFloat();
  }

  /**
   * @deprecated use COMPONENT_ARC.get() instead
   */
  @Deprecated
  public static float arc() {
    return COMPONENT_ARC.getFloat();
  }

  /**
   * @deprecated use BUTTON_ARC.get() instead
   */
  @Deprecated
  public static float buttonArc() {
    return BUTTON_ARC.get();
  }

  public static Insets paddings() {
    return JBUI.insets(1);
  }

  public static Color getOutlineColor(boolean enabled, boolean focused) {
    return enabled
           ? focused
             ? JBColor.namedColor("Component.focusedBorderColor", JBColor.namedColor("Outline.focusedColor", 0x87AFDA))
             : JBColor.namedColor("Component.borderColor", JBColor.namedColor("Outline.color", Gray.xBF))
           : JBColor.namedColor("Component.disabledBorderColor", JBColor.namedColor("Outline.disabledColor", Gray.xCF));
  }

  @Deprecated
  public static Color getArrowButtonBackgroundColor(boolean enabled, boolean editable) {
    return JBCurrentTheme.Arrow.backgroundColor(enabled, editable);
  }

  @Deprecated
  public static Color getArrowButtonForegroundColor(boolean enabled) {
    return JBCurrentTheme.Arrow.foregroundColor(enabled);
  }

  public static Dimension maximize(@Nullable Dimension s1, @Nonnull Dimension s2) {
    return isEmpty(s1) ? s2 : new Dimension(Math.max(s1.width, s2.width), Math.max(s1.height, s2.height));
  }

  private static boolean isEmpty(Dimension d) {
    return d == null || d.width == 0 && d.height == 0;
  }

  public static Color getButtonTextColor(@Nonnull AbstractButton button) {
    Color fg = button.getForeground();
    if (fg instanceof UIResource && DarculaButtonUI.isDefaultButton(button)) {
      Color selectedFg = UIManager.getColor("Button.default.foreground");
      if (selectedFg == null) {
        selectedFg = UIManager.getColor("Button.darcula.selectedButtonForeground");
      }

      if (selectedFg != null) {
        return selectedFg;
      }
    }
    return fg;
  }

  public static boolean isComboBoxEditor(Component c) {
    return UIUtil.getParentOfType(JComboBox.class, c) != null;
  }

  public static boolean isMultiLineHTML(@Nullable String text) {
    if (text != null) {
      text = text.toLowerCase(Locale.getDefault());
      return BasicHTML.isHTMLString(text) && (text.contains("<br>") || text.contains("<br/>"));
    }
    return false;
  }
}
