// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.openapi.options.ex;

import consulo.ide.impl.idea.ide.ui.search.SearchUtil;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.JBTabbedPane;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.util.ColorUtil;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.Kernel;
import java.util.HashSet;
import java.util.Set;

public class GlassPanel extends JComponent {
  private final Set<JComponent> myLightComponents = new HashSet<>();
  private final JComponent myPanel;
  private static final Insets EMPTY_INSETS = JBUI.emptyInsets();
  private static final JBColor SPOTLIGHT_BORDER_COLOR = JBColor.namedColor("Settings.Spotlight.borderColor", ColorUtil.toAlpha(JBColor.ORANGE, 100));

  public GlassPanel(JComponent containingPanel) {
    myPanel = containingPanel;
    setVisible(false);
  }

  @Override
  public void paintComponent(Graphics g) {
    paintSpotlights(g);
  }

  protected void paintSpotlights(Graphics g) {
    paintSpotlight(g, this);
  }

  public void paintSpotlight(Graphics g, JComponent surfaceComponent) {
    Dimension size = surfaceComponent.getSize();
    if (myLightComponents.size() > 0) {
      int stroke = 2;

      Rectangle visibleRect = myPanel.getVisibleRect();
      Point leftPoint = SwingUtilities.convertPoint(myPanel, new Point(visibleRect.x, visibleRect.y), surfaceComponent);
      Area innerPanel = new Area(new Rectangle2D.Double(leftPoint.x, leftPoint.y, visibleRect.width, visibleRect.height));
      Area mask = new Area(new Rectangle(-stroke, -stroke, 2 * stroke + size.width, 2 * stroke + size.height));
      for (JComponent lightComponent : myLightComponents) {
        Area area = getComponentArea(surfaceComponent, lightComponent, 1);
        if (area == null) continue;

        if (lightComponent instanceof JLabel) {
          JLabel label = (JLabel)lightComponent;
          Component labelFor = label.getLabelFor();
          if (labelFor instanceof JComponent) {
            Area labelForArea = getComponentArea(surfaceComponent, (JComponent)labelFor, 1);
            if (labelForArea != null) {
              area.add(labelForArea);
            }
          }
        }

        area.intersect(innerPanel);
        mask.subtract(area);
      }
      Graphics clip = g.create(0, 0, size.width, size.height);
      try {
        Graphics2D g2 = (Graphics2D)clip;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);

        Color background = surfaceComponent.getBackground();
        g2.setColor(ColorUtil.toAlpha(background == null ? null : background.darker(), 100));
        g2.fill(mask);

        g2.setStroke(new BasicStroke(stroke));
        g2.setColor(SPOTLIGHT_BORDER_COLOR);
        g2.draw(mask);
      }
      finally {
        clip.dispose();
      }
    }
  }

  @Nullable
  private Area getComponentArea(JComponent surfaceComponent, JComponent lightComponent, int offset) {
    if (!lightComponent.isShowing()) return null;

    Point panelPoint = SwingUtilities.convertPoint(lightComponent, new Point(0, 0), surfaceComponent);
    int x = panelPoint.x;
    int y = panelPoint.y;

    Insets insetsToIgnore = lightComponent.getInsets();
    boolean isWithBorder = Boolean.TRUE.equals(lightComponent.getClientProperty(SearchUtil.HIGHLIGHT_WITH_BORDER));
    boolean isLabelFromTabbedPane = Boolean.TRUE.equals(lightComponent.getClientProperty(JBTabbedPane.LABEL_FROM_TABBED_PANE));

    if (insetsToIgnore == null || isWithBorder) {
      insetsToIgnore = EMPTY_INSETS;
    }

    int hInset = getComponentHInset(isWithBorder, isLabelFromTabbedPane);
    int vInset = getComponentVInset(isWithBorder, isLabelFromTabbedPane);
    hInset += offset;
    vInset += offset;
    int xCoord = x - hInset + insetsToIgnore.left;
    int yCoord = y - vInset + insetsToIgnore.top;
    int width = lightComponent.getWidth() + hInset * 2 - insetsToIgnore.right - insetsToIgnore.left;
    int height = lightComponent.getHeight() + vInset * 2 - insetsToIgnore.top - insetsToIgnore.bottom;
    return new Area(new RoundRectangle2D.Double(xCoord, yCoord, width, height, Math.min(height, 30), Math.min(height, 30)));
  }

  protected int getComponentHInset(boolean isWithBorder, boolean isLabelFromTabbedPane) {
    return isWithBorder ? 7 : isLabelFromTabbedPane ? 20 : 7;
  }

  protected int getComponentVInset(boolean isWithBorder, boolean isLabelFromTabbedPane) {
    return isWithBorder ? 1 : isLabelFromTabbedPane ? 10 : 5;
  }

  protected static Kernel getBlurKernel(int blurSize) {
    if (blurSize <= 0) return null;

    int size = blurSize * blurSize;
    float coeff = 1.0f / size;
    float[] kernelData = new float[size];

    for (int i = 0; i < size; i++) {
      kernelData[i] = coeff;
    }

    return new Kernel(blurSize, blurSize, kernelData);
  }


  public static double getArea(JComponent component) {
    return Math.PI * component.getWidth() * component.getHeight() / 4.0;
  }

  public void addSpotlight(JComponent component) {
    myLightComponents.add(component);
    setVisible(true);
  }

  public void removeSpotlight(JComponent component) {
    myLightComponents.remove(component);
    if (myLightComponents.isEmpty()) {
      setVisible(false);
    }
  }

  public void clear() {
    myLightComponents.clear();
    setVisible(false);
  }
}
