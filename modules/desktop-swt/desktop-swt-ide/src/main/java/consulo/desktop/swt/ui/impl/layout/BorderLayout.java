/*
 -----------------------------------------------------------------------------
  (c) Copyright IBM Corp. 2003  All rights reserved.

 The sample program(s) is/are owned by International Business Machines
 Corporation or one of its subsidiaries ("IBM") and is/are copyrighted and
 licensed, not sold.

 You may copy, modify, and distribute this/these sample program(s) in any form
 without payment to IBM, for any purpose including developing, using, marketing
 or distributing programs that include or are derivative works of the sample
 program(s).

 The sample program(s) is/are provided to you on an "AS IS" basis, without
 warranty of any kind.  IBM HEREBY EXPRESSLY DISCLAIMS ALL WARRANTIES, EITHER
 EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  Some jurisdictions do
 not allow for the exclusion or limitation of implied warranties, so the above
 limitations or exclusions may not apply to you.  IBM shall not be liable for
 any damages you suffer as a result of using, modifying or distributing the
 sample program(s) or its/their derivatives.

 Each copy of any portion of this/these sample program(s) or any derivative
 work, must include the above copyright notice and disclaimer of warranty.

 -----------------------------------------------------------------------------
*/

package consulo.desktop.swt.ui.impl.layout;

import consulo.desktop.swt.ui.impl.layout.data.LayoutDataWithSize;
import consulo.ui.Size;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

/**
 * Port of AWT BorderLayout to SWT.
 *
 * @author Yannick Saillet
 */
class BorderLayout extends Layout {
  public static class BorderData implements LayoutDataWithSize {
    public String position;

    public int width = -1;

    public int height = -1;

    public BorderData(String position) {
      this.position = position;
    }

    @Override
    public void setSize(Size size) {
      width = size.getWidth();
      height = size.getHeight();
    }

    @Override
    public String toString() {
      return "BorderData{" + "position='" + position + '\'' + '}';
    }
  }

  public final static String CENTER = "Center";
  public final static String EAST = "East";
  public final static String NORTH = "North";
  public final static String SOUTH = "South";
  public final static String WEST = "West";

  //-----------------------

  private int hgap, vgap;
  private Control centerChild, eastChild, northChild, southChild, westChild;

  public BorderLayout() {
    super();
  }

  public BorderLayout(int hgap, int vgap) {
    this.hgap = hgap;
    this.vgap = vgap;
  }

  @Override
  protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
    readLayoutData(composite);
    Point size = new Point(0, 0);

    Point preferredSize;
    if (northChild != null) {
      preferredSize = getPreferredSize(northChild, wHint, SWT.DEFAULT, flushCache);
      size.y += preferredSize.y + vgap;
    }

    if (southChild != null) {
      preferredSize = getPreferredSize(southChild, wHint, SWT.DEFAULT, flushCache);
      size.y += preferredSize.y + vgap;
    }

    if (westChild != null) {
      preferredSize = getPreferredSize(westChild, SWT.DEFAULT, hHint, flushCache);
      size.x += preferredSize.x + hgap;

      if (preferredSize.y > size.y) {
        size.y = preferredSize.y;
      }
    }

    if (eastChild != null) {
      preferredSize = getPreferredSize(eastChild, SWT.DEFAULT, hHint, flushCache);
      size.x += preferredSize.x + hgap;

      if (preferredSize.y > size.y) {
        size.y = preferredSize.y;
      }
    }

    if (centerChild != null) {
      preferredSize = getPreferredSize(centerChild, wHint, hHint, flushCache);
      size.x += preferredSize.x;
      size.y += preferredSize.y;
    }
    return size;
  }

  @Override
  protected void layout(Composite composite, boolean flushCache) {
    readLayoutData(composite);
    Rectangle clientArea = composite.getClientArea();
    int top = clientArea.y;
    int bottom = clientArea.y + clientArea.height;
    int left = clientArea.x;
    int right = clientArea.x + clientArea.width;

    Point preferredSize;
    if (northChild != null) {
      preferredSize = getPreferredSize(northChild, clientArea.width, SWT.DEFAULT, flushCache);
      northChild.setBounds(left, top, right - left, preferredSize.y);
      top += preferredSize.y + vgap;
    }

    if (southChild != null) {
      preferredSize = getPreferredSize(southChild, clientArea.width, SWT.DEFAULT, flushCache);
      southChild.setBounds(left, bottom - preferredSize.y, right - left, preferredSize.y);
      bottom -= preferredSize.y + vgap;
    }

    if (westChild != null) {
      preferredSize = getPreferredSize(westChild, SWT.DEFAULT, bottom - top, flushCache);
      westChild.setBounds(left, top, preferredSize.x, bottom - top);
      left += preferredSize.x + hgap;
    }

    if (eastChild != null) {
      preferredSize = getPreferredSize(eastChild, SWT.DEFAULT, bottom - top, flushCache);
      eastChild.setBounds(right - preferredSize.x, top, preferredSize.x, bottom - top);
      right -= preferredSize.x + hgap;
    }

    if (centerChild != null) {
      centerChild.setBounds(left, top, right - left, bottom - top);
    }
  }

  /**
   * Read the layout data of the children of a composite.
   *
   * @param composite the parent composite
   */
  private void readLayoutData(Composite composite) {
    northChild = southChild = eastChild = westChild = centerChild = null;
    Control[] children = composite.getChildren();
    for (int i = 0; i < children.length; i++) {
      //if (!children[i].isVisible())
      //  continue;

      BorderData layoutData = (BorderData)children[i].getLayoutData();
      String position = layoutData.position;

      if (NORTH.equals(position)) {
        northChild = children[i];
      }
      else if (SOUTH.equals(position)) {
        southChild = children[i];
      }
      else if (EAST.equals(position)) {
        eastChild = children[i];
      }
      else if (WEST.equals(position)) {
        westChild = children[i];
      }
      else {
        centerChild = children[i];
      }
    }
  }

  protected Point getPreferredSize(Control control, int wHint, int hHint, boolean changed) {
    if (!control.isVisible()) {
      return new Point(0, 0);
    }
    
    Point point = control.computeSize(wHint, hHint, changed);

    Object layoutData = control.getLayoutData();
    if (layoutData instanceof BorderData data) {
      if (data.width != -1) {
        point.x = data.width;
      }

      if (data.height != -1) {
        point.y = data.height;
      }
    }
    return point;
  }

  /**
   * @return Returns the hgap.
   */
  public int getHgap() {
    return hgap;
  }

  /**
   * @param hgap The hgap to set.
   */
  public void setHgap(int hgap) {
    this.hgap = hgap;
  }

  /**
   * @return Returns the vgap.
   */
  public int getVgap() {
    return vgap;
  }

  /**
   * @param vgap The vgap to set.
   */
  public void setVgap(int vgap) {
    this.vgap = vgap;
  }
}
