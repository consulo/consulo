package com.intellij.util;

import com.intellij.openapi.util.ScalableIcon;
import com.intellij.util.ui.JBUI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

public class IconUtil2 {
  /**
   * Returns a scaled icon instance.
   * <p>
   * The method delegates to {@link ScalableIcon#scale(float)} when applicable,
   * otherwise defaults to {@link #scale(Icon, double)}
   * <p>
   * In the following example:
   * <pre>
   * Icon myIcon = new MyIcon();
   * Icon scaledIcon = IconUtil.scale(myIcon, myComp, 2f);
   * Icon anotherScaledIcon = IconUtil.scale(scaledIcon, myComp, 2f);
   * assert(scaledIcon.getIconWidth() == anotherScaledIcon.getIconWidth()); // compare the scale of the icons
   * </pre>
   * The result of the assertion depends on {@code MyIcon} implementation. When {@code scaledIcon} is an instance of {@link ScalableIcon},
   * then {@code anotherScaledIcon} should be scaled according to the {@link ScalableIcon} javadoc, and the assertion should pass.
   * Otherwise, {@code anotherScaledIcon} should be 2 times bigger than {@code scaledIcon}, and 4 times bigger than {@code myIcon}.
   * So, prior to scale the icon recursively, the returned icon should be inspected for its type to understand the result.
   * But recursive scale should better be avoided.
   *
   * @param icon     the icon to scale
   * @param ancestor the component (or its ancestor) painting the icon, or null when not available
   * @param scale    the scale factor
   * @return the scaled icon
   */
  @Nonnull
  public static Icon scale(@Nonnull Icon icon, @Nullable Component ancestor, float scale) {
    if (icon instanceof ScalableIcon) {
      if (icon instanceof JBUI.ScaleContextAware) {
        ((JBUI.ScaleContextAware)icon).updateScaleContext(ancestor != null ? JBUI.ScaleContext.create(ancestor) : null);
      }
      return ((ScalableIcon)icon).scale(scale);
    }
    return scale(icon, scale);
  }

  /**
   * @deprecated use {@link #scale(Icon, Component, float)}
   */
  @Deprecated
  @Nonnull
  public static Icon scale(@Nonnull final Icon source, double _scale) {
    final double scale = Math.min(32, Math.max(.1, _scale));
    return new Icon() {
      @Override
      public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D)g.create();
        try {
          AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
          transform.preConcatenate(g2d.getTransform());
          g2d.setTransform(transform);
          source.paintIcon(c, g2d, x, y);
        }
        finally {
          g2d.dispose();
        }
      }

      @Override
      public int getIconWidth() {
        return (int)(source.getIconWidth() * scale);
      }

      @Override
      public int getIconHeight() {
        return (int)(source.getIconHeight() * scale);
      }
    };
  }
}
