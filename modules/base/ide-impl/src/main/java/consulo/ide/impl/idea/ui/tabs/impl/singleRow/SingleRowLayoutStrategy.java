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
package consulo.ide.impl.idea.ui.tabs.impl.singleRow;

import consulo.application.AllIcons;
import consulo.ide.impl.idea.ui.tabs.impl.JBTabsImpl;
import consulo.ide.impl.idea.ui.tabs.impl.ShapeTransform;
import consulo.ide.impl.idea.ui.tabs.impl.TabLabel;
import consulo.ide.impl.idea.ui.tabs.impl.TabLayout;

import java.awt.*;

public abstract class SingleRowLayoutStrategy {

  private static final int MIN_TAB_WIDTH = 50;
  final SingleRowLayout myLayout;
  final JBTabsImpl myTabs;

  protected SingleRowLayoutStrategy(SingleRowLayout layout) {
    myLayout = layout;
    myTabs = myLayout.myTabs;
  }

  abstract int getMoreRectAxisSize();

  public abstract int getStartPosition(SingleRowPassInfo data);

  public abstract int getToFitLength(SingleRowPassInfo data);

  public abstract int getLengthIncrement(Dimension dimension);

  public abstract int getMinPosition(Rectangle bounds);

  public abstract int getMaxPosition(Rectangle bounds);

  protected abstract int getFixedFitLength(SingleRowPassInfo data);

  public Rectangle getLayoutRect(SingleRowPassInfo data, int position, int length) {
    return getLayoutRec(position, getFixedPosition(data), length, getFixedFitLength(data));
  }

  protected abstract Rectangle getLayoutRec(int position, int fixedPos, int length, int fixedFitLength);

  protected abstract int getFixedPosition(SingleRowPassInfo data);

  public abstract Rectangle getMoreRect(SingleRowPassInfo data);

  public abstract boolean isToCenterTextWhenStretched();

  public abstract ShapeTransform createShapeTransform(Rectangle rectangle);

  public abstract boolean canBeStretched();

  public abstract void layoutComp(SingleRowPassInfo data);

  public boolean isSideComponentOnTabs() {
    return false;
  }

  public abstract boolean isDragOut(TabLabel tabLabel, int deltaX, int deltaY);

  /**
   * Whether a tab that didn't fit completely on the right/bottom side in scrollable layout should be clipped or hidden altogether.
   *
   * @return true if the tab should be clipped, false if hidden.
   */
  public abstract boolean drawPartialOverflowTabs();

  /**
   * Return the change of scroll offset for every unit of mouse wheel scrolling.
   *
   * @param label the first visible tab label
   * @return the scroll amount
   */
  public abstract int getScrollUnitIncrement(TabLabel label);

  abstract static class Horizontal extends SingleRowLayoutStrategy {
    protected Horizontal(SingleRowLayout layout) {
      super(layout);
    }

    @Override
    public boolean isToCenterTextWhenStretched() {
      return true;
    }

    @Override
    public boolean canBeStretched() {
      return true;
    }

    @Override
    public boolean isDragOut(TabLabel tabLabel, int deltaX, int deltaY) {
      return Math.abs(deltaY) > tabLabel.getHeight() * TabLayout.getDragOutMultiplier();
    }

    @Override
    public int getMoreRectAxisSize() {
      return AllIcons.Actions.FindAndShowNextMatchesSmall.getWidth() + 4;
    }

    @Override
    public int getToFitLength(SingleRowPassInfo data) {
      if (data.hToolbar != null) {
        return myTabs.getWidth() - data.insets.left - data.insets.right - data.hToolbar.getMinimumSize().width;  
      } else {
        return myTabs.getWidth() - data.insets.left - data.insets.right;
      }
    }

    @Override
    public int getLengthIncrement(Dimension labelPrefSize) {
      return labelPrefSize.width < MIN_TAB_WIDTH ? MIN_TAB_WIDTH : labelPrefSize.width;
    }

    @Override
    public int getMinPosition(Rectangle bounds) {
      return (int)bounds.getX();
    }

    @Override
    public int getMaxPosition(Rectangle bounds) {
      return (int)bounds.getMaxX();
    }

    @Override
    public int getFixedFitLength(SingleRowPassInfo data) {
      return myTabs.myHeaderFitSize.height;
    }

    @Override
    public Rectangle getLayoutRec(int position, int fixedPos, int length, int fixedFitLength) {
      return new Rectangle(position, fixedPos, length, fixedFitLength);
    }

    @Override
    public int getStartPosition(SingleRowPassInfo data) {
      return data.insets.left;
    }

    @Override
    public boolean drawPartialOverflowTabs() {
      return true;
    }

    @Override
    public int getScrollUnitIncrement(TabLabel label) {
      return 10;
    }
  }

  static class Top extends Horizontal {

    Top(SingleRowLayout layout) {
      super(layout);
    }

    @Override
    public boolean isSideComponentOnTabs() {
      return !myTabs.isSideComponentVertical() && myTabs.isSideComponentOnTabs();
    }

    @Override
    public ShapeTransform createShapeTransform(Rectangle labelRec) {
      return new ShapeTransform.Top(labelRec);
    }

    @Override
    public int getFixedPosition(SingleRowPassInfo data) {
      return data.insets.top;
    }

    @Override
    public Rectangle getMoreRect(SingleRowPassInfo data) {
      int x = data.layoutSize.width - data.moreRectAxisSize - 1;
      return new Rectangle(x, data.insets.top + JBTabsImpl.getSelectionTabVShift(),
                                            data.moreRectAxisSize - 1, myTabs.myHeaderFitSize.height - 1);
    }


    @Override
    public void layoutComp(SingleRowPassInfo data) {
      if (myTabs.isHideTabs()) {
        myTabs.layoutComp(data, 0, 0);
      } else {
        int vToolbarWidth = data.vToolbar != null ? data.vToolbar.getPreferredSize().width : 0;
        int x = vToolbarWidth > 0 ? vToolbarWidth + 1 : 0;
        int y = myTabs.myHeaderFitSize.height;

        if (data.hToolbar != null) {
          Rectangle compBounds = myTabs.layoutComp(x, y, data.comp);
          if (myTabs.isSideComponentOnTabs()) {
            int toolbarX = data.moreRect != null ? (int)data.moreRect.getMaxX() + myTabs.getTabBorderSize() : (data.position + myTabs.getTabBorderSize());
            Rectangle rec =
              new Rectangle(toolbarX, data.insets.top, myTabs.getSize().width - data.insets.left - toolbarX, myTabs.myHeaderFitSize.height - myTabs.getTabBorderSize());
            myTabs.layout(data.hToolbar, rec);
          } else {
            int toolbarHeight = data.hToolbar.getPreferredSize().height - 2;
            myTabs.layout(data.hToolbar, compBounds.x, compBounds.y - toolbarHeight - 1, compBounds.width, toolbarHeight);
          }
        } else if (data.vToolbar != null) {
          Rectangle compBounds = myTabs.layoutComp(x, y, data.comp);
          int toolbarWidth = data.vToolbar.getPreferredSize().width;
          myTabs.layout(data.vToolbar, compBounds.x - toolbarWidth - 1, compBounds.y, toolbarWidth, compBounds.height);
        } else {
          myTabs.layoutComp(x, y, data.comp);
        }
      }
    }
  }

  static class Bottom extends Horizontal {
    Bottom(SingleRowLayout layout) {
      super(layout);
    }

    @Override
    public void layoutComp(SingleRowPassInfo data) {
      if (myTabs.isHideTabs()) {
        myTabs.layoutComp(data, 0, 0);
      } else {
        myTabs.layoutComp(data, 0, 0);
      }
    }

    @Override
    public int getFixedPosition(SingleRowPassInfo data) {
      return myTabs.getSize().height - data.insets.bottom - myTabs.myHeaderFitSize.height - 1;
    }

    @Override
    public Rectangle getMoreRect(SingleRowPassInfo data) {
      return new Rectangle(myTabs.getWidth() - data.insets.right - data.moreRectAxisSize + 2, getFixedPosition(data),
                                            data.moreRectAxisSize - 1, myTabs.myHeaderFitSize.height - 1);
    }

    @Override
    public ShapeTransform createShapeTransform(Rectangle labelRec) {
      return new ShapeTransform.Bottom(labelRec);
    }
  }

  abstract static class Vertical extends SingleRowLayoutStrategy {
    protected Vertical(SingleRowLayout layout) {
      super(layout);
    }

    @Override
    public boolean isDragOut(TabLabel tabLabel, int deltaX, int deltaY) {
      return Math.abs(deltaX) > tabLabel.getHeight() * TabLayout.getDragOutMultiplier();
    }

    @Override
    public boolean isToCenterTextWhenStretched() {
      return false;
    }

    @Override
    int getMoreRectAxisSize() {
      return AllIcons.General.MoreTabs.getHeight() + 4;
    }

    @Override
    public boolean canBeStretched() {
      return false;
    }

    @Override
    public int getStartPosition(SingleRowPassInfo data) {
      return data.insets.top;
    }

    @Override
    public int getToFitLength(SingleRowPassInfo data) {
      return myTabs.getHeight() - data.insets.top - data.insets.bottom;
    }

    @Override
    public int getLengthIncrement(Dimension labelPrefSize) {
      return labelPrefSize.height;
    }

    @Override
    public int getMinPosition(Rectangle bounds) {
      return (int) bounds.getMinY();
    }

    @Override
    public int getMaxPosition(Rectangle bounds) {
      int maxY = (int)bounds.getMaxY();
      return myTabs.isEditorTabs() ? maxY - 1 : maxY;
    }

    @Override
    public int getFixedFitLength(SingleRowPassInfo data) {
      return myTabs.myHeaderFitSize.width;
    }

    @Override
    public boolean drawPartialOverflowTabs() {
      return false;
    }

    @Override
    public int getScrollUnitIncrement(TabLabel label) {
      return label.getPreferredSize().height;
    }
  }

  static class Left extends Vertical {
    Left(SingleRowLayout layout) {
      super(layout);
    }


    @Override
    public void layoutComp(SingleRowPassInfo data) {
      if (myTabs.isHideTabs()) {
        myTabs.layoutComp(data, 0, 0);
      } else {
        myTabs.layoutComp(data, myTabs.myHeaderFitSize.width + 1, 0);
      }
    }

    @Override
    public ShapeTransform createShapeTransform(Rectangle labelRec) {
      return new ShapeTransform.Left(labelRec);
    }

    @Override
    public Rectangle getLayoutRec(int position, int fixedPos, int length, int fixedFitLength) {
      return new Rectangle(fixedPos, position, fixedFitLength, length);
    }

    @Override
    public int getFixedPosition(SingleRowPassInfo data) {
      return data.insets.left;
    }

    @Override
    public Rectangle getMoreRect(SingleRowPassInfo data) {
      return new Rectangle(data.insets.left + JBTabsImpl.getSelectionTabVShift(),
                           myTabs.getHeight() - data.insets.bottom - data.moreRectAxisSize - 1,
                           myTabs.myHeaderFitSize.width - 1,
                           data.moreRectAxisSize - 1);
    }

  }

  static class Right extends Vertical {
    Right(SingleRowLayout layout) {
      super(layout);
    }

    @Override
    public void layoutComp(SingleRowPassInfo data) {
      if (myTabs.isHideTabs()) {
        myTabs.layoutComp(data, 0, 0);
      } else {
        myTabs.layoutComp(data, 0, 0);
      }
    }

    @Override
    public ShapeTransform createShapeTransform(Rectangle labelRec) {
      return new ShapeTransform.Right(labelRec);
    }

    @Override
    public Rectangle getLayoutRec(int position, int fixedPos, int length, int fixedFitLength) {
      return new Rectangle(fixedPos, position, fixedFitLength - 1, length);
    }

    @Override
    public int getFixedPosition(SingleRowPassInfo data) {
      return data.layoutSize.width - myTabs.myHeaderFitSize.width - data.insets.right;
    }

    @Override
    public Rectangle getMoreRect(SingleRowPassInfo data) {
      return new Rectangle(data.layoutSize.width - myTabs.myHeaderFitSize.width,
                        myTabs.getHeight() - data.insets.bottom - data.moreRectAxisSize - 1,
                        myTabs.myHeaderFitSize.width - 1,
                        data.moreRectAxisSize - 1);
    }
  }

}
