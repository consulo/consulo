package com.mxgraph.shape;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;

import java.awt.*;

public class mxDoubleEllipseShape extends mxEllipseShape {

  /**
   *
   */
  public void paintShape(mxGraphics2DCanvas canvas, mxCellState state) {
    super.paintShape(canvas, state);

    int inset = (int)Math.round((mxUtils.getFloat(state.getStyle(), mxConstants.STYLE_STROKEWIDTH, 1) + 3) * canvas.getScale());

    Rectangle rect = state.getRectangle();
    int x = rect.x + inset;
    int y = rect.y + inset;
    int w = rect.width - 2 * inset;
    int h = rect.height - 2 * inset;

    canvas.getGraphics().drawOval(x, y, w, h);
  }

}
