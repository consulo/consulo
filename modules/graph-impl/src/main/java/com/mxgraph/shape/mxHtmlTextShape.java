/**
 * $Id: mxHtmlTextShape.java,v 1.1 2012/11/15 13:26:44 gaudenz Exp $
 * Copyright (c) 2010, Gaudenz Alder, David Benson
 */
package com.mxgraph.shape;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxLightweightLabel;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * To set global CSS for all HTML labels, use the following code:
 * <p/>
 * <pre>
 * mxGraphics2DCanvas.putTextShape(mxGraphics2DCanvas.TEXT_SHAPE_HTML,
 *   new mxHtmlTextShape()
 *   {
 *     protected String createHtmlDocument(Map<String, Object> style, String text)
 *     {
 *       return mxUtils.createHtmlDocument(style, text, 1, 0,
 *           "<style type=\"text/css\">.selectRef { " +
 *           "font-size:9px;font-weight:normal; }</style>");
 *     }
 *   }
 * );
 * </pre>
 */
public class mxHtmlTextShape implements mxITextShape {

  /**
   * Specifies if linefeeds should be replaced with breaks in HTML markup.
   * Default is true.
   */
  protected boolean replaceHtmlLinefeeds = true;

  /**
   * Returns replaceHtmlLinefeeds
   */
  public boolean isReplaceHtmlLinefeeds() {
    return replaceHtmlLinefeeds;
  }

  /**
   * Returns replaceHtmlLinefeeds
   */
  public void setReplaceHtmlLinefeeds(boolean value) {
    replaceHtmlLinefeeds = value;
  }

  /**
   *
   */
  protected String createHtmlDocument(Map<String, Object> style, String text) {
    return mxUtils.createHtmlDocument(style, text);
  }

  /**
   *
   */
  public void paintShape(mxGraphics2DCanvas canvas, String text, mxCellState state, Map<String, Object> style) {
    mxLightweightLabel textRenderer = mxLightweightLabel.getSharedInstance();
    CellRendererPane rendererPane = canvas.getRendererPane();
    Rectangle rect = state.getLabelBounds().getRectangle();
    Graphics2D g = canvas.getGraphics();

    if (textRenderer != null && rendererPane != null && (g.getClipBounds() == null || g.getClipBounds().intersects(rect))) {
      double scale = canvas.getScale();
      int x = rect.x;
      int y = rect.y;
      int w = rect.width;
      int h = rect.height;

      if (!mxUtils.isTrue(style, mxConstants.STYLE_HORIZONTAL, true)) {
        g.rotate(-Math.PI / 2, x + w / 2, y + h / 2);
        g.translate(w / 2 - h / 2, h / 2 - w / 2);

        int tmp = w;
        w = h;
        h = tmp;
      }

      // Replaces the linefeeds with BR tags
      if (isReplaceHtmlLinefeeds()) {
        text = text.replaceAll("\n", "<br>");
      }

      // Renders the scaled text
      textRenderer.setText(createHtmlDocument(style, text));
      textRenderer.setFont(mxUtils.getFont(style, canvas.getScale()));
      g.scale(scale, scale);
      rendererPane.paintComponent(g, textRenderer, rendererPane, (int)(x / scale) + mxConstants.LABEL_INSET,
                                  (int)(y / scale) + mxConstants.LABEL_INSET, (int)(w / scale), (int)(h / scale), true);
    }
  }

}
