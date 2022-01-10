/**
 * $Id: mxITextShape.java,v 1.1 2012/11/15 13:26:44 gaudenz Exp $
 * Copyright (c) 2010, Gaudenz Alder, David Benson
 */
package com.mxgraph.shape;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.view.mxCellState;

import java.util.Map;

public interface mxITextShape {
  /**
   *
   */
  void paintShape(mxGraphics2DCanvas canvas, String text, mxCellState state, Map<String, Object> style);

}
