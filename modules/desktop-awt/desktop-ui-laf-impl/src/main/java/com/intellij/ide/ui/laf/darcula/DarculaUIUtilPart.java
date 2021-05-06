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
package com.intellij.ide.ui.laf.darcula;

import com.intellij.ide.ui.laf.intellij.IntelliJLaf;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.MacUIUtil;
import com.intellij.util.ui.UIUtil;

import java.awt.*;
import java.awt.geom.Path2D;

public class DarculaUIUtilPart extends DarculaUIUtil {
  public static void paintFocusBorder(Graphics2D g, int width, int height, int arc, boolean symmetric) {
    g.setPaint(IntelliJLaf.isGraphite() ? MAC_GRAPHITE_COLOR : MAC_REGULAR_COLOR);
    doPaint(g, width, height, arc, symmetric);
  }

  public static void paintErrorBorder(Graphics2D g, int width, int height, int arc, boolean symmetric, boolean hasFocus) {
    g.setPaint(hasFocus ? ACTIVE_ERROR_COLOR : INACTIVE_ERROR_COLOR);
    doPaint(g, width, height, arc, symmetric);
  }

  @SuppressWarnings("SuspiciousNameCombination")
  private static void doPaint(Graphics2D g, int width, int height, int arc, boolean symmetric) {
    double bw = UIUtil.isRetina(g) ? 0.5 : 1.0;
    double lw = JBUI.scale(UIUtil.isUnderDefaultMacTheme() ? 3 : 2);

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, MacUIUtil.USE_QUARTZ ? RenderingHints.VALUE_STROKE_PURE : RenderingHints.VALUE_STROKE_NORMALIZE);

    double outerArc = arc > 0 ? arc + lw - JBUI.scale(2) : lw;
    double rightOuterArc = symmetric ? outerArc : JBUI.scale(6);
    Path2D outerRect = new Path2D.Double(Path2D.WIND_EVEN_ODD);
    outerRect.moveTo(width - rightOuterArc, 0);
    outerRect.quadTo(width, 0, width, rightOuterArc);
    outerRect.lineTo(width, height - rightOuterArc);
    outerRect.quadTo(width, height, width - rightOuterArc, height);
    outerRect.lineTo(outerArc, height);
    outerRect.quadTo(0, height, 0, height - outerArc);
    outerRect.lineTo(0, outerArc);
    outerRect.quadTo(0, 0, outerArc, 0);
    outerRect.closePath();

    lw += bw;
    double rightInnerArc = symmetric ? outerArc : JBUI.scale(7);
    Path2D innerRect = new Path2D.Double(Path2D.WIND_EVEN_ODD);
    innerRect.moveTo(width - rightInnerArc, lw);
    innerRect.quadTo(width - lw, lw, width - lw, rightInnerArc);
    innerRect.lineTo(width - lw, height - rightInnerArc);
    innerRect.quadTo(width - lw, height - lw, width - rightInnerArc, height - lw);
    innerRect.lineTo(outerArc, height - lw);
    innerRect.quadTo(lw, height - lw, lw, height - outerArc);
    innerRect.lineTo(lw, outerArc);
    innerRect.quadTo(lw, lw, outerArc, lw);
    innerRect.closePath();

    Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
    path.append(outerRect, false);
    path.append(innerRect, false);
    g.fill(path);
  }
}
