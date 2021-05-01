/******************************************************************************
 * Copyright (c) 1998, 2004 Jackwind Li Guojie
 * All right reserved.
 *
 * Created on Jan 30, 2004 11:52:21 PM by JACK
 * $Id$
 *
 * visit: http://www.asprise.com/swt
 *****************************************************************************/

package consulo.desktop.swt.ui.impl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

/**
 * Lays out a composite, arranging and resizing its components to fit in five
 * regions: north, south, east, west, and center.
 */
class BorderLayout extends Layout {
  // Region constants.
  public static final int NORTH = 0;
  public static final int SOUTH = 1;
  public static final int CENTER = 2;
  public static final int EAST = 3;
  public static final int WEST = 4;

  /**
   * Indicates the region that a control belongs to.
   */
  public static class BorderData {
    public int region = CENTER; // default.

    public BorderData() {
    }

    public BorderData(int region) {
      this.region = region;
    }

    @Override
    public String toString() {
      return "BorderData: " + region;
    }
  }

  // Controls in all the regions.
  public Control[] controls = new Control[5];

  // Cached sizes.
  Point[] sizes;

  // Preferred width and height
  int width;
  int height;

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.swt.widgets.Layout#computeSize(org.eclipse.swt.widgets.Composite,
   *      int, int, boolean)
   */
  protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {

    if (sizes == null || flushCache == true) refreshSizes(composite.getChildren());
    int w = wHint;
    int h = hHint;
    if (w == SWT.DEFAULT) w = width;
    if (h == SWT.DEFAULT) h = height;

    return new Point(w, h);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.swt.widgets.Layout#layout(org.eclipse.swt.widgets.Composite,
   *      boolean)
   */
  protected void layout(Composite composite, boolean flushCache) {
    if (flushCache || sizes == null) refreshSizes(composite.getChildren());

    Rectangle clientArea = composite.getClientArea();

    // Enough space for all.
    if (controls[NORTH] != null) {
      controls[NORTH].setBounds(clientArea.x, clientArea.y, clientArea.width, sizes[NORTH].y);
    }
    if (controls[SOUTH] != null) {
      controls[SOUTH].setBounds(clientArea.x, clientArea.y + clientArea.height - sizes[SOUTH].y, clientArea.width, sizes[SOUTH].y);
    }
    if (controls[WEST] != null) {
      controls[WEST].setBounds(clientArea.x, clientArea.y + sizes[NORTH].y, sizes[WEST].x, clientArea.height - sizes[NORTH].y - sizes[SOUTH].y);
    }
    if (controls[EAST] != null) {
      controls[EAST].setBounds(clientArea.x + clientArea.width - sizes[EAST].x, clientArea.y + sizes[NORTH].y, sizes[EAST].x, clientArea.height - sizes[NORTH].y - sizes[SOUTH].y);
    }
    if (controls[CENTER] != null) {
      controls[CENTER].setBounds(clientArea.x + sizes[WEST].x, clientArea.y + sizes[NORTH].y, clientArea.width - sizes[WEST].x - sizes[EAST].x, clientArea.height - sizes[NORTH].y - sizes[SOUTH].y);
    }

  }

  private void refreshSizes(Control[] children) {
    for (int i = 0; i < children.length; i++) {
      Object layoutData = children[i].getLayoutData();
      if (layoutData == null || (!(layoutData instanceof BorderData))) continue;
      BorderData borderData = (BorderData)layoutData;
      if (borderData.region < 0 || borderData.region > 4) // Invalid.
        continue;
      controls[borderData.region] = children[i];
    }

    width = 0;
    height = 0;

    if (sizes == null) sizes = new Point[5];

    for (int i = 0; i < controls.length; i++) {
      Control control = controls[i];
      if (control == null) {
        sizes[i] = new Point(0, 0);
      }
      else {
        sizes[i] = control.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
      }
    }

    width = Math.max(width, sizes[NORTH].x);
    width = Math.max(width, sizes[WEST].x + sizes[CENTER].x + sizes[EAST].x);
    width = Math.max(width, sizes[SOUTH].x);

    height = Math.max(Math.max(sizes[WEST].y, sizes[EAST].y), sizes[CENTER].y) + sizes[NORTH].y + sizes[SOUTH].y;

  }
}
