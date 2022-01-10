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
package consulo.ide.ui.laf;

import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;

/**
 * @author VISTALL
 * @since 23-Aug-15
 */
public class DPIAwareSliderUI extends BasicSliderUI {
  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  public static ComponentUI createUI(JComponent c) {
    return new DPIAwareSliderUI((JSlider)c);
  }

  public DPIAwareSliderUI(JSlider b) {
    super(b);
  }

  @Override
  public void paintThumb(Graphics g) {
    Rectangle knobBounds = thumbRect;
    int w = knobBounds.width;
    int h = knobBounds.height;

    g.translate(knobBounds.x, knobBounds.y);

    if (slider.isEnabled()) {
      g.setColor(slider.getBackground());
    }
    else {
      g.setColor(slider.getBackground().darker());
    }

    Boolean paintThumbArrowShape = (Boolean)slider.getClientProperty("Slider.paintThumbArrowShape");

    if ((!slider.getPaintTicks() && paintThumbArrowShape == null) || paintThumbArrowShape == Boolean.FALSE) {

      // "plain" version
      g.fillRect(0, 0, w, h);

      g.setColor(Color.black);
      g.drawLine(0, h - JBUI.scale(1), w - JBUI.scale(1), h - JBUI.scale(1));
      g.drawLine(w - JBUI.scale(1), 0, w - JBUI.scale(1), h - JBUI.scale(1));

      g.setColor(getHighlightColor());
      g.drawLine(0, 0, 0, h - JBUI.scale(2));
      g.drawLine(JBUI.scale(1), 0, w - JBUI.scale(2), 0);

      g.setColor(getShadowColor());
      g.drawLine(JBUI.scale(1), h - JBUI.scale(2), w - JBUI.scale(2), h - JBUI.scale(2));
      g.drawLine(w - JBUI.scale(2), JBUI.scale(1), w - JBUI.scale(2), h - JBUI.scale(3));
    }
    else if (slider.getOrientation() == JSlider.HORIZONTAL) {
      int cw = w / 2;
      g.fillRect(JBUI.scale(1), JBUI.scale(1), w - JBUI.scale(3), h - JBUI.scale(1) - cw);
      Polygon p = new Polygon();
      p.addPoint(JBUI.scale(1), h - cw);
      p.addPoint(cw - JBUI.scale(1), h - JBUI.scale(1));
      p.addPoint(w - JBUI.scale(2), h - JBUI.scale(1) - cw);
      g.fillPolygon(p);

      g.setColor(getHighlightColor());
      g.drawLine(0, 0, w - JBUI.scale(2), 0);
      g.drawLine(0, JBUI.scale(1), 0, h - JBUI.scale(1) - cw);
      g.drawLine(0, h - cw, cw - JBUI.scale(1), h - JBUI.scale(1));

      g.setColor(Color.black);
      g.drawLine(w - JBUI.scale(1), 0, w - JBUI.scale(1), h - JBUI.scale(2) - cw);
      g.drawLine(w - JBUI.scale(1), h - JBUI.scale(1) - cw, w - JBUI.scale(1) - cw, h - JBUI.scale(1));

      g.setColor(getShadowColor());
      g.drawLine(w - JBUI.scale(2), JBUI.scale(1), w - JBUI.scale(2), h - JBUI.scale(2) - cw);
      g.drawLine(w - JBUI.scale(2), h - JBUI.scale(1) - cw, w - JBUI.scale(1) - cw, h - JBUI.scale(2));
    }
    else {  // vertical
      int cw = h / 2;
      if (slider.getComponentOrientation().isLeftToRight()) {
        g.fillRect(JBUI.scale(1), JBUI.scale(1), w - JBUI.scale(1) - cw, h - JBUI.scale(3));
        Polygon p = new Polygon();
        p.addPoint(w - cw - JBUI.scale(1), 0);
        p.addPoint(w - JBUI.scale(1), cw);
        p.addPoint(w - JBUI.scale(1) - cw, h - JBUI.scale(2));
        g.fillPolygon(p);

        g.setColor(getHighlightColor());
        g.drawLine(0, 0, 0, h - JBUI.scale(2));                  // left
        g.drawLine(JBUI.scale(1), 0, w - JBUI.scale(1) - cw, 0);                 // top
        g.drawLine(w - cw - JBUI.scale(1), 0, w - JBUI.scale(1), cw);              // top slant

        g.setColor(Color.black);
        g.drawLine(0, h - JBUI.scale(1), w - JBUI.scale(2) - cw, h - JBUI.scale(1));             // bottom
        g.drawLine(w - JBUI.scale(1) - cw, h - JBUI.scale(1), w - JBUI.scale(1), h - JBUI.scale(1) - cw);        // bottom slant

        g.setColor(getShadowColor());
        g.drawLine(JBUI.scale(1), h - JBUI.scale(2), w - JBUI.scale(2) - cw, h - JBUI.scale(2));         // bottom
        g.drawLine(w - JBUI.scale(1) - cw, h - JBUI.scale(2), w - JBUI.scale(2), h - cw - JBUI.scale(1));     // bottom slant
      }
      else {
        g.fillRect(JBUI.scale(5), JBUI.scale(1), w - JBUI.scale(1) - cw, h - JBUI.scale(3));
        Polygon p = new Polygon();
        p.addPoint(cw, 0);
        p.addPoint(0, cw);
        p.addPoint(cw, h - JBUI.scale(2));
        g.fillPolygon(p);

        g.setColor(getHighlightColor());
        g.drawLine(cw - JBUI.scale(1), 0, w - JBUI.scale(2), 0);             // top
        g.drawLine(0, cw, cw, 0);                // top slant

        g.setColor(Color.black);
        g.drawLine(0, h - JBUI.scale(1) - cw, cw, h - JBUI.scale(1));         // bottom slant
        g.drawLine(cw, h - JBUI.scale(1), w - JBUI.scale(1), h - JBUI.scale(1));           // bottom

        g.setColor(getShadowColor());
        g.drawLine(cw, h - JBUI.scale(2), w - JBUI.scale(2), h - JBUI.scale(2));         // bottom
        g.drawLine(w - JBUI.scale(1), JBUI.scale(1), w - JBUI.scale(1), h - JBUI.scale(2));          // right
      }
    }

    g.translate(-knobBounds.x, -knobBounds.y);
  }

  @Override
  protected Dimension getThumbSize() {
    Dimension size = new Dimension();

    if (slider.getOrientation() == JSlider.VERTICAL) {
      size.width = 20;
      size.height = 11;
    }
    else {
      size.width = 11;
      size.height = 20;
    }

    return JBUI.size(size);
  }

  @Override
  public Dimension getPreferredHorizontalSize() {
    return JBUI.size(200, 21);
  }

  @Override
  public Dimension getPreferredVerticalSize() {
    return JBUI.size(21, 200);
  }

  @Override
  public Dimension getMinimumHorizontalSize() {
    return JBUI.size(36, 21);
  }

  @Override
  public Dimension getMinimumVerticalSize() {
    return JBUI.size(21, 36);
  }
}
