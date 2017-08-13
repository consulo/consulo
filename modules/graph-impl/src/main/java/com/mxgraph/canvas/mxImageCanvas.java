/**
 * $Id: mxImageCanvas.java,v 1.1 2012/11/15 13:26:47 gaudenz Exp $
 * Copyright (c) 2007-2010, Gaudenz Alder, David Benson
 */
package com.mxgraph.canvas;

import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * An implementation of a canvas that uses Graphics2D for painting. To use an
 * image canvas for an existing graphics canvas and create an image the
 * following code is used:
 * <p/>
 * <code>BufferedImage image = mxCellRenderer.createBufferedImage(graph, cells, 1, Color.white, true, null, canvas);</code>
 */
public class mxImageCanvas implements mxICanvas {

  /**
   *
   */
  protected mxGraphics2DCanvas canvas;

  /**
   *
   */
  protected Graphics2D previousGraphics;

  /**
   *
   */
  protected BufferedImage image;

  /**
   *
   */
  public mxImageCanvas(mxGraphics2DCanvas canvas, int width, int height, Color background, boolean antiAlias) {
    this.canvas = canvas;
    previousGraphics = canvas.getGraphics();
    image = mxUtils.createBufferedImage(width, height, background);

    if (image != null) {
      Graphics2D g = image.createGraphics();
      mxUtils.setAntiAlias(g, antiAlias, true);
      canvas.setGraphics(g);
    }
  }

  /**
   *
   */
  public mxGraphics2DCanvas getGraphicsCanvas() {
    return canvas;
  }

  /**
   *
   */
  public BufferedImage getImage() {
    return image;
  }

  /**
   *
   */
  public Object drawCell(mxCellState state) {
    return canvas.drawCell(state);
  }

  /**
   *
   */
  public Object drawLabel(String label, mxCellState state, boolean html) {
    return canvas.drawLabel(label, state, html);
  }

  /**
   *
   */
  public double getScale() {
    return canvas.getScale();
  }

  /**
   *
   */
  public Point getTranslate() {
    return canvas.getTranslate();
  }

  /**
   *
   */
  public void setScale(double scale) {
    canvas.setScale(scale);
  }

  /**
   *
   */
  public void setTranslate(int dx, int dy) {
    canvas.setTranslate(dx, dy);
  }

  /**
   *
   */
  public BufferedImage destroy() {
    BufferedImage tmp = image;

    if (canvas.getGraphics() != null) {
      canvas.getGraphics().dispose();
    }

    canvas.setGraphics(previousGraphics);

    previousGraphics = null;
    canvas = null;
    image = null;

    return tmp;
  }

}
