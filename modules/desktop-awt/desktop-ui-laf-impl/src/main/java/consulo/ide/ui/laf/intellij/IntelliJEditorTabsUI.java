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
import com.intellij.ui.paint.LinePainter2D;
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

import javax.swing.*;
import java.awt.*;
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
    tabs.setBackground(UIUtil.getPanelBackground());
    tabs.setForeground(UIUtil.getLabelForeground());
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
      size.height += tabs.getTabBorderSize();
    }
    else {
      size.width += tabs.getTabBorderSize();
    }

    return size;
  }

  @Override
  public void paint(Graphics g, JComponent c) {
    JBTabsImpl tabs = (JBTabsImpl)c;

    if (tabs.getVisibleInfos().isEmpty()) return;

    Graphics2D g2d = (Graphics2D)g;

    final GraphicsConfig config = new GraphicsConfig(g2d);
    config.setAntialiasing(true);

    try {
      final Rectangle clip = g2d.getClipBounds();

      Runnable tabLineRunner = doPaintBackground(tabs, g2d, clip);

      final TabInfo selected = tabs.getSelectedInfo();

      if (selected != null) {
        Rectangle compBounds = selected.getComponent().getBounds();
        if (compBounds.contains(clip) && !compBounds.intersects(clip)) return;
      }

      tabLineRunner.run();

      if (!tabs.isStealthModeEffective() && !tabs.isHideTabs()) {
        paintNonSelectedTabs(tabs, g2d);
      }

      doPaintActive(tabs, (Graphics2D)g);

      final TabLabel selectedLabel = tabs.getSelectedLabel();
      if (selected != null) {
        selectedLabel.paintImage(g);
      }

      tabs.getSingleRowLayoutInternal().myMoreIcon.paintIcon(tabs, g);

      if (tabs.isSideComponentVertical()) {
        JBTabsImpl.Toolbar toolbarComp = tabs.myInfo2Toolbar.get(tabs.getSelectedInfoInternal());
        if (toolbarComp != null && !toolbarComp.isEmpty()) {
          Rectangle toolBounds = toolbarComp.getBounds();
          g2d.setColor(JBColor.border());
          LinePainter2D.paint(g2d, toolBounds.getMaxX(), toolBounds.y, toolBounds.getMaxX(), toolBounds.getMaxY() - 1);
        }
      }
      else if (!tabs.isSideComponentOnTabs()) {
        JBTabsImpl.Toolbar toolbarComp = tabs.myInfo2Toolbar.get(tabs.getSelectedInfoInternal());
        if (toolbarComp != null && !toolbarComp.isEmpty()) {
          Rectangle toolBounds = toolbarComp.getBounds();
          g2d.setColor(JBColor.border());
          LinePainter2D.paint(g2d, toolBounds.x, toolBounds.getMaxY(), toolBounds.getMaxX() - 1, toolBounds.getMaxY());
        }
      }
    }
    finally {
      config.restore();
    }
  }

  protected void paintNonSelectedTabs(JBTabsImpl t, final Graphics2D g2d) {
    TabInfo selected = t.getSelectedInfo();
    for (int eachRow = 0; eachRow < t.getLastLayoutPass().getRowCount(); eachRow++) {
      for (int eachColumn = t.getLastLayoutPass().getColumnCount(eachRow) - 1; eachColumn >= 0; eachColumn--) {
        final TabInfo each = t.getLastLayoutPass().getTabAt(eachRow, eachColumn);
        if (t.getSelectedInfo() == each) {
          continue;
        }
        paintNonSelected(t, g2d, each);
      }
    }

    myLastPaintedSelection = selected;
  }

  @Override
  public void clearLastPaintedTab() {
    myLastPaintedSelection = null;
  }

  protected void paintNonSelected(JBTabsImpl t, final Graphics2D g2d, final TabInfo each) {
    if (t.getDropInfo() == each) return;

    final TabLabel label = t.myInfo2Label.get(each);
    if (label.getBounds().width == 0) return;

    doPaintInactive(t, g2d, label);
  }

  protected Runnable doPaintBackground(JBTabsImpl tabs, Graphics2D g2d, Rectangle clip) {
    List<TabInfo> visibleInfos = tabs.getVisibleInfos();

    final boolean vertical = tabs.getTabsPosition() == JBTabsPosition.left || tabs.getTabsPosition() == JBTabsPosition.right;

    Insets insets = JBUI.emptyInsets();

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

    Rectangle rect = tabs.getBounds();

    Rectangle rectangle;
    if (vertical) {
      rectangle = new Rectangle(insets.left, maxOffset, tabs.getWidth(), rect.height - maxOffset - insets.top - insets.bottom);
    }
    else {
      int y = rect.y + insets.top;
      int height = maxLength - insets.top - insets.bottom;
      if (tabs.getTabsPosition() == JBTabsPosition.bottom) {
        y = rect.height - height - insets.top;
      }

      rectangle = new Rectangle(maxOffset, y, rect.width - maxOffset - insets.left - insets.right, height);
    }

    doPaintBackground(g2d, rectangle);

    doPaintAdditionalBackgroundIfFirstOffsetSet(tabs, g2d, clip);

    final int finalMaxLength = maxLength;
    return () -> {
      if (tabs.getPosition() == JBTabsPosition.top) {
        g2d.setColor(JBColor.border());

        int borderHeight = finalMaxLength - tabs.getTabBorderSize();
        LinePainter2D.paint(g2d, rect.x, borderHeight, rect.x + rect.getWidth(), borderHeight);
      }
    };
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

  public void doPaintBackground(Graphics2D g, Rectangle rectangle) {
    g.setPaint(UIUtil.getPanelBackground());
    g.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
  }

  protected void doPaintActive(JBTabsImpl tabs, Graphics2D g2d) {
    if (tabs.getSelectedInfo() == null || tabs.isHideTabs()) return;

    TabLabel label = tabs.getSelectedLabel();
    Rectangle rect = label.getBounds();

    ShapeInfo shape = computeLabelShape(tabs, label);

    Color tabColor = label.getInfo().getTabColor();
    if (tabs.isPaintFocus() && tabs.isHoveredTab(label)) {
      tabColor = ColorUtil.toAlpha(tabColor == null ? new JBColor(Gray._255, Gray._85) : tabColor, 180);
    } else {
      tabColor = tabColor == null ? new JBColor(Gray._255, Gray._85) : tabColor;
    }

    g2d.setColor(tabColor);

    g2d.fill(shape.fillPath.getShape());

    doPaintLabelBorder(g2d, tabs, label, rect, tabColor, true);
  }

  protected void doPaintInactive(JBTabsImpl tabs, Graphics2D g2d, TabLabel label) {
    ShapeInfo shape = computeLabelShape(tabs, label);

    Color tabColor = label.getInfo().getTabColor();
    if (tabs.isPaintFocus() && tabs.isHoveredTab(label)) {
      tabColor = ColorUtil.toAlpha(tabColor != null ? tabColor : UIUtil.getControlColor(), 180);
    } else {
      tabColor = tabColor != null ? ColorUtil.toAlpha(tabColor, 150) : UIUtil.getControlColor();
    }

    Rectangle rect = label.getBounds();

    g2d.setColor(JBColor.border());

    g2d.fill(shape.fillPath.getShape());

    g2d.setColor(tabColor);

    g2d.fill(shape.fillPath.getShape());

    doPaintLabelBorder(g2d, tabs, label, rect, tabColor, false);
  }

  private void doPaintLabelBorder(Graphics2D g2d, JBTabsImpl tabs, TabLabel label, Rectangle rect, Color tabColor, boolean repaintDownLine) {
    if (!tabs.isTabsBorderEnabled()) {
      return;
    }
    
    g2d.setColor(JBColor.border());

    RectanglePainter2D.DRAW.paint(g2d, rect.x, rect.y, rect.getWidth(), rect.getHeight());

    if (tabs.getPosition() == JBTabsPosition.top) {
      g2d.setColor(tabColor);

      int borderSize = tabs.getTabBorderSize();
      if (repaintDownLine) {
        // override border at bottom
        // but do not eat border size
        int x = rect.x;
        int y = rect.y + rect.height - borderSize;

        LinePainter2D.paint(g2d, x + borderSize, y, x + rect.getWidth() - borderSize * 2, y);
      }

      // eat left border for first tab
      int tabIndex = tabs.getIndexOf(label.getInfo());
      int firstTabOffset = tabs.getFirstTabOffset();
      if (firstTabOffset == 0 && tabIndex == 0) {
        LinePainter2D.paint(g2d, rect.x, rect.y + borderSize, rect.x, rect.y + rect.height);
      }
    }
  }

  protected ShapeInfo computeLabelShape(JBTabsImpl tabs, TabLabel tabLabel) {
    final ShapeInfo shape = new ShapeInfo();

    shape.path = tabs.getEffectiveLayout().createShapeTransform(tabs.getSize());
    shape.insets = shape.path.transformInsets(tabs.getLayoutInsets());
    shape.labelPath = shape.path.createTransform(tabLabel.getBounds());

    shape.labelBottomY = shape.labelPath.getMaxY();
    shape.labelTopY = shape.labelPath.getY();
    shape.labelLeftX = shape.labelPath.getX();
    shape.labelRightX = shape.labelPath.getMaxX();

    int leftX = shape.insets.left;

    shape.path.moveTo(leftX, shape.labelBottomY);
    shape.path.lineTo(shape.labelLeftX, shape.labelBottomY);
    shape.path.lineTo(shape.labelLeftX, shape.labelTopY);
    shape.path.lineTo(shape.labelRightX, shape.labelTopY);
    shape.path.lineTo(shape.labelRightX, shape.labelBottomY);

    shape.path.closePath();
    shape.fillPath = shape.path.copy();

    return shape;
  }
}
