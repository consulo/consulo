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
package com.intellij.ide.ui.laf;

import com.intellij.ui.tabs.impl.JBTabsImpl;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public abstract class JBEditorTabsPainter {
  private Color myModifyTabColor;

  public void doPaintInactive(Graphics2D g2d, Rectangle effectiveBounds, int x, int y, int w, int h, Color tabColor, int row, int column, boolean vertical) {
    doPaintInactiveImpl(g2d, effectiveBounds, x, y, w, h, modifyTabColor(tabColor), row, column, vertical);
  }

  protected abstract void doPaintInactiveImpl(Graphics2D g2d,
                                              Rectangle effectiveBounds,
                                              int x,
                                              int y,
                                              int w,
                                              int h,
                                              Color tabColor,
                                              int row,
                                              int column,
                                              boolean vertical);

  public abstract void doPaintBackground(Graphics2D g, Rectangle clip, boolean vertical, Rectangle rectangle);

  public void paintSelectionAndBorder(Graphics2D g2d,
                                      Rectangle rect,
                                      JBTabsImpl.ShapeInfo selectedShape,
                                      Insets insets,
                                      Color tabColor,
                                      boolean horizontalTabs) {
    paintSelectionAndBorderImpl(g2d, rect, selectedShape, insets, modifyTabColor(tabColor), horizontalTabs);
  }

  protected abstract void paintSelectionAndBorderImpl(Graphics2D g2d,
                                                   Rectangle rect,
                                                   JBTabsImpl.ShapeInfo selectedShape,
                                                   Insets insets,
                                                   Color tabColor,
                                                   boolean horizontalTabs);

  public abstract Color getBackgroundColor();

  @Nullable
  private Color modifyTabColor(@Nullable Color tabColor) {
    if(myModifyTabColor != null) {
      return myModifyTabColor;
    }
    return tabColor;
  }

  public void setModifyTabColor(Color modifyTabColor) {
    myModifyTabColor = modifyTabColor;
  }
}
