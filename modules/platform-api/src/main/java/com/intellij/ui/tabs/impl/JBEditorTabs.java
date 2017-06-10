/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ui.tabs.impl;

import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.laf.JBEditorTabsPainter;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.tabs.JBTabsPosition;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.singleRow.CompressibleSingleRowLayout;
import com.intellij.ui.tabs.impl.singleRow.ScrollableSingleRowLayout;
import com.intellij.ui.tabs.impl.singleRow.SingleRowLayout;
import com.intellij.ui.tabs.impl.table.TableLayout;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author pegov
 */
public class JBEditorTabs extends JBTabsImpl {
  private static class PainterUtil {
    @NotNull
    private static JBEditorTabsPainter createPainter() {
      JBEditorTabsPainter painter = PainterUtil.createPainterFromLaf();

      if(painter == null) {
        painter = PainterUtil.createDefaultPainter();
      }
      return painter;
    }

    private static JBEditorTabsPainter createPainterFromLaf() {
      String className = UIManager.getString("jbeditor.tabs.painter");
      if(className == null) {
        return null;
      }
      return createPainter(className);
    }

    private static JBEditorTabsPainter createDefaultPainter() {
      return createPainter("com.intellij.ide.ui.laf.intellij.DefaultEditorTabsPainter");
    }

    private static JBEditorTabsPainter createPainter(String clazz) {
      try {
        Class<?> c = Class.forName(clazz);

        return (JBEditorTabsPainter)ReflectionUtil.createInstance(c.getConstructors()[0]);
      }
      catch (ClassNotFoundException e) {
        return null;
      }
    }
  }

  public static final String TABS_ALPHABETICAL_KEY = "tabs.alphabetical";
  private JBEditorTabsPainter myTabPainter;

  public JBEditorTabs(@Nullable Project project, ActionManager actionManager, IdeFocusManager focusManager, @NotNull Disposable parent) {
    super(project, actionManager, focusManager, parent, true);
  }

  @Override
  protected SingleRowLayout createSingleRowLayout() {
    if (!UISettings.getInstance().HIDE_TABS_IF_NEED && supportsCompression()) {
      return new CompressibleSingleRowLayout(this);
    }
    else if (ApplicationManager.getApplication().isInternal() || Registry.is("editor.use.scrollable.tabs")) {
      return new ScrollableSingleRowLayout(this);
    }
    return super.createSingleRowLayout();
  }

  @Override
  public void updateUI() {
    myTabPainter = null;

    super.updateUI();
  }

  @Override
  public boolean supportsCompression() {
    return true;
  }

  @Nullable
  public Rectangle getSelectedBounds() {
    TabLabel label = getSelectedLabel();
    return label != null ? label.getBounds() : null;
  }

  @Override
  public boolean isEditorTabs() {
    return true;
  }

  @Override
  protected void paintFirstGhost(Graphics2D g2d) {
  }

  @Override
  protected void paintLastGhost(Graphics2D g2d) {
  }

  @Override
  public boolean isGhostsAlwaysVisible() {
    return false;
  }

  @Override
  public boolean useSmallLabels() {
    return UISettings.getInstance().USE_SMALL_LABELS_ON_TABS;
  }

  @Override
  public boolean useBoldLabels() {
    return SystemInfo.isMac && Registry.is("ide.mac.boldEditorTabs");
  }

  @Override
  public boolean hasUnderline() {
    return true;
  }

  @Override
  protected void doPaintInactive(Graphics2D g2d,
                                 boolean leftGhostExists,
                                 TabLabel label,
                                 Rectangle effectiveBounds,
                                 boolean rightGhostExists, int row, int column) {
    Insets insets = getTabsBorder().getEffectiveBorder();

    int _x = effectiveBounds.x + insets.left;
    int _y = effectiveBounds.y + insets.top;
    int _width = effectiveBounds.width - insets.left - insets.right + (getTabsPosition() == JBTabsPosition.right ? 1 : 0);
    int _height = effectiveBounds.height - insets.top - insets.bottom;


    if ((!isSingleRow() /* for multiline */) || (isSingleRow() && isHorizontalTabs()))  {
      if (isSingleRow() && getPosition() == JBTabsPosition.bottom) {
        _y += getActiveTabUnderlineHeight();
      } else {
        if (isSingleRow()) {
          _height -= getActiveTabUnderlineHeight();
        } else {
          TabInfo info = label.getInfo();
          if (((TableLayout)getEffectiveLayout()).isLastRow(info)) {
            _height -= getActiveTabUnderlineHeight();
          }
        }
      }
    }

    final boolean vertical = getTabsPosition() == JBTabsPosition.left || getTabsPosition() == JBTabsPosition.right;
    final Color tabColor = label.getInfo().getTabColor();
    getPainter().doPaintInactive(g2d, effectiveBounds, _x, _y, _width, _height, tabColor, row, column, vertical);
  }

  @NotNull
  private JBEditorTabsPainter getPainter() {
    if(myTabPainter == null) {
      myTabPainter = createPainter();
    }
    return myTabPainter;
  }

  protected JBEditorTabsPainter createPainter() {
    return PainterUtil.createPainter();
  }

  @Override
  public int getActiveTabUnderlineHeight() {
    return hasUnderline() ? super.getActiveTabUnderlineHeight() : 1;
  }

  @Override
  public boolean isAlphabeticalMode() {
    return Registry.is(TABS_ALPHABETICAL_KEY);
  }

  public static void setAlphabeticalMode(boolean on) {
    Registry.get(TABS_ALPHABETICAL_KEY).setValue(on);
  }

  @Override
  protected void doPaintBackground(Graphics2D g2d, Rectangle clip) {
    List<TabInfo> visibleInfos = getVisibleInfos();

    final boolean vertical = getTabsPosition() == JBTabsPosition.left || getTabsPosition() == JBTabsPosition.right;

    Insets insets = getTabsBorder().getEffectiveBorder();

    int maxOffset = 0;
    int maxLength = 0;

    for (int i = visibleInfos.size() - 1; i >= 0; i--) {
      TabInfo visibleInfo = visibleInfos.get(i);
      TabLabel tabLabel = myInfo2Label.get(visibleInfo);
      Rectangle r = tabLabel.getBounds();
      if (r.width == 0 || r.height == 0) continue;
      maxOffset = vertical ? r.y + r.height : r.x + r.width;
      maxLength = vertical ? r.width : r.height;
      break;
    }

    maxOffset++;

    Rectangle r2 = getBounds();

    Rectangle rectangle;
    if (vertical) {
      rectangle = new Rectangle(insets.left, maxOffset, getWidth(),
                                r2.height - maxOffset - insets.top - insets.bottom);
    }
    else {
      int y = r2.y + insets.top;
      int height = maxLength - insets.top - insets.bottom;
      if (getTabsPosition() == JBTabsPosition.bottom) {
        y = r2.height - height - insets.top + getActiveTabUnderlineHeight();
      }
      else {
        height -= getActiveTabUnderlineHeight();
      }

      rectangle = new Rectangle(maxOffset, y, r2.width - maxOffset - insets.left - insets.right, height);
    }

    getPainter().doPaintBackground(g2d, clip, vertical, rectangle);
  }

  @Override
  protected void paintSelectionAndBorder(Graphics2D g2d) {
    if (getSelectedInfo() == null || isHideTabs()) return;

    TabLabel label = getSelectedLabel();
    Rectangle r = label.getBounds();

    ShapeInfo selectedShape = _computeSelectedLabelShape();

    Insets insets = getTabsBorder().getEffectiveBorder();

    Color tabColor = label.getInfo().getTabColor();
    final boolean isHorizontalTabs = isHorizontalTabs();

    getPainter().paintSelectionAndBorder(g2d, r, selectedShape, insets, tabColor, isHorizontalTabs);
  }

  @Override
  public Color getBackground() {
    return getPainter().getBackgroundColor();
  }

  @Override
  public Color getForeground() {
    return UIUtil.getLabelForeground();
  }

  protected ShapeInfo _computeSelectedLabelShape() {
    final ShapeInfo shape = new ShapeInfo();

    shape.path = getEffectiveLayout().createShapeTransform(getSize());
    shape.insets = shape.path.transformInsets(getLayoutInsets());
    shape.labelPath = shape.path.createTransform(getSelectedLabel().getBounds());

    shape.labelBottomY = shape.labelPath.getMaxY() - shape.labelPath.deltaY(getActiveTabUnderlineHeight() - 1);
    shape.labelTopY =
      shape.labelPath.getY() + (getPosition() == JBTabsPosition.top || getPosition() == JBTabsPosition.bottom ? shape.labelPath.deltaY(1) : 0) ;
    shape.labelLeftX = shape.labelPath.getX() + (getPosition() == JBTabsPosition.top || getPosition() == JBTabsPosition.bottom ? 0 : shape.labelPath.deltaX(
      1));
    shape.labelRightX = shape.labelPath.getMaxX() - shape.labelPath.deltaX(1);

    int leftX = shape.insets.left + (getPosition() == JBTabsPosition.top || getPosition() == JBTabsPosition.bottom ? 0 : shape.labelPath.deltaX(1));

    shape.path.moveTo(leftX, shape.labelBottomY);
    shape.path.lineTo(shape.labelLeftX, shape.labelBottomY);
    shape.path.lineTo(shape.labelLeftX, shape.labelTopY);
    shape.path.lineTo(shape.labelRightX, shape.labelTopY);
    shape.path.lineTo(shape.labelRightX, shape.labelBottomY);

    int lastX = shape.path.getWidth() - shape.path.deltaX(shape.insets.right);

    shape.path.lineTo(lastX, shape.labelBottomY);
    shape.path.lineTo(lastX, shape.labelBottomY + shape.labelPath.deltaY(getActiveTabUnderlineHeight() - 1));
    shape.path.lineTo(leftX, shape.labelBottomY + shape.labelPath.deltaY(getActiveTabUnderlineHeight() - 1));

    shape.path.closePath();
    shape.fillPath = shape.path.copy();

    return shape;
  }
}
