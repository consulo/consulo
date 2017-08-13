/**
 * $Id: mxImageShape.java,v 1.1 2012/11/15 13:26:44 gaudenz Exp $
 * Copyright (c) 2007-2010, Gaudenz Alder, David Benson
 */
package com.mxgraph.shape;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;

import java.awt.*;

/**
 * A rectangular shape that contains a single image
 */
public class mxImageShape extends mxRectangleShape {

  /**
   *
   */
  public void paintShape(mxGraphics2DCanvas canvas, mxCellState state) {
    super.paintShape(canvas, state);

    boolean flipH = mxUtils.isTrue(state.getStyle(), mxConstants.STYLE_IMAGE_FLIPH, false);
    boolean flipV = mxUtils.isTrue(state.getStyle(), mxConstants.STYLE_IMAGE_FLIPV, false);

    canvas
      .drawImage(getImageBounds(canvas, state), getImageForStyle(canvas, state), mxGraphics2DCanvas.PRESERVE_IMAGE_ASPECT, flipH, flipV);
  }

  /**
   *
   */
  public Rectangle getImageBounds(mxGraphics2DCanvas canvas, mxCellState state) {
    return state.getRectangle();
  }

  /**
   *
   */
  public boolean hasGradient(mxGraphics2DCanvas canvas, mxCellState state) {
    return false;
  }

  /**
   *
   */
  public String getImageForStyle(mxGraphics2DCanvas canvas, mxCellState state) {
    return canvas.getImageForStyle(state.getStyle());
  }

  /**
   *
   */
  public Color getFillColor(mxGraphics2DCanvas canvas, mxCellState state) {
    return mxUtils.getColor(state.getStyle(), mxConstants.STYLE_IMAGE_BACKGROUND);
  }

  /**
   *
   */
  public Color getStrokeColor(mxGraphics2DCanvas canvas, mxCellState state) {
    return mxUtils.getColor(state.getStyle(), mxConstants.STYLE_IMAGE_BORDER);
  }

}
