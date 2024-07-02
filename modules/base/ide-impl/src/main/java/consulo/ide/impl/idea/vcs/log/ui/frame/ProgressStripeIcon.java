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
package consulo.ide.impl.idea.vcs.log.ui.frame;

import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ui.ex.Gray;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.AsyncProcessIcon;
import consulo.ui.ex.awt.GraphicsConfig;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.image.Image;
import consulo.ui.style.StyleManager;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

// TODO [VISTALL] we need impl UI Image, due we don't have unified impl
public abstract class ProgressStripeIcon implements Icon, Image {
  private static final int TRANSLATE = 1;
  private static final int HEIGHT = 3;
  @Nonnull
  private final JComponent myReferenceComponent;
  private final int myShift;

  private ProgressStripeIcon(@Nonnull JComponent component, int shift) {
    myReferenceComponent = component;
    myShift = shift;
  }

  public abstract int getChunkWidth();

  protected abstract void paint(@Nonnull Graphics2D g2, int x, int y, int shift);

  @Override
  public void paintIcon(Component c, Graphics g, int x, int y) {
    GraphicsConfig config = GraphicsUtil.setupAAPainting(g);
    Graphics2D g2 = (Graphics2D)g;

    int shift = myShift - getChunkWidth();
    while (shift < getIconWidth()) {
      paint(g2, x, y, shift);
      shift += getChunkWidth();
    }

    config.restore();
  }

  @Override
  public int getIconWidth() {
    return myReferenceComponent.getWidth();
  }

  @Override
  public int getIconHeight() {
    return getScaledHeight();
  }

  @Override
  public int getHeight() {
    return getIconHeight();
  }

  @Override
  public int getWidth() {
    return getIconWidth();
  }

  public static int getScaledHeight() {
    return JBUI.scale(HEIGHT);
  }

  private static class StripeIcon extends ProgressStripeIcon {
    private static final double ALPHA = 0.8;
    private static final JBColor BG_COLOR = new JBColor(ColorUtil.withAlpha(Gray._165, ALPHA), ColorUtil.withAlpha(Gray._110, ALPHA));
    private static final int WIDTH = 16;

    private StripeIcon(@Nonnull JComponent component, int shift) {
      super(component, shift);
    }

    @Override
    public int getChunkWidth() {
      return JBUI.scale(WIDTH);
    }

    @Override
    protected void paint(@Nonnull Graphics2D g2, int x, int y, int shift) {
      g2.setColor(BG_COLOR);

      Path2D.Double path = new Path2D.Double();
      int height = JBUI.scale(HEIGHT);
      float incline = height / 2.0f;
      float length = JBUI.scale(WIDTH) / 2.0f;
      float start = length / 2.0f;
      path.moveTo(x + shift + start, y + height);
      path.lineTo(x + shift + start + incline, y);
      path.lineTo(x + shift + start + incline + length, y);
      path.lineTo(x + shift + start + length, y + height);
      path.lineTo(x + shift + start, y + height);
      path.closePath();

      g2.fill(new Area(path));
    }
  }

  // this icon is not used under darcula
  @SuppressWarnings("UseJBColor")
  private static class GradientIcon extends ProgressStripeIcon {
    private static final double ALPHA = 0.5;
    private static final Color DARK_BLUE = new Color(0x4d9ff8);
    private static final Color DARK_GRAY = Gray._165;
    private static final Color LIGHT_BLUE = ColorUtil.withAlpha(new Color(0x90c2f8), ALPHA);
    private static final Color LIGHT_GRAY = ColorUtil.withAlpha(new Color(0xdbdbdb), ALPHA);
    private static final int GRADIENT = 128;
    private static final int GRADIENT_HEIGHT = 2;

    private GradientIcon(@Nonnull JComponent component, int shift) {
      super(component, shift);
    }

    @Override
    public int getChunkWidth() {
      return 2 * JBUI.scale(GRADIENT);
    }

    @Override
    public void paint(@Nonnull Graphics2D g2, int x, int y, int shift) {
      Color dark;
      Color light;
      if (UIUtil.isGraphite()) {
        dark = DARK_GRAY;
        light = LIGHT_GRAY;
      }
      else {
        dark = DARK_BLUE;
        light = LIGHT_BLUE;
      }
      g2.setPaint(new GradientPaint(x + shift, y, dark, x + shift + JBUI.scale(GRADIENT), y, light));
      g2.fill(new Rectangle(x + shift, y, JBUI.scale(GRADIENT), getIconHeight()));
      g2.setPaint(new GradientPaint(x + shift + JBUI.scale(GRADIENT), y, light, x + shift + 2 * JBUI.scale(GRADIENT), y, dark));
      g2.fill(new Rectangle(x + shift + JBUI.scale(GRADIENT), y, JBUI.scale(GRADIENT), getIconHeight()));
    }

    @Override
    public int getIconHeight() {
      return JBUI.scale(GRADIENT_HEIGHT);
    }
  }

  @Nonnull
  public static AsyncProcessIcon generateIcon(@Nonnull JComponent component) {
    List<Image> result = new ArrayList<>();
    if (UIUtil.isUnderAquaBasedLookAndFeel() && !StyleManager.get().getCurrentStyle().isDark()) {
      for (int i = 0; i < 2 * JBUI.scale(GradientIcon.GRADIENT); i += JBUI.scale(TRANSLATE)) {
        result.add(new GradientIcon(component, i));
      }
    }
    else {
      for (int i = 0; i < JBUI.scale(StripeIcon.WIDTH); i += JBUI.scale(TRANSLATE)) {
        result.add(new StripeIcon(component, i));
      }
      result = ContainerUtil.reverse(result);
    }

    Image passive = result.get(0);
    AsyncProcessIcon icon = new AsyncProcessIcon("ProgressWithStripes", result.toArray(new Image[result.size()]), passive) {
      @Override
      public Dimension getPreferredSize() {
        return new Dimension(component.getWidth(), passive.getHeight());
      }
    };
    component.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        super.componentResized(e);
        icon.revalidate();
      }
    });
    return icon;
  }
}
