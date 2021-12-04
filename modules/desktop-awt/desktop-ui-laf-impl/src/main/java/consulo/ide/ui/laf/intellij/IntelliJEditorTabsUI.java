/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ide.ui.laf.intellij;

import com.intellij.openapi.ui.GraphicsConfig;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.paint.RectanglePainter2D;
import com.intellij.ui.tabs.JBTabsPosition;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.ui.tabs.impl.ShapeTransform;
import com.intellij.ui.tabs.impl.TabLabel;
import com.intellij.util.Function;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.ide.ui.laf.JBEditorTabsUI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;

/**
 * @author VISTALL
 * @since 15-Jun-17
 */
public class IntelliJEditorTabsUI extends JBEditorTabsUI {
  public static JBEditorTabsUI createUI(JComponent c) {
    return new IntelliJEditorTabsUI();
  }

  public static class ShapeInfo {
    public ShapeInfo() {
    }

    public ShapeTransform path;
    public ShapeTransform fillPath;
    public ShapeTransform labelPath;
    public int labelBottomY;
    public int labelTopY;
    public int labelLeftX;
    public int labelRightX;
    public Insets insets;
    public Color from;
    public Color to;
  }

  private TabInfo myLastPaintedSelection;

  @Override
  public void installUI(JComponent c) {
    super.installUI(c);

    JBTabsImpl tabs = (JBTabsImpl)c;
    tabs.setBackground(getBackground());
    tabs.setForeground(getForeground());
  }

  @Override
  public void uninstallUI(JComponent c) {
    super.uninstallUI(c);
    myLastPaintedSelection = null;
  }

  @Override
  public Dimension getMinimumSize(JComponent c) {
    return computeSize((JBTabsImpl)c, JComponent::getMinimumSize, 1);
  }

  @Override
  public Dimension getPreferredSize(JComponent c) {
    return computeSize((JBTabsImpl)c, JComponent::getPreferredSize, 3);
  }

  protected Dimension computeSize(JBTabsImpl tabs, Function<JComponent, Dimension> transform, int tabCount) {
    Dimension size = new Dimension();
    for (TabInfo each : tabs.getVisibleInfos()) {
      final JComponent c = each.getComponent();
      if (c != null) {
        final Dimension eachSize = transform.fun(c);
        size.width = Math.max(eachSize.width, size.width);
        size.height = Math.max(eachSize.height, size.height);
      }
    }

    addHeaderSize(tabs, size, tabCount);
    return size;
  }

  protected void addHeaderSize(JBTabsImpl tabs, Dimension size, final int tabsCount) {
    Dimension header = computeHeaderPreferredSize(tabs, tabsCount);

    final boolean horizontal = tabs.getTabsPosition() == JBTabsPosition.top || tabs.getTabsPosition() == JBTabsPosition.bottom;
    if (horizontal) {
      size.height += header.height;
      size.width = Math.max(size.width, header.width);
    }
    else {
      size.height += Math.max(size.height, header.height);
      size.width += header.width;
    }

    final Insets insets = tabs.getLayoutInsets();
    size.width += insets.left + insets.right + 1;
    size.height += insets.top + insets.bottom + 1;
  }

  protected Dimension computeHeaderPreferredSize(JBTabsImpl tabs, int tabsCount) {
    final Iterator<TabInfo> infos = tabs.myInfo2Label.keySet().iterator();
    Dimension size = new Dimension();
    int currentTab = 0;

    final boolean horizontal = tabs.getTabsPosition() == JBTabsPosition.top || tabs.getTabsPosition() == JBTabsPosition.bottom;

    while (infos.hasNext()) {
      final boolean canGrow = currentTab < tabsCount;

      TabInfo eachInfo = infos.next();
      final TabLabel eachLabel = tabs.myInfo2Label.get(eachInfo);
      final Dimension eachPrefSize = eachLabel.getPreferredSize();
      if (horizontal) {
        if (canGrow) {
          size.width += eachPrefSize.width;
        }
        size.height = Math.max(size.height, eachPrefSize.height);
      }
      else {
        size.width = Math.max(size.width, eachPrefSize.width);
        if (canGrow) {
          size.height += eachPrefSize.height;
        }
      }

      currentTab++;
    }


    if (horizontal) {
      size.height += tabs.getTabsBorder().getTabBorderSize();
    }
    else {
      size.width += tabs.getTabsBorder().getTabBorderSize();
    }

    return size;
  }


  protected Color getBackground() {
    return new JBColor(Gray._142, UIUtil.getPanelBackground());
  }

  protected Color getForeground() {
    return UIUtil.getLabelForeground();
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    JBTabsImpl tabs = (JBTabsImpl)c;

    if (tabs.getVisibleInfos().isEmpty()) return;

    Graphics2D g2d = (Graphics2D)g;

    final GraphicsConfig config = new GraphicsConfig(g2d);
    config.setAntialiasing(true);

    final Rectangle clip = g2d.getClipBounds();

    doPaintBackground(tabs, g2d, clip);

    final TabInfo selected = tabs.getSelectedInfo();

    if (selected != null) {
      Rectangle compBounds = selected.getComponent().getBounds();
      if (compBounds.contains(clip) && !compBounds.intersects(clip)) return;
    }

    boolean leftGhostExists = tabs.isSingleRow();
    boolean rightGhostExists = tabs.isSingleRow();

    if (!tabs.isStealthModeEffective() && !tabs.isHideTabs()) {
      paintNonSelectedTabs(tabs, g2d, leftGhostExists, rightGhostExists);
    }

    config.setAntialiasing(false);

    if (tabs.isSideComponentVertical()) {
      JBTabsImpl.Toolbar toolbarComp = tabs.myInfo2Toolbar.get(tabs.getSelectedInfoInternal());
      if (toolbarComp != null && !toolbarComp.isEmpty()) {
        Rectangle toolBounds = toolbarComp.getBounds();
        g2d.setColor(UIUtil.getBorderColor());
        g2d.drawLine((int)toolBounds.getMaxX(), toolBounds.y, (int)toolBounds.getMaxX(), (int)toolBounds.getMaxY() - 1);
      }
    }
    else if (!tabs.isSideComponentOnTabs()) {
      JBTabsImpl.Toolbar toolbarComp = tabs.myInfo2Toolbar.get(tabs.getSelectedInfoInternal());
      if (toolbarComp != null && !toolbarComp.isEmpty()) {
        Rectangle toolBounds = toolbarComp.getBounds();
        g2d.setColor(UIUtil.getBorderColor());
        g2d.drawLine(toolBounds.x, (int)toolBounds.getMaxY(), (int)toolBounds.getMaxX() - 1, (int)toolBounds.getMaxY());
      }
    }

    config.restore();
  }

  protected void paintNonSelectedTabs(JBTabsImpl t, final Graphics2D g2d, final boolean leftGhostExists, final boolean rightGhostExists) {
    TabInfo selected = t.getSelectedInfo();
    if (myLastPaintedSelection == null || !myLastPaintedSelection.equals(selected)) {
      List<TabInfo> tabs = t.getTabs();
      for (TabInfo each : tabs) {
        t.myInfo2Label.get(each).setInactiveStateImage(null);
      }
    }

    for (int eachRow = 0; eachRow < t.getLastLayoutPass().getRowCount(); eachRow++) {
      for (int eachColumn = t.getLastLayoutPass().getColumnCount(eachRow) - 1; eachColumn >= 0; eachColumn--) {
        final TabInfo each = t.getLastLayoutPass().getTabAt(eachRow, eachColumn);
        if (t.getSelectedInfo() == each) {
          continue;
        }
        paintNonSelected(t, g2d, each, leftGhostExists, rightGhostExists, eachRow, eachColumn);
      }
    }

    myLastPaintedSelection = selected;
  }

  @Override
  public void clearLastPaintedTab() {
    myLastPaintedSelection = null;
  }

  protected void paintNonSelected(JBTabsImpl t,
                                final Graphics2D g2d,
                                final TabInfo each,
                                final boolean leftGhostExists,
                                final boolean rightGhostExists,
                                int row,
                                int column) {
    if (t.getDropInfo() == each) return;

    final TabLabel label = t.myInfo2Label.get(each);
    if (label.getBounds().width == 0) return;

    int imageInsets = t.getArcSize() + 1;

    Rectangle bounds = label.getBounds();

    int x = bounds.x - imageInsets;
    int y = bounds.y;
    int width = bounds.width + imageInsets * 2 + 1;
    int height = bounds.height + t.getArcSize() + 1;

    if (t.isToBufferPainting()) {
      BufferedImage img = label.getInactiveStateImage(bounds);

      if (img == null) {
        img = UIUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D imgG2d = img.createGraphics();
        imgG2d.addRenderingHints(g2d.getRenderingHints());
        doPaintInactive(t, imgG2d, leftGhostExists, label, new Rectangle(imageInsets, 0, label.getWidth(), label.getHeight()), rightGhostExists, row, column);
        imgG2d.dispose();
      }

      g2d.drawImage(img, x, y, width, height, null);

      label.setInactiveStateImage(img);
    }
    else {
      doPaintInactive(t, g2d, leftGhostExists, label, label.getBounds(), rightGhostExists, row, column);
      label.setInactiveStateImage(null);
    }
  }

  protected void doPaintInactive(JBTabsImpl t,
                                 Graphics2D g2d,
                                 boolean leftGhostExists,
                                 TabLabel label,
                                 Rectangle effectiveBounds,
                                 boolean rightGhostExists,
                                 int row,
                                 int column) {
    Insets insets = t.getTabsBorder().getEffectiveBorder();

    int _x = effectiveBounds.x + insets.left;
    int _y = effectiveBounds.y + insets.top;
    int _width = effectiveBounds.width - insets.left - insets.right + (t.getTabsPosition() == JBTabsPosition.right ? 1 : 0);
    int _height = effectiveBounds.height - insets.top - insets.bottom;

    final boolean vertical = t.getTabsPosition() == JBTabsPosition.left || t.getTabsPosition() == JBTabsPosition.right;
    final Color tabColor = label.getInfo().getTabColor();
    doPaintInactive(g2d, effectiveBounds, _x, _y, _width, _height, tabColor, row, column, vertical);
  }

  public void doPaintInactive(Graphics2D g2d, Rectangle effectiveBounds, int x, int y, int w, int h, Color tabColor, int row, int column, boolean vertical) {
    doPaintInactiveImpl(g2d, effectiveBounds, x, y, w, h, tabColor, row, column, vertical);
  }

  public void doPaintInactiveImpl(Graphics2D g2d, Rectangle effectiveBounds, int x, int y, int w, int h, Color tabColor, int row, int column, boolean vertical) {
    if (tabColor != null) {
      g2d.setColor(ColorUtil.toAlpha(tabColor, 150));
      g2d.fillRect(x, y, w, h);
    }
    else {
      g2d.setPaint(UIUtil.getControlColor());
      g2d.fillRect(x, y, w, h);
    }
  }

  protected void doPaintBackground(JBTabsImpl tabs, Graphics2D g2d, Rectangle clip) {
    List<TabInfo> visibleInfos = tabs.getVisibleInfos();

    final boolean vertical = tabs.getTabsPosition() == JBTabsPosition.left || tabs.getTabsPosition() == JBTabsPosition.right;

    Insets insets = tabs.getTabsBorder().getEffectiveBorder();

    int maxOffset = 0;
    int maxLength = 0;

    for (int i = visibleInfos.size() - 1; i >= 0; i--) {
      TabInfo visibleInfo = visibleInfos.get(i);
      TabLabel tabLabel = tabs.myInfo2Label.get(visibleInfo);
      Rectangle r = tabLabel.getBounds();
      if (r.width == 0 || r.height == 0) continue;
      maxOffset = vertical ? r.y + r.height : r.x + r.width;
      maxLength = vertical ? r.width : r.height;
      break;
    }

    maxOffset++;

    Rectangle r2 = tabs.getBounds();

    Rectangle rectangle;
    if (vertical) {
      rectangle = new Rectangle(insets.left, maxOffset, tabs.getWidth(), r2.height - maxOffset - insets.top - insets.bottom);
    }
    else {
      int y = r2.y + insets.top;
      int height = maxLength - insets.top - insets.bottom;
      if (tabs.getTabsPosition() == JBTabsPosition.bottom) {
        y = r2.height - height - insets.top;
      }
      else {
      }

      rectangle = new Rectangle(maxOffset, y, r2.width - maxOffset - insets.left - insets.right, height);
    }

    doPaintBackground(g2d, clip, vertical, rectangle);

    doPaintAdditionalBackgroundIfFirstOffsetSet(tabs, g2d, clip);
  }

  protected void doPaintAdditionalBackgroundIfFirstOffsetSet(JBTabsImpl tabs, Graphics2D g2d, Rectangle clip) {
    int firstTabOffset = tabs.getFirstTabOffset();
    if (tabs.getTabsPosition() == JBTabsPosition.top && tabs.isSingleRow() && firstTabOffset > 0) {
      int maxLength = 0;

      for (int i = tabs.getVisibleInfos().size() - 1; i >= 0; i--) {
        TabInfo visibleInfo = tabs.getVisibleInfos().get(i);
        TabLabel tabLabel = tabs.myInfo2Label.get(visibleInfo);
        Rectangle r = tabLabel.getBounds();
        if (r.width == 0 || r.height == 0) continue;
        maxLength = r.height;
        break;
      }

      g2d.setPaint(UIUtil.getPanelBackground());

      g2d.fillRect(clip.x, clip.y, clip.x + JBUI.scale(firstTabOffset - 1), clip.y + maxLength);
    }
  }

  public void doPaintBackground(Graphics2D g, Rectangle clip, boolean vertical, Rectangle rectangle) {
    g.setColor(JBColor.border());
    g.fill(clip);

    final int x = rectangle.x;
    final int y = rectangle.y;
    g.setPaint(UIUtil.getPanelBackground());
    g.fillRect(x, rectangle.y, rectangle.width, rectangle.height + (vertical ? 1 : 0));
  }

  @Override
  public void paintChildren(JBTabsImpl tabs, Graphics g) {
    final GraphicsConfig config = new GraphicsConfig(g);
    config.setAntialiasing(true);
    paintSelectionAndBorder(tabs, (Graphics2D)g);
    config.restore();

    final TabLabel selected = tabs.getSelectedLabel();
    if (selected != null) {
      selected.paintImage(g);
    }

    tabs.getSingleRowLayoutInternal().myMoreIcon.paintIcon(tabs, g);
  }

  protected void paintSelectionAndBorder(JBTabsImpl tabs, Graphics2D g2d) {
    if (tabs.getSelectedInfo() == null || tabs.isHideTabs()) return;

    TabLabel label = tabs.getSelectedLabel();
    Rectangle r = label.getBounds();

    ShapeInfo selectedShape = _computeSelectedLabelShape(tabs);

    Insets insets = tabs.getTabsBorder().getEffectiveBorder();

    Color tabColor = label.getInfo().getTabColor();
    final boolean isHorizontalTabs = tabs.isHorizontalTabs();

    paintSelectionAndBorder(g2d, r, selectedShape, insets, tabColor, isHorizontalTabs);
  }

  public void paintSelectionAndBorder(Graphics2D g2d, Rectangle rect, ShapeInfo selectedShape, Insets insets, Color tabColor, boolean horizontalTabs) {
    paintSelectionAndBorderImpl(g2d, rect, selectedShape, insets, tabColor, horizontalTabs);
  }

  public void paintSelectionAndBorderImpl(Graphics2D g2d, Rectangle rect, IntelliJEditorTabsUI.ShapeInfo selectedShape, Insets insets, Color tabColor, boolean horizontalTabs) {
    g2d.setColor(prepareColorForTab(tabColor));
    g2d.fill(selectedShape.fillPath.getShape());

    if (horizontalTabs) {
      g2d.setColor(prepareColorForTab(tabColor));

      RectanglePainter2D.FILL.paint(g2d, rect.x, rect.y + rect.height, rect.getWidth(), JBUI.scale(1));
    }
  }

  @Nonnull
  protected Color prepareColorForTab(@Nullable Color c) {
    if (c == null) {
      return new JBColor(Gray._255, Gray._85);
    }
    return c;
  }

  protected ShapeInfo _computeSelectedLabelShape(JBTabsImpl tabs) {
    final ShapeInfo shape = new ShapeInfo();

    shape.path = tabs.getEffectiveLayout().createShapeTransform(tabs.getSize());
    shape.insets = shape.path.transformInsets(tabs.getLayoutInsets());
    shape.labelPath = shape.path.createTransform(tabs.getSelectedLabel().getBounds());

    shape.labelBottomY = shape.labelPath.getMaxY();
    shape.labelTopY = shape.labelPath.getY() + (tabs.getPosition() == JBTabsPosition.top || tabs.getPosition() == JBTabsPosition.bottom ? shape.labelPath.deltaY(1) : 0);
    shape.labelLeftX = shape.labelPath.getX() + (tabs.getPosition() == JBTabsPosition.top || tabs.getPosition() == JBTabsPosition.bottom ? 0 : shape.labelPath.deltaX(1));
    shape.labelRightX = shape.labelPath.getMaxX();

    int leftX = shape.insets.left + (tabs.getPosition() == JBTabsPosition.top || tabs.getPosition() == JBTabsPosition.bottom ? 0 : shape.labelPath.deltaX(1));

    shape.path.moveTo(leftX, shape.labelBottomY);
    shape.path.lineTo(shape.labelLeftX, shape.labelBottomY);
    shape.path.lineTo(shape.labelLeftX, shape.labelTopY);
    shape.path.lineTo(shape.labelRightX, shape.labelTopY);
    shape.path.lineTo(shape.labelRightX, shape.labelBottomY);

    int lastX = shape.path.getWidth() - shape.path.deltaX(shape.insets.right);

    shape.path.lineTo(lastX, shape.labelBottomY);
    shape.path.lineTo(lastX, shape.labelBottomY);
    shape.path.lineTo(leftX, shape.labelBottomY);

    shape.path.closePath();
    shape.fillPath = shape.path.copy();

    return shape;
  }
}
